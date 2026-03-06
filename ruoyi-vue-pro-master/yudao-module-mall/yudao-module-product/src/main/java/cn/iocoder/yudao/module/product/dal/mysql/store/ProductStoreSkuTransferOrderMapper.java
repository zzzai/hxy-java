package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuTransferOrderPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuTransferOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductStoreSkuTransferOrderMapper extends BaseMapperX<ProductStoreSkuTransferOrderDO> {

    default PageResult<ProductStoreSkuTransferOrderDO> selectPage(ProductStoreSkuTransferOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductStoreSkuTransferOrderDO>()
                .likeIfPresent(ProductStoreSkuTransferOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(ProductStoreSkuTransferOrderDO::getFromStoreId, reqVO.getFromStoreId())
                .eqIfPresent(ProductStoreSkuTransferOrderDO::getToStoreId, reqVO.getToStoreId())
                .eqIfPresent(ProductStoreSkuTransferOrderDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ProductStoreSkuTransferOrderDO::getApplyOperator, reqVO.getApplyOperator())
                .eqIfPresent(ProductStoreSkuTransferOrderDO::getLastActionCode, reqVO.getLastActionCode())
                .likeIfPresent(ProductStoreSkuTransferOrderDO::getLastActionOperator, reqVO.getLastActionOperator())
                .betweenIfPresent(ProductStoreSkuTransferOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductStoreSkuTransferOrderDO::getId));
    }

    default ProductStoreSkuTransferOrderDO selectByOrderNo(String orderNo) {
        return selectOne(ProductStoreSkuTransferOrderDO::getOrderNo, orderNo);
    }

    default int updateStatusByIdAndOldStatus(ProductStoreSkuTransferOrderDO updateObj, Integer oldStatus) {
        return update(null, new LambdaUpdateWrapper<ProductStoreSkuTransferOrderDO>()
                .eq(ProductStoreSkuTransferOrderDO::getId, updateObj.getId())
                .eq(ProductStoreSkuTransferOrderDO::getStatus, oldStatus)
                .set(updateObj.getStatus() != null, ProductStoreSkuTransferOrderDO::getStatus, updateObj.getStatus())
                .set(updateObj.getApproveOperator() != null, ProductStoreSkuTransferOrderDO::getApproveOperator,
                        updateObj.getApproveOperator())
                .set(updateObj.getApproveRemark() != null, ProductStoreSkuTransferOrderDO::getApproveRemark,
                        updateObj.getApproveRemark())
                .set(updateObj.getApproveTime() != null, ProductStoreSkuTransferOrderDO::getApproveTime,
                        updateObj.getApproveTime())
                .set(updateObj.getLastActionCode() != null, ProductStoreSkuTransferOrderDO::getLastActionCode,
                        updateObj.getLastActionCode())
                .set(updateObj.getLastActionOperator() != null, ProductStoreSkuTransferOrderDO::getLastActionOperator,
                        updateObj.getLastActionOperator())
                .set(updateObj.getLastActionTime() != null, ProductStoreSkuTransferOrderDO::getLastActionTime,
                        updateObj.getLastActionTime()));
    }
}
