package cn.iocoder.yudao.module.product.api.store;

import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuUpdateStockReqDTO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.Map;

/**
 * 门店 SKU 对外 API
 */
public interface ProductStoreSkuApi {

    /**
     * 根据门店 + SKU 集合，获取门店 SKU 覆写信息
     *
     * @param storeId 门店编号
     * @param skuIds  SKU 编号集合
     * @return key=skuId, value=门店 SKU 覆写信息
     */
    Map<Long, ProductStoreSkuRespDTO> getStoreSkuMap(Long storeId, Collection<Long> skuIds);

    /**
     * 更新门店 SKU 库存
     *
     * @param updateStockReqDTO 库存更新请求
     */
    void updateStoreSkuStock(@Valid ProductStoreSkuUpdateStockReqDTO updateStockReqDTO);
}

