package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;
import cn.iocoder.yudao.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import cn.iocoder.yudao.module.pay.api.refund.dto.PayRefundRespDTO;
import cn.iocoder.yudao.module.pay.enums.refund.PayRefundStatusEnum;
import cn.iocoder.yudao.module.promotion.api.combination.CombinationRecordApi;
import cn.iocoder.yudao.module.promotion.api.combination.dto.CombinationRecordRespDTO;
import cn.iocoder.yudao.module.promotion.enums.combination.CombinationRecordStatusEnum;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.AfterSaleDisagreeReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.AfterSalePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.AfterSaleRefuseReqVO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSaleDeliveryReqVO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSalePageReqVO;
import cn.iocoder.yudao.module.trade.convert.aftersale.AfterSaleConvert;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.delivery.DeliveryExpressDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderLogDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleMapper;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeServiceOrderMapper;
import cn.iocoder.yudao.module.trade.dal.redis.no.TradeNoRedisDAO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleOperateTypeEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleTypeEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleWayEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderItemAfterSaleStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderStatusEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeOrderTypeEnum;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import cn.iocoder.yudao.module.trade.framework.aftersale.core.annotations.AfterSaleLog;
import cn.iocoder.yudao.module.trade.framework.aftersale.core.utils.AfterSaleLogUtils;
import cn.iocoder.yudao.module.trade.framework.order.config.TradeOrderProperties;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleRefundDecisionBO;
import cn.iocoder.yudao.module.trade.service.delivery.DeliveryExpressService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderQueryService;
import cn.iocoder.yudao.module.trade.service.order.TradeOrderUpdateService;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.*;

/**
 * 售后订单 Service 实现类
 *
 * @author 芋道源码
 */
@Slf4j
@Service
@Validated
public class AfterSaleServiceImpl implements AfterSaleService {

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private TradeOrderUpdateService tradeOrderUpdateService;
    @Resource
    private TradeOrderQueryService tradeOrderQueryService;
    @Resource
    private DeliveryExpressService deliveryExpressService;

    @Resource
    private AfterSaleMapper tradeAfterSaleMapper;
    @Resource
    private TradeServiceOrderMapper tradeServiceOrderMapper;
    @Resource
    private TradeNoRedisDAO tradeNoRedisDAO;

    @Resource
    private PayRefundApi payRefundApi;
    @Resource
    private CombinationRecordApi combinationRecordApi;
    @Resource
    private AfterSaleRefundDecisionService afterSaleRefundDecisionService;

    @Resource
    private TradeOrderProperties tradeOrderProperties;

    @Override
    public PageResult<AfterSaleDO> getAfterSalePage(AfterSalePageReqVO pageReqVO) {
        return tradeAfterSaleMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<AfterSaleDO> getAfterSalePage(Long userId, AppAfterSalePageReqVO pageReqVO) {
        return tradeAfterSaleMapper.selectPage(userId, pageReqVO);
    }

    @Override
    public AfterSaleDO getAfterSale(Long userId, Long id) {
        return tradeAfterSaleMapper.selectByIdAndUserId(id, userId);
    }

    @Override
    public AfterSaleDO getAfterSale(Long id) {
        return tradeAfterSaleMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AfterSaleLog(operateType = AfterSaleOperateTypeEnum.MEMBER_CREATE)
    public Long createAfterSale(Long userId, AppAfterSaleCreateReqVO createReqVO) {
        // 第一步，前置校验
        TradeOrderItemDO tradeOrderItem = validateOrderItemApplicable(userId, createReqVO);
        RefundLimitDecision refundLimitDecision = resolveRefundLimitDecision(tradeOrderItem);

        // 第二步，存储售后订单
        AfterSaleDO afterSale = createAfterSale(createReqVO, tradeOrderItem, refundLimitDecision);
        return afterSale.getId();
    }

    /**
     * 校验交易订单项是否可以申请售后
     *
     * @param userId      用户编号
     * @param createReqVO 售后创建信息
     * @return 交易订单项
     */
    private TradeOrderItemDO validateOrderItemApplicable(Long userId, AppAfterSaleCreateReqVO createReqVO) {
        // 校验订单项存在
        TradeOrderItemDO orderItem = tradeOrderQueryService.getOrderItem(userId, createReqVO.getOrderItemId());
        if (orderItem == null) {
            throw exception(ORDER_ITEM_NOT_FOUND);
        }
        // 已申请售后，不允许再发起售后申请
        if (!TradeOrderItemAfterSaleStatusEnum.isNone(orderItem.getAfterSaleStatus())) {
            throw exception(AFTER_SALE_CREATE_FAIL_ORDER_ITEM_APPLIED);
        }
        // 申请退款金额上限：min(订单项实付金额, 套餐子项可退上限)
        RefundLimitDecision refundLimitDecision = resolveRefundLimitDecision(orderItem);
        if (createReqVO.getRefundPrice() > refundLimitDecision.getUpperBound()) {
            throw exception(AFTER_SALE_CREATE_FAIL_REFUND_PRICE_ERROR);
        }

        // 校验订单存在
        TradeOrderDO order = tradeOrderQueryService.getOrder(userId, orderItem.getOrderId());
        if (order == null) {
            throw exception(ORDER_NOT_FOUND);
        }
        // 校验售后申请时间限制（从订单完成时间开始计算）
        if (tradeOrderProperties.getAfterSaleExpireTime() != null
                && order.getFinishTime() != null) {
            LocalDateTime expireTime = order.getFinishTime().plus(tradeOrderProperties.getAfterSaleExpireTime());
            if (LocalDateTime.now().isAfter(expireTime)) {
                throw exception(AFTER_SALE_CREATE_FAIL_ORDER_EXPIRED);
            }
        }
        // 已取消，无法发起售后
        if (TradeOrderStatusEnum.isCanceled(order.getStatus())) {
            throw exception(AFTER_SALE_CREATE_FAIL_ORDER_STATUS_CANCELED);
        }
        // 未支付，无法发起售后
        if (!TradeOrderStatusEnum.havePaid(order.getStatus())) {
            throw exception(AFTER_SALE_CREATE_FAIL_ORDER_STATUS_NO_PAID);
        }
        // 如果是【退货退款】的情况，需要额外校验是否发货
        if (createReqVO.getWay().equals(AfterSaleWayEnum.RETURN_AND_REFUND.getWay())
                && !TradeOrderStatusEnum.haveDelivered(order.getStatus())) {
            throw exception(AFTER_SALE_CREATE_FAIL_ORDER_STATUS_NO_DELIVERED);
        }
        // 如果是拼团订单，则进行中不允许售后
        if (TradeOrderTypeEnum.isCombination(order.getType())) {
            CombinationRecordRespDTO combinationRecord = combinationRecordApi.getCombinationRecordByOrderId(
                    order.getUserId(), order.getId());
            if (combinationRecord != null && CombinationRecordStatusEnum.isInProgress(combinationRecord.getStatus())) {
                throw exception(AFTER_SALE_CREATE_FAIL_ORDER_STATUS_COMBINATION_IN_PROGRESS);
            }
        }
        return orderItem;
    }

    private RefundLimitDecision resolveRefundLimitDecision(TradeOrderItemDO orderItem) {
        Integer payPrice = ObjUtil.defaultIfNull(orderItem.getPayPrice(), 0);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("orderItemId", orderItem.getId());
        detail.put("payPrice", payPrice);

        TradeServiceOrderDO serviceOrder = tradeServiceOrderMapper.selectByOrderItemId(orderItem.getId());
        Integer serviceOrderCapPrice = resolveServiceOrderSnapshotCap(serviceOrder, detail);
        if (serviceOrderCapPrice != null) {
            int upperBound = Math.min(payPrice, serviceOrderCapPrice);
            detail.put("upperBound", upperBound);
            return new RefundLimitDecision(upperBound, "SERVICE_ORDER_SNAPSHOT", JsonUtils.toJsonString(detail));
        }

        BundleRefundComputation orderItemComputation = resolveBundleRefundableDetail(orderItem.getPriceSourceSnapshotJson());
        Integer orderItemCapPrice = orderItemComputation == null ? null : orderItemComputation.getRefundablePrice();
        if (orderItemCapPrice != null) {
            int upperBound = Math.min(payPrice, orderItemCapPrice);
            detail.put("bundleRefundablePrice", orderItemCapPrice);
            detail.put("snapshotField", "priceSourceSnapshotJson");
            if (orderItemComputation.getHasChildComputation()) {
                detail.put("bundleChildren", orderItemComputation.getBundleChildren());
            }
            detail.put("upperBound", upperBound);
            return new RefundLimitDecision(upperBound, "ORDER_ITEM_PRICE_SOURCE", JsonUtils.toJsonString(detail));
        }

        detail.put("upperBound", payPrice);
        return new RefundLimitDecision(payPrice, "ORDER_ITEM_PAY_PRICE", JsonUtils.toJsonString(detail));
    }

    private Integer resolveServiceOrderSnapshotCap(TradeServiceOrderDO serviceOrder, Map<String, Object> detail) {
        if (serviceOrder == null) {
            return null;
        }
        detail.put("serviceOrderId", serviceOrder.getId());
        detail.put("serviceOrderStatus", serviceOrder.getStatus());
        BundleRefundComputation computation = null;
        if (StrUtil.isNotBlank(serviceOrder.getOrderItemSnapshotJson())) {
            BundleRefundSnapshotPayload snapshotPayload = extractBundleRefundSnapshot(serviceOrder.getOrderItemSnapshotJson());
            if (snapshotPayload != null) {
                detail.put("serviceSnapshotField", snapshotPayload.getSnapshotField());
                computation = resolveBundleRefundableDetail(snapshotPayload.getSnapshotJson());
            }
        }
        if (computation != null && computation.getHasChildComputation()) {
            detail.put("bundleChildren", computation.getBundleChildren());
        }
        Integer cap = computation == null ? null : computation.getRefundablePrice();
        if (cap != null) {
            detail.put("bundleRefundablePrice", cap);
        }
        if (ObjUtil.equal(serviceOrder.getStatus(), TradeServiceOrderStatusEnum.FINISHED.getStatus())) {
            detail.put("blockedByFinished", true);
            return 0;
        }
        return cap;
    }

    private BundleRefundSnapshotPayload extractBundleRefundSnapshot(String orderItemSnapshotJson) {
        if (StrUtil.isBlank(orderItemSnapshotJson)) {
            return null;
        }
        JsonNode snapshotRoot = JsonUtils.parseTree(orderItemSnapshotJson);
        if (snapshotRoot == null || snapshotRoot.isMissingNode()) {
            return null;
        }
        String bundleRefundSnapshotJson = parseJsonNodeAsString(snapshotRoot.get("bundleRefundSnapshotJson"));
        if (StrUtil.isNotBlank(bundleRefundSnapshotJson)) {
            return new BundleRefundSnapshotPayload(bundleRefundSnapshotJson, "bundleRefundSnapshotJson");
        }
        String priceSourceSnapshotJson = parseJsonNodeAsString(snapshotRoot.get("priceSourceSnapshotJson"));
        if (StrUtil.isNotBlank(priceSourceSnapshotJson)) {
            return new BundleRefundSnapshotPayload(priceSourceSnapshotJson, "priceSourceSnapshotJson");
        }
        return null;
    }

    private String parseJsonNodeAsString(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isTextual() ? node.asText() : node.toString();
    }

    private Integer resolveBundleRefundablePrice(String priceSourceSnapshotJson) {
        BundleRefundComputation computation = resolveBundleRefundableDetail(priceSourceSnapshotJson);
        return computation == null ? null : computation.getRefundablePrice();
    }

    private BundleRefundComputation resolveBundleRefundableDetail(String priceSourceSnapshotJson) {
        if (StrUtil.isBlank(priceSourceSnapshotJson)) {
            return null;
        }
        BundleRefundSnapshot bundleSnapshot = JsonUtils.parseObjectQuietly(priceSourceSnapshotJson,
                new TypeReference<BundleRefundSnapshot>() {});
        if (bundleSnapshot == null) {
            return null;
        }

        int sum = 0;
        boolean matched = false;
        List<Map<String, Object>> childDetails = new java.util.ArrayList<>();
        if (bundleSnapshot.getBundleChildren() != null) {
            for (BundleChildRefundSnapshot child : bundleSnapshot.getBundleChildren()) {
                if (child == null) {
                    continue;
                }
                Integer childRefundCap = normalizeNonNegative(child.getRefundCapPrice());
                if (childRefundCap == null) {
                    continue;
                }
                boolean included = Boolean.TRUE.equals(child.getRefundable()) || !Boolean.TRUE.equals(child.getFulfilled());
                Map<String, Object> childDetail = new LinkedHashMap<>();
                childDetail.put("childCode", child.getChildCode());
                childDetail.put("refundCapPrice", childRefundCap);
                childDetail.put("fulfilled", child.getFulfilled());
                childDetail.put("refundable", child.getRefundable());
                childDetail.put("included", included);
                childDetails.add(childDetail);
                if (included) {
                    sum += childRefundCap;
                    matched = true;
                }
            }
        }
        if (matched) {
            return new BundleRefundComputation(sum, childDetails, true);
        }
        Integer explicitRefundablePrice = normalizeNonNegative(bundleSnapshot.getBundleRefundablePrice());
        if (explicitRefundablePrice != null) {
            return new BundleRefundComputation(explicitRefundablePrice, childDetails, false);
        }
        return null;
    }

    private Integer normalizeNonNegative(Integer price) {
        if (price == null) {
            return null;
        }
        return Math.max(price, 0);
    }

    private AfterSaleDO createAfterSale(AppAfterSaleCreateReqVO createReqVO,
                                        TradeOrderItemDO orderItem,
                                        RefundLimitDecision refundLimitDecision) {
        // 创建售后单
        AfterSaleDO afterSale = AfterSaleConvert.INSTANCE.convert(createReqVO, orderItem);
        afterSale.setNo(tradeNoRedisDAO.generate(TradeNoRedisDAO.AFTER_SALE_NO_PREFIX));
        afterSale.setStatus(AfterSaleStatusEnum.APPLY.getStatus());
        afterSale.setRefundLimitSource(refundLimitDecision.getSource());
        afterSale.setRefundLimitDetailJson(refundLimitDecision.getDetailJson());
        // 标记是售中还是售后
        TradeOrderDO order = tradeOrderQueryService.getOrder(orderItem.getUserId(), orderItem.getOrderId());
        afterSale.setOrderNo(order.getNo()); // 记录 orderNo 订单流水，方便后续检索
        afterSale.setType(TradeOrderStatusEnum.isCompleted(order.getStatus())
                ? AfterSaleTypeEnum.AFTER_SALE.getType() : AfterSaleTypeEnum.IN_SALE.getType());
        tradeAfterSaleMapper.insert(afterSale);

        // 更新交易订单项的售后状态
        tradeOrderUpdateService.updateOrderItemWhenAfterSaleCreate(orderItem.getId(), afterSale.getId());

        // 记录售后日志
        AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), null,
                AfterSaleStatusEnum.APPLY.getStatus());
        return afterSale;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AfterSaleLog(operateType = AfterSaleOperateTypeEnum.ADMIN_AGREE_APPLY)
    public void agreeAfterSale(Long userId, Long id) {
        // 校验售后单存在，并状态未审批
        AfterSaleDO afterSale = validateAfterSaleAuditable(id);

        // 更新售后单的状态
        // 情况一：退款：标记为 WAIT_REFUND 状态。后续等退款发起成功后，在标记为 COMPLETE 状态
        // 情况二：退货退款：需要等用户退货后，才能发起退款
        Integer newStatus = afterSale.getWay().equals(AfterSaleWayEnum.REFUND.getWay()) ?
                AfterSaleStatusEnum.WAIT_REFUND.getStatus() : AfterSaleStatusEnum.SELLER_AGREE.getStatus();
        updateAfterSaleStatus(afterSale.getId(), AfterSaleStatusEnum.APPLY.getStatus(), new AfterSaleDO()
                .setStatus(newStatus).setAuditUserId(userId).setAuditTime(LocalDateTime.now()));

        // 记录售后日志
        AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), afterSale.getStatus(), newStatus);

        // 退款型售后在同意后直接进入分层决策：低风险自动执行，高风险保留人工复核队列
        if (ObjectUtil.equals(newStatus, AfterSaleStatusEnum.WAIT_REFUND.getStatus())
                && ObjectUtil.equals(afterSale.getWay(), AfterSaleWayEnum.REFUND.getWay())) {
            autoExecuteRefundIfEligible(afterSale.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AfterSaleLog(operateType = AfterSaleOperateTypeEnum.ADMIN_DISAGREE_APPLY)
    public void disagreeAfterSale(Long userId, AfterSaleDisagreeReqVO auditReqVO) {
        // 校验售后单存在，并状态未审批
        AfterSaleDO afterSale = validateAfterSaleAuditable(auditReqVO.getId());

        // 更新售后单的状态
        Integer newStatus = AfterSaleStatusEnum.SELLER_DISAGREE.getStatus();
        updateAfterSaleStatus(afterSale.getId(), AfterSaleStatusEnum.APPLY.getStatus(), new AfterSaleDO()
                .setStatus(newStatus).setAuditUserId(userId).setAuditTime(LocalDateTime.now())
                .setAuditReason(auditReqVO.getAuditReason()));

        // 记录售后日志
        AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), afterSale.getStatus(), newStatus);

        // 更新交易订单项的售后状态为【未申请】
        tradeOrderUpdateService.updateOrderItemWhenAfterSaleCancel(afterSale.getOrderItemId());
    }

    /**
     * 校验售后单是否可审批（同意售后、拒绝售后）
     *
     * @param id 售后编号
     * @return 售后单
     */
    private AfterSaleDO validateAfterSaleAuditable(Long id) {
        AfterSaleDO afterSale = tradeAfterSaleMapper.selectById(id);
        if (afterSale == null) {
            throw exception(AFTER_SALE_NOT_FOUND);
        }
        if (ObjectUtil.notEqual(afterSale.getStatus(), AfterSaleStatusEnum.APPLY.getStatus())) {
            throw exception(AFTER_SALE_AUDIT_FAIL_STATUS_NOT_APPLY);
        }
        return afterSale;
    }

    private void updateAfterSaleStatus(Long id, Integer status, AfterSaleDO updateObj) {
        int updateCount = tradeAfterSaleMapper.updateByIdAndStatus(id, status, updateObj);
        if (updateCount == 0) {
            throw exception(AFTER_SALE_UPDATE_STATUS_FAIL);
        }
    }

    private void autoExecuteRefundIfEligible(Long afterSaleId) {
        AfterSaleDO afterSale = tradeAfterSaleMapper.selectById(afterSaleId);
        if (afterSale == null
                || !ObjectUtil.equals(afterSale.getWay(), AfterSaleWayEnum.REFUND.getWay())
                || !ObjectUtil.equals(afterSale.getStatus(), AfterSaleStatusEnum.WAIT_REFUND.getStatus())) {
            return;
        }

        AfterSaleRefundDecisionBO decision = afterSaleRefundDecisionService.evaluate(afterSale);
        afterSaleRefundDecisionService.auditDecision(
                TradeOrderLogDO.USER_ID_SYSTEM, TradeOrderLogDO.USER_TYPE_SYSTEM, afterSale, decision, false);
        if (!Boolean.TRUE.equals(decision.getAutoPass())) {
            log.info("[autoExecuteRefundIfEligible][afterSale({}) 命中人工复核规则({}) reason({})]",
                    afterSale.getId(), decision.getRuleCode(), decision.getReason());
            return;
        }

        try {
            processRefundAfterSale("system-auto", afterSale);
            log.info("[autoExecuteRefundIfEligible][afterSale({}) 已触发自动退款执行]", afterSale.getId());
        } catch (Exception e) {
            // 自动路径失败不回滚售后同意动作，保留 WAIT_REFUND 供总部复核补偿
            log.error("[autoExecuteRefundIfEligible][afterSale({}) 自动退款执行失败，需人工复核补偿]",
                    afterSale.getId(), e);
            try {
                AfterSaleRefundDecisionBO failDecision = AfterSaleRefundDecisionBO.manual(
                        "AUTO_REFUND_EXECUTE_FAIL",
                        StrUtil.format("自动退款执行失败：{}", e.getClass().getSimpleName()));
                afterSaleRefundDecisionService.auditDecision(
                        TradeOrderLogDO.USER_ID_SYSTEM,
                        TradeOrderLogDO.USER_TYPE_SYSTEM,
                        afterSale,
                        failDecision,
                        false);
            } catch (Exception auditEx) {
                log.error("[autoExecuteRefundIfEligible][afterSale({}) 自动建工单审计失败]",
                        afterSale.getId(), auditEx);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AfterSaleLog(operateType = AfterSaleOperateTypeEnum.MEMBER_DELIVERY)
    public void deliveryAfterSale(Long userId, AppAfterSaleDeliveryReqVO deliveryReqVO) {
        // 校验售后单存在，并状态未退货
        AfterSaleDO afterSale = tradeAfterSaleMapper.selectByIdAndUserId(deliveryReqVO.getId(), userId);
        if (afterSale == null) {
            throw exception(AFTER_SALE_NOT_FOUND);
        }
        if (ObjectUtil.notEqual(afterSale.getStatus(), AfterSaleStatusEnum.SELLER_AGREE.getStatus())) {
            throw exception(AFTER_SALE_DELIVERY_FAIL_STATUS_NOT_SELLER_AGREE);
        }
        DeliveryExpressDO express = deliveryExpressService.validateDeliveryExpress(deliveryReqVO.getLogisticsId());

        // 更新售后单的物流信息
        updateAfterSaleStatus(afterSale.getId(), AfterSaleStatusEnum.SELLER_AGREE.getStatus(), new AfterSaleDO()
                .setStatus(AfterSaleStatusEnum.BUYER_DELIVERY.getStatus())
                .setLogisticsId(deliveryReqVO.getLogisticsId()).setLogisticsNo(deliveryReqVO.getLogisticsNo())
                .setDeliveryTime(LocalDateTime.now()));

        // 记录售后日志
        AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), afterSale.getStatus(),
                AfterSaleStatusEnum.BUYER_DELIVERY.getStatus(),
                MapUtil.<String, Object>builder().put("deliveryName", express.getName())
                        .put("logisticsNo", deliveryReqVO.getLogisticsNo()).build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AfterSaleLog(operateType = AfterSaleOperateTypeEnum.ADMIN_AGREE_RECEIVE)
    public void receiveAfterSale(Long userId, Long id) {
        // 校验售后单存在，并状态为已退货
        AfterSaleDO afterSale = validateAfterSaleReceivable(id);

        // 更新售后单的状态
        updateAfterSaleStatus(afterSale.getId(), AfterSaleStatusEnum.BUYER_DELIVERY.getStatus(), new AfterSaleDO()
                .setStatus(AfterSaleStatusEnum.WAIT_REFUND.getStatus()).setReceiveTime(LocalDateTime.now()));

        // 记录售后日志
        AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), afterSale.getStatus(),
                AfterSaleStatusEnum.WAIT_REFUND.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AfterSaleLog(operateType = AfterSaleOperateTypeEnum.ADMIN_DISAGREE_RECEIVE)
    public void refuseAfterSale(Long userId, AfterSaleRefuseReqVO refuseReqVO) {
        // 校验售后单存在，并状态为已退货
        AfterSaleDO afterSale = tradeAfterSaleMapper.selectById(refuseReqVO.getId());
        if (afterSale == null) {
            throw exception(AFTER_SALE_NOT_FOUND);
        }
        if (ObjectUtil.notEqual(afterSale.getStatus(), AfterSaleStatusEnum.BUYER_DELIVERY.getStatus())) {
            throw exception(AFTER_SALE_CONFIRM_FAIL_STATUS_NOT_BUYER_DELIVERY);
        }

        // 更新售后单的状态
        updateAfterSaleStatus(afterSale.getId(), AfterSaleStatusEnum.BUYER_DELIVERY.getStatus(), new AfterSaleDO()
                .setStatus(AfterSaleStatusEnum.SELLER_REFUSE.getStatus()).setReceiveTime(LocalDateTime.now())
                .setReceiveReason(refuseReqVO.getRefuseMemo()));

        // 记录售后日志
        AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), afterSale.getStatus(),
                AfterSaleStatusEnum.SELLER_REFUSE.getStatus(),
                MapUtil.of("reason", refuseReqVO.getRefuseMemo()));

        // 更新交易订单项的售后状态为【未申请】
        tradeOrderUpdateService.updateOrderItemWhenAfterSaleCancel(afterSale.getOrderItemId());
    }

    /**
     * 校验售后单是否可收货，即处于买家已发货
     *
     * @param id 售后编号
     * @return 售后单
     */
    private AfterSaleDO validateAfterSaleReceivable(Long id) {
        AfterSaleDO afterSale = tradeAfterSaleMapper.selectById(id);
        if (afterSale == null) {
            throw exception(AFTER_SALE_NOT_FOUND);
        }
        if (ObjectUtil.notEqual(afterSale.getStatus(), AfterSaleStatusEnum.BUYER_DELIVERY.getStatus())) {
            throw exception(AFTER_SALE_CONFIRM_FAIL_STATUS_NOT_BUYER_DELIVERY);
        }
        return afterSale;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AfterSaleLog(operateType = AfterSaleOperateTypeEnum.ADMIN_REFUND)
    public void refundAfterSale(Long userId, String userIp, Long id) {
        // 校验售后单的状态，并状态待退款
        AfterSaleDO afterSale = tradeAfterSaleMapper.selectById(id);
        if (afterSale == null) {
            throw exception(AFTER_SALE_NOT_FOUND);
        }
        if (ObjectUtil.notEqual(afterSale.getStatus(), AfterSaleStatusEnum.WAIT_REFUND.getStatus())) {
            throw exception(AFTER_SALE_REFUND_FAIL_STATUS_NOT_WAIT_REFUND);
        }
        processRefundAfterSale(userIp, afterSale);
    }

    private void processRefundAfterSale(String userIp, AfterSaleDO afterSale) {
        validateRefundLimitBeforeRefund(afterSale);

        Integer newStatus;
        if (ObjUtil.equals(afterSale.getRefundPrice(), 0)) {
            // 特殊：退款为 0 的订单，直接标记为完成（积分商城）。关联案例：https://t.zsxq.com/AQEvL
            updateAfterSaleStatus(afterSale.getId(), AfterSaleStatusEnum.WAIT_REFUND.getStatus(), new AfterSaleDO()
                    .setStatus(AfterSaleStatusEnum.COMPLETE.getStatus()).setRefundTime(LocalDateTime.now()));
            newStatus = AfterSaleStatusEnum.COMPLETE.getStatus();
        } else {
            // 发起退款单。注意，需要在事务提交后，再进行发起，避免重复发起
            createPayRefund(userIp, afterSale);
            newStatus = afterSale.getStatus();  // 特殊：这里状态不变，而是最终 updateAfterSaleRefunded 处理！！！
        }

        // 记录售后日志
        AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), afterSale.getStatus(), newStatus);
    }

    private void validateRefundLimitBeforeRefund(AfterSaleDO afterSale) {
        TradeOrderItemDO orderItem = tradeOrderQueryService.getOrderItem(afterSale.getUserId(), afterSale.getOrderItemId());
        if (orderItem == null) {
            throw exception(ORDER_ITEM_NOT_FOUND);
        }
        RefundLimitDecision latestDecision = resolveRefundLimitDecision(orderItem);
        tradeAfterSaleMapper.updateById(new AfterSaleDO()
                .setId(afterSale.getId())
                .setRefundLimitSource(latestDecision.getSource())
                .setRefundLimitDetailJson(latestDecision.getDetailJson()));
        if (afterSale.getRefundPrice() > latestDecision.getUpperBound()) {
            AfterSaleRefundDecisionBO manualDecision = AfterSaleRefundDecisionBO.manual(
                    "REFUND_LIMIT_CHANGED",
                    StrUtil.format("退款上限收紧为{}分，当前申请退款{}分",
                            latestDecision.getUpperBound(), afterSale.getRefundPrice()));
            afterSaleRefundDecisionService.auditDecision(
                    TradeOrderLogDO.USER_ID_SYSTEM,
                    TradeOrderLogDO.USER_TYPE_SYSTEM,
                    afterSale,
                    manualDecision,
                    false);
            throw exception(AFTER_SALE_REFUND_FAIL_REFUND_LIMIT_CHANGED);
        }
    }

    private void createPayRefund(String userIp, AfterSaleDO afterSale) {
        // 创建退款单
        PayRefundCreateReqDTO createReqDTO = AfterSaleConvert.INSTANCE.convert(userIp, afterSale, tradeOrderProperties)
                .setUserId(afterSale.getUserId()).setUserType(UserTypeEnum.MEMBER.getValue())
                .setReason(StrUtil.format("退款【{}】", afterSale.getSpuName()));
        Long payRefundId = payRefundApi.createRefund(createReqDTO);

        // 更新售后单的退款单号
        tradeAfterSaleMapper.updateById(new AfterSaleDO().setId(afterSale.getId()).setPayRefundId(payRefundId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AfterSaleLog(operateType = AfterSaleOperateTypeEnum.SYSTEM_REFUND_SUCCESS)
    public void updateAfterSaleRefunded(Long id, Long orderId, Long payRefundId) {
        // 1. 校验售后单的状态，并状态待退款
        AfterSaleDO afterSale = tradeAfterSaleMapper.selectById(id);
        if (afterSale == null) {
            throw exception(AFTER_SALE_NOT_FOUND);
        }
        if (ObjectUtil.notEqual(afterSale.getStatus(), AfterSaleStatusEnum.WAIT_REFUND.getStatus())) {
            throw exception(AFTER_SALE_REFUND_FAIL_STATUS_NOT_WAIT_REFUND);
        }

        // 2. 校验退款单
        PayRefundRespDTO payRefund = validatePayRefund(afterSale, payRefundId);

        // 3. 处理退款结果
        if (PayRefundStatusEnum.isSuccess(payRefund.getStatus())) {
            // 【情况一：退款成功】
            updateAfterSaleStatus(afterSale.getId(), AfterSaleStatusEnum.WAIT_REFUND.getStatus(), new AfterSaleDO()
                .setStatus(AfterSaleStatusEnum.COMPLETE.getStatus()).setRefundTime(LocalDateTime.now()));

            // 记录售后日志
            AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), afterSale.getStatus(), AfterSaleStatusEnum.COMPLETE.getStatus());

            // 更新交易订单项的售后状态为【已完成】
            tradeOrderUpdateService.updateOrderItemWhenAfterSaleSuccess(afterSale.getOrderItemId(), afterSale.getRefundPrice());
            // 【情况二：退款失败】
        } else if (PayRefundStatusEnum.isFailure(payRefund.getStatus())) {
            // 记录售后日志
            AfterSaleLogUtils.setAfterSaleOperateType(AfterSaleOperateTypeEnum.SYSTEM_REFUND_FAIL);
            AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), afterSale.getStatus(), afterSale.getStatus());
        }
    }

    /**
     * 校验退款单的合法性
     *
     * @param afterSale 售后单
     * @param payRefundId 退款单编号
     * @return 退款单
     */
    private PayRefundRespDTO validatePayRefund(AfterSaleDO afterSale, Long payRefundId) {
        // 1. 校验退款单是否存在
        PayRefundRespDTO payRefund = payRefundApi.getRefund(payRefundId);
        if (payRefund == null) {
            log.error("[validatePayRefund][afterSale({}) payRefund({}) 不存在，请进行处理！]", afterSale.getId(), payRefundId);
            throw exception(AFTER_SALE_REFUND_FAIL_REFUND_NOT_FOUND);
        }
        // 2.1 校验退款单无退款结果（成功、失败）
        if (!PayRefundStatusEnum.isSuccess(payRefund.getStatus())
            && !PayRefundStatusEnum.isFailure(payRefund.getStatus())) {
            log.error("[validatePayRefund][afterSale({}) payRefund({}) 无退款结果，请进行处理！payRefund 数据是：{}]",
                    afterSale.getId(), payRefundId, toJsonString(payRefund));
            throw exception(AFTER_SALE_REFUND_FAIL_REFUND_NOT_SUCCESS_OR_FAILURE);
        }
        // 2.2 校验退款金额一致
        if (ObjectUtil.notEqual(payRefund.getRefundPrice(), afterSale.getRefundPrice())) {
            log.error("[validatePayRefund][afterSale({}) payRefund({}) 退款金额不匹配，请进行处理！afterSale 数据是：{}，payRefund 数据是：{}]",
                    afterSale.getId(), payRefundId, toJsonString(afterSale), toJsonString(payRefund));
            throw exception(AFTER_SALE_REFUND_FAIL_REFUND_PRICE_NOT_MATCH);
        }
        // 2.3 校验退款订单匹配（二次）
        if (ObjectUtil.notEqual(payRefund.getMerchantRefundId(), afterSale.getId().toString())) {
            log.error("[validatePayRefund][afterSale({}) 退款单不匹配({})，请进行处理！payRefund 数据是：{}]",
                    afterSale.getId(), payRefundId, toJsonString(payRefund));
            throw exception(AFTER_SALE_REFUND_FAIL_REFUND_ORDER_ID_ERROR);
        }
        return payRefund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AfterSaleLog(operateType = AfterSaleOperateTypeEnum.MEMBER_CANCEL)
    public void cancelAfterSale(Long userId, Long id) {
        // 校验售后单的状态，并状态待退款
        AfterSaleDO afterSale = tradeAfterSaleMapper.selectByIdAndUserId(id, userId);
        if (afterSale == null) {
            throw exception(AFTER_SALE_NOT_FOUND);
        }
        if (!ObjectUtils.equalsAny(afterSale.getStatus(), AfterSaleStatusEnum.APPLY.getStatus(),
                AfterSaleStatusEnum.SELLER_AGREE.getStatus(),
                AfterSaleStatusEnum.BUYER_DELIVERY.getStatus())) {
            throw exception(AFTER_SALE_CANCEL_FAIL_STATUS_NOT_APPLY_OR_AGREE_OR_BUYER_DELIVERY);
        }

        // 更新售后单的状态为【已取消】
        updateAfterSaleStatus(afterSale.getId(), afterSale.getStatus(), new AfterSaleDO()
                .setStatus(AfterSaleStatusEnum.BUYER_CANCEL.getStatus()));

        // 记录售后日志
        AfterSaleLogUtils.setAfterSaleInfo(afterSale.getId(), afterSale.getStatus(),
                AfterSaleStatusEnum.BUYER_CANCEL.getStatus());

        // 更新交易订单项的售后状态为【未申请】
        tradeOrderUpdateService.updateOrderItemWhenAfterSaleCancel(afterSale.getOrderItemId());
    }

    @Override
    public Long getApplyingAfterSaleCount(Long userId) {
        return tradeAfterSaleMapper.selectCountByUserIdAndStatus(userId, AfterSaleStatusEnum.APPLYING_STATUSES);
    }

    @Data
    private static class RefundLimitDecision {
        /**
         * 退款上限（分）
         */
        private final Integer upperBound;
        /**
         * 上限来源
         */
        private final String source;
        /**
         * 审计明细（JSON）
         */
        private final String detailJson;
    }

    @Data
    private static class BundleRefundSnapshot {
        @JsonAlias({"bundleRefundablePrice", "refundablePrice", "maxRefundPrice"})
        private Integer bundleRefundablePrice;
        @JsonAlias({"bundleChildren", "children", "items"})
        private List<BundleChildRefundSnapshot> bundleChildren;
    }

    @Data
    private static class BundleChildRefundSnapshot {
        @JsonAlias({"childCode", "code", "itemCode", "skuCode"})
        private String childCode;
        @JsonAlias({"refundCapPrice", "refundablePrice", "maxRefundPrice"})
        private Integer refundCapPrice;
        @JsonAlias({"fulfilled", "served", "completed", "serviceFulfilled"})
        private Boolean fulfilled;
        @JsonAlias({"refundable", "allowRefund"})
        private Boolean refundable;
    }

    @Data
    private static class BundleRefundComputation {
        /**
         * 可退金额（分）
         */
        private final Integer refundablePrice;
        /**
         * 子项明细快照（用于审计）
         */
        private final List<Map<String, Object>> bundleChildren;
        /**
         * true 表示由子项履约状态计算得出；false 表示显式金额兜底
         */
        private final Boolean hasChildComputation;
    }

    @Data
    private static class BundleRefundSnapshotPayload {
        /**
         * 快照 JSON
         */
        private final String snapshotJson;
        /**
         * 快照字段来源
         */
        private final String snapshotField;
    }

}
