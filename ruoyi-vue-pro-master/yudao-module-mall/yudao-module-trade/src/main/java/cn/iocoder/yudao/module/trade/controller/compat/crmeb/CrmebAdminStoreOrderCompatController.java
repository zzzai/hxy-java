package cn.iocoder.yudao.module.trade.controller.compat.crmeb;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.controller.app.compat.crmeb.CrmebCompatResult;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.delivery.DeliveryExpressDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.delivery.DeliveryExpressMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderItemMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderItemAfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderRefundStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleRefundDecisionService;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderQueryService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderUpdateService;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * CRMEB 后台订单接口兼容层（管理端）
 */
@RestController
@RequestMapping("/api/admin/store/order")
@Validated
@Hidden
@Slf4j
public class CrmebAdminStoreOrderCompatController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String STORE_REFUND_EXECUTION_DISABLED_MESSAGE = "门店侧已禁用退款执行，请提交售后工单由总部处理";

    @Resource
    private TradeOrderQueryService tradeOrderQueryService;
    @Resource
    private TradeOrderUpdateService tradeOrderUpdateService;
    @Resource
    private TradeOrderMapper tradeOrderMapper;
    @Resource
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Resource
    private DeliveryExpressMapper deliveryExpressMapper;
    @Resource
    private AfterSaleService afterSaleService;
    @Resource
    private AfterSaleRefundDecisionService afterSaleRefundDecisionService;
    @Resource
    private AfterSaleMapper afterSaleMapper;
    @Value("${yudao.trade.compat.crmeb.store-refund-execute-enabled:false}")
    private boolean storeRefundExecuteEnabled;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CrmebCompatResult<CrmebPageRespVO<CrmebStoreOrderListItemRespVO>> list(
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "dateLimit", required = false) String dateLimit,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false, defaultValue = "2") Integer type,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        PageParam pageParam = buildPageParam(page, limit);
        LambdaQueryWrapperX<TradeOrderDO> queryWrapper = buildOrderQuery(orderNo, dateLimit, status, type, false);
        PageResult<TradeOrderDO> pageResult = tradeOrderMapper.selectPage(pageParam, queryWrapper);
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return CrmebCompatResult.success(CrmebPageRespVO.empty(pageParam.getPageNo(), pageParam.getPageSize()));
        }

        Map<Long, List<TradeOrderItemDO>> orderItemMap = buildOrderItemMap(pageResult.getList());
        List<CrmebStoreOrderListItemRespVO> list = pageResult.getList().stream()
                .map(order -> convertOrderListItem(order, orderItemMap.getOrDefault(order.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return CrmebCompatResult.success(CrmebPageRespVO.of(pageResult.getTotal(), pageParam.getPageNo(), pageParam.getPageSize(), list));
    }

    @GetMapping("/info")
    @PreAuthorize("@ss.hasPermission('trade:order:query')")
    public CrmebCompatResult<CrmebStoreOrderInfoRespVO> info(@RequestParam("orderNo") String orderNo) {
        TradeOrderDO order = findOrder(orderNo);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }
        List<TradeOrderItemDO> items = tradeOrderQueryService.getOrderItemListByOrderId(order.getId());
        return CrmebCompatResult.success(convertOrderInfo(order, items));
    }

    @PostMapping({"/delivery", "/send"})
    @PreAuthorize("@ss.hasPermission('trade:order:update')")
    public CrmebCompatResult<String> delivery(@RequestBody CrmebOrderDeliveryReqVO reqVO) {
        if (reqVO == null) {
            return CrmebCompatResult.failed("请求参数不能为空");
        }
        TradeOrderDO order = resolveOrderForDelivery(reqVO);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }
        try {
            Long logisticsId = resolveLogisticsId(reqVO, order);
            TradeOrderDeliveryReq deliveryReq = new TradeOrderDeliveryReq();
            deliveryReq.setId(order.getId());
            deliveryReq.setLogisticsId(logisticsId);
            deliveryReq.setLogisticsNo(StrUtil.blankToDefault(reqVO.getExpressNumber(), ""));
            tradeOrderUpdateService.deliveryOrder(deliveryReq.toTradeReqVO());
            return CrmebCompatResult.success("success");
        } catch (ServiceException e) {
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-admin-order-delivery][orderNo({}) id({}) 发货失败]", reqVO.getOrderNo(), reqVO.getId(), e);
            return CrmebCompatResult.failed("发货失败");
        }
    }

    @GetMapping("/refund/ticket/list")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:query')")
    public CrmebCompatResult<CrmebPageRespVO<CrmebStoreOrderListItemRespVO>> refundTicketList(
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "dateLimit", required = false) String dateLimit,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false, defaultValue = "2") Integer type,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        PageParam pageParam = buildPageParam(page, limit);
        LambdaQueryWrapperX<TradeOrderDO> queryWrapper = buildOrderQuery(orderNo, dateLimit, status, type, true);
        PageResult<TradeOrderDO> pageResult = tradeOrderMapper.selectPage(pageParam, queryWrapper);
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return CrmebCompatResult.success(CrmebPageRespVO.empty(pageParam.getPageNo(), pageParam.getPageSize()));
        }
        Map<Long, List<TradeOrderItemDO>> orderItemMap = buildOrderItemMap(pageResult.getList());
        List<CrmebStoreOrderListItemRespVO> list = pageResult.getList().stream()
                .map(order -> convertOrderListItem(order, orderItemMap.getOrDefault(order.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return CrmebCompatResult.success(CrmebPageRespVO.of(pageResult.getTotal(), pageParam.getPageNo(), pageParam.getPageSize(), list));
    }

    @PostMapping("/refund/confirm")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CrmebCompatResult<Boolean> refundConfirm(
            @RequestBody(required = false) CrmebRefundConfirmReqVO body,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "afterSaleId", required = false) Long afterSaleId,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "forcePass", required = false) Boolean forcePass,
            @RequestParam(value = "force", required = false) Boolean force) {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        String targetOrderNo = resolveOrderNo(body, orderNo);
        if (!storeRefundExecuteEnabled) {
            log.warn("[crmeb-admin-store-refund-blocked][userId({}) id({}) afterSaleId({}) orderNo({})]",
                    loginUserId, id, afterSaleId, targetOrderNo);
            return CrmebCompatResult.failed(STORE_REFUND_EXECUTION_DISABLED_MESSAGE);
        }
        Long targetAfterSaleId = resolveAfterSaleId(body, id, afterSaleId);
        AfterSaleDO afterSale = resolveAfterSale(targetAfterSaleId, targetOrderNo);
        if (afterSale == null) {
            return CrmebCompatResult.failed("售后工单不存在");
        }
        try {
            boolean forceExecute = isForcePass(
                    body == null ? null : body.getForcePass(),
                    body == null ? null : body.getForce(),
                    forcePass, force);
            if (ObjectUtil.equals(afterSale.getStatus(), AfterSaleStatusEnum.APPLY.getStatus())) {
                afterSaleService.agreeAfterSale(loginUserId, afterSale.getId());
                afterSale = afterSaleService.getAfterSale(afterSale.getId());
            }
            if (ObjectUtil.equals(afterSale.getStatus(), AfterSaleStatusEnum.WAIT_REFUND.getStatus())) {
                afterSaleRefundDecisionService.checkAndAuditForExecution(
                        loginUserId, UserTypeEnum.ADMIN.getValue(), afterSale, forceExecute);
                afterSaleService.refundAfterSale(loginUserId, getClientIP(), afterSale.getId());
                return CrmebCompatResult.success(Boolean.TRUE);
            }
            if (ObjectUtil.equals(afterSale.getStatus(), AfterSaleStatusEnum.COMPLETE.getStatus())) {
                return CrmebCompatResult.success(Boolean.TRUE);
            }
            return CrmebCompatResult.failed("当前售后状态不允许确认退款");
        } catch (ServiceException e) {
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-admin-order-refund-confirm][afterSaleId({}) orderNo({}) 确认退款失败]",
                    afterSale.getId(), afterSale.getOrderNo(), e);
            return CrmebCompatResult.failed("退款确认失败");
        }
    }

    @GetMapping("/refund/confirm")
    @PreAuthorize("@ss.hasPermission('trade:after-sale:refund')")
    public CrmebCompatResult<Boolean> refundConfirmByQuery(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "afterSaleId", required = false) Long afterSaleId,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "forcePass", required = false) Boolean forcePass,
            @RequestParam(value = "force", required = false) Boolean force) {
        return refundConfirm(null, id, afterSaleId, orderNo, forcePass, force);
    }

    private boolean isForcePass(Boolean... forceFlags) {
        if (forceFlags == null) {
            return false;
        }
        for (Boolean forceFlag : forceFlags) {
            if (Boolean.TRUE.equals(forceFlag)) {
                return true;
            }
        }
        return false;
    }

    private LambdaQueryWrapperX<TradeOrderDO> buildOrderQuery(String orderNo, String dateLimit, String status,
                                                               Integer type, boolean refundTicketOnly) {
        LambdaQueryWrapperX<TradeOrderDO> queryWrapper = new LambdaQueryWrapperX<>();
        String normalizedOrderNo = StrUtil.trim(orderNo);
        queryWrapper.eqIfPresent(TradeOrderDO::getNo, StrUtil.isBlank(normalizedOrderNo) ? null : normalizedOrderNo);
        if (type != null && !ObjectUtil.equals(type, 2)) {
            queryWrapper.eq(TradeOrderDO::getType, type);
        }
        LocalDateTime[] createTime = parseDateLimit(dateLimit);
        if (createTime != null) {
            queryWrapper.between(TradeOrderDO::getCreateTime, createTime[0], createTime[1]);
        }
        if (refundTicketOnly) {
            Set<Long> ticketOrderIds = tradeOrderItemMapper.selectOrderIdsByAfterSaleStatuses(
                    Collections.singleton(TradeOrderItemAfterSaleStatusEnum.APPLY.getStatus()));
            if (CollUtil.isEmpty(ticketOrderIds)) {
                queryWrapper.eq(TradeOrderDO::getId, -1L);
                return queryWrapper;
            }
            queryWrapper.in(TradeOrderDO::getId, ticketOrderIds);
        }
        applyStatusFilter(queryWrapper, status);
        queryWrapper.orderByDesc(TradeOrderDO::getId);
        return queryWrapper;
    }

    private void applyStatusFilter(LambdaQueryWrapperX<TradeOrderDO> queryWrapper, String status) {
        if (StrUtil.isBlank(status) || StrUtil.equalsIgnoreCase(status, "all")) {
            return;
        }
        String target = StrUtil.trim(status);
        switch (target) {
            case "unPaid":
                queryWrapper.eq(TradeOrderDO::getPayStatus, Boolean.FALSE)
                        .eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.UNPAID.getStatus());
                break;
            case "notShipped":
                queryWrapper.eq(TradeOrderDO::getPayStatus, Boolean.TRUE)
                        .eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.UNDELIVERED.getStatus())
                        .eq(TradeOrderDO::getRefundStatus, TradeOrderRefundStatusEnum.NONE.getStatus())
                        .eq(TradeOrderDO::getDeliveryType, DeliveryTypeEnum.EXPRESS.getType());
                break;
            case "spike":
                queryWrapper.eq(TradeOrderDO::getPayStatus, Boolean.TRUE)
                        .eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.DELIVERED.getStatus())
                        .eq(TradeOrderDO::getRefundStatus, TradeOrderRefundStatusEnum.NONE.getStatus());
                break;
            case "bargain":
                queryWrapper.eq(TradeOrderDO::getPayStatus, Boolean.TRUE)
                        .eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.COMPLETED.getStatus())
                        .eq(TradeOrderDO::getCommentStatus, Boolean.FALSE)
                        .eq(TradeOrderDO::getRefundStatus, TradeOrderRefundStatusEnum.NONE.getStatus());
                break;
            case "complete":
                queryWrapper.eq(TradeOrderDO::getPayStatus, Boolean.TRUE)
                        .eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.COMPLETED.getStatus())
                        .eq(TradeOrderDO::getRefundStatus, TradeOrderRefundStatusEnum.NONE.getStatus());
                break;
            case "toBeWrittenOff":
                queryWrapper.eq(TradeOrderDO::getPayStatus, Boolean.TRUE)
                        .eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.UNDELIVERED.getStatus())
                        .eq(TradeOrderDO::getRefundStatus, TradeOrderRefundStatusEnum.NONE.getStatus())
                        .eq(TradeOrderDO::getDeliveryType, DeliveryTypeEnum.PICK_UP.getType());
                break;
            case "refunding":
                Set<Long> refundingOrderIds = tradeOrderItemMapper.selectOrderIdsByAfterSaleStatuses(
                        Collections.singleton(TradeOrderItemAfterSaleStatusEnum.APPLY.getStatus()));
                if (CollUtil.isEmpty(refundingOrderIds)) {
                    queryWrapper.eq(TradeOrderDO::getId, -1L);
                } else {
                    queryWrapper.eq(TradeOrderDO::getPayStatus, Boolean.TRUE)
                            .in(TradeOrderDO::getId, refundingOrderIds);
                }
                break;
            case "refunded":
                Set<Long> refundedOrderIds = tradeOrderItemMapper.selectOrderIdsByAfterSaleStatuses(
                        Collections.singleton(TradeOrderItemAfterSaleStatusEnum.SUCCESS.getStatus()));
                if (CollUtil.isEmpty(refundedOrderIds)) {
                    queryWrapper.eq(TradeOrderDO::getPayStatus, Boolean.TRUE)
                            .in(TradeOrderDO::getRefundStatus,
                                    Arrays.asList(TradeOrderRefundStatusEnum.PART.getStatus(), TradeOrderRefundStatusEnum.ALL.getStatus()));
                } else {
                    queryWrapper.eq(TradeOrderDO::getPayStatus, Boolean.TRUE)
                            .in(TradeOrderDO::getId, refundedOrderIds);
                }
                break;
            case "deleted":
                queryWrapper.eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.CANCELED.getStatus());
                break;
            default:
                break;
        }
    }

    private Map<Long, List<TradeOrderItemDO>> buildOrderItemMap(List<TradeOrderDO> orders) {
        Set<Long> orderIds = orders.stream().map(TradeOrderDO::getId).collect(Collectors.toSet());
        return tradeOrderQueryService.getOrderItemListByOrderId(orderIds).stream()
                .collect(Collectors.groupingBy(TradeOrderItemDO::getOrderId, LinkedHashMap::new, Collectors.toList()));
    }

    private TradeOrderDO findOrder(String orderNoOrId) {
        if (StrUtil.isBlank(orderNoOrId)) {
            return null;
        }
        if (NumberUtil.isLong(orderNoOrId)) {
            TradeOrderDO byId = tradeOrderQueryService.getOrder(NumberUtil.parseLong(orderNoOrId));
            if (byId != null) {
                return byId;
            }
        }
        return tradeOrderMapper.selectFirstByNoAndUserId(orderNoOrId, null);
    }

    private TradeOrderDO resolveOrderForDelivery(CrmebOrderDeliveryReqVO reqVO) {
        if (reqVO.getId() != null) {
            TradeOrderDO byId = tradeOrderQueryService.getOrder(reqVO.getId());
            if (byId != null) {
                return byId;
            }
        }
        return findOrder(reqVO.getOrderNo());
    }

    private Long resolveLogisticsId(CrmebOrderDeliveryReqVO reqVO, TradeOrderDO order) {
        if (StrUtil.equalsAnyIgnoreCase(reqVO.getDeliveryType(), "send", "fictitious")) {
            return TradeOrderDO.LOGISTICS_ID_NULL;
        }
        if (reqVO.getLogisticsId() != null) {
            return reqVO.getLogisticsId();
        }
        if (StrUtil.isNotBlank(reqVO.getExpressCode())) {
            DeliveryExpressDO express = deliveryExpressMapper.selectByCode(reqVO.getExpressCode());
            if (express != null) {
                return express.getId();
            }
        }
        if (order.getLogisticsId() != null && order.getLogisticsId() > 0) {
            return order.getLogisticsId();
        }
        throw new ServiceException(500, "快递公司不能为空");
    }

    private AfterSaleDO resolveAfterSale(Long afterSaleId, String orderNo) {
        if (afterSaleId != null) {
            return afterSaleService.getAfterSale(afterSaleId);
        }
        if (StrUtil.isBlank(orderNo)) {
            return null;
        }
        return afterSaleMapper.selectFirstByOrderNoAndStatuses(orderNo, AfterSaleStatusEnum.APPLYING_STATUSES);
    }

    private Long resolveAfterSaleId(CrmebRefundConfirmReqVO body, Long id, Long afterSaleId) {
        if (body != null) {
            if (body.getAfterSaleId() != null) {
                return body.getAfterSaleId();
            }
            if (body.getId() != null) {
                return body.getId();
            }
        }
        if (afterSaleId != null) {
            return afterSaleId;
        }
        return id;
    }

    private String resolveOrderNo(CrmebRefundConfirmReqVO body, String orderNo) {
        if (body != null && StrUtil.isNotBlank(body.getOrderNo())) {
            return body.getOrderNo();
        }
        return orderNo;
    }

    private CrmebStoreOrderListItemRespVO convertOrderListItem(TradeOrderDO order, List<TradeOrderItemDO> items) {
        CrmebStoreOrderListItemRespVO respVO = new CrmebStoreOrderListItemRespVO();
        Integer refundStatus = mapRefundStatus(order, items);
        respVO.setOrderId(order.getNo());
        respVO.setPayPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getPayPrice(), 0)));
        respVO.setPayType(mapPayType(order));
        respVO.setCreateTime(order.getCreateTime());
        respVO.setStatus(mapOrderStatus(order));
        respVO.setProductList(items.stream().map(this::convertOrderInfo).collect(Collectors.toList()));
        respVO.setStatusStr(mapStatus(order, refundStatus));
        respVO.setPayTypeStr(mapPayTypeStr(order));
        respVO.setIsDel(Boolean.FALSE);
        respVO.setRefundPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getRefundPrice(), 0)));
        respVO.setRefundStatus(refundStatus);
        respVO.setVerifyCode(order.getPickUpVerifyCode());
        respVO.setOrderType(mapOrderType(order));
        respVO.setRemark(order.getRemark());
        respVO.setRealName(order.getReceiverName());
        respVO.setProTotalPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getTotalPrice(), 0)));
        respVO.setCouponPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getCouponPrice(), 0)));
        respVO.setBeforePayPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getPayPrice(), 0)));
        respVO.setPaid(Boolean.TRUE.equals(order.getPayStatus()) || TradeOrderStatusEnum.havePaid(order.getStatus()));
        respVO.setType(order.getType());
        respVO.setIsAlterPrice(ObjectUtil.notEqual(ObjectUtil.defaultIfNull(order.getAdjustPrice(), 0), 0));
        return respVO;
    }

    private CrmebStoreOrderInfoRespVO convertOrderInfo(TradeOrderDO order, List<TradeOrderItemDO> items) {
        CrmebStoreOrderInfoRespVO respVO = new CrmebStoreOrderInfoRespVO();
        Integer refundStatus = mapRefundStatus(order, items);
        respVO.setId(order.getId());
        respVO.setOrderId(order.getNo());
        respVO.setUid(order.getUserId());
        respVO.setRealName(order.getReceiverName());
        respVO.setUserPhone(order.getReceiverMobile());
        respVO.setUserAddress(order.getReceiverDetailAddress());
        respVO.setTotalNum(items.stream().mapToInt(item -> ObjectUtil.defaultIfNull(item.getCount(), 0)).sum());
        respVO.setTotalPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getTotalPrice(), 0)));
        respVO.setPayPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getPayPrice(), 0)));
        respVO.setPayPostage(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getDeliveryPrice(), 0)));
        respVO.setCouponPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getCouponPrice(), 0)));
        respVO.setDeductionPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getPointPrice(), 0)));
        respVO.setPayType(mapPayType(order));
        respVO.setCreateTime(order.getCreateTime());
        respVO.setStatus(mapOrderStatus(order));
        respVO.setRefundStatus(refundStatus);
        respVO.setDeliveryName(resolveDeliveryName(order));
        respVO.setDeliveryType(order.getDeliveryType() != null && order.getDeliveryType() == DeliveryTypeEnum.PICK_UP.getType() ? "fictitious" : "express");
        respVO.setDeliveryId(order.getLogisticsNo());
        respVO.setMark(order.getUserRemark());
        respVO.setIsDel(Boolean.FALSE);
        respVO.setRemark(order.getRemark());
        respVO.setRefundPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getRefundPrice(), 0)));
        respVO.setUseIntegral(order.getUsePoint());
        respVO.setVerifyCode(order.getPickUpVerifyCode());
        respVO.setShippingType(order.getDeliveryType());
        respVO.setStatusStr(mapStatus(order, refundStatus));
        respVO.setPayTypeStr(mapPayTypeStr(order));
        respVO.setOrderInfo(items.stream().map(this::convertOrderInfo).collect(Collectors.toList()));
        respVO.setProTotalPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getTotalPrice(), 0)));
        respVO.setOrderTypeText(mapOrderType(order));
        return respVO;
    }

    private CrmebOrderInfoRespVO convertOrderInfo(TradeOrderItemDO item) {
        CrmebOrderInfoRespVO info = new CrmebOrderInfoRespVO();
        info.setAttrId(item.getSkuId() == null ? null : String.valueOf(item.getSkuId()));
        info.setProductId(item.getSpuId());
        info.setCartNum(item.getCount());
        info.setImage(item.getPicUrl());
        info.setStoreName(item.getSpuName());
        info.setPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(item.getPrice(), 0)));
        if (item.getProperties() != null && !item.getProperties().isEmpty()) {
            info.setSku(item.getProperties().stream()
                    .map(p -> StrUtil.blankToDefault(p.getValueName(), p.getPropertyName()))
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.joining(",")));
        }
        return info;
    }

    private String mapPayType(TradeOrderDO order) {
        if (StrUtil.equalsIgnoreCase(order.getPayChannelCode(), "wallet")) {
            return "yue";
        }
        return "weixin";
    }

    private String mapPayTypeStr(TradeOrderDO order) {
        if (StrUtil.equalsIgnoreCase(order.getPayChannelCode(), "wallet")) {
            return "余额支付";
        }
        return "微信支付";
    }

    private Integer mapOrderStatus(TradeOrderDO order) {
        if (!Boolean.TRUE.equals(order.getPayStatus()) || TradeOrderStatusEnum.isUnpaid(order.getStatus())) {
            return 0;
        }
        if (TradeOrderStatusEnum.isUndelivered(order.getStatus())) {
            return 0;
        }
        if (TradeOrderStatusEnum.isDelivered(order.getStatus())) {
            return 1;
        }
        if (TradeOrderStatusEnum.isCompleted(order.getStatus())) {
            return Boolean.TRUE.equals(order.getCommentStatus()) ? 3 : 2;
        }
        if (TradeOrderStatusEnum.isCanceled(order.getStatus())) {
            return 3;
        }
        return 0;
    }

    private Integer mapRefundStatus(TradeOrderDO order, Collection<TradeOrderItemDO> items) {
        boolean hasApplying = CollUtil.isNotEmpty(items) && items.stream()
                .anyMatch(item -> ObjectUtil.equals(item.getAfterSaleStatus(), TradeOrderItemAfterSaleStatusEnum.APPLY.getStatus()));
        boolean hasSuccess = CollUtil.isNotEmpty(items) && items.stream()
                .anyMatch(item -> ObjectUtil.equals(item.getAfterSaleStatus(), TradeOrderItemAfterSaleStatusEnum.SUCCESS.getStatus()));
        if (hasApplying && ObjectUtil.defaultIfNull(order.getRefundStatus(), 0) > 0) {
            return 3;
        }
        if (hasApplying) {
            return 1;
        }
        if (hasSuccess || ObjectUtil.defaultIfNull(order.getRefundStatus(), 0) > 0) {
            return 2;
        }
        return 0;
    }

    private Map<String, String> mapStatus(TradeOrderDO order, Integer refundStatus) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key", "");
        map.put("value", "");
        if (!Boolean.TRUE.equals(order.getPayStatus()) || TradeOrderStatusEnum.isUnpaid(order.getStatus())) {
            map.put("key", "unPaid");
            map.put("value", "未支付");
            return map;
        }
        if (ObjectUtil.equals(refundStatus, 1)) {
            map.put("key", "applyRefunding");
            map.put("value", "申请退款中");
            return map;
        }
        if (ObjectUtil.equals(refundStatus, 3)) {
            map.put("key", "refunding");
            map.put("value", "退款中");
            return map;
        }
        if (ObjectUtil.equals(refundStatus, 2)) {
            map.put("key", "refunded");
            map.put("value", "已退款");
            return map;
        }
        if (TradeOrderStatusEnum.isUndelivered(order.getStatus())) {
            if (order.getDeliveryType() != null && order.getDeliveryType() == DeliveryTypeEnum.PICK_UP.getType()) {
                map.put("key", "toBeWrittenOff");
                map.put("value", "待核销");
            } else {
                map.put("key", "notShipped");
                map.put("value", "未发货");
            }
            return map;
        }
        if (TradeOrderStatusEnum.isDelivered(order.getStatus())) {
            map.put("key", "spike");
            map.put("value", "待收货");
            return map;
        }
        if (TradeOrderStatusEnum.isCompleted(order.getStatus())) {
            if (Boolean.TRUE.equals(order.getCommentStatus())) {
                map.put("key", "complete");
                map.put("value", "交易完成");
            } else {
                map.put("key", "bargain");
                map.put("value", "待评价");
            }
            return map;
        }
        if (TradeOrderStatusEnum.isCanceled(order.getStatus())) {
            map.put("key", "deleted");
            map.put("value", "已删除");
        }
        return map;
    }

    private String mapOrderType(TradeOrderDO order) {
        if (order.getSeckillActivityId() != null && order.getSeckillActivityId() > 0) {
            return "[秒杀订单]";
        }
        if (order.getBargainActivityId() != null && order.getBargainActivityId() > 0) {
            return "[砍价订单]";
        }
        if (order.getCombinationActivityId() != null && order.getCombinationActivityId() > 0) {
            return "[拼团订单]";
        }
        if (order.getType() != null && order.getType() == 1) {
            return "[视频号订单]";
        }
        if (StrUtil.isNotBlank(order.getPickUpVerifyCode())) {
            return "[核销订单]";
        }
        return "[普通订单]";
    }

    private String resolveDeliveryName(TradeOrderDO order) {
        if (order.getLogisticsId() == null || order.getLogisticsId() <= 0) {
            return "";
        }
        DeliveryExpressDO express = deliveryExpressMapper.selectById(order.getLogisticsId());
        return express == null ? "" : express.getName();
    }

    private LocalDateTime[] parseDateLimit(String dateLimit) {
        if (StrUtil.isBlank(dateLimit)) {
            return null;
        }
        String target = StrUtil.trim(dateLimit);
        LocalDate today = LocalDate.now();
        switch (target) {
            case "today":
                return range(today, today);
            case "yesterday":
                return range(today.minusDays(1), today.minusDays(1));
            case "lately7":
                return range(today.minusDays(6), today);
            case "lately30":
                return range(today.minusDays(29), today);
            case "month":
                return range(today.withDayOfMonth(1), today);
            case "year":
                return range(today.withDayOfYear(1), today);
            default:
                return parseCustomDateLimit(target);
        }
    }

    private LocalDateTime[] parseCustomDateLimit(String dateLimit) {
        String normalized = dateLimit.replace("/", "");
        String[] parts = normalized.split(",");
        if (parts.length != 2) {
            return null;
        }
        LocalDateTime start = parseDateTime(parts[0], true);
        LocalDateTime end = parseDateTime(parts[1], false);
        if (start == null || end == null) {
            return null;
        }
        return new LocalDateTime[]{start, end};
    }

    private LocalDateTime parseDateTime(String value, boolean start) {
        String target = StrUtil.trim(value);
        if (StrUtil.isBlank(target)) {
            return null;
        }
        try {
            if (target.length() == 10) {
                LocalDate date = LocalDate.parse(target);
                return start ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
            }
            return LocalDateTime.parse(target, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            log.warn("[parseDateTime][value({}) 解析失败]", value);
            return null;
        }
    }

    private LocalDateTime[] range(LocalDate start, LocalDate end) {
        return new LocalDateTime[]{start.atStartOfDay(), end.atTime(LocalTime.MAX)};
    }

    private PageParam buildPageParam(Integer page, Integer limit) {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(page == null || page <= 0 ? 1 : page);
        pageParam.setPageSize(limit == null || limit <= 0 ? 20 : Math.min(limit, 200));
        return pageParam;
    }

    @Data
    public static class CrmebOrderDeliveryReqVO {
        private Long id;
        private String orderNo;
        private String deliveryType;
        private Long logisticsId;
        private String expressCode;
        private String expressNumber;
    }

    @Data
    public static class CrmebRefundConfirmReqVO {
        private Long id;
        private Long afterSaleId;
        private String orderNo;
        private Boolean forcePass;
        private Boolean force;
    }

    @Data
    public static class CrmebPageRespVO<T> {
        private Integer page;
        private Integer limit;
        private Integer totalPage;
        private Long total;
        private List<T> list;

        public static <T> CrmebPageRespVO<T> empty(Integer page, Integer limit) {
            CrmebPageRespVO<T> respVO = new CrmebPageRespVO<>();
            respVO.setPage(page);
            respVO.setLimit(limit);
            respVO.setTotalPage(0);
            respVO.setTotal(0L);
            respVO.setList(Collections.emptyList());
            return respVO;
        }

        public static <T> CrmebPageRespVO<T> of(Long total, Integer page, Integer limit, List<T> list) {
            CrmebPageRespVO<T> respVO = new CrmebPageRespVO<>();
            respVO.setPage(page);
            respVO.setLimit(limit);
            respVO.setTotal(ObjectUtil.defaultIfNull(total, 0L));
            respVO.setTotalPage(limit == null || limit <= 0 ? 0 : (int) ((respVO.getTotal() + limit - 1) / limit));
            respVO.setList(list);
            return respVO;
        }
    }

    @Data
    public static class CrmebStoreOrderListItemRespVO {
        private String orderId;
        private BigDecimal payPrice;
        private String payType;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;
        private Integer status;
        private List<CrmebOrderInfoRespVO> productList;
        private Map<String, String> statusStr;
        private String payTypeStr;
        private Boolean isDel;
        private BigDecimal refundPrice;
        private Integer refundStatus;
        private String verifyCode;
        private String orderType;
        private String remark;
        private String realName;
        private BigDecimal proTotalPrice;
        private BigDecimal couponPrice;
        private BigDecimal beforePayPrice;
        private Boolean paid;
        private Integer type;
        private Boolean isAlterPrice;
    }

    @Data
    public static class CrmebStoreOrderInfoRespVO {
        private Long id;
        private String orderId;
        private Long uid;
        private String realName;
        private String userPhone;
        private String userAddress;
        private Integer totalNum;
        private BigDecimal totalPrice;
        private BigDecimal payPrice;
        private BigDecimal payPostage;
        private BigDecimal couponPrice;
        private BigDecimal deductionPrice;
        private String payType;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;
        private Integer status;
        private Integer refundStatus;
        private String deliveryName;
        private String deliveryType;
        private String deliveryId;
        private String mark;
        private Boolean isDel;
        private String remark;
        private BigDecimal refundPrice;
        private Integer useIntegral;
        private String verifyCode;
        private Integer shippingType;
        private Map<String, String> statusStr;
        private String payTypeStr;
        private List<CrmebOrderInfoRespVO> orderInfo;
        private BigDecimal proTotalPrice;
        private String orderTypeText;
    }

    @Data
    public static class CrmebOrderInfoRespVO {
        private String attrId;
        private Long productId;
        private Integer cartNum;
        private String image;
        private String storeName;
        private BigDecimal price;
        private String sku;
    }

    @Data
    private static class TradeOrderDeliveryReq {
        private Long id;
        private Long logisticsId;
        private String logisticsNo;

        private cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO toTradeReqVO() {
            cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO reqVO =
                    new cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO();
            reqVO.setId(id);
            reqVO.setLogisticsId(logisticsId);
            reqVO.setLogisticsNo(logisticsNo);
            return reqVO;
        }
    }

}
