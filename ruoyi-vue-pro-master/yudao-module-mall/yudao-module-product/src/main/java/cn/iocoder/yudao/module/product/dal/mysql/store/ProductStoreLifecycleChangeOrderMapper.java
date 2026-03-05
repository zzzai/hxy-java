package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleChangeOrderPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleChangeOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductStoreLifecycleChangeOrderMapper extends BaseMapperX<ProductStoreLifecycleChangeOrderDO> {

    Integer STATUS_PENDING = 10;

    default PageResult<ProductStoreLifecycleChangeOrderDO> selectPage(ProductStoreLifecycleChangeOrderPageReqVO reqVO) {
        LambdaQueryWrapperX<ProductStoreLifecycleChangeOrderDO> wrapper = new LambdaQueryWrapperX<ProductStoreLifecycleChangeOrderDO>()
                .likeIfPresent(ProductStoreLifecycleChangeOrderDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(ProductStoreLifecycleChangeOrderDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(ProductStoreLifecycleChangeOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ProductStoreLifecycleChangeOrderDO::getFromLifecycleStatus, reqVO.getFromLifecycleStatus())
                .eqIfPresent(ProductStoreLifecycleChangeOrderDO::getToLifecycleStatus, reqVO.getToLifecycleStatus())
                .likeIfPresent(ProductStoreLifecycleChangeOrderDO::getApplyOperator, reqVO.getApplyOperator())
                .eqIfPresent(ProductStoreLifecycleChangeOrderDO::getLastActionCode, reqVO.getLastActionCode())
                .likeIfPresent(ProductStoreLifecycleChangeOrderDO::getLastActionOperator, reqVO.getLastActionOperator())
                .betweenIfPresent(ProductStoreLifecycleChangeOrderDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductStoreLifecycleChangeOrderDO::getId);
        if (Boolean.TRUE.equals(reqVO.getOverdue())) {
            wrapper.eq(ProductStoreLifecycleChangeOrderDO::getStatus, STATUS_PENDING)
                    .isNotNull(ProductStoreLifecycleChangeOrderDO::getSlaDeadlineTime)
                    .lt(ProductStoreLifecycleChangeOrderDO::getSlaDeadlineTime, LocalDateTime.now());
        }
        return selectPage(reqVO, wrapper);
    }

    default ProductStoreLifecycleChangeOrderDO selectByOrderNo(String orderNo) {
        return selectOne(ProductStoreLifecycleChangeOrderDO::getOrderNo, orderNo);
    }

    default List<ProductStoreLifecycleChangeOrderDO> selectSlaExpiredPendingList(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<ProductStoreLifecycleChangeOrderDO>()
                .eq(ProductStoreLifecycleChangeOrderDO::getStatus, STATUS_PENDING)
                .isNotNull(ProductStoreLifecycleChangeOrderDO::getSlaDeadlineTime)
                .lt(ProductStoreLifecycleChangeOrderDO::getSlaDeadlineTime, now)
                .orderByAsc(ProductStoreLifecycleChangeOrderDO::getId)
                .last("LIMIT " + limit));
    }

    default int updateStatusByIdAndOldStatus(ProductStoreLifecycleChangeOrderDO updateObj, Integer oldStatus) {
        return update(null, new LambdaUpdateWrapper<ProductStoreLifecycleChangeOrderDO>()
                .eq(ProductStoreLifecycleChangeOrderDO::getId, updateObj.getId())
                .eq(ProductStoreLifecycleChangeOrderDO::getStatus, oldStatus)
                .set(updateObj.getStatus() != null, ProductStoreLifecycleChangeOrderDO::getStatus, updateObj.getStatus())
                .set(updateObj.getGuardSnapshotJson() != null, ProductStoreLifecycleChangeOrderDO::getGuardSnapshotJson,
                        updateObj.getGuardSnapshotJson())
                .set(updateObj.getGuardBlocked() != null, ProductStoreLifecycleChangeOrderDO::getGuardBlocked,
                        updateObj.getGuardBlocked())
                .set(updateObj.getGuardWarnings() != null, ProductStoreLifecycleChangeOrderDO::getGuardWarnings,
                        updateObj.getGuardWarnings())
                .set(updateObj.getApproveOperator() != null, ProductStoreLifecycleChangeOrderDO::getApproveOperator,
                        updateObj.getApproveOperator())
                .set(updateObj.getApproveRemark() != null, ProductStoreLifecycleChangeOrderDO::getApproveRemark,
                        updateObj.getApproveRemark())
                .set(updateObj.getApproveTime() != null, ProductStoreLifecycleChangeOrderDO::getApproveTime,
                        updateObj.getApproveTime())
                .set(updateObj.getSubmitTime() != null, ProductStoreLifecycleChangeOrderDO::getSubmitTime,
                        updateObj.getSubmitTime())
                .set(updateObj.getSlaDeadlineTime() != null, ProductStoreLifecycleChangeOrderDO::getSlaDeadlineTime,
                        updateObj.getSlaDeadlineTime())
                .set(updateObj.getLastActionCode() != null, ProductStoreLifecycleChangeOrderDO::getLastActionCode,
                        updateObj.getLastActionCode())
                .set(updateObj.getLastActionOperator() != null, ProductStoreLifecycleChangeOrderDO::getLastActionOperator,
                        updateObj.getLastActionOperator())
                .set(updateObj.getLastActionTime() != null, ProductStoreLifecycleChangeOrderDO::getLastActionTime,
                        updateObj.getLastActionTime()));
    }
}
