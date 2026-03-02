package cn.iocoder.yudao.module.product.dal.mysql.template;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductAttributeOptionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ProductAttributeOptionMapper extends BaseMapperX<ProductAttributeOptionDO> {

    default List<ProductAttributeOptionDO> selectListByAttributeIds(Collection<Long> attributeIds) {
        return selectList(new LambdaQueryWrapperX<ProductAttributeOptionDO>()
                .inIfPresent(ProductAttributeOptionDO::getAttributeId, attributeIds));
    }

    default List<ProductAttributeOptionDO> selectListByIds(Collection<Long> ids) {
        return selectBatchIds(ids);
    }
}
