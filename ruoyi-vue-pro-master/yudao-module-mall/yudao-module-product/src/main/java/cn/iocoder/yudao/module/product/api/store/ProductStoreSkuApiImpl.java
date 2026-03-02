package cn.iocoder.yudao.module.product.api.store;

import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuUpdateStockReqDTO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreMappingService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Map;

/**
 * 门店 SKU API 实现
 */
@Service
@Validated
public class ProductStoreSkuApiImpl implements ProductStoreSkuApi {

    @Resource
    private ProductStoreMappingService productStoreMappingService;

    @Override
    public Map<Long, ProductStoreSkuRespDTO> getStoreSkuMap(Long storeId, Collection<Long> skuIds) {
        return productStoreMappingService.getStoreSkuMap(storeId, skuIds);
    }

    @Override
    public void updateStoreSkuStock(ProductStoreSkuUpdateStockReqDTO updateStockReqDTO) {
        productStoreMappingService.updateStoreSkuStock(updateStockReqDTO);
    }
}

