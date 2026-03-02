package cn.iocoder.yudao.module.product.dal.mysql.template;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductCategoryAttrTplItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductCategoryAttrTplItemMapper extends BaseMapperX<ProductCategoryAttrTplItemDO> {

    default List<ProductCategoryAttrTplItemDO> selectListByTemplateVersionId(Long templateVersionId) {
        return selectList(new LambdaQueryWrapperX<ProductCategoryAttrTplItemDO>()
                .eq(ProductCategoryAttrTplItemDO::getTemplateVersionId, templateVersionId)
                .orderByAsc(ProductCategoryAttrTplItemDO::getSort, ProductCategoryAttrTplItemDO::getId));
    }
}
