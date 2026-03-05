package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleBatchLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductStoreLifecycleBatchLogMapper extends BaseMapperX<ProductStoreLifecycleBatchLogDO> {

    default PageResult<ProductStoreLifecycleBatchLogDO> selectPage(ProductStoreLifecycleBatchLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductStoreLifecycleBatchLogDO>()
                .likeIfPresent(ProductStoreLifecycleBatchLogDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(ProductStoreLifecycleBatchLogDO::getTargetLifecycleStatus, reqVO.getTargetLifecycleStatus())
                .likeIfPresent(ProductStoreLifecycleBatchLogDO::getOperator, reqVO.getOperator())
                .eqIfPresent(ProductStoreLifecycleBatchLogDO::getSource, reqVO.getSource())
                .betweenIfPresent(ProductStoreLifecycleBatchLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductStoreLifecycleBatchLogDO::getId));
    }

    default ProductStoreLifecycleBatchLogDO selectLatestByBatchNo(String batchNo) {
        return selectOne(new LambdaQueryWrapperX<ProductStoreLifecycleBatchLogDO>()
                .eqIfPresent(ProductStoreLifecycleBatchLogDO::getBatchNo, batchNo)
                .orderByDesc(ProductStoreLifecycleBatchLogDO::getId)
                .last("LIMIT 1"));
    }
}
