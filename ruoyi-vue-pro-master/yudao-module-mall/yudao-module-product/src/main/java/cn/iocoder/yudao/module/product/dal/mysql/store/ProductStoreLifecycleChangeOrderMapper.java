package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleChangeOrderPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleChangeOrderDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductStoreLifecycleChangeOrderMapper extends BaseMapperX<ProductStoreLifecycleChangeOrderDO> {

    default PageResult<ProductStoreLifecycleChangeOrderDO> selectPage(ProductStoreLifecycleChangeOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductStoreLifecycleChangeOrderDO>()
                .likeIfPresent(ProductStoreLifecycleChangeOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(ProductStoreLifecycleChangeOrderDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(ProductStoreLifecycleChangeOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ProductStoreLifecycleChangeOrderDO::getFromLifecycleStatus, reqVO.getFromLifecycleStatus())
                .eqIfPresent(ProductStoreLifecycleChangeOrderDO::getToLifecycleStatus, reqVO.getToLifecycleStatus())
                .likeIfPresent(ProductStoreLifecycleChangeOrderDO::getApplyOperator, reqVO.getApplyOperator())
                .betweenIfPresent(ProductStoreLifecycleChangeOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductStoreLifecycleChangeOrderDO::getId));
    }

    default ProductStoreLifecycleChangeOrderDO selectByOrderNo(String orderNo) {
        return selectOne(ProductStoreLifecycleChangeOrderDO::getOrderNo, orderNo);
    }
}
