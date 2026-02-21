package cn.iocoder.yudao.module.trade.controller.admin.compat.crmeb;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.AfterSalePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderDeliveryReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.TradeOrderPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.delivery.DeliveryExpressDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderItemAfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleRefundDecisionService;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleService;
import cn.iocoder.yudao.module.trade.service.delivery.DeliveryExpressService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderQueryService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderUpdateService;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * CRMEB 管理端订单/售后兼容层（P0/P1）
 */
@RestController
@RequestMapping("/api/admin/store/order")
@Validated
@Hidden
@Slf4j
@ConditionalOnProperty(
        prefix = "yudao.trade.compat.crmeb",
        name = "legacy-admin-order-controller-enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class CrmebAdminOrderCompatController {

    @Resource
    private TradeOrderQueryService tradeOrderQueryService;
    @Resource
    private TradeOrderUpdateService tradeOrderUpdateService;
    @Resource
    private TradeOrderMapper tradeOrderMapper;
    @Resource
    private AfterSaleService afterSaleService;
    @Resource
    private AfterSaleRefundDecisionService afterSaleRefundDecisionService;
    @Resource
    private MemberUserApi memberUserApi;
    @Resource
    private DeliveryExpressService deliveryExpressService;

    @GetMapping("/list")
    public CrmebCompatResult<CrmebPageRespVO<CrmebAdminOrderListItemRespVO>> list(
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "userPhone", required = false) String userPhone,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        int pageNo = page != null && page > 0 ? page : 1;
        int pageSize = limit != null && limit > 0 ? Math.min(limit, 200) : 20;
        TradeOrderPageReqVO reqVO = new TradeOrderPageReqVO();
        reqVO.setPageNo(pageNo);
        reqVO.setPageSize(pageSize);
        reqVO.setNo(StrUtil.blankToDefault(orderNo, keywords));
        reqVO.setStatus(mapAdminOrderStatus(status));
        if (type != null && type != 2) {
            reqVO.setType(type);
        }
        if (StrUtil.isNotBlank(userPhone)) {
            MemberUserRespDTO user = memberUserApi.getUserByMobile(userPhone.trim());
            if (user == null) {
                return CrmebCompatResult.success(CrmebPageRespVO.empty(pageNo, pageSize));
            }
            reqVO.setUserId(user.getId());
        }

        PageResult<TradeOrderDO> pageResult = tradeOrderQueryService.getOrderPage(reqVO);
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return CrmebCompatResult.success(CrmebPageRespVO.empty(pageNo, pageSize));
        }

        Map<Long, MemberUserRespDTO> userMap = buildUserMap(pageResult.getList().stream()
                .map(TradeOrderDO::getUserId).collect(Collectors.toSet()));
        Map<Long, List<TradeOrderItemDO>> orderItemMap = groupOrderItems(pageResult.getList().stream()
                .map(TradeOrderDO::getId).collect(Collectors.toSet()));

        List<CrmebAdminOrderListItemRespVO> list = pageResult.getList().stream()
                .map(order -> convertAdminOrderListItem(order, orderItemMap.getOrDefault(order.getId(), Collections.emptyList()),
                        userMap.get(order.getUserId())))
                .collect(Collectors.toList());
        return CrmebCompatResult.success(CrmebPageRespVO.of(pageResult.getTotal(), pageNo, pageSize, list));
    }

    @GetMapping("/info")
    public CrmebCompatResult<CrmebAdminOrderInfoRespVO> info(@RequestParam("orderNo") String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }
        TradeOrderDO order = findOrder(orderNo);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }
        List<TradeOrderItemDO> items = tradeOrderQueryService.getOrderItemListByOrderId(order.getId());
        MemberUserRespDTO user = memberUserApi.getUser(order.getUserId());
        return CrmebCompatResult.success(convertAdminOrderInfo(order, items, user));
    }

    @PostMapping({"/delivery", "/send"})
    public CrmebCompatResult<Boolean> delivery(@RequestBody CrmebAdminOrderDeliveryReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getOrderNo())) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }
        TradeOrderDO order = findOrder(reqVO.getOrderNo());
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }
        try {
            TradeOrderDeliveryReqVO deliveryReqVO = new TradeOrderDeliveryReqVO();
            deliveryReqVO.setId(order.getId());
            deliveryReqVO.setLogisticsId(resolveLogisticsId(reqVO));
            deliveryReqVO.setLogisticsNo(resolveLogisticsNo(reqVO));
            tradeOrderUpdateService.deliveryOrder(deliveryReqVO);
            return CrmebCompatResult.success(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("[crmeb-admin-order-delivery][orderNo({}) req({}) 发货失败]",
                    reqVO.getOrderNo(), reqVO, e);
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-admin-order-delivery][orderNo({}) req({}) 发货异常]",
                    reqVO.getOrderNo(), reqVO, e);
            return CrmebCompatResult.failed("发货失败");
        }
    }

    @GetMapping("/refund/ticket/list")
    public CrmebCompatResult<CrmebPageRespVO<CrmebRefundTicketItemRespVO>> refundTicketList(
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        int pageNo = page != null && page > 0 ? page : 1;
        int pageSize = limit != null && limit > 0 ? Math.min(limit, 200) : 20;

        AfterSalePageReqVO reqVO = new AfterSalePageReqVO();
        reqVO.setPageNo(pageNo);
        reqVO.setPageSize(pageSize);
        reqVO.setOrderNo(StrUtil.isBlank(orderNo) ? null : orderNo.trim());
        reqVO.setStatus(mapAfterSaleStatus(status));

        PageResult<AfterSaleDO> pageResult = afterSaleService.getAfterSalePage(reqVO);
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return CrmebCompatResult.success(CrmebPageRespVO.empty(pageNo, pageSize));
        }

        Map<Long, MemberUserRespDTO> userMap = buildUserMap(pageResult.getList().stream()
                .map(AfterSaleDO::getUserId).collect(Collectors.toSet()));
        List<CrmebRefundTicketItemRespVO> list = pageResult.getList().stream()
                .map(ticket -> convertRefundTicket(ticket, userMap.get(ticket.getUserId())))
                .collect(Collectors.toList());
        return CrmebCompatResult.success(CrmebPageRespVO.of(pageResult.getTotal(), pageNo, pageSize, list));
    }

    @PostMapping("/refund/confirm")
    public CrmebCompatResult<Boolean> refundConfirm(@RequestBody(required = false) CrmebRefundTicketConfirmReqVO reqVO,
                                                    @RequestParam(value = "forcePass", required = false) Boolean forcePass,
                                                    @RequestParam(value = "force", required = false) Boolean force) {
        CrmebRefundTicketConfirmReqVO request = reqVO == null ? new CrmebRefundTicketConfirmReqVO() : reqVO;
        Long afterSaleId = resolveAfterSaleId(request.getId(), request.getOrderNo());
        if (afterSaleId == null) {
            return CrmebCompatResult.failed("售后单不存在");
        }
        Long adminId = getLoginUserId();
        if (adminId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        try {
            boolean forceExecute = isForcePass(request.getForcePass(), request.getForce(), forcePass, force);
            AfterSaleDO afterSale = afterSaleService.getAfterSale(afterSaleId);
            if (afterSale == null) {
                return CrmebCompatResult.failed("售后单不存在");
            }
            if (ObjectUtil.equals(afterSale.getStatus(), AfterSaleStatusEnum.APPLY.getStatus())) {
                afterSaleService.agreeAfterSale(adminId, afterSale.getId());
                afterSale = afterSaleService.getAfterSale(afterSale.getId());
            }
            if (ObjectUtil.equals(afterSale.getStatus(), AfterSaleStatusEnum.COMPLETE.getStatus())) {
                return CrmebCompatResult.success(Boolean.TRUE);
            }
            if (!ObjectUtil.equals(afterSale.getStatus(), AfterSaleStatusEnum.WAIT_REFUND.getStatus())) {
                return CrmebCompatResult.failed("当前售后状态不允许确认退款");
            }
            afterSaleRefundDecisionService.checkAndAuditForExecution(
                    adminId, UserTypeEnum.ADMIN.getValue(), afterSale, forceExecute);
            afterSaleService.refundAfterSale(adminId, resolveClientIp(), afterSaleId);
            return CrmebCompatResult.success(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("[crmeb-admin-refund-confirm][adminId({}) afterSaleId({}) refund failed]",
                    adminId, afterSaleId, e);
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-admin-refund-confirm][adminId({}) afterSaleId({}) refund exception]",
                    adminId, afterSaleId, e);
            return CrmebCompatResult.failed("退款确认失败");
        }
    }

    @GetMapping("/refund/confirm")
    public CrmebCompatResult<Boolean> refundConfirmByQuery(@RequestParam(value = "id", required = false) Long id,
                                                           @RequestParam(value = "orderNo", required = false) String orderNo,
                                                           @RequestParam(value = "forcePass", required = false) Boolean forcePass,
                                                           @RequestParam(value = "force", required = false) Boolean force) {
        CrmebRefundTicketConfirmReqVO reqVO = new CrmebRefundTicketConfirmReqVO();
        reqVO.setId(id);
        reqVO.setOrderNo(orderNo);
        reqVO.setForcePass(forcePass);
        reqVO.setForce(force);
        return refundConfirm(reqVO, forcePass, force);
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

    private Map<Long, MemberUserRespDTO> buildUserMap(Set<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<MemberUserRespDTO> users = memberUserApi.getUserList(userIds);
        if (CollUtil.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream().filter(Objects::nonNull).collect(Collectors.toMap(
                MemberUserRespDTO::getId, user -> user, (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Long, List<TradeOrderItemDO>> groupOrderItems(Set<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }
        List<TradeOrderItemDO> orderItems = tradeOrderQueryService.getOrderItemListByOrderId(orderIds);
        if (CollUtil.isEmpty(orderItems)) {
            return Collections.emptyMap();
        }
        return orderItems.stream().collect(Collectors.groupingBy(TradeOrderItemDO::getOrderId,
                LinkedHashMap::new, Collectors.toList()));
    }

    private TradeOrderDO findOrder(String orderNoOrId) {
        TradeOrderDO byNo = tradeOrderMapper.selectFirstByNoAndUserId(orderNoOrId, null);
        if (byNo != null) {
            return byNo;
        }
        if (NumberUtil.isLong(orderNoOrId)) {
            return tradeOrderQueryService.getOrder(NumberUtil.parseLong(orderNoOrId));
        }
        return null;
    }

    private CrmebAdminOrderListItemRespVO convertAdminOrderListItem(TradeOrderDO order, List<TradeOrderItemDO> items,
                                                                     MemberUserRespDTO user) {
        CrmebAdminOrderListItemRespVO respVO = new CrmebAdminOrderListItemRespVO();
        Integer refundStatus = mapRefundStatus(order, items);
        String orderStatusText = mapOrderStatusMsg(order, refundStatus);

        respVO.setOrderId(order.getNo());
        respVO.setPayPrice(toYuan(order.getPayPrice()));
        respVO.setPayType(mapPayType(order));
        respVO.setCreateTime(order.getCreateTime());
        respVO.setStatus(mapOrderStatus(order));
        respVO.setProductList(items.stream().map(this::convertOrderItem).collect(Collectors.toList()));
        respVO.setStatusStr(buildStatusMap(orderStatusText));
        respVO.setPayTypeStr(mapPayTypeStr(order));
        respVO.setRemark(order.getRemark());
        respVO.setRefundPrice(toYuan(order.getRefundPrice()));
        respVO.setRefundStatus(refundStatus);
        respVO.setPaid(Boolean.TRUE.equals(order.getPayStatus()) || TradeOrderStatusEnum.havePaid(order.getStatus()));
        respVO.setRealName(ObjectUtil.defaultIfNull(order.getReceiverName(), user != null ? user.getNickname() : ""));
        return respVO;
    }

    private CrmebAdminOrderInfoRespVO convertAdminOrderInfo(TradeOrderDO order, List<TradeOrderItemDO> items,
                                                             MemberUserRespDTO user) {
        CrmebAdminOrderInfoRespVO respVO = new CrmebAdminOrderInfoRespVO();
        Integer refundStatus = mapRefundStatus(order, items);
        String orderStatusText = mapOrderStatusMsg(order, refundStatus);

        respVO.setId(order.getId());
        respVO.setOrderId(order.getNo());
        respVO.setUid(order.getUserId());
        respVO.setRealName(ObjectUtil.defaultIfNull(order.getReceiverName(), user != null ? user.getNickname() : ""));
        respVO.setUserPhone(order.getReceiverMobile());
        respVO.setUserAddress(order.getReceiverDetailAddress());
        respVO.setTotalNum(items.stream().mapToInt(item -> ObjectUtil.defaultIfNull(item.getCount(), 0)).sum());
        respVO.setTotalPrice(toYuan(order.getTotalPrice()));
        respVO.setPayPrice(toYuan(order.getPayPrice()));
        respVO.setPayPostage(toYuan(order.getDeliveryPrice()));
        respVO.setCouponPrice(toYuan(order.getCouponPrice()));
        respVO.setDeductionPrice(toYuan(order.getPointPrice()));
        respVO.setPayType(mapPayType(order));
        respVO.setCreateTime(order.getCreateTime());
        respVO.setStatus(mapOrderStatus(order));
        respVO.setRefundStatus(refundStatus);
        respVO.setDeliveryName(order.getDeliveryType() != null && order.getDeliveryType() == 2 ? "门店自提" : "快递");
        respVO.setDeliveryType(order.getDeliveryType() != null && order.getDeliveryType() == 2 ? "fictitious" : "express");
        respVO.setDeliveryId(order.getLogisticsNo());
        respVO.setMark(order.getUserRemark());
        respVO.setRemark(order.getRemark());
        respVO.setRefundPrice(toYuan(order.getRefundPrice()));
        respVO.setUseIntegral(order.getUsePoint());
        respVO.setVerifyCode(order.getPickUpVerifyCode());
        respVO.setShippingType(order.getDeliveryType());
        respVO.setStatusStr(buildStatusMap(orderStatusText));
        respVO.setPayTypeStr(mapPayTypeStr(order));
        respVO.setNikeName(user != null ? user.getNickname() : null);
        respVO.setPhone(user != null ? user.getMobile() : null);
        respVO.setOrderInfo(items.stream().map(this::convertOrderItem).collect(Collectors.toList()));
        return respVO;
    }

    private CrmebRefundTicketItemRespVO convertRefundTicket(AfterSaleDO ticket, MemberUserRespDTO user) {
        CrmebRefundTicketItemRespVO respVO = new CrmebRefundTicketItemRespVO();
        respVO.setId(ticket.getId());
        respVO.setNo(ticket.getNo());
        respVO.setOrderNo(ticket.getOrderNo());
        respVO.setStatus(ticket.getStatus());
        respVO.setStatusText(mapAfterSaleStatusName(ticket.getStatus()));
        respVO.setUserId(ticket.getUserId());
        respVO.setUserName(user != null ? user.getNickname() : null);
        respVO.setUserPhone(user != null ? user.getMobile() : null);
        respVO.setSpuName(ticket.getSpuName());
        respVO.setRefundPrice(toYuan(ticket.getRefundPrice()));
        respVO.setCreateTime(ticket.getCreateTime());
        return respVO;
    }

    private CrmebAdminOrderProductRespVO convertOrderItem(TradeOrderItemDO item) {
        CrmebAdminOrderProductRespVO respVO = new CrmebAdminOrderProductRespVO();
        respVO.setAttrId(item.getSkuId() == null ? null : String.valueOf(item.getSkuId()));
        respVO.setProductId(item.getSpuId());
        respVO.setImage(item.getPicUrl());
        respVO.setStoreName(item.getSpuName());
        respVO.setPrice(toYuan(item.getPrice()));
        respVO.setCartNum(item.getCount());
        if (CollUtil.isNotEmpty(item.getProperties())) {
            respVO.setSku(item.getProperties().stream()
                    .map(property -> StrUtil.blankToDefault(property.getValueName(), property.getPropertyName()))
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.joining(",")));
        }
        return respVO;
    }

    private Integer mapAdminOrderStatus(String status) {
        if (StrUtil.isBlank(status) || StrUtil.equalsIgnoreCase(status, "all")) {
            return null;
        }
        if (NumberUtil.isInteger(status)) {
            return NumberUtil.parseInt(status);
        }
        switch (status) {
            case "unPaid":
                return TradeOrderStatusEnum.UNPAID.getStatus();
            case "notShipped":
            case "toBeWrittenOff":
                return TradeOrderStatusEnum.UNDELIVERED.getStatus();
            case "spike":
                return TradeOrderStatusEnum.DELIVERED.getStatus();
            case "bargain":
            case "complete":
                return TradeOrderStatusEnum.COMPLETED.getStatus();
            default:
                return null;
        }
    }

    private Integer mapAfterSaleStatus(String status) {
        if (StrUtil.isBlank(status) || StrUtil.equalsIgnoreCase(status, "all")) {
            return null;
        }
        if (NumberUtil.isInteger(status)) {
            return NumberUtil.parseInt(status);
        }
        if (StrUtil.equalsIgnoreCase(status, "refunded")) {
            return AfterSaleStatusEnum.COMPLETE.getStatus();
        }
        if (StrUtil.equalsIgnoreCase(status, "refunding")) {
            return AfterSaleStatusEnum.APPLY.getStatus();
        }
        return null;
    }

    private String mapAfterSaleStatusName(Integer status) {
        AfterSaleStatusEnum value = AfterSaleStatusEnum.valueOf(status);
        return value != null ? value.getName() : "未知状态";
    }

    private Long resolveLogisticsId(CrmebAdminOrderDeliveryReqVO reqVO) {
        if (reqVO.getLogisticsId() != null) {
            return reqVO.getLogisticsId();
        }
        if (StrUtil.equalsAnyIgnoreCase(reqVO.getDeliveryType(), "send", "fictitious")) {
            return TradeOrderDO.LOGISTICS_ID_NULL;
        }
        List<DeliveryExpressDO> expressList = deliveryExpressService
                .getDeliveryExpressListByStatus(CommonStatusEnum.ENABLE.getStatus());
        if (CollUtil.isEmpty(expressList)) {
            throw new ServiceException(500, "未配置可用快递公司");
        }
        DeliveryExpressDO matched = expressList.stream().filter(express ->
                        (StrUtil.isNotBlank(reqVO.getExpressCode()) && StrUtil.equalsIgnoreCase(reqVO.getExpressCode(), express.getCode()))
                                || (StrUtil.isNotBlank(reqVO.getExpressName()) && StrUtil.equals(reqVO.getExpressName(), express.getName())))
                .findFirst().orElse(null);
        if (matched == null) {
            throw new ServiceException(500, "请传 logisticsId 或有效快递公司编码");
        }
        return matched.getId();
    }

    private String resolveLogisticsNo(CrmebAdminOrderDeliveryReqVO reqVO) {
        if (StrUtil.equalsAnyIgnoreCase(reqVO.getDeliveryType(), "send", "fictitious")) {
            return "";
        }
        return StrUtil.blankToDefault(reqVO.getExpressNumber(), "");
    }

    private Long resolveAfterSaleId(Long id, String orderNo) {
        if (id != null) {
            return id;
        }
        if (StrUtil.isBlank(orderNo)) {
            return null;
        }
        AfterSalePageReqVO pageReqVO = new AfterSalePageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(1);
        pageReqVO.setOrderNo(orderNo.trim());
        PageResult<AfterSaleDO> pageResult = afterSaleService.getAfterSalePage(pageReqVO);
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return null;
        }
        return pageResult.getList().get(0).getId();
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
            return -1;
        }
        return 0;
    }

    private String mapOrderStatusMsg(TradeOrderDO order, Integer refundStatus) {
        if (!Boolean.TRUE.equals(order.getPayStatus()) || TradeOrderStatusEnum.isUnpaid(order.getStatus())) {
            return "待支付";
        }
        if (ObjectUtil.equals(refundStatus, 1)) {
            return "申请退款中";
        }
        if (ObjectUtil.equals(refundStatus, 2)) {
            return "已退款";
        }
        if (TradeOrderStatusEnum.isUndelivered(order.getStatus())) {
            return order.getDeliveryType() != null && order.getDeliveryType() == 2 ? "待核销" : "待发货";
        }
        if (TradeOrderStatusEnum.isDelivered(order.getStatus())) {
            return "待收货";
        }
        if (TradeOrderStatusEnum.isCompleted(order.getStatus())) {
            return Boolean.TRUE.equals(order.getCommentStatus()) ? "已完成" : "待评价";
        }
        if (TradeOrderStatusEnum.isCanceled(order.getStatus())) {
            return "已取消";
        }
        return "处理中";
    }

    private Integer mapRefundStatus(TradeOrderDO order, Collection<TradeOrderItemDO> items) {
        boolean hasApplying = CollUtil.isNotEmpty(items) && items.stream()
                .anyMatch(item -> ObjectUtil.equals(item.getAfterSaleStatus(), TradeOrderItemAfterSaleStatusEnum.APPLY.getStatus()));
        boolean hasSuccess = CollUtil.isNotEmpty(items) && items.stream()
                .anyMatch(item -> ObjectUtil.equals(item.getAfterSaleStatus(), TradeOrderItemAfterSaleStatusEnum.SUCCESS.getStatus()));
        if (hasApplying) {
            return 1;
        }
        if (hasSuccess || ObjectUtil.defaultIfNull(order.getRefundStatus(), 0) > 0) {
            return 2;
        }
        return 0;
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

    private Map<String, String> buildStatusMap(String value) {
        Map<String, String> statusMap = new LinkedHashMap<>();
        statusMap.put("value", value);
        statusMap.put("title", value);
        return statusMap;
    }

    private BigDecimal toYuan(Integer fen) {
        return MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(fen, 0))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveClientIp() {
        try {
            String ip = ServletUtils.getClientIP();
            return StrUtil.blankToDefault(ip, "127.0.0.1");
        } catch (Exception ex) {
            return "127.0.0.1";
        }
    }

    @Data
    public static class CrmebAdminOrderDeliveryReqVO {
        private String orderNo;
        /**
         * express 发货，send 送货，fictitious 虚拟
         */
        private String deliveryType;
        private String expressName;
        private String expressCode;
        private String expressNumber;
        private Long logisticsId;
    }

    @Data
    public static class CrmebRefundTicketConfirmReqVO {
        private Long id;
        @JsonProperty("orderNo")
        private String orderNo;
        @JsonProperty("forcePass")
        private Boolean forcePass;
        @JsonProperty("force")
        private Boolean force;
    }

    @Data
    public static class CrmebPageRespVO<T> {
        private Integer page;
        private Integer limit;
        private Integer totalPage;
        private Long total;
        private List<T> list = Collections.emptyList();

        public static <T> CrmebPageRespVO<T> empty(Integer page, Integer limit) {
            return of(0L, page, limit, Collections.emptyList());
        }

        public static <T> CrmebPageRespVO<T> of(Long total, Integer page, Integer limit, List<T> list) {
            CrmebPageRespVO<T> respVO = new CrmebPageRespVO<>();
            int totalCount = ObjectUtil.defaultIfNull(total, 0L).intValue();
            int pageSize = limit == null || limit <= 0 ? 20 : limit;
            respVO.setPage(page == null || page <= 0 ? 1 : page);
            respVO.setLimit(pageSize);
            respVO.setTotal(ObjectUtil.defaultIfNull(total, 0L));
            respVO.setTotalPage(pageSize <= 0 ? 0 : (int) Math.ceil((double) totalCount / pageSize));
            respVO.setList(list == null ? Collections.emptyList() : list);
            return respVO;
        }
    }

    @Data
    public static class CrmebAdminOrderProductRespVO {
        private Long productId;
        private String attrId;
        private String storeName;
        private String image;
        private Integer cartNum;
        private BigDecimal price;
        private String sku;
    }

    @Data
    public static class CrmebAdminOrderListItemRespVO {
        private String orderId;
        private BigDecimal payPrice;
        private String payType;
        private LocalDateTime createTime;
        private Integer status;
        private List<CrmebAdminOrderProductRespVO> productList;
        private Map<String, String> statusStr;
        private String payTypeStr;
        private String realName;
        private Boolean paid;
        private Integer refundStatus;
        private BigDecimal refundPrice;
        private String remark;
    }

    @Data
    public static class CrmebAdminOrderInfoRespVO {
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
        private LocalDateTime createTime;
        private Integer status;
        private Integer refundStatus;
        private String deliveryName;
        private String deliveryType;
        private String deliveryId;
        private String mark;
        private String remark;
        private BigDecimal refundPrice;
        private Integer useIntegral;
        private String verifyCode;
        private Integer shippingType;
        private Map<String, String> statusStr;
        private String payTypeStr;
        private String nikeName;
        private String phone;
        private List<CrmebAdminOrderProductRespVO> orderInfo;
    }

    @Data
    public static class CrmebRefundTicketItemRespVO {
        private Long id;
        private String no;
        private String orderNo;
        private Integer status;
        private String statusText;
        private Long userId;
        private String userName;
        private String userPhone;
        private String spuName;
        private BigDecimal refundPrice;
        private LocalDateTime createTime;
    }
}
