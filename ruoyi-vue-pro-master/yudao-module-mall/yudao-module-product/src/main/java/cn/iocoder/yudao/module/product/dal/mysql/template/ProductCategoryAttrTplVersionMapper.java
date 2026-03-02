package cn.iocoder.yudao.module.product.dal.mysql.template;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductCategoryAttrTplVersionDO;
import cn.iocoder.yudao.module.product.enums.template.ProductTemplateConstants;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductCategoryAttrTplVersionMapper extends BaseMapperX<ProductCategoryAttrTplVersionDO> {

    default ProductCategoryAttrTplVersionDO selectPublishedByCategoryId(Long categoryId) {
        return selectOne(new LambdaQueryWrapperX<ProductCategoryAttrTplVersionDO>()
                .eq(ProductCategoryAttrTplVersionDO::getCategoryId, categoryId)
                .eq(ProductCategoryAttrTplVersionDO::getStatus, ProductTemplateConstants.TEMPLATE_STATUS_PUBLISHED)
                .orderByDesc(ProductCategoryAttrTplVersionDO::getVersionNo)
                .last("LIMIT 1"));
    }
}
