package cn.iocoder.yudao.module.product.dal.mysql.template;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductAttributeDefinitionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ProductAttributeDefinitionMapper extends BaseMapperX<ProductAttributeDefinitionDO> {

    default List<ProductAttributeDefinitionDO> selectListByIds(Collection<Long> ids) {
        return selectBatchIds(ids);
    }
}
