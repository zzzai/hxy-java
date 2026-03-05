package cn.iocoder.yudao.module.product.service.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleBatchLogDO;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreLifecycleBatchLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

@Service
@Validated
public class ProductStoreLifecycleBatchLogServiceImpl implements ProductStoreLifecycleBatchLogService {

    @Resource
    private ProductStoreLifecycleBatchLogMapper lifecycleBatchLogMapper;

    @Override
    public Long createLifecycleBatchLog(ProductStoreLifecycleBatchLogDO log) {
        lifecycleBatchLogMapper.insert(log);
        return log.getId();
    }

    @Override
    public PageResult<ProductStoreLifecycleBatchLogDO> getLifecycleBatchLogPage(ProductStoreLifecycleBatchLogPageReqVO reqVO) {
        return lifecycleBatchLogMapper.selectPage(reqVO);
    }

    @Override
    public ProductStoreLifecycleBatchLogDO getLifecycleBatchLog(Long id) {
        if (id == null) {
            return null;
        }
        return lifecycleBatchLogMapper.selectById(id);
    }

    @Override
    public ProductStoreLifecycleBatchLogDO getLatestLifecycleBatchLogByBatchNo(String batchNo) {
        return lifecycleBatchLogMapper.selectLatestByBatchNo(batchNo);
    }
}
