package cn.iocoder.yudao.module.product.dal.mysql.template;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductSkuGenerateTaskItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductSkuGenerateTaskItemMapper extends BaseMapperX<ProductSkuGenerateTaskItemDO> {

    default List<ProductSkuGenerateTaskItemDO> selectListByTaskId(Long taskId) {
        return selectList(new LambdaQueryWrapperX<ProductSkuGenerateTaskItemDO>()
                .eq(ProductSkuGenerateTaskItemDO::getTaskId, taskId)
                .orderByAsc(ProductSkuGenerateTaskItemDO::getId));
    }

    default List<ProductSkuGenerateTaskItemDO> selectListByTaskIdAndStatus(Long taskId, Integer status) {
        return selectList(new LambdaQueryWrapperX<ProductSkuGenerateTaskItemDO>()
                .eq(ProductSkuGenerateTaskItemDO::getTaskId, taskId)
                .eq(ProductSkuGenerateTaskItemDO::getStatus, status)
                .orderByAsc(ProductSkuGenerateTaskItemDO::getId));
    }
}
