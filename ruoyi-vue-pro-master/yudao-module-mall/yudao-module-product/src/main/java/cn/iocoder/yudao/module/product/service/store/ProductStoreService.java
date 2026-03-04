package cn.iocoder.yudao.module.product.service.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.*;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreCategoryDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagGroupDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 门店主数据 Service
 */
public interface ProductStoreService {

    Long saveStore(@Valid ProductStoreSaveReqVO reqVO);

    void deleteStore(Long id);

    ProductStoreDO getStore(Long id);

    Map<Long, ProductStoreDO> getStoreMap(Collection<Long> ids);

    PageResult<ProductStoreDO> getStorePage(ProductStorePageReqVO reqVO);

    List<ProductStoreOptionRespVO> getStoreOptions(String keyword);

    List<ProductStoreSimpleRespVO> getStoreSimpleList(String keyword);

    List<Long> getStoreTagIds(Long storeId);

    void validateStoreExists(Long id);

    Long saveCategory(@Valid ProductStoreCategorySaveReqVO reqVO);

    void deleteCategory(Long id);

    ProductStoreCategoryDO getCategory(Long id);

    List<ProductStoreCategoryDO> getCategoryList(ProductStoreCategoryListReqVO reqVO);

    Long saveTag(@Valid ProductStoreTagSaveReqVO reqVO);

    void deleteTag(Long id);

    ProductStoreTagDO getTag(Long id);

    List<ProductStoreTagDO> getTagList(ProductStoreTagListReqVO reqVO);

    Long saveTagGroup(@Valid ProductStoreTagGroupSaveReqVO reqVO);

    void deleteTagGroup(Long id);

    ProductStoreTagGroupDO getTagGroup(Long id);

    List<ProductStoreTagGroupDO> getTagGroupList(ProductStoreTagGroupListReqVO reqVO);

    void updateStoreLifecycle(Long id, Integer lifecycleStatus, String reason);

    ProductStoreLaunchReadinessRespVO getLaunchReadiness(Long id);

    void batchUpdateCategory(@Valid ProductStoreBatchCategoryReqVO reqVO);

    void batchUpdateTags(@Valid ProductStoreBatchTagReqVO reqVO);

    void batchUpdateLifecycle(@Valid ProductStoreBatchLifecycleReqVO reqVO);

    ProductStoreBatchLifecycleExecuteRespVO batchUpdateLifecycleWithResult(@Valid ProductStoreBatchLifecycleReqVO reqVO);
}
