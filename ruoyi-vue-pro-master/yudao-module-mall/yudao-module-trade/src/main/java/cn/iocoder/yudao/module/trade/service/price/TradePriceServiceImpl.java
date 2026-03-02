package cn.iocoder.yudao.module.trade.service.price;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.member.api.level.dto.MemberLevelRespDTO;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import cn.iocoder.yudao.module.product.api.store.ProductStoreSkuApi;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.template.ProductTemplateVersionApi;
import cn.iocoder.yudao.module.product.api.template.dto.ProductTemplateVersionRespDTO;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.product.enums.template.ProductTemplateConstants;
import cn.iocoder.yudao.module.promotion.api.discount.DiscountActivityApi;
import cn.iocoder.yudao.module.promotion.api.discount.dto.DiscountProductRespDTO;
import cn.iocoder.yudao.module.promotion.api.reward.RewardActivityApi;
import cn.iocoder.yudao.module.promotion.api.reward.dto.RewardActivityMatchRespDTO;
import cn.iocoder.yudao.module.promotion.enums.common.PromotionTypeEnum;
import cn.iocoder.yudao.module.trade.controller.app.order.vo.AppTradeProductSettlementRespVO;
import cn.iocoder.yudao.module.trade.enums.delivery.DeliveryTypeEnum;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateReqBO;
import cn.iocoder.yudao.module.trade.service.price.bo.TradePriceCalculateRespBO;
import cn.iocoder.yudao.module.trade.service.price.calculator.TradeDiscountActivityPriceCalculator;
import cn.iocoder.yudao.module.trade.service.price.calculator.TradePriceCalculator;
import cn.iocoder.yudao.module.trade.service.price.calculator.TradePriceCalculatorHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SKU_NOT_EXISTS;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SKU_STOCK_NOT_ENOUGH;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.*;

/**
 * 价格计算 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class TradePriceServiceImpl implements TradePriceService {

    @Resource
    private ProductSkuApi productSkuApi;
    @Resource
    private ProductSpuApi productSpuApi;
    @Resource
    private ProductStoreSkuApi productStoreSkuApi;
    @Resource
    private DiscountActivityApi discountActivityApi;
    @Resource
    private RewardActivityApi rewardActivityApi;
    @Resource
    private ProductTemplateVersionApi productTemplateVersionApi;

    @Resource
    private List<TradePriceCalculator> priceCalculators;

    @Resource
    private TradeDiscountActivityPriceCalculator discountActivityPriceCalculator;

    @Override
    public TradePriceCalculateRespBO calculateOrderPrice(TradePriceCalculateReqBO calculateReqBO) {
        // 1.1 获得商品 SKU 数组
        List<ProductSkuRespDTO> skuList = getSkuList(calculateReqBO);
        // 1.2 获得商品 SPU 数组
        List<ProductSpuRespDTO> spuList = checkSpuList(skuList);
        // 1.3 校验服务/配送分流和模板版本快照约束
        validateDeliveryAndTemplate(calculateReqBO, skuList, spuList);
        // 1.3 校验库存
        checkSkuStock(calculateReqBO, skuList, spuList);

        // 2.1 计算价格
        TradePriceCalculateRespBO calculateRespBO = TradePriceCalculatorHelper
                .buildCalculateResp(calculateReqBO, spuList, skuList);
        priceCalculators.forEach(calculator -> calculator.calculate(calculateReqBO, calculateRespBO));
        // 2.2  如果最终支付金额小于等于 0，则抛出业务异常
        if (calculateReqBO.getPointActivityId() == null // 积分订单，允许支付金额为 0
                && calculateRespBO.getPrice().getPayPrice() <= 0) {
            log.error("[calculatePrice][价格计算不正确，请求 calculateReqDTO({})，结果 priceCalculate({})]",
                    calculateReqBO, calculateRespBO);
            throw exception(PRICE_CALCULATE_PAY_PRICE_ILLEGAL);
        }
        return calculateRespBO;
    }

    private List<ProductSkuRespDTO> getSkuList(TradePriceCalculateReqBO reqBO) {
        // 获得商品 SKU 数组
        Map<Long, Integer> skuIdCountMap = convertMap(reqBO.getItems(),
                TradePriceCalculateReqBO.Item::getSkuId, TradePriceCalculateReqBO.Item::getCount);
        List<ProductSkuRespDTO> skus = productSkuApi.getSkuList(skuIdCountMap.keySet());
        applyStoreSkuOverrideIfNecessary(reqBO, skus, skuIdCountMap);
        return skus;
    }

    private void checkSkuStock(TradePriceCalculateReqBO reqBO, List<ProductSkuRespDTO> skus,
                               List<ProductSpuRespDTO> spuList) {
        Map<Long, Integer> skuIdCountMap = convertMap(reqBO.getItems(),
                TradePriceCalculateReqBO.Item::getSkuId, TradePriceCalculateReqBO.Item::getCount);
        if (skus.size() != skuIdCountMap.size()) {
            throw exception(SKU_NOT_EXISTS);
        }
        Map<Long, ProductSpuRespDTO> spuMap = convertMap(spuList, ProductSpuRespDTO::getId);
        skus.forEach(sku -> {
            Integer count = skuIdCountMap.get(sku.getId());
            if (count == null) {
                throw exception(SKU_NOT_EXISTS);
            }
            ProductSpuRespDTO spu = spuMap.get(sku.getSpuId());
            if (spu != null && ProductTypeEnum.isService(spu.getProductType())) {
                return;
            }
            Integer stock = sku.getStock();
            if (stock == null || count > stock) {
                throw exception(SKU_STOCK_NOT_ENOUGH);
            }
        });
    }

    private void applyStoreSkuOverrideIfNecessary(TradePriceCalculateReqBO reqBO, List<ProductSkuRespDTO> skus,
                                                   Map<Long, Integer> skuIdCountMap) {
        if (!Objects.equals(reqBO.getDeliveryType(), DeliveryTypeEnum.PICK_UP.getType())
                || reqBO.getPickUpStoreId() == null) {
            return;
        }
        Map<Long, ProductStoreSkuRespDTO> storeSkuMap = productStoreSkuApi
                .getStoreSkuMap(reqBO.getPickUpStoreId(), skuIdCountMap.keySet());
        skus.forEach(sku -> {
            ProductStoreSkuRespDTO storeSku = storeSkuMap.get(sku.getId());
            if (storeSku == null || !Objects.equals(storeSku.getSaleStatus(), 0)) {
                throw exception(SKU_NOT_EXISTS);
            }
            if (storeSku.getSalePrice() != null) {
                sku.setPrice(storeSku.getSalePrice());
            }
            if (storeSku.getMarketPrice() != null) {
                sku.setMarketPrice(storeSku.getMarketPrice());
            }
            if (storeSku.getStock() != null) {
                sku.setStock(storeSku.getStock());
            }
        });
    }

    private void validateDeliveryAndTemplate(TradePriceCalculateReqBO reqBO, List<ProductSkuRespDTO> skus,
                                             List<ProductSpuRespDTO> spuList) {
        Map<Long, ProductSkuRespDTO> skuMap = convertMap(skus, ProductSkuRespDTO::getId);
        Map<Long, ProductSpuRespDTO> spuMap = convertMap(spuList, ProductSpuRespDTO::getId);
        Set<Long> templateVersionIds = convertSet(reqBO.getItems(), TradePriceCalculateReqBO.Item::getTemplateVersionId);
        Map<Long, ProductTemplateVersionRespDTO> templateVersionMap =
                CollUtil.isEmpty(templateVersionIds)
                        ? Collections.emptyMap()
                        : productTemplateVersionApi.getTemplateVersionMap(templateVersionIds);

        // 第一轮：逐条校验分流与快照必填
        reqBO.getItems().forEach(item -> {
            ProductSkuRespDTO sku = skuMap.get(item.getSkuId());
            if (sku == null) {
                return;
            }
            ProductSpuRespDTO spu = spuMap.get(sku.getSpuId());
            if (spu == null) {
                return;
            }
            if (Objects.equals(reqBO.getDeliveryType(), DeliveryTypeEnum.EXPRESS.getType())
                    && ProductTypeEnum.isService(spu.getProductType())) {
                throw exception(PRICE_CALCULATE_SERVICE_ITEM_EXPRESS_FORBIDDEN);
            }
            if (item.getTemplateVersionId() != null && StrUtil.isBlank(item.getTemplateSnapshotJson())) {
                ProductTemplateVersionRespDTO version = templateVersionMap.get(item.getTemplateVersionId());
                if (version != null && StrUtil.isNotBlank(version.getSnapshotJson())) {
                    item.setTemplateSnapshotJson(version.getSnapshotJson());
                } else {
                    throw exception(PRICE_CALCULATE_TEMPLATE_VERSION_SNAPSHOT_REQUIRED);
                }
            }
        });

        if (CollUtil.isEmpty(templateVersionIds)) {
            return;
        }

        // 第二轮：校验模板版本存在、已发布且类目一致
        reqBO.getItems().forEach(item -> {
            Long templateVersionId = item.getTemplateVersionId();
            if (templateVersionId == null) {
                return;
            }
            ProductTemplateVersionRespDTO templateVersion = templateVersionMap.get(templateVersionId);
            if (templateVersion == null) {
                throw exception(PRICE_CALCULATE_TEMPLATE_VERSION_NOT_FOUND);
            }
            if (!Objects.equals(templateVersion.getStatus(), ProductTemplateConstants.TEMPLATE_STATUS_PUBLISHED)) {
                throw exception(PRICE_CALCULATE_TEMPLATE_VERSION_NOT_PUBLISHED);
            }
            ProductSkuRespDTO sku = skuMap.get(item.getSkuId());
            if (sku == null) {
                return;
            }
            ProductSpuRespDTO spu = spuMap.get(sku.getSpuId());
            if (spu == null) {
                return;
            }
            if (!Objects.equals(templateVersion.getCategoryId(), spu.getCategoryId())) {
                throw exception(PRICE_CALCULATE_TEMPLATE_VERSION_CATEGORY_MISMATCH);
            }
        });
    }

    private List<ProductSpuRespDTO> checkSpuList(List<ProductSkuRespDTO> skuList) {
        return productSpuApi.validateSpuList(convertSet(skuList, ProductSkuRespDTO::getSpuId));
    }

    @Override
    public List<AppTradeProductSettlementRespVO> calculateProductPrice(Long userId, List<Long> spuIds) {
        // 1.1 获得 SPU 与 SKU 的映射
        List<ProductSkuRespDTO> allSkuList = productSkuApi.getSkuListBySpuId(spuIds);
        Map<Long, List<ProductSkuRespDTO>> spuIdAndSkuListMap = convertMultiMap(allSkuList, ProductSkuRespDTO::getSpuId);
        // 1.2 获得会员等级
        MemberLevelRespDTO level = discountActivityPriceCalculator.getMemberLevel(userId);
        // 1.3 获得限时折扣活动
        Map<Long, DiscountProductRespDTO> skuIdAndDiscountMap = convertMap(
                discountActivityApi.getMatchDiscountProductListBySkuIds(convertSet(allSkuList, ProductSkuRespDTO::getId)),
                DiscountProductRespDTO::getSkuId);
        // 1.4 获得满减送活动
       List<RewardActivityMatchRespDTO> rewardActivityMap = rewardActivityApi.getMatchRewardActivityListBySpuIds(spuIds);

        // 2. 价格计算
        return convertList(spuIds, spuId -> {
            AppTradeProductSettlementRespVO spuVO = new AppTradeProductSettlementRespVO().setSpuId(spuId);
            // 2.1 优惠价格
            List<ProductSkuRespDTO> skuList = spuIdAndSkuListMap.get(spuId);
            List<AppTradeProductSettlementRespVO.Sku> skuVOList = convertList(skuList, sku -> {
                AppTradeProductSettlementRespVO.Sku skuVO = new AppTradeProductSettlementRespVO.Sku()
                        .setId(sku.getId());
                TradePriceCalculateRespBO.OrderItem orderItem = new TradePriceCalculateRespBO.OrderItem()
                        .setPayPrice(sku.getPrice()).setCount(1);
                // 计算限时折扣的优惠价格
                DiscountProductRespDTO discountProduct = skuIdAndDiscountMap.get(sku.getId());
                Integer discountPrice = discountActivityPriceCalculator.calculateActivityPrice(discountProduct, orderItem);
                // 计算 VIP 优惠金额
                Integer vipPrice = discountActivityPriceCalculator.calculateVipPrice(level, orderItem);
                if (discountPrice <= 0 && vipPrice <= 0) {
                    return skuVO;
                }
                // 选择一个大的优惠
                if (discountPrice > vipPrice) {
                    return skuVO.setPromotionPrice(sku.getPrice() - discountPrice)
                            .setPromotionType(PromotionTypeEnum.DISCOUNT_ACTIVITY.getType())
                            .setPromotionId(discountProduct.getId()).setPromotionEndTime(discountProduct.getActivityEndTime());
                } else {
                    return skuVO.setPromotionPrice(sku.getPrice() - vipPrice)
                            .setPromotionType(PromotionTypeEnum.MEMBER_LEVEL.getType());
                }
            });
            spuVO.setSkus(skuVOList);
            // 2.2 满减送活动
            RewardActivityMatchRespDTO rewardActivity = CollUtil.findOne(rewardActivityMap,
                    activity -> CollUtil.contains(activity.getSpuIds(), spuId));
            spuVO.setRewardActivity(BeanUtils.toBean(rewardActivity, AppTradeProductSettlementRespVO.RewardActivity.class));
            return spuVO;
        });
    }

}
