package cn.iocoder.yudao.module.product.service.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleRecheckLogDO;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreLifecycleRecheckLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

@Service
@Validated
public class ProductStoreLifecycleRecheckLogServiceImpl implements ProductStoreLifecycleRecheckLogService {

    @Resource
    private ProductStoreLifecycleRecheckLogMapper lifecycleRecheckLogMapper;

    @Override
    public Long createLifecycleRecheckLog(ProductStoreLifecycleRecheckLogDO log) {
        lifecycleRecheckLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public PageResult<ProductStoreLifecycleRecheckLogDO> getLifecycleRecheckLogPage(
            ProductStoreLifecycleRecheckLogPageReqVO reqVO) {
        return lifecycleRecheckLogMapper.selectPage(reqVO);
    }

    @Override
    public ProductStoreLifecycleRecheckLogDO getLifecycleRecheckLog(Long id) {
        if (id == null) {
            return null;
        }
        return lifecycleRecheckLogMapper.selectById(id);
    }
}
