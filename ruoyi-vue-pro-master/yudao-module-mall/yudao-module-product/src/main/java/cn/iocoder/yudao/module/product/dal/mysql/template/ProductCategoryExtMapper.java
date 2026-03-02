package cn.iocoder.yudao.module.product.dal.mysql.template;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductCategoryExtDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductCategoryExtMapper extends BaseMapperX<ProductCategoryExtDO> {

    default ProductCategoryExtDO selectByCategoryId(Long categoryId) {
        return selectOne(ProductCategoryExtDO::getCategoryId, categoryId);
    }
}
