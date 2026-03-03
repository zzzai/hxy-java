package cn.iocoder.yudao.module.trade.service.order;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeOrderItemDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.dal.mysql.order.TradeServiceOrderMapper;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import cn.iocoder.yudao.module.trade.service.order.bo.TradeBundleItemSnapshotBO;
import com.fasterxml.jackson.core.type.TypeReference;
import cn.iocoder.yudao.module.trade.service.order.booking.TradeServiceBookingGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.SERVICE_ORDER_NOT_FOUND;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.SERVICE_ORDER_STATUS_ILLEGAL;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.SERVICE_ORDER_UPDATE_STATUS_FAIL;

/**
 * 服务履约单 Service 实现类
 *
 * @author HXY
 */
@Service
@Slf4j
public class TradeServiceOrderServiceImpl implements TradeServiceOrderService {

    private static final String SOURCE_PAY_CALLBACK = "PAY_CALLBACK";
    private static final String BOOKING_PLACEHOLDER_REMARK = "支付成功自动创建，待预约";
    private static final int BOOKING_NO_MAX_LENGTH = 64;
    private static final int REMARK_MAX_LENGTH = 255;
    private static final int RETRY_LIMIT_DEFAULT = 200;
    private static final int RETRY_LIMIT_MAX = 1000;
    private static final String BUNDLE_REFUND_SNAPSHOT_KEY = "bundleRefundSnapshotJson";
    private static final String BUNDLE_ITEM_SNAPSHOT_KEY = "bundleItemSnapshotJson";

    @Resource
    private TradeServiceOrderMapper tradeServiceOrderMapper;
    @Resource
    private TradeServiceBookingGateway tradeServiceBookingGateway;

    @Override
    public int createByPaidOrder(TradeOrderDO order, List<TradeOrderItemDO> serviceItems) {
        if (CollUtil.isEmpty(serviceItems)) {
            return 0;
        }
        int createdCount = 0;
        for (TradeOrderItemDO item : serviceItems) {
            // 冗余保护：handler 已过滤过一次，这里再次防御，避免误建单
            if (!ProductTypeEnum.isService(item.getProductType())) {
                continue;
            }
            if (tradeServiceOrderMapper.selectByOrderItemId(item.getId()) != null) {
                log.info("[createByPaidOrder][order({}) orderItem({}) 已存在服务履约单，跳过]", order.getId(), item.getId());
                continue;
            }
            TradeServiceOrderDO serviceOrder = TradeServiceOrderDO.builder()
                    .orderId(order.getId())
                    .orderNo(order.getNo())
                    .orderItemId(item.getId())
                    .userId(order.getUserId())
                    .payOrderId(order.getPayOrderId())
                    .spuId(item.getSpuId())
                    .skuId(item.getSkuId())
                    .addonType(item.getAddonType())
                    .addonSnapshotJson(item.getAddonSnapshotJson())
                    .orderItemSnapshotJson(buildOrderItemSnapshot(item))
                    .status(TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus())
                    .source(SOURCE_PAY_CALLBACK)
                    .remark(BOOKING_PLACEHOLDER_REMARK)
                    .build();
            try {
                tradeServiceOrderMapper.insert(serviceOrder);
                createdCount++;
                try {
                    createBookingPlaceholder(serviceOrder.getId());
                } catch (Exception ex) {
                    // 预约域异常不应影响支付主链路，记录告警后交由后续重试任务收敛
                    log.error("[createByPaidOrder][order({}) orderItem({}) serviceOrder({}) 预约占位失败，已降级忽略]",
                            order.getId(), item.getId(), serviceOrder.getId(), ex);
                }
            } catch (DuplicateKeyException ex) {
                // 并发回调保护：依赖 order_item_id 唯一键兜底，遇到重复键按幂等成功处理
                log.warn("[createByPaidOrder][order({}) orderItem({}) 并发重复创建服务履约单，已按幂等忽略]",
                        order.getId(), item.getId());
            }
        }
        return createdCount;
    }

    @Override
    public void createBookingPlaceholder(Long serviceOrderId) {
        TradeServiceOrderDO serviceOrder = tradeServiceOrderMapper.selectById(serviceOrderId);
        if (serviceOrder == null) {
            log.warn("[createBookingPlaceholder][serviceOrder({}) 不存在，跳过]", serviceOrderId);
            return;
        }
        tradeServiceBookingGateway.createPendingBooking(serviceOrder);
    }

    @Override
    public void markBooked(Long serviceOrderId, String bookingNo, String remark) {
        TradeServiceOrderDO serviceOrder = getRequiredServiceOrder(serviceOrderId);
        String normalizedBookingNo = normalizeBookingNo(bookingNo);
        if (!ObjUtil.equal(serviceOrder.getStatus(), TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus())) {
            throw exception(SERVICE_ORDER_STATUS_ILLEGAL,
                    TradeServiceOrderStatusEnum.getNameByStatus(serviceOrder.getStatus()),
                    TradeServiceOrderStatusEnum.WAIT_BOOKING.getName());
        }
        int updateCount = tradeServiceOrderMapper.updateByIdAndStatus(serviceOrderId,
                TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus(), TradeServiceOrderDO.builder()
                        .status(TradeServiceOrderStatusEnum.BOOKED.getStatus())
                        .bookingNo(normalizedBookingNo)
                        .remark(mergeRemark("BOOKED", remark))
                        .build());
        if (updateCount == 0) {
            throw exception(SERVICE_ORDER_UPDATE_STATUS_FAIL);
        }
        log.info("[markBooked][serviceOrderId({}) status {} -> {} bookingNo({})]", serviceOrderId,
                TradeServiceOrderStatusEnum.WAIT_BOOKING.getName(),
                TradeServiceOrderStatusEnum.BOOKED.getName(), normalizedBookingNo);
    }

    @Override
    public void startServing(Long serviceOrderId, String remark) {
        transitionStatus(serviceOrderId, TradeServiceOrderStatusEnum.BOOKED,
                TradeServiceOrderStatusEnum.SERVING, mergeRemark("SERVING", remark));
    }

    @Override
    public void finishServing(Long serviceOrderId, String remark) {
        transitionStatus(serviceOrderId, TradeServiceOrderStatusEnum.SERVING,
                TradeServiceOrderStatusEnum.FINISHED, mergeRemark("FINISHED", remark));
    }

    @Override
    public void cancelServiceOrder(Long serviceOrderId, String remark) {
        TradeServiceOrderDO serviceOrder = getRequiredServiceOrder(serviceOrderId);
        if (TradeServiceOrderStatusEnum.FINISHED.getStatus().equals(serviceOrder.getStatus())) {
            throw exception(SERVICE_ORDER_STATUS_ILLEGAL,
                    TradeServiceOrderStatusEnum.getNameByStatus(serviceOrder.getStatus()),
                    "待预约/已预约/服务中");
        }
        if (TradeServiceOrderStatusEnum.CANCELLED.getStatus().equals(serviceOrder.getStatus())) {
            return;
        }
        int updateCount = tradeServiceOrderMapper.updateByIdAndStatus(serviceOrderId, serviceOrder.getStatus(),
                TradeServiceOrderDO.builder()
                        .status(TradeServiceOrderStatusEnum.CANCELLED.getStatus())
                        .remark(mergeRemark("CANCELLED", remark))
                        .build());
        if (updateCount == 0) {
            throw exception(SERVICE_ORDER_UPDATE_STATUS_FAIL);
        }
        log.info("[cancelServiceOrder][serviceOrderId({}) status {} -> {}]", serviceOrderId,
                TradeServiceOrderStatusEnum.getNameByStatus(serviceOrder.getStatus()),
                TradeServiceOrderStatusEnum.CANCELLED.getName());
    }

    @Override
    public TradeServiceOrderDO getServiceOrder(Long serviceOrderId) {
        return tradeServiceOrderMapper.selectById(serviceOrderId);
    }

    @Override
    public List<TradeServiceOrderDO> getServiceOrderListByOrderId(Long orderId) {
        return tradeServiceOrderMapper.selectListByOrderId(orderId);
    }

    @Override
    public PageResult<TradeServiceOrderDO> getServiceOrderPage(TradeServiceOrderPageReqVO pageReqVO) {
        return tradeServiceOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public int retryCreateBookingPlaceholder(Integer limit) {
        int safeLimit = Math.max(1, Math.min(ObjUtil.defaultIfNull(limit, RETRY_LIMIT_DEFAULT), RETRY_LIMIT_MAX));
        List<TradeServiceOrderDO> candidates = tradeServiceOrderMapper.selectListForBookingPlaceholderRetry(
                TradeServiceOrderStatusEnum.WAIT_BOOKING.getStatus(), safeLimit);
        if (CollUtil.isEmpty(candidates)) {
            return 0;
        }
        int successCount = 0;
        for (TradeServiceOrderDO candidate : candidates) {
            try {
                tradeServiceBookingGateway.createPendingBooking(candidate);
                successCount++;
            } catch (Exception ex) {
                log.error("[retryCreateBookingPlaceholder][serviceOrderId({}) 预约占位重试失败]", candidate.getId(), ex);
            }
        }
        log.info("[retryCreateBookingPlaceholder][candidates={} success={} failed={}]",
                candidates.size(), successCount, candidates.size() - successCount);
        return successCount;
    }

    private void transitionStatus(Long serviceOrderId, TradeServiceOrderStatusEnum expected,
                                  TradeServiceOrderStatusEnum target, String remark) {
        TradeServiceOrderDO serviceOrder = getRequiredServiceOrder(serviceOrderId);
        if (!ObjUtil.equal(serviceOrder.getStatus(), expected.getStatus())) {
            throw exception(SERVICE_ORDER_STATUS_ILLEGAL,
                    TradeServiceOrderStatusEnum.getNameByStatus(serviceOrder.getStatus()),
                    expected.getName());
        }
        TradeServiceOrderDO updateObj = TradeServiceOrderDO.builder()
                .status(target.getStatus())
                .remark(remark)
                .build();
        String snapshotAfterTransition = freezeBundleRefundSnapshotIfNeeded(serviceOrder, target);
        if (snapshotAfterTransition != null) {
            updateObj.setOrderItemSnapshotJson(snapshotAfterTransition);
        }
        int updateCount = tradeServiceOrderMapper.updateByIdAndStatus(serviceOrderId, expected.getStatus(),
                updateObj);
        if (updateCount == 0) {
            throw exception(SERVICE_ORDER_UPDATE_STATUS_FAIL);
        }
        log.info("[transitionStatus][serviceOrderId({}) status {} -> {}]", serviceOrderId,
                expected.getName(), target.getName());
    }

    private String freezeBundleRefundSnapshotIfNeeded(TradeServiceOrderDO serviceOrder, TradeServiceOrderStatusEnum target) {
        if (!ObjUtil.equal(target.getStatus(), TradeServiceOrderStatusEnum.FINISHED.getStatus())) {
            return null;
        }
        return freezeBundleRefundSnapshot(serviceOrder, serviceOrder == null ? null : serviceOrder.getOrderItemSnapshotJson());
    }

    private String freezeBundleRefundSnapshot(TradeServiceOrderDO serviceOrder, String orderItemSnapshotJson) {
        if (StrUtil.isBlank(orderItemSnapshotJson)) {
            return orderItemSnapshotJson;
        }
        try {
            JsonNode rootNode = JsonUtils.parseTree(orderItemSnapshotJson);
            if (!(rootNode instanceof ObjectNode)) {
                return orderItemSnapshotJson;
            }
            ObjectNode snapshotRoot = (ObjectNode) rootNode;
            boolean changed = freezeBundleSnapshotField(snapshotRoot, BUNDLE_REFUND_SNAPSHOT_KEY, serviceOrder);
            // 新字段：显式固化套餐子项快照，避免后续只依赖 priceSource 原始 JSON
            changed = freezeBundleSnapshotField(snapshotRoot, BUNDLE_ITEM_SNAPSHOT_KEY, serviceOrder) || changed;
            if (!changed) {
                return orderItemSnapshotJson;
            }
            return JsonUtils.toJsonString(snapshotRoot);
        } catch (RuntimeException ex) {
            log.warn("[freezeBundleRefundSnapshot][snapshot parse failed, keep original]", ex);
            return orderItemSnapshotJson;
        }
    }

    private boolean freezeBundleSnapshotField(ObjectNode snapshotRoot, String fieldKey, TradeServiceOrderDO serviceOrder) {
        ObjectNode bundleSnapshot = parseBundleRefundSnapshot(snapshotRoot.get(fieldKey));
        if (bundleSnapshot == null) {
            return false;
        }
        ArrayNode children = resolveChildrenArray(bundleSnapshot);
        if (children == null) {
            bundleSnapshot.put("bundleRefundablePrice", 0);
            snapshotRoot.put(fieldKey, JsonUtils.toJsonString(bundleSnapshot));
            return true;
        }
        freezeChildren(children, serviceOrder);
        bundleSnapshot.put("bundleRefundablePrice", calculateRefundablePrice(children));
        snapshotRoot.put(fieldKey, JsonUtils.toJsonString(bundleSnapshot));
        return true;
    }

    private ObjectNode parseBundleRefundSnapshot(JsonNode bundleNode) {
        if (bundleNode == null || bundleNode.isNull()) {
            return null;
        }
        JsonNode parsedNode = bundleNode;
        if (bundleNode.isTextual()) {
            String raw = bundleNode.asText();
            if (StrUtil.isBlank(raw)) {
                return null;
            }
            parsedNode = JsonUtils.parseTree(raw);
        }
        if (!(parsedNode instanceof ObjectNode)) {
            return null;
        }
        return (ObjectNode) parsedNode.deepCopy();
    }

    private ArrayNode resolveChildrenArray(ObjectNode bundleSnapshot) {
        JsonNode bundleChildren = bundleSnapshot.get("bundleChildren");
        if (bundleChildren instanceof ArrayNode) {
            return (ArrayNode) bundleChildren;
        }
        JsonNode children = bundleSnapshot.get("children");
        if (children instanceof ArrayNode) {
            return (ArrayNode) children;
        }
        JsonNode items = bundleSnapshot.get("items");
        if (items instanceof ArrayNode) {
            return (ArrayNode) items;
        }
        return null;
    }

    private void freezeChildren(ArrayNode children, TradeServiceOrderDO serviceOrder) {
        Set<String> matchCandidates = buildMatchCandidates(serviceOrder);
        List<Integer> frozenIndexes = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            JsonNode child = children.get(i);
            if (child instanceof ObjectNode && matchChild((ObjectNode) child, matchCandidates)) {
                frozenIndexes.add(i);
            }
        }
        if (frozenIndexes.isEmpty()) {
            if (children.size() == 1) {
                frozenIndexes.add(0);
            } else {
                for (int i = 0; i < children.size(); i++) {
                    frozenIndexes.add(i);
                }
            }
        }
        for (Integer index : frozenIndexes) {
            JsonNode node = children.get(index);
            if (!(node instanceof ObjectNode)) {
                continue;
            }
            ObjectNode childNode = (ObjectNode) node;
            childNode.put("fulfilled", true);
            childNode.put("refundable", false);
            childNode.put("refundCapPrice", 0);
        }
    }

    private Set<String> buildMatchCandidates(TradeServiceOrderDO serviceOrder) {
        Set<String> candidates = new HashSet<>();
        if (serviceOrder == null) {
            return candidates;
        }
        if (serviceOrder.getOrderItemId() != null) {
            candidates.add(String.valueOf(serviceOrder.getOrderItemId()));
        }
        if (serviceOrder.getSkuId() != null) {
            candidates.add(String.valueOf(serviceOrder.getSkuId()));
        }
        if (serviceOrder.getSpuId() != null) {
            candidates.add(String.valueOf(serviceOrder.getSpuId()));
        }
        return candidates;
    }

    private boolean matchChild(ObjectNode childNode, Set<String> matchCandidates) {
        if (CollUtil.isEmpty(matchCandidates)) {
            return false;
        }
        String[] keys = new String[] {"childCode", "code", "itemCode", "skuCode", "skuId", "spuId", "orderItemId"};
        for (String key : keys) {
            String value = toComparableValue(childNode.get(key));
            if (StrUtil.isNotBlank(value) && matchCandidates.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String toComparableValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return String.valueOf(node.longValue());
        }
        String text = StrUtil.trim(node.asText());
        return StrUtil.isBlank(text) ? null : text;
    }

    private int calculateRefundablePrice(ArrayNode children) {
        int sum = 0;
        for (JsonNode child : children) {
            if (!(child instanceof ObjectNode)) {
                continue;
            }
            ObjectNode childNode = (ObjectNode) child;
            Integer childRefundCap = normalizeNonNegative(readIntByAlias(childNode,
                    "refundCapPrice", "refundablePrice", "maxRefundPrice"));
            if (childRefundCap == null) {
                continue;
            }
            Boolean fulfilled = readBooleanByAlias(childNode, "fulfilled", "served", "completed", "serviceFulfilled");
            Boolean refundable = readBooleanByAlias(childNode, "refundable", "allowRefund");
            boolean included = Boolean.TRUE.equals(refundable) || !Boolean.TRUE.equals(fulfilled);
            if (included) {
                sum += childRefundCap;
            }
        }
        return Math.max(sum, 0);
    }

    private Integer normalizeNonNegative(Integer price) {
        if (price == null) {
            return null;
        }
        return Math.max(price, 0);
    }

    private Integer readIntByAlias(ObjectNode node, String... keys) {
        for (String key : keys) {
            JsonNode valueNode = node.get(key);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }
            if (valueNode.isNumber()) {
                return valueNode.intValue();
            }
            if (valueNode.isTextual() && StrUtil.isNotBlank(valueNode.asText())) {
                try {
                    return Integer.parseInt(valueNode.asText().trim());
                } catch (NumberFormatException ignore) {
                    // ignore invalid alias value
                }
            }
        }
        return null;
    }

    private Boolean readBooleanByAlias(ObjectNode node, String... keys) {
        for (String key : keys) {
            JsonNode valueNode = node.get(key);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }
            if (valueNode.isBoolean()) {
                return valueNode.booleanValue();
            }
            if (valueNode.isTextual() && StrUtil.isNotBlank(valueNode.asText())) {
                String text = valueNode.asText().trim();
                if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                    return false;
                }
            }
        }
        return null;
    }

    private TradeServiceOrderDO getRequiredServiceOrder(Long serviceOrderId) {
        TradeServiceOrderDO serviceOrder = tradeServiceOrderMapper.selectById(serviceOrderId);
        if (serviceOrder == null) {
            throw exception(SERVICE_ORDER_NOT_FOUND);
        }
        return serviceOrder;
    }

    private String mergeRemark(String action, String remark) {
        if (StrUtil.isBlank(remark)) {
            return StrUtil.maxLength(action, REMARK_MAX_LENGTH);
        }
        return StrUtil.maxLength(StrUtil.join(";", action, remark), REMARK_MAX_LENGTH);
    }

    private String normalizeBookingNo(String bookingNo) {
        return StrUtil.maxLength(StrUtil.blankToDefault(bookingNo, ""), BOOKING_NO_MAX_LENGTH);
    }

    private String buildOrderItemSnapshot(TradeOrderItemDO item) {
        String bundleItemSnapshotJson = extractBundleItemSnapshotJson(item.getPriceSourceSnapshotJson());
        ServiceOrderItemSnapshot snapshot = new ServiceOrderItemSnapshot();
        snapshot.setSnapshotVersion("v1");
        snapshot.setOrderItemId(item.getId());
        snapshot.setSpuId(item.getSpuId());
        snapshot.setSpuName(item.getSpuName());
        snapshot.setSkuId(item.getSkuId());
        snapshot.setProductType(item.getProductType());
        snapshot.setCount(item.getCount());
        snapshot.setPicUrl(item.getPicUrl());
        snapshot.setPrice(item.getPrice());
        snapshot.setDiscountPrice(item.getDiscountPrice());
        snapshot.setDeliveryPrice(item.getDeliveryPrice());
        snapshot.setAdjustPrice(item.getAdjustPrice());
        snapshot.setPayPrice(item.getPayPrice());
        snapshot.setAddonType(item.getAddonType());
        snapshot.setAddonSnapshotJson(item.getAddonSnapshotJson());
        snapshot.setTemplateVersionId(item.getTemplateVersionId());
        snapshot.setTemplateSnapshotJson(item.getTemplateSnapshotJson());
        snapshot.setPriceSourceSnapshotJson(item.getPriceSourceSnapshotJson());
        snapshot.setBundleItemSnapshotJson(ObjUtil.defaultIfNull(bundleItemSnapshotJson, item.getPriceSourceSnapshotJson()));
        snapshot.setBundleRefundSnapshotJson(ObjUtil.defaultIfNull(bundleItemSnapshotJson, item.getPriceSourceSnapshotJson()));
        snapshot.setProperties(item.getProperties() == null ? null : item.getProperties().stream()
                .map(property -> {
                    ServiceOrderItemSnapshot.PropertySnapshot x = new ServiceOrderItemSnapshot.PropertySnapshot();
                    x.setPropertyId(property.getPropertyId());
                    x.setPropertyName(property.getPropertyName());
                    x.setValueId(property.getValueId());
                    x.setValueName(property.getValueName());
                    return x;
                }).collect(Collectors.toList()));
        return JsonUtils.toJsonString(snapshot);
    }

    private String extractBundleItemSnapshotJson(String priceSourceSnapshotJson) {
        if (StrUtil.isBlank(priceSourceSnapshotJson)) {
            return null;
        }
        TradeBundleItemSnapshotBO bundleSnapshot = JsonUtils.parseObjectQuietly(priceSourceSnapshotJson,
                new TypeReference<TradeBundleItemSnapshotBO>() {});
        if (bundleSnapshot == null) {
            return null;
        }
        if (bundleSnapshot.getBundleRefundablePrice() == null && CollUtil.isEmpty(bundleSnapshot.getBundleChildren())) {
            return null;
        }
        return JsonUtils.toJsonString(bundleSnapshot);
    }

    @lombok.Data
    private static class ServiceOrderItemSnapshot {
        private String snapshotVersion;
        private Long orderItemId;
        private Long spuId;
        private String spuName;
        private Long skuId;
        private Integer productType;
        private Integer count;
        private String picUrl;
        private Integer price;
        private Integer discountPrice;
        private Integer deliveryPrice;
        private Integer adjustPrice;
        private Integer payPrice;
        private Integer addonType;
        private String addonSnapshotJson;
        private Long templateVersionId;
        private String templateSnapshotJson;
        private String priceSourceSnapshotJson;
        private String bundleItemSnapshotJson;
        private String bundleRefundSnapshotJson;
        private List<PropertySnapshot> properties;

        @lombok.Data
        private static class PropertySnapshot {
            private Long propertyId;
            private String propertyName;
            private Long valueId;
            private String valueName;
        }
    }

}
