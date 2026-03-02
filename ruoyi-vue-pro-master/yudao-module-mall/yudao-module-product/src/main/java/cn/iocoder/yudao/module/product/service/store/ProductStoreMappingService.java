package cn.iocoder.yudao.module.product.service.store;

import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuUpdateStockReqDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchAdjustReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuBatchSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuSaveReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSpuDO;

import java.util.Collection;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 总部商品到门店商品映射 Service
 */
public interface ProductStoreMappingService {

    Long saveStoreSpu(@Valid ProductStoreSpuSaveReqVO reqVO);

    Integer batchSaveStoreSpu(@Valid ProductStoreSpuBatchSaveReqVO reqVO);

    void deleteStoreSpu(Long id);

    ProductStoreSpuDO getStoreSpu(Long id);

    PageResult<ProductStoreSpuDO> getStoreSpuPage(ProductStoreSpuPageReqVO reqVO);

    List<ProductStoreOptionRespVO> getStoreOptions(String keyword);

    List<ProductStoreSpuOptionRespVO> getSpuOptions(Integer productType, String keyword);

    List<ProductStoreSkuOptionRespVO> getSkuOptions(Long spuId);

    Long saveStoreSku(@Valid ProductStoreSkuSaveReqVO reqVO);

    Integer batchSaveStoreSku(@Valid ProductStoreSkuBatchSaveReqVO reqVO);

    Integer batchAdjustStoreSku(@Valid ProductStoreSkuBatchAdjustReqVO reqVO);

    void deleteStoreSku(Long id);

    ProductStoreSkuDO getStoreSku(Long id);

    PageResult<ProductStoreSkuDO> getStoreSkuPage(ProductStoreSkuPageReqVO reqVO);

    Map<Long, ProductStoreSkuRespDTO> getStoreSkuMap(Long storeId, Collection<Long> skuIds);

    void updateStoreSkuStock(@Valid ProductStoreSkuUpdateStockReqDTO updateStockReqDTO);

    int retryStoreSkuStockFlow(Integer limit);
}
