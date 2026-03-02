package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuStockFlowDO;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuStockFlowStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductStoreSkuStockFlowMapper extends BaseMapperX<ProductStoreSkuStockFlowDO> {

    default ProductStoreSkuStockFlowDO selectByBizKey(String bizType, String bizNo, Long storeId, Long skuId) {
        return selectOne(new LambdaQueryWrapperX<ProductStoreSkuStockFlowDO>()
                .eq(ProductStoreSkuStockFlowDO::getBizType, bizType)
                .eq(ProductStoreSkuStockFlowDO::getBizNo, bizNo)
                .eq(ProductStoreSkuStockFlowDO::getStoreId, storeId)
                .eq(ProductStoreSkuStockFlowDO::getSkuId, skuId));
    }

    default List<ProductStoreSkuStockFlowDO> selectRetryableList(LocalDateTime now, Integer limit) {
        LambdaQueryWrapperX<ProductStoreSkuStockFlowDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(ProductStoreSkuStockFlowDO::getStatus, 0, 2);
        wrapper.le(ProductStoreSkuStockFlowDO::getNextRetryTime, now);
        wrapper.orderByAsc(ProductStoreSkuStockFlowDO::getId);
        wrapper.last("LIMIT " + limit);
        return selectList(wrapper);
    }

    default int updateStatusByIdAndOldStatus(Long id, Integer oldStatus, Integer newStatus,
                                             Integer retryCount, LocalDateTime nextRetryTime, String lastErrorMsg) {
        LambdaUpdateWrapper<ProductStoreSkuStockFlowDO> updateWrapper = new LambdaUpdateWrapper<ProductStoreSkuStockFlowDO>()
                .eq(ProductStoreSkuStockFlowDO::getId, id)
                .eq(ProductStoreSkuStockFlowDO::getStatus, oldStatus)
                .set(ProductStoreSkuStockFlowDO::getStatus, newStatus)
                .set(ProductStoreSkuStockFlowDO::getRetryCount, retryCount)
                .set(ProductStoreSkuStockFlowDO::getNextRetryTime, nextRetryTime)
                .set(ProductStoreSkuStockFlowDO::getLastErrorMsg, lastErrorMsg)
                .set(ProductStoreSkuStockFlowDO::getExecuteTime, LocalDateTime.now());
        return update(null, updateWrapper);
    }

    default int markProcessingTimeoutAsFailed(LocalDateTime now, String lastErrorMsg) {
        LambdaUpdateWrapper<ProductStoreSkuStockFlowDO> updateWrapper = new LambdaUpdateWrapper<ProductStoreSkuStockFlowDO>()
                .eq(ProductStoreSkuStockFlowDO::getStatus, ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus())
                .le(ProductStoreSkuStockFlowDO::getNextRetryTime, now)
                .set(ProductStoreSkuStockFlowDO::getStatus, ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus())
                .setSql("retry_count = retry_count + 1")
                .set(ProductStoreSkuStockFlowDO::getNextRetryTime, now)
                .set(ProductStoreSkuStockFlowDO::getLastErrorMsg, lastErrorMsg)
                .set(ProductStoreSkuStockFlowDO::getExecuteTime, now);
        return update(null, updateWrapper);
    }

    default Long selectCountByStoreIdAndStatuses(Long storeId, Collection<Integer> statuses) {
        return selectCount(new LambdaQueryWrapperX<ProductStoreSkuStockFlowDO>()
                .eq(ProductStoreSkuStockFlowDO::getStoreId, storeId)
                .inIfPresent(ProductStoreSkuStockFlowDO::getStatus, statuses));
    }

}
