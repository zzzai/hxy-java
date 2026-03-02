package cn.iocoder.yudao.module.product.dal.mysql.template;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductSkuGenerateTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProductSkuGenerateTaskMapper extends BaseMapperX<ProductSkuGenerateTaskDO> {

    default ProductSkuGenerateTaskDO selectByTaskNo(String taskNo) {
        return selectOne(ProductSkuGenerateTaskDO::getTaskNo, taskNo);
    }

    default ProductSkuGenerateTaskDO selectByIdempotency(Long spuId, Integer mode, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<ProductSkuGenerateTaskDO>()
                .eq(ProductSkuGenerateTaskDO::getSpuId, spuId)
                .eq(ProductSkuGenerateTaskDO::getMode, mode)
                .eq(ProductSkuGenerateTaskDO::getIdempotencyKey, idempotencyKey));
    }

    default List<ProductSkuGenerateTaskDO> selectRetryableList(LocalDateTime now, Integer limit) {
        return selectList(new LambdaQueryWrapperX<ProductSkuGenerateTaskDO>()
                .in(ProductSkuGenerateTaskDO::getStatus, 3, 4)
                .le(ProductSkuGenerateTaskDO::getNextRetryTime, now)
                .orderByAsc(ProductSkuGenerateTaskDO::getNextRetryTime, ProductSkuGenerateTaskDO::getId)
                .last("LIMIT " + limit));
    }
}
