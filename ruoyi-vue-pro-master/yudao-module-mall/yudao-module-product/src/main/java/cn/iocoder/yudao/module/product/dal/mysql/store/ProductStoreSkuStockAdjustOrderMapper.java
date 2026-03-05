package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuStockAdjustOrderPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuStockAdjustOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductStoreSkuStockAdjustOrderMapper extends BaseMapperX<ProductStoreSkuStockAdjustOrderDO> {

    default PageResult<ProductStoreSkuStockAdjustOrderDO> selectPage(ProductStoreSkuStockAdjustOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductStoreSkuStockAdjustOrderDO>()
                .likeIfPresent(ProductStoreSkuStockAdjustOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(ProductStoreSkuStockAdjustOrderDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(ProductStoreSkuStockAdjustOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ProductStoreSkuStockAdjustOrderDO::getBizType, reqVO.getBizType())
                .likeIfPresent(ProductStoreSkuStockAdjustOrderDO::getApplyOperator, reqVO.getApplyOperator())
                .eqIfPresent(ProductStoreSkuStockAdjustOrderDO::getLastActionCode, reqVO.getLastActionCode())
                .likeIfPresent(ProductStoreSkuStockAdjustOrderDO::getLastActionOperator, reqVO.getLastActionOperator())
                .betweenIfPresent(ProductStoreSkuStockAdjustOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductStoreSkuStockAdjustOrderDO::getId));
    }

    default ProductStoreSkuStockAdjustOrderDO selectByOrderNo(String orderNo) {
        return selectOne(ProductStoreSkuStockAdjustOrderDO::getOrderNo, orderNo);
    }

    default int updateStatusByIdAndOldStatus(ProductStoreSkuStockAdjustOrderDO updateObj, Integer oldStatus) {
        return update(null, new LambdaUpdateWrapper<ProductStoreSkuStockAdjustOrderDO>()
                .eq(ProductStoreSkuStockAdjustOrderDO::getId, updateObj.getId())
                .eq(ProductStoreSkuStockAdjustOrderDO::getStatus, oldStatus)
                .set(updateObj.getStatus() != null, ProductStoreSkuStockAdjustOrderDO::getStatus, updateObj.getStatus())
                .set(updateObj.getApproveOperator() != null, ProductStoreSkuStockAdjustOrderDO::getApproveOperator,
                        updateObj.getApproveOperator())
                .set(updateObj.getApproveRemark() != null, ProductStoreSkuStockAdjustOrderDO::getApproveRemark,
                        updateObj.getApproveRemark())
                .set(updateObj.getApproveTime() != null, ProductStoreSkuStockAdjustOrderDO::getApproveTime,
                        updateObj.getApproveTime())
                .set(updateObj.getLastActionCode() != null, ProductStoreSkuStockAdjustOrderDO::getLastActionCode,
                        updateObj.getLastActionCode())
                .set(updateObj.getLastActionOperator() != null, ProductStoreSkuStockAdjustOrderDO::getLastActionOperator,
                        updateObj.getLastActionOperator())
                .set(updateObj.getLastActionTime() != null, ProductStoreSkuStockAdjustOrderDO::getLastActionTime,
                        updateObj.getLastActionTime()));
    }
}
