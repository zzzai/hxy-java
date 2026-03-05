package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleRecheckLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductStoreLifecycleRecheckLogMapper extends BaseMapperX<ProductStoreLifecycleRecheckLogDO> {

    default PageResult<ProductStoreLifecycleRecheckLogDO> selectPage(ProductStoreLifecycleRecheckLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductStoreLifecycleRecheckLogDO>()
                .likeIfPresent(ProductStoreLifecycleRecheckLogDO::getRecheckNo, reqVO.getRecheckNo())
                .eqIfPresent(ProductStoreLifecycleRecheckLogDO::getLogId, reqVO.getLogId())
                .likeIfPresent(ProductStoreLifecycleRecheckLogDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(ProductStoreLifecycleRecheckLogDO::getTargetLifecycleStatus, reqVO.getTargetLifecycleStatus())
                .likeIfPresent(ProductStoreLifecycleRecheckLogDO::getOperator, reqVO.getOperator())
                .eqIfPresent(ProductStoreLifecycleRecheckLogDO::getSource, reqVO.getSource())
                .betweenIfPresent(ProductStoreLifecycleRecheckLogDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductStoreLifecycleRecheckLogDO::getId));
    }
}
