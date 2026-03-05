package cn.iocoder.yudao.module.product.service.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleBatchLogDO;

public interface ProductStoreLifecycleBatchLogService {

    Long createLifecycleBatchLog(ProductStoreLifecycleBatchLogDO log);

    PageResult<ProductStoreLifecycleBatchLogDO> getLifecycleBatchLogPage(ProductStoreLifecycleBatchLogPageReqVO reqVO);

    ProductStoreLifecycleBatchLogDO getLifecycleBatchLog(Long id);

    ProductStoreLifecycleBatchLogDO getLatestLifecycleBatchLogByBatchNo(String batchNo);
}
