package com.hxy.module.booking.service;

/**
 * 预约加钟/升级 Service 接口
 */
public interface BookingAddonService {

    /**
     * 加钟 — 延长当前技师服务时间
     *
     * @param parentOrderId 原订单ID
     * @param userId        用户ID
     * @param spuId         服务商品SPU ID
     * @param skuId         服务商品SKU ID
     * @return 加钟子订单ID
     */
    Long createExtendOrder(Long parentOrderId, Long userId, Long spuId, Long skuId);

    /**
     * 升级 — 更换为更高级服务（差价计算）
     *
     * @param parentOrderId 原订单ID
     * @param userId        用户ID
     * @param newSkuId      新SKU ID
     * @return 升级子订单ID
     */
    Long createUpgradeOrder(Long parentOrderId, Long userId, Long newSkuId);

    /**
     * 加项目 — 在当前服务基础上增加附加项目
     *
     * @param parentOrderId 原订单ID
     * @param userId        用户ID
     * @param addonSpuId    附加项目SPU ID
     * @param addonSkuId    附加项目SKU ID
     * @return 加项目子订单ID
     */
    Long createAddItemOrder(Long parentOrderId, Long userId, Long addonSpuId, Long addonSkuId);

}
