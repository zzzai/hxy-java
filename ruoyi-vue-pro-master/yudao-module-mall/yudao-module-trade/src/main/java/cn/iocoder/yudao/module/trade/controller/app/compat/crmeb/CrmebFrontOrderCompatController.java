package cn.iocoder.yudao.module.trade.controller.app.compat.crmeb;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.pay.api.channel.PayChannelApi;
import cn.iocoder.yudao.module.pay.api.wallet.PayWalletApi;
import cn.iocoder.yudao.module.pay.api.wallet.dto.PayWalletRespDTO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.app.base.property.AppProductPropertyValueDetailRespVO;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderSettlementReqVO;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeOrderSettlementRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.config.TradeConfigDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderItemMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeOrderMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleWayEnum;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderItemAfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderRefundStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleService;
import cn.iocoder.yudao.module.trade.service.config.TradeConfigService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderQueryService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderUpdateService;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/**
 * CRMEB 前台订单接口兼容层（P0 第一批）
 */
@RestController
@RequestMapping("/api/front/order")
@Validated
@Hidden
@Slf4j
public class CrmebFrontOrderCompatController {

    private static final List<String> DEFAULT_REFUND_REASONS = Arrays.asList(
            "不想买了", "信息填写错误", "商品与描述不符", "商品质量问题", "其他");
    private static final String PRE_ORDER_TYPE_SHOPPING_CART = "shoppingCart";
    private static final String PRE_ORDER_TYPE_BUY_NOW = "buyNow";
    private static final String PRE_ORDER_TYPE_AGAIN = "again";
    private static final String PRE_ORDER_TYPE_VIDEO = "video";

    @Resource
    private AfterSaleService afterSaleService;
    @Resource
    private TradeOrderQueryService tradeOrderQueryService;
    @Resource
    private TradeOrderMapper tradeOrderMapper;
    @Resource
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Resource
    private TradeOrderUpdateService tradeOrderUpdateService;
    @Resource
    private CrmebPreOrderStore crmebPreOrderStore;
    @Resource
    private PayChannelApi payChannelApi;
    @Resource
    private PayWalletApi payWalletApi;
    @Resource
    private TradeConfigService tradeConfigService;

    @GetMapping("/detail/{orderId}")
    public CrmebCompatResult<CrmebStoreOrderDetailRespVO> detail(@PathVariable("orderId") String orderId) {
        if (StrUtil.isBlank(orderId)) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        TradeOrderDO order = findOrder(userId, orderId);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }

        List<TradeOrderItemDO> items = tradeOrderQueryService.getOrderItemListByOrderId(order.getId());
        return CrmebCompatResult.success(convertOrderDetail(order, items));
    }

    @GetMapping("/list")
    public CrmebCompatResult<CrmebPageRespVO<CrmebOrderListItemRespVO>> list(
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam("type") Integer type,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "limit", required = false) Integer limit) {
        if (type == null) {
            return CrmebCompatResult.failed("订单类型不能为空");
        }
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        PageParam pageParam = new PageParam();
        pageParam.setPageNo(page == null || page <= 0 ? 1 : page);
        pageParam.setPageSize(limit == null || limit <= 0 ? 20 : Math.min(limit, 200));

        Set<Long> afterSaleOrderIds = resolveAfterSaleOrderIds(userId, type);
        if (isAfterSaleType(type) && CollUtil.isEmpty(afterSaleOrderIds)) {
            return CrmebCompatResult.success(CrmebPageRespVO.empty(pageParam.getPageNo(), pageParam.getPageSize()));
        }

        LambdaQueryWrapperX<TradeOrderDO> queryWrapper = buildListQueryWrapper(userId, keywords, type, afterSaleOrderIds);
        PageResult<TradeOrderDO> pageResult = tradeOrderMapper.selectPage(pageParam, queryWrapper);
        if (pageResult == null || CollUtil.isEmpty(pageResult.getList())) {
            return CrmebCompatResult.success(CrmebPageRespVO.empty(pageParam.getPageNo(), pageParam.getPageSize()));
        }

        Map<Long, List<TradeOrderItemDO>> orderItemMap = tradeOrderQueryService.getOrderItemListByOrderId(
                        pageResult.getList().stream().map(TradeOrderDO::getId).collect(Collectors.toSet()))
                .stream().collect(Collectors.groupingBy(TradeOrderItemDO::getOrderId, LinkedHashMap::new, Collectors.toList()));

        List<CrmebOrderListItemRespVO> list = pageResult.getList().stream()
                .map(order -> convertOrderListItem(order, orderItemMap.getOrDefault(order.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return CrmebCompatResult.success(CrmebPageRespVO.of(pageResult.getTotal(), pageParam.getPageNo(), pageParam.getPageSize(), list));
    }

    @GetMapping("/data")
    public CrmebCompatResult<CrmebOrderDataRespVO> data() {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        CrmebOrderDataRespVO respVO = new CrmebOrderDataRespVO();
        respVO.setOrderCount(toInt(tradeOrderQueryService.getOrderCount(userId, null, null)));
        respVO.setUnPaidCount(toInt(tradeOrderQueryService.getOrderCount(userId, TradeOrderStatusEnum.UNPAID.getStatus(), null)));
        respVO.setUnShippedCount(toInt(tradeOrderQueryService.getOrderCount(userId, TradeOrderStatusEnum.UNDELIVERED.getStatus(), null)));
        respVO.setReceivedCount(toInt(tradeOrderQueryService.getOrderCount(userId, TradeOrderStatusEnum.DELIVERED.getStatus(), null)));
        respVO.setEvaluatedCount(toInt(tradeOrderQueryService.getOrderCount(userId, TradeOrderStatusEnum.COMPLETED.getStatus(), false)));
        respVO.setCompleteCount(toInt(tradeOrderQueryService.getOrderCount(userId, TradeOrderStatusEnum.COMPLETED.getStatus(), null)));
        respVO.setRefundCount(resolveAfterSaleOrderIds(userId, -1).size());
        respVO.setSumPrice(new BigDecimal(ObjectUtil.defaultIfNull(tradeOrderMapper.selectSumPayPriceByUserId(userId), 0L))
                .movePointLeft(2).setScale(2, RoundingMode.HALF_UP));
        return CrmebCompatResult.success(respVO);
    }

    @PostMapping("/pre/order")
    public CrmebCompatResult<Map<String, Object>> preOrder(@RequestBody CrmebPreOrderRequest reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getPreOrderType())) {
            return CrmebCompatResult.failed("预下单类型不能为空");
        }
        if (CollUtil.isEmpty(reqVO.getOrderDetails())) {
            return CrmebCompatResult.failed("预下单订单详情列表不能为空");
        }
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        try {
            CrmebPreOrderStore.CrmebPreOrderContext context = buildPreOrderContext(userId, reqVO);
            AppTradeOrderSettlementRespVO settlementRespVO = tradeOrderUpdateService
                    .settlementOrder(userId, buildSettlementReqVO(context));
            if (settlementRespVO != null && settlementRespVO.getAddress() != null && settlementRespVO.getAddress().getId() != null) {
                context.setAddressId(settlementRespVO.getAddress().getId());
            }
            crmebPreOrderStore.save(userId, context);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("preOrderNo", context.getPreOrderNo());
            return CrmebCompatResult.success(data);
        } catch (ServiceException e) {
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-front-order-pre-order][userId({}) preOrderType({}) 预下单失败]",
                    userId, reqVO.getPreOrderType(), e);
            return CrmebCompatResult.failed("预下单失败");
        }
    }

    @GetMapping("/load/pre/{preOrderNo}")
    public CrmebCompatResult<CrmebPreOrderLoadRespVO> loadPreOrder(@PathVariable("preOrderNo") String preOrderNo) {
        if (StrUtil.isBlank(preOrderNo)) {
            return CrmebCompatResult.failed("预下单订单号不能为空");
        }
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        CrmebPreOrderStore.CrmebPreOrderContext context = crmebPreOrderStore.get(userId, preOrderNo);
        if (context == null) {
            return CrmebCompatResult.failed("预下单订单不存在");
        }

        try {
            AppTradeOrderSettlementRespVO settlementRespVO = tradeOrderUpdateService
                    .settlementOrder(userId, buildSettlementReqVO(context));
            if (settlementRespVO != null && settlementRespVO.getAddress() != null && settlementRespVO.getAddress().getId() != null) {
                context.setAddressId(settlementRespVO.getAddress().getId());
                crmebPreOrderStore.save(userId, context);
            }
            return CrmebCompatResult.success(buildLoadPreOrderResp(userId, context, settlementRespVO));
        } catch (ServiceException e) {
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-front-order-load-pre][userId({}) preOrderNo({}) 加载失败]", userId, preOrderNo, e);
            return CrmebCompatResult.failed("加载预下单失败");
        }
    }

    @PostMapping("/computed/price")
    public CrmebCompatResult<CrmebComputedOrderPriceRespVO> computedPrice(@RequestBody CrmebOrderComputedPriceRequest reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getPreOrderNo())) {
            return CrmebCompatResult.failed("预下单订单号不能为空");
        }
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        CrmebPreOrderStore.CrmebPreOrderContext context = crmebPreOrderStore.get(userId, reqVO.getPreOrderNo());
        if (context == null) {
            return CrmebCompatResult.failed("预下单订单不存在");
        }

        mergeContextByComputedPriceRequest(context, reqVO);
        crmebPreOrderStore.save(userId, context);
        try {
            AppTradeOrderSettlementRespVO settlementRespVO = tradeOrderUpdateService
                    .settlementOrder(userId, buildSettlementReqVO(context));
            return CrmebCompatResult.success(buildComputedOrderPriceResp(settlementRespVO, context.getUseIntegral()));
        } catch (ServiceException e) {
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-front-order-computed-price][userId({}) preOrderNo({}) 计算失败]",
                    userId, reqVO.getPreOrderNo(), e);
            return CrmebCompatResult.failed("订单金额计算失败");
        }
    }

    @PostMapping("/create")
    public CrmebCompatResult<Map<String, Object>> createOrder(@RequestBody CrmebCreateOrderRequest reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getPreOrderNo())) {
            return CrmebCompatResult.failed("预下单订单号不能为空");
        }
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        CrmebPreOrderStore.CrmebPreOrderContext context = crmebPreOrderStore.get(userId, reqVO.getPreOrderNo());
        if (context == null) {
            return CrmebCompatResult.failed("预下单订单不存在");
        }

        mergeContextByCreateOrderRequest(context, reqVO);
        AppTradeOrderCreateReqVO createReqVO = buildCreateOrderReqVO(context);
        try {
            TradeOrderDO order = tradeOrderUpdateService.createOrder(userId, createReqVO);
            crmebPreOrderStore.remove(userId, context.getPreOrderNo());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderNo", order.getNo());
            return CrmebCompatResult.success(data);
        } catch (ServiceException e) {
            return CrmebCompatResult.failed(e.getMessage());
        } catch (Exception e) {
            log.warn("[crmeb-front-order-create][userId({}) preOrderNo({}) 下单失败]",
                    userId, reqVO.getPreOrderNo(), e);
            return CrmebCompatResult.failed("下单失败");
        }
    }

    @GetMapping("/get/pay/config")
    public CrmebCompatResult<CrmebPayConfigRespVO> getPayConfig() {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        return CrmebCompatResult.success(buildPayConfig(userId, null));
    }

    @GetMapping("/refund/reason")
    public CrmebCompatResult<List<String>> refundReason() {
        return CrmebCompatResult.success(DEFAULT_REFUND_REASONS);
    }

    @GetMapping("/apply/refund/{orderId}")
    public CrmebCompatResult<CrmebApplyRefundOrderInfoRespVO> applyRefundOrder(@PathVariable("orderId") String orderId) {
        if (StrUtil.isBlank(orderId)) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        TradeOrderDO order = findOrder(userId, orderId);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }

        List<TradeOrderItemDO> items = tradeOrderQueryService.getOrderItemListByOrderId(order.getId());
        CrmebApplyRefundOrderInfoRespVO respVO = new CrmebApplyRefundOrderInfoRespVO();
        respVO.setId(order.getId());
        respVO.setOrderId(order.getNo());
        respVO.setPaid(Boolean.TRUE.equals(order.getPayStatus()) || TradeOrderStatusEnum.havePaid(order.getStatus()));
        respVO.setPayPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getPayPrice(), 0)));
        respVO.setTotalNum(items.stream().mapToInt(item -> ObjectUtil.defaultIfNull(item.getCount(), 0)).sum());
        respVO.setOrderInfoList(items.stream().map(this::convertOrderInfo).collect(Collectors.toList()));
        return CrmebCompatResult.success(respVO);
    }

    @PostMapping("/refund")
    public CrmebCompatResult<Boolean> refundApply(@RequestBody CrmebOrderRefundApplyRequest reqVO) {
        if (reqVO == null) {
            return CrmebCompatResult.failed("请求参数不能为空");
        }
        if (StrUtil.isBlank(reqVO.getText())) {
            return CrmebCompatResult.failed("退款原因必须填写");
        }
        if (reqVO.getId() == null && StrUtil.isBlank(reqVO.getUni())) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }

        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        TradeOrderDO order = findOrder(userId, reqVO);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }

        TradeOrderItemDO orderItem;
        try {
            orderItem = findOrderItem(userId, order, reqVO.getOrderItemId());
        } catch (ServiceException e) {
            return CrmebCompatResult.failed(e.getMessage());
        }
        if (orderItem == null) {
            return CrmebCompatResult.failed("订单商品不存在");
        }
        if (!TradeOrderItemAfterSaleStatusEnum.isNone(orderItem.getAfterSaleStatus())) {
            return CrmebCompatResult.failed("该商品已申请售后");
        }

        Integer refundPrice = buildRefundPrice(reqVO.getRefundPrice(), orderItem.getPayPrice());
        if (refundPrice == null || refundPrice <= 0) {
            return CrmebCompatResult.failed("退款金额异常");
        }
        if (refundPrice > orderItem.getPayPrice()) {
            return CrmebCompatResult.failed("退款金额不能超过商品实付金额");
        }

        Integer way = buildAfterSaleWay(reqVO.getWay());
        AppAfterSaleCreateReqVO createReqVO = new AppAfterSaleCreateReqVO();
        createReqVO.setOrderItemId(orderItem.getId());
        createReqVO.setWay(way);
        createReqVO.setRefundPrice(refundPrice);
        createReqVO.setApplyReason(reqVO.getText());
        createReqVO.setApplyDescription(reqVO.getExplain());
        createReqVO.setApplyPicUrls(parseReasonImages(reqVO.getReasonImage()));

        try {
            afterSaleService.createAfterSale(userId, createReqVO);
            return CrmebCompatResult.success(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("[crmeb-front-order-refund][orderId({}) orderNo({}) userId({}) 提交失败]",
                    order.getId(), order.getNo(), userId, e);
            return CrmebCompatResult.failed(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public CrmebCompatResult<Boolean> cancel(@RequestBody(required = false) CrmebOrderCancelRequest reqVO,
                                             @RequestParam(value = "id", required = false) String id) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }

        String orderIdentifier = StrUtil.blankToDefault(id, reqVO != null ? reqVO.getId() : null);
        if (StrUtil.isBlank(orderIdentifier)) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }
        TradeOrderDO order = findOrder(userId, orderIdentifier);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }

        try {
            tradeOrderUpdateService.cancelOrderByMember(userId, order.getId());
            return CrmebCompatResult.success(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("[crmeb-front-order-cancel][orderId({}) orderNo({}) userId({}) 取消失败]",
                    order.getId(), order.getNo(), userId, e);
            return CrmebCompatResult.failed(e.getMessage());
        }
    }

    @PostMapping("/take")
    public CrmebCompatResult<Boolean> take(@RequestBody(required = false) CrmebOrderCancelRequest reqVO,
                                           @RequestParam(value = "id", required = false) String id) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        String orderIdentifier = StrUtil.blankToDefault(id, reqVO != null ? reqVO.getId() : null);
        if (StrUtil.isBlank(orderIdentifier)) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }
        TradeOrderDO order = findOrder(userId, orderIdentifier);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }
        try {
            tradeOrderUpdateService.receiveOrderByMember(userId, order.getId());
            return CrmebCompatResult.success(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("[crmeb-front-order-take][orderId({}) orderNo({}) userId({}) 收货失败]",
                    order.getId(), order.getNo(), userId, e);
            return CrmebCompatResult.failed(e.getMessage());
        }
    }

    @PostMapping("/del")
    public CrmebCompatResult<Boolean> delete(@RequestBody(required = false) CrmebOrderCancelRequest reqVO,
                                             @RequestParam(value = "id", required = false) String id) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return CrmebCompatResult.failed("请先登录");
        }
        String orderIdentifier = StrUtil.blankToDefault(id, reqVO != null ? reqVO.getId() : null);
        if (StrUtil.isBlank(orderIdentifier)) {
            return CrmebCompatResult.failed("订单编号不能为空");
        }
        TradeOrderDO order = findOrder(userId, orderIdentifier);
        if (order == null) {
            return CrmebCompatResult.failed("订单不存在");
        }
        try {
            tradeOrderUpdateService.deleteOrder(userId, order.getId());
            return CrmebCompatResult.success(Boolean.TRUE);
        } catch (ServiceException e) {
            log.warn("[crmeb-front-order-del][orderId({}) orderNo({}) userId({}) 删除失败]",
                    order.getId(), order.getNo(), userId, e);
            return CrmebCompatResult.failed(e.getMessage());
        }
    }

    private CrmebPreOrderStore.CrmebPreOrderContext buildPreOrderContext(Long userId, CrmebPreOrderRequest reqVO) {
        CrmebPreOrderStore.CrmebPreOrderContext context = new CrmebPreOrderStore.CrmebPreOrderContext();
        context.setPreOrderNo("pre" + System.currentTimeMillis() + RandomUtil.randomNumbers(6));
        context.setPreOrderType(reqVO.getPreOrderType());
        context.setShippingType(DeliveryTypeEnum.EXPRESS.getType());
        context.setUseIntegral(Boolean.FALSE);
        context.setItems(new ArrayList<>());

        for (CrmebPreOrderDetailRequest detail : reqVO.getOrderDetails()) {
            if (detail == null) {
                continue;
            }
            if (detail.getShoppingCartId() != null && detail.getShoppingCartId() > 0) {
                CrmebPreOrderStore.CrmebPreOrderItem item = new CrmebPreOrderStore.CrmebPreOrderItem();
                item.setCartId(detail.getShoppingCartId());
                context.getItems().add(item);
            } else if (StrUtil.equalsIgnoreCase(reqVO.getPreOrderType(), PRE_ORDER_TYPE_AGAIN)
                    && StrUtil.isNotBlank(detail.getOrderNo())) {
                fillPreOrderItemsByAgainOrder(userId, detail.getOrderNo(), context);
            } else {
                if (detail.getAttrValueId() == null || detail.getAttrValueId() <= 0) {
                    throw new ServiceException(500, "商品规格不能为空");
                }
                CrmebPreOrderStore.CrmebPreOrderItem item = new CrmebPreOrderStore.CrmebPreOrderItem();
                item.setSkuId(detail.getAttrValueId().longValue());
                item.setCount(detail.getProductNum() == null || detail.getProductNum() <= 0 ? 1 : detail.getProductNum());
                context.getItems().add(item);
            }
            context.setSeckillActivityId(firstPositive(context.getSeckillActivityId(), detail.getSeckillId()));
            context.setBargainActivityId(firstPositive(context.getBargainActivityId(), detail.getBargainId()));
            context.setCombinationActivityId(firstPositive(context.getCombinationActivityId(), detail.getCombinationId()));
            context.setCombinationHeadId(firstPositive(context.getCombinationHeadId(), detail.getPinkId()));
            context.setBargainRecordId(firstPositive(context.getBargainRecordId(), detail.getBargainUserId()));
        }

        if (CollUtil.isEmpty(context.getItems())) {
            throw new ServiceException(500, "预下单商品不能为空");
        }
        return context;
    }

    private void fillPreOrderItemsByAgainOrder(Long userId, String orderNo,
                                               CrmebPreOrderStore.CrmebPreOrderContext context) {
        TradeOrderDO sourceOrder = tradeOrderMapper.selectFirstByNoAndUserId(orderNo, userId);
        if (sourceOrder == null) {
            throw new ServiceException(500, "原订单不存在");
        }
        List<TradeOrderItemDO> sourceItems = tradeOrderQueryService.getOrderItemListByOrderId(sourceOrder.getId());
        if (CollUtil.isEmpty(sourceItems)) {
            throw new ServiceException(500, "原订单商品不存在");
        }
        for (TradeOrderItemDO sourceItem : sourceItems) {
            CrmebPreOrderStore.CrmebPreOrderItem item = new CrmebPreOrderStore.CrmebPreOrderItem();
            item.setSkuId(sourceItem.getSkuId());
            item.setCount(sourceItem.getCount() == null || sourceItem.getCount() <= 0 ? 1 : sourceItem.getCount());
            context.getItems().add(item);
        }
        context.setSeckillActivityId(firstPositive(context.getSeckillActivityId(), sourceOrder.getSeckillActivityId()));
        context.setBargainActivityId(firstPositive(context.getBargainActivityId(), sourceOrder.getBargainActivityId()));
        context.setCombinationActivityId(firstPositive(context.getCombinationActivityId(), sourceOrder.getCombinationActivityId()));
        context.setCombinationHeadId(firstPositive(context.getCombinationHeadId(), sourceOrder.getCombinationRecordId()));
        context.setBargainRecordId(firstPositive(context.getBargainRecordId(), sourceOrder.getBargainRecordId()));
        context.setPointActivityId(firstPositive(context.getPointActivityId(), sourceOrder.getPointActivityId()));
    }

    private void mergeContextByComputedPriceRequest(CrmebPreOrderStore.CrmebPreOrderContext context,
                                                    CrmebOrderComputedPriceRequest reqVO) {
        context.setShippingType(normalizeShippingType(reqVO.getShippingType()));
        context.setAddressId(toLong(reqVO.getAddressId()));
        context.setCouponId(toLong(reqVO.getCouponId()));
        context.setUseIntegral(Boolean.TRUE.equals(reqVO.getUseIntegral()));
    }

    private void mergeContextByCreateOrderRequest(CrmebPreOrderStore.CrmebPreOrderContext context,
                                                  CrmebCreateOrderRequest reqVO) {
        context.setShippingType(normalizeShippingType(
                reqVO.getShippingType() != null ? reqVO.getShippingType() : context.getShippingType()));
        context.setAddressId(toLong(reqVO.getAddressId()) != null ? toLong(reqVO.getAddressId()) : context.getAddressId());
        context.setCouponId(toLong(reqVO.getCouponId()) != null ? toLong(reqVO.getCouponId()) : context.getCouponId());
        context.setUseIntegral(reqVO.getUseIntegral() != null ? reqVO.getUseIntegral() : context.getUseIntegral());
        context.setStoreId(toLong(reqVO.getStoreId()) != null ? toLong(reqVO.getStoreId()) : context.getStoreId());
        context.setRealName(StrUtil.isNotBlank(reqVO.getRealName()) ? reqVO.getRealName() : context.getRealName());
        context.setPhone(StrUtil.isNotBlank(reqVO.getPhone()) ? reqVO.getPhone() : context.getPhone());
        context.setMark(StrUtil.isNotBlank(reqVO.getMark()) ? reqVO.getMark() : context.getMark());
    }

    private AppTradeOrderSettlementReqVO buildSettlementReqVO(CrmebPreOrderStore.CrmebPreOrderContext context) {
        AppTradeOrderSettlementReqVO reqVO = new AppTradeOrderSettlementReqVO();
        reqVO.setItems(context.getItems().stream().map(item -> {
            AppTradeOrderSettlementReqVO.Item settlementItem = new AppTradeOrderSettlementReqVO.Item();
            if (item.getCartId() != null && item.getCartId() > 0) {
                settlementItem.setCartId(item.getCartId());
            } else {
                settlementItem.setSkuId(item.getSkuId());
                settlementItem.setCount(item.getCount() == null || item.getCount() <= 0 ? 1 : item.getCount());
            }
            return settlementItem;
        }).collect(Collectors.toList()));
        reqVO.setCouponId(context.getCouponId());
        reqVO.setPointStatus(Boolean.TRUE.equals(context.getUseIntegral()));
        reqVO.setDeliveryType(normalizeShippingType(context.getShippingType()));
        if (ObjectUtil.equals(reqVO.getDeliveryType(), DeliveryTypeEnum.EXPRESS.getType())) {
            reqVO.setAddressId(context.getAddressId());
        } else {
            reqVO.setPickUpStoreId(context.getStoreId());
            reqVO.setReceiverName(context.getRealName());
            reqVO.setReceiverMobile(context.getPhone());
        }
        reqVO.setSeckillActivityId(context.getSeckillActivityId());
        reqVO.setCombinationActivityId(context.getCombinationActivityId());
        reqVO.setCombinationHeadId(context.getCombinationHeadId());
        reqVO.setBargainRecordId(context.getBargainRecordId());
        reqVO.setPointActivityId(context.getPointActivityId());
        return reqVO;
    }

    private AppTradeOrderCreateReqVO buildCreateOrderReqVO(CrmebPreOrderStore.CrmebPreOrderContext context) {
        AppTradeOrderCreateReqVO reqVO = new AppTradeOrderCreateReqVO();
        AppTradeOrderSettlementReqVO settlementReqVO = buildSettlementReqVO(context);
        reqVO.setItems(settlementReqVO.getItems());
        reqVO.setCouponId(settlementReqVO.getCouponId());
        reqVO.setPointStatus(settlementReqVO.getPointStatus());
        reqVO.setDeliveryType(settlementReqVO.getDeliveryType());
        reqVO.setAddressId(settlementReqVO.getAddressId());
        reqVO.setPickUpStoreId(settlementReqVO.getPickUpStoreId());
        reqVO.setReceiverName(settlementReqVO.getReceiverName());
        reqVO.setReceiverMobile(settlementReqVO.getReceiverMobile());
        reqVO.setSeckillActivityId(settlementReqVO.getSeckillActivityId());
        reqVO.setCombinationActivityId(settlementReqVO.getCombinationActivityId());
        reqVO.setCombinationHeadId(settlementReqVO.getCombinationHeadId());
        reqVO.setBargainRecordId(settlementReqVO.getBargainRecordId());
        reqVO.setPointActivityId(settlementReqVO.getPointActivityId());
        reqVO.setRemark(context.getMark());
        return reqVO;
    }

    private CrmebPreOrderLoadRespVO buildLoadPreOrderResp(Long userId,
                                                           CrmebPreOrderStore.CrmebPreOrderContext context,
                                                           AppTradeOrderSettlementRespVO settlementRespVO) {
        CrmebPayConfigRespVO payConfig = buildPayConfig(userId, context.getPreOrderType());
        CrmebPreOrderLoadRespVO respVO = new CrmebPreOrderLoadRespVO();
        respVO.setPreOrderNo(context.getPreOrderNo());
        respVO.setOrderInfoVo(buildPreOrderInfoResp(context, settlementRespVO, payConfig.getUserBalance()));
        respVO.setStoreSelfMention(payConfig.getStoreSelfMention());
        respVO.setYuePayStatus(payConfig.getYuePayStatus());
        respVO.setPayWeixinOpen(payConfig.getPayWeixinOpen());
        respVO.setAliPayStatus(payConfig.getAliPayStatus());
        return respVO;
    }

    private CrmebOrderInfoVoRespVO buildPreOrderInfoResp(CrmebPreOrderStore.CrmebPreOrderContext context,
                                                         AppTradeOrderSettlementRespVO settlementRespVO,
                                                         BigDecimal userBalance) {
        AppTradeOrderSettlementRespVO.Price price = settlementRespVO != null ? settlementRespVO.getPrice() : null;
        AppTradeOrderSettlementRespVO.Address address = settlementRespVO != null ? settlementRespVO.getAddress() : null;
        List<AppTradeOrderSettlementRespVO.Item> settlementItems = settlementRespVO != null ? settlementRespVO.getItems() : Collections.emptyList();
        CrmebOrderInfoVoRespVO respVO = new CrmebOrderInfoVoRespVO();
        respVO.setFreightFee(toYuan(price != null ? price.getDeliveryPrice() : 0));
        respVO.setUserCouponId(safeInteger(context.getCouponId()));
        respVO.setCouponFee(toYuan(price != null ? price.getCouponPrice() : 0));
        respVO.setProTotalFee(toYuan(price != null ? price.getTotalPrice() : 0));
        respVO.setOrderProNum(settlementItems.stream().mapToInt(item -> ObjectUtil.defaultIfNull(item.getCount(), 0)).sum());
        respVO.setPayFee(toYuan(price != null ? price.getPayPrice() : 0));
        respVO.setAddressId(address != null ? safeInteger(address.getId()) : safeInteger(context.getAddressId()));
        respVO.setRealName(address != null ? address.getName() : null);
        respVO.setPhone(address != null ? address.getMobile() : null);
        if (address != null) {
            respVO.setProvince(address.getAreaName());
        }
        respVO.setUserIntegral(settlementRespVO != null ? ObjectUtil.defaultIfNull(settlementRespVO.getTotalPoint(), 0) : 0);
        respVO.setUserBalance(userBalance);
        respVO.setRemark(context.getMark());
        respVO.setOrderDetailList(settlementItems.stream().map(this::buildPreOrderInfoDetail).collect(Collectors.toList()));
        respVO.setSeckillId(safeInteger(context.getSeckillActivityId()));
        respVO.setBargainId(safeInteger(context.getBargainActivityId()));
        respVO.setBargainUserId(safeInteger(context.getBargainRecordId()));
        respVO.setCombinationId(safeInteger(context.getCombinationActivityId()));
        respVO.setPinkId(safeInteger(context.getCombinationHeadId()));
        respVO.setCartIdList(context.getItems().stream()
                .map(CrmebPreOrderStore.CrmebPreOrderItem::getCartId)
                .filter(ObjectUtil::isNotNull)
                .collect(Collectors.toList()));
        respVO.setIsVideo(StrUtil.equalsIgnoreCase(context.getPreOrderType(), PRE_ORDER_TYPE_VIDEO));
        return respVO;
    }

    private CrmebOrderInfoDetailRespVO buildPreOrderInfoDetail(AppTradeOrderSettlementRespVO.Item item) {
        CrmebOrderInfoDetailRespVO detail = new CrmebOrderInfoDetailRespVO();
        detail.setProductId(safeInteger(item.getSpuId()));
        detail.setProductName(item.getSpuName());
        detail.setAttrValueId(item.getSkuId());
        detail.setImage(item.getPicUrl());
        detail.setSku(buildSku(item.getProperties()));
        detail.setPrice(toYuan(item.getPrice()));
        detail.setPayNum(item.getCount());
        detail.setIsReply(0);
        detail.setProductType(0);
        return detail;
    }

    private String buildSku(List<AppProductPropertyValueDetailRespVO> properties) {
        if (CollUtil.isEmpty(properties)) {
            return "";
        }
        return properties.stream()
                .map(property -> StrUtil.blankToDefault(property.getValueName(), property.getPropertyName()))
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(","));
    }

    private CrmebComputedOrderPriceRespVO buildComputedOrderPriceResp(AppTradeOrderSettlementRespVO settlementRespVO,
                                                                      Boolean useIntegral) {
        AppTradeOrderSettlementRespVO.Price price = settlementRespVO != null ? settlementRespVO.getPrice() : null;
        CrmebComputedOrderPriceRespVO respVO = new CrmebComputedOrderPriceRespVO();
        respVO.setCouponFee(toYuan(price != null ? price.getCouponPrice() : 0));
        respVO.setDeductionPrice(toYuan(price != null ? price.getPointPrice() : 0));
        respVO.setFreightFee(toYuan(price != null ? price.getDeliveryPrice() : 0));
        respVO.setPayFee(toYuan(price != null ? price.getPayPrice() : 0));
        respVO.setProTotalFee(toYuan(price != null ? price.getTotalPrice() : 0));
        int totalPoint = settlementRespVO != null ? ObjectUtil.defaultIfNull(settlementRespVO.getTotalPoint(), 0) : 0;
        int usedPoint = settlementRespVO != null ? ObjectUtil.defaultIfNull(settlementRespVO.getUsePoint(), 0) : 0;
        respVO.setSurplusIntegral(Math.max(totalPoint - usedPoint, 0));
        respVO.setUseIntegral(Boolean.TRUE.equals(useIntegral));
        respVO.setUsedIntegral(usedPoint);
        return respVO;
    }

    private CrmebPayConfigRespVO buildPayConfig(Long userId, String preOrderType) {
        CrmebPayConfigRespVO respVO = new CrmebPayConfigRespVO();
        boolean weixinEnabled = payChannelApi.existsEnabledChannelByCodePrefix("wx_");
        boolean yueEnabled = payChannelApi.existsEnabledChannelByCodePrefix("wallet");
        boolean alipayEnabled = payChannelApi.existsEnabledChannelByCodePrefix("alipay_");
        TradeConfigDO tradeConfig = tradeConfigService.getTradeConfig();
        boolean storeSelfMentionEnabled = tradeConfig != null && Boolean.TRUE.equals(tradeConfig.getDeliveryPickUpEnabled());

        if (StrUtil.equalsIgnoreCase(preOrderType, PRE_ORDER_TYPE_VIDEO)) {
            yueEnabled = false;
            storeSelfMentionEnabled = false;
        }

        PayWalletRespDTO wallet = payWalletApi.getOrCreateWallet(userId, UserTypeEnum.MEMBER.getValue());
        respVO.setYuePayStatus(yueEnabled ? "1" : "0");
        respVO.setPayWeixinOpen(weixinEnabled ? "1" : "0");
        respVO.setAliPayStatus(alipayEnabled ? "1" : "0");
        respVO.setStoreSelfMention(storeSelfMentionEnabled ? "1" : "0");
        respVO.setUserBalance(toYuan(wallet != null ? wallet.getBalance() : 0));
        return respVO;
    }

    private BigDecimal toYuan(Integer fen) {
        return MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(fen, 0));
    }

    private Long toLong(Number value) {
        return value == null ? null : value.longValue();
    }

    private Integer safeInteger(Long value) {
        if (value == null) {
            return null;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return value.intValue();
    }

    private Long firstPositive(Long current, Number candidate) {
        if (current != null && current > 0) {
            return current;
        }
        if (candidate == null || candidate.longValue() <= 0) {
            return null;
        }
        return candidate.longValue();
    }

    private Integer normalizeShippingType(Integer shippingType) {
        if (ObjectUtil.equals(shippingType, DeliveryTypeEnum.PICK_UP.getType())) {
            return DeliveryTypeEnum.PICK_UP.getType();
        }
        return DeliveryTypeEnum.EXPRESS.getType();
    }

    private LambdaQueryWrapperX<TradeOrderDO> buildListQueryWrapper(Long userId, String keywords, Integer type,
                                                                     Set<Long> afterSaleOrderIds) {
        LambdaQueryWrapperX<TradeOrderDO> queryWrapper = new LambdaQueryWrapperX<TradeOrderDO>()
                .eq(TradeOrderDO::getUserId, userId)
                .orderByDesc(TradeOrderDO::getId);
        if (StrUtil.isNotBlank(keywords)) {
            queryWrapper.like(TradeOrderDO::getNo, keywords.trim());
        }
        switch (type) {
            case 0:
                queryWrapper.eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.UNPAID.getStatus())
                        .eq(TradeOrderDO::getPayStatus, false)
                        .eq(TradeOrderDO::getRefundStatus, TradeOrderRefundStatusEnum.NONE.getStatus());
                return queryWrapper;
            case 1:
                queryWrapper.eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.UNDELIVERED.getStatus())
                        .eq(TradeOrderDO::getPayStatus, true);
                return queryWrapper;
            case 2:
                queryWrapper.eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.DELIVERED.getStatus())
                        .eq(TradeOrderDO::getPayStatus, true);
                return queryWrapper;
            case 3:
                queryWrapper.eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.COMPLETED.getStatus())
                        .eq(TradeOrderDO::getPayStatus, true)
                        .eq(TradeOrderDO::getCommentStatus, false);
                return queryWrapper;
            case 4:
                queryWrapper.eq(TradeOrderDO::getStatus, TradeOrderStatusEnum.COMPLETED.getStatus())
                        .eq(TradeOrderDO::getPayStatus, true);
                return queryWrapper;
            case -1:
            case -2:
            case -3:
                queryWrapper.eq(TradeOrderDO::getPayStatus, true)
                        .in(TradeOrderDO::getId, afterSaleOrderIds);
                return queryWrapper;
            default:
                return queryWrapper;
        }
    }

    private Set<Long> resolveAfterSaleOrderIds(Long userId, Integer type) {
        if (type == null) {
            return Collections.emptySet();
        }
        if (type == -1) {
            return tradeOrderItemMapper.selectOrderIdsByUserIdAndAfterSaleStatuses(userId,
                    Collections.singletonList(TradeOrderItemAfterSaleStatusEnum.APPLY.getStatus()));
        }
        if (type == -2) {
            return tradeOrderItemMapper.selectOrderIdsByUserIdAndAfterSaleStatuses(userId,
                    Collections.singletonList(TradeOrderItemAfterSaleStatusEnum.SUCCESS.getStatus()));
        }
        if (type == -3) {
            return tradeOrderItemMapper.selectOrderIdsByUserIdAndAfterSaleStatuses(userId,
                    Arrays.asList(TradeOrderItemAfterSaleStatusEnum.APPLY.getStatus(),
                            TradeOrderItemAfterSaleStatusEnum.SUCCESS.getStatus()));
        }
        return Collections.emptySet();
    }

    private boolean isAfterSaleType(Integer type) {
        return ObjectUtil.equals(type, -1) || ObjectUtil.equals(type, -2) || ObjectUtil.equals(type, -3);
    }

    private TradeOrderDO findOrder(Long userId, CrmebOrderRefundApplyRequest reqVO) {
        if (reqVO.getId() != null) {
            TradeOrderDO byId = tradeOrderQueryService.getOrder(userId, reqVO.getId());
            if (byId != null) {
                return byId;
            }
        }
        return findOrder(userId, reqVO.getUni());
    }

    private TradeOrderDO findOrder(Long userId, String orderIdOrNo) {
        if (StrUtil.isBlank(orderIdOrNo)) {
            return null;
        }
        if (NumberUtil.isLong(orderIdOrNo)) {
            TradeOrderDO byNumeric = tradeOrderQueryService.getOrder(userId, NumberUtil.parseLong(orderIdOrNo));
            if (byNumeric != null) {
                return byNumeric;
            }
        }
        return tradeOrderMapper.selectFirstByNoAndUserId(orderIdOrNo, userId);
    }

    private TradeOrderItemDO findOrderItem(Long userId, TradeOrderDO order, Long orderItemId) {
        if (orderItemId != null) {
            TradeOrderItemDO orderItem = tradeOrderQueryService.getOrderItem(userId, orderItemId);
            if (orderItem == null || !order.getId().equals(orderItem.getOrderId())) {
                return null;
            }
            return orderItem;
        }
        List<TradeOrderItemDO> availableItems = tradeOrderQueryService.getOrderItemListByOrderId(order.getId()).stream()
                .filter(item -> TradeOrderItemAfterSaleStatusEnum.isNone(item.getAfterSaleStatus()))
                .collect(Collectors.toList());
        if (availableItems.isEmpty()) {
            return null;
        }
        if (availableItems.size() > 1) {
            throw new ServiceException(500, "订单包含多个商品，请补充 orderItemId");
        }
        return availableItems.get(0);
    }

    private Integer buildRefundPrice(Integer reqRefundPrice, Integer orderItemPayPrice) {
        if (reqRefundPrice != null && reqRefundPrice > 0) {
            return reqRefundPrice;
        }
        return orderItemPayPrice;
    }

    private Integer buildAfterSaleWay(Integer reqWay) {
        if (reqWay != null && ArrayUtil.contains(AfterSaleWayEnum.ARRAYS, reqWay)) {
            return reqWay;
        }
        return AfterSaleWayEnum.REFUND.getWay();
    }

    private List<String> parseReasonImages(String reasonImage) {
        if (StrUtil.isBlank(reasonImage)) {
            return null;
        }
        List<String> imageUrls = StrUtil.splitTrim(reasonImage, ',').stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        return imageUrls.isEmpty() ? null : imageUrls;
    }

    private CrmebOrderInfoRespVO convertOrderInfo(TradeOrderItemDO item) {
        CrmebOrderInfoRespVO info = new CrmebOrderInfoRespVO();
        info.setAttrId(item.getSkuId() == null ? null : String.valueOf(item.getSkuId()));
        info.setProductId(item.getSpuId());
        info.setCartNum(item.getCount());
        info.setImage(item.getPicUrl());
        info.setStoreName(item.getSpuName());
        info.setPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(item.getPrice(), 0)));
        info.setIsReply(Boolean.TRUE.equals(item.getCommentStatus()) ? 1 : 0);
        if (item.getProperties() != null && !item.getProperties().isEmpty()) {
            info.setSku(item.getProperties().stream()
                    .map(p -> StrUtil.blankToDefault(p.getValueName(), p.getPropertyName()))
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.joining(",")));
        }
        return info;
    }

    private CrmebStoreOrderDetailRespVO convertOrderDetail(TradeOrderDO order, List<TradeOrderItemDO> items) {
        CrmebStoreOrderDetailRespVO respVO = new CrmebStoreOrderDetailRespVO();
        Integer refundStatus = mapRefundStatus(order, items);
        respVO.setId(order.getId());
        respVO.setOrderId(order.getNo());
        respVO.setRealName(order.getReceiverName());
        respVO.setUserPhone(order.getReceiverMobile());
        respVO.setUserAddress(order.getReceiverDetailAddress());
        respVO.setFreightPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getDeliveryPrice(), 0)));
        respVO.setTotalPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getTotalPrice(), 0)));
        respVO.setProTotalPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getTotalPrice(), 0)));
        respVO.setPayPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getPayPrice(), 0)));
        respVO.setPayPostage(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getDeliveryPrice(), 0)));
        respVO.setDeductionPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getPointPrice(), 0)));
        respVO.setCouponId(order.getCouponId());
        respVO.setCouponPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getCouponPrice(), 0)));
        respVO.setPaid(Boolean.TRUE.equals(order.getPayStatus()) || TradeOrderStatusEnum.havePaid(order.getStatus()));
        respVO.setPayTime(order.getPayTime());
        respVO.setPayType(mapPayType(order));
        respVO.setCreateTime(order.getCreateTime());
        respVO.setStatus(mapOrderStatus(order));
        respVO.setRefundStatus(refundStatus);
        respVO.setRefundPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getRefundPrice(), 0)));
        respVO.setDeliveryName(buildDeliveryName(order));
        respVO.setDeliveryType(order.getDeliveryType() != null && order.getDeliveryType() == 2 ? "fictitious" : "express");
        respVO.setDeliveryId(order.getLogisticsNo());
        respVO.setUseIntegral(order.getUsePoint());
        respVO.setMark(order.getUserRemark());
        respVO.setIsMerCheck(0);
        respVO.setCombinationId(order.getCombinationActivityId());
        respVO.setPinkId(order.getCombinationRecordId());
        respVO.setSeckillId(order.getSeckillActivityId());
        respVO.setBargainId(order.getBargainActivityId());
        respVO.setVerifyCode(order.getPickUpVerifyCode());
        respVO.setStoreId(order.getPickUpStoreId());
        respVO.setShippingType(order.getDeliveryType());
        respVO.setIsChannel(1);
        respVO.setType(order.getType());
        respVO.setPayTypeStr(mapPayTypeStr(order));
        respVO.setOrderStatusMsg(mapOrderStatusMsg(order, refundStatus));
        respVO.setStatusPic("");
        respVO.setOrderInfoList(items.stream().map(this::convertOrderInfo).collect(Collectors.toList()));
        return respVO;
    }

    private CrmebOrderListItemRespVO convertOrderListItem(TradeOrderDO order, List<TradeOrderItemDO> items) {
        CrmebOrderListItemRespVO respVO = new CrmebOrderListItemRespVO();
        Integer refundStatus = mapRefundStatus(order, items);
        respVO.setId(order.getId());
        respVO.setOrderId(order.getNo());
        respVO.setCreateTime(order.getCreateTime());
        respVO.setPaid(Boolean.TRUE.equals(order.getPayStatus()) || TradeOrderStatusEnum.havePaid(order.getStatus()));
        respVO.setPayTime(order.getPayTime());
        respVO.setPayPrice(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getPayPrice(), 0)));
        respVO.setStatus(mapOrderStatus(order));
        respVO.setOrderStatus(mapOrderStatusMsg(order, refundStatus));
        respVO.setTotalNum(items.stream().mapToInt(item -> ObjectUtil.defaultIfNull(item.getCount(), 0)).sum());
        respVO.setPayPostage(MoneyUtils.fenToYuan(ObjectUtil.defaultIfNull(order.getDeliveryPrice(), 0)));
        respVO.setRefundStatus(refundStatus);
        respVO.setDeliveryName(buildDeliveryName(order));
        respVO.setDeliveryType(order.getDeliveryType() != null && order.getDeliveryType() == 2 ? "fictitious" : "express");
        respVO.setDeliveryId(order.getLogisticsNo());
        respVO.setPinkId(order.getCombinationRecordId());
        respVO.setBargainId(order.getBargainActivityId());
        respVO.setVerifyCode(order.getPickUpVerifyCode());
        respVO.setStoreId(order.getPickUpStoreId());
        respVO.setShippingType(order.getDeliveryType());
        respVO.setActivityType(mapActivityType(order));
        respVO.setType(order.getType());
        respVO.setStatusPic("");
        respVO.setOrderInfoList(items.stream().map(this::convertOrderInfo).collect(Collectors.toList()));
        return respVO;
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

    private String mapOrderStatusMsg(TradeOrderDO order, Integer refundStatus) {
        if (!Boolean.TRUE.equals(order.getPayStatus()) || TradeOrderStatusEnum.isUnpaid(order.getStatus())) {
            return "待支付";
        }
        if (ObjectUtil.equals(refundStatus, 1)) {
            return "申请退款中";
        }
        if (ObjectUtil.equals(refundStatus, 3)) {
            return "退款中";
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

    private String mapActivityType(TradeOrderDO order) {
        if (order.getSeckillActivityId() != null && order.getSeckillActivityId() > 0) {
            return "秒杀";
        }
        if (order.getCombinationActivityId() != null && order.getCombinationActivityId() > 0) {
            return "拼团";
        }
        if (order.getBargainActivityId() != null && order.getBargainActivityId() > 0) {
            return "砍价";
        }
        if (order.getType() != null && order.getType() == 1) {
            return "视频号";
        }
        if (order.getDeliveryType() != null && order.getDeliveryType() == 2) {
            return "核销";
        }
        return "普通";
    }

    private int toInt(Long value) {
        if (value == null || value <= 0) {
            return 0;
        }
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return value.intValue();
    }

    private String buildDeliveryName(TradeOrderDO order) {
        if (order.getDeliveryType() != null && order.getDeliveryType() == 2) {
            return "门店自提";
        }
        return "快递";
    }

    @Data
    public static class CrmebOrderRefundApplyRequest {
        /**
         * CRMEB 历史字段：订单 id
         */
        private Long id;
        /**
         * CRMEB 历史字段：订单唯一标识（部分版本传 orderNo）
         */
        private String uni;
        /**
         * 退款原因
         */
        private String text;
        /**
         * 退款凭证图片（多个英文逗号分隔）
         */
        @JsonProperty("refund_reason_wap_img")
        private String reasonImage;
        /**
         * 退款备注说明
         */
        @JsonProperty("refund_reason_wap_explain")
        private String explain;

        /**
         * 兼容扩展：多商品订单时可显式指定订单项
         */
        private Long orderItemId;
        /**
         * 兼容扩展：指定退款金额（分），默认商品实付金额
         */
        private Integer refundPrice;
        /**
         * 兼容扩展：售后方式（10 仅退款，20 退货退款）
         */
        private Integer way;
    }

    @Data
    public static class CrmebOrderCancelRequest {
        private String id;
    }

    @Data
    public static class CrmebPreOrderRequest {
        private String preOrderType;
        private List<CrmebPreOrderDetailRequest> orderDetails;
    }

    @Data
    public static class CrmebPreOrderDetailRequest {
        private Long shoppingCartId;
        private Integer productId;
        private Integer attrValueId;
        private Integer productNum;
        private String orderNo;
        private Integer bargainId;
        private Integer bargainUserId;
        private Integer combinationId;
        private Integer pinkId;
        private Integer seckillId;
    }

    @Data
    public static class CrmebOrderComputedPriceRequest {
        private String preOrderNo;
        private Integer addressId;
        private Integer couponId;
        private Integer shippingType;
        private Boolean useIntegral;
    }

    @Data
    public static class CrmebCreateOrderRequest {
        private String preOrderNo;
        private Integer shippingType;
        private Integer addressId;
        private Integer couponId;
        private Boolean useIntegral;
        private String mark;
        private Integer storeId;
        private String realName;
        private String phone;
    }

    @Data
    public static class CrmebPreOrderLoadRespVO {
        private CrmebOrderInfoVoRespVO orderInfoVo;
        private String preOrderNo;
        private String storeSelfMention;
        private String yuePayStatus;
        private String payWeixinOpen;
        private String aliPayStatus;
    }

    @Data
    public static class CrmebOrderInfoVoRespVO {
        private BigDecimal freightFee;
        private Integer userCouponId;
        private BigDecimal couponFee;
        private BigDecimal proTotalFee;
        private Integer orderProNum;
        private BigDecimal payFee;
        private Integer addressId;
        private String realName;
        private String phone;
        private String province;
        private String city;
        private String district;
        private String detail;
        private Integer userIntegral;
        private BigDecimal userBalance;
        private String remark;
        private List<CrmebOrderInfoDetailRespVO> orderDetailList;
        private Integer seckillId = 0;
        private Integer bargainId = 0;
        private Integer bargainUserId;
        private Integer combinationId = 0;
        private Integer pinkId = 0;
        private List<Long> cartIdList;
        private Boolean isVideo = false;
    }

    @Data
    public static class CrmebOrderInfoDetailRespVO {
        private Integer productId;
        private String productName;
        private Integer attrValueId;
        private String image;
        private String sku;
        private BigDecimal price;
        private Integer payNum;
        private BigDecimal weight;
        private BigDecimal volume;
        private Integer tempId;
        private Integer giveIntegral;
        private Integer isReply;
        private Boolean isSub;
        private BigDecimal vipPrice;
        private Integer productType;
    }

    @Data
    public static class CrmebComputedOrderPriceRespVO {
        private BigDecimal couponFee;
        private BigDecimal deductionPrice;
        private BigDecimal freightFee;
        private BigDecimal payFee;
        private BigDecimal proTotalFee;
        private Integer surplusIntegral;
        private Boolean useIntegral;
        private Integer usedIntegral;
    }

    @Data
    public static class CrmebPayConfigRespVO {
        private String yuePayStatus;
        private String payWeixinOpen;
        private String aliPayStatus;
        private String storeSelfMention;
        private BigDecimal userBalance;
    }

    @Data
    public static class CrmebApplyRefundOrderInfoRespVO {
        private Long id;
        private String orderId;
        private Boolean paid;
        private java.math.BigDecimal payPrice;
        private Integer totalNum;
        private List<CrmebOrderInfoRespVO> orderInfoList;
    }

    @Data
    public static class CrmebPageRespVO<T> {
        private Integer page;
        private Integer limit;
        private Integer totalPage;
        private Long total;
        private List<T> list;

        public static <T> CrmebPageRespVO<T> empty(Integer page, Integer limit) {
            return of(0L, page, limit, Collections.emptyList());
        }

        public static <T> CrmebPageRespVO<T> of(Long total, Integer page, Integer limit, List<T> list) {
            CrmebPageRespVO<T> respVO = new CrmebPageRespVO<>();
            int safeLimit = limit == null || limit <= 0 ? 20 : limit;
            int safePage = page == null || page <= 0 ? 1 : page;
            long safeTotal = ObjectUtil.defaultIfNull(total, 0L);
            int totalPage = safeTotal == 0 ? 0 : (int) ((safeTotal + safeLimit - 1) / safeLimit);
            respVO.setPage(safePage);
            respVO.setLimit(safeLimit);
            respVO.setTotalPage(totalPage);
            respVO.setTotal(safeTotal);
            respVO.setList(list == null ? Collections.emptyList() : list);
            return respVO;
        }
    }

    @Data
    public static class CrmebOrderListItemRespVO {
        private Long id;
        private String orderId;
        private java.time.LocalDateTime createTime;
        private Boolean paid;
        private java.time.LocalDateTime payTime;
        private java.math.BigDecimal payPrice;
        private Integer status;
        private String orderStatus;
        private Integer totalNum;
        private java.math.BigDecimal payPostage;
        private Integer refundStatus;
        private String deliveryName;
        private String deliveryType;
        private String deliveryId;
        private Long pinkId;
        private Long bargainId;
        private String verifyCode;
        private Long storeId;
        private Integer shippingType;
        private String activityType;
        private Integer type;
        private String statusPic;
        private List<CrmebOrderInfoRespVO> orderInfoList;
    }

    @Data
    public static class CrmebOrderDataRespVO {
        private Integer completeCount;
        private Integer evaluatedCount;
        private Integer orderCount;
        private Integer receivedCount;
        private Integer refundCount;
        private java.math.BigDecimal sumPrice;
        private Integer unPaidCount;
        private Integer unShippedCount;
    }

    @Data
    public static class CrmebOrderInfoRespVO {
        private String attrId;
        private Long productId;
        private Integer cartNum;
        private String image;
        private String storeName;
        private java.math.BigDecimal price;
        private Integer isReply;
        private String sku;
    }

    @Data
    public static class CrmebStoreOrderDetailRespVO {
        private Long id;
        private String orderId;
        private String realName;
        private String userPhone;
        private String userAddress;
        private java.math.BigDecimal freightPrice;
        private java.math.BigDecimal totalPrice;
        private java.math.BigDecimal proTotalPrice;
        private java.math.BigDecimal payPrice;
        private java.math.BigDecimal payPostage;
        private java.math.BigDecimal deductionPrice;
        private Long couponId;
        private java.math.BigDecimal couponPrice;
        private Boolean paid;
        private java.time.LocalDateTime payTime;
        private String payType;
        private java.time.LocalDateTime createTime;
        private Integer status;
        private Integer refundStatus;
        private java.math.BigDecimal refundPrice;
        private String deliveryName;
        private String deliveryType;
        private String deliveryId;
        private Integer useIntegral;
        private String mark;
        private Integer isMerCheck;
        private Long combinationId;
        private Long pinkId;
        private Long seckillId;
        private Long bargainId;
        private String verifyCode;
        private Long storeId;
        private Integer shippingType;
        private int isChannel;
        private Integer type;
        private String payTypeStr;
        private String orderStatusMsg;
        private String statusPic;
        private List<CrmebOrderInfoRespVO> orderInfoList;
    }
}
