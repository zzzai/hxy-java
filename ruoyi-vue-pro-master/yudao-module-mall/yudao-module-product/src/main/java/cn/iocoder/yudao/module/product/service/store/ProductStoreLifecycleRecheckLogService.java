package cn.iocoder.yudao.module.product.service.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleRecheckLogDO;

public interface ProductStoreLifecycleRecheckLogService {

    Long createLifecycleRecheckLog(ProductStoreLifecycleRecheckLogDO log);

    PageResult<ProductStoreLifecycleRecheckLogDO> getLifecycleRecheckLogPage(ProductStoreLifecycleRecheckLogPageReqVO reqVO);

    ProductStoreLifecycleRecheckLogDO getLifecycleRecheckLog(Long id);
}
