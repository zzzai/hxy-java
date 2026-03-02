package cn.iocoder.yudao.module.product.api.template;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.api.template.dto.ProductTemplateVersionRespDTO;
import cn.iocoder.yudao.module.product.dal.dataobject.template.ProductCategoryAttrTplVersionDO;
import cn.iocoder.yudao.module.product.dal.mysql.template.ProductCategoryAttrTplVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 类目模板版本 API 实现
 */
@Service
@Validated
public class ProductTemplateVersionApiImpl implements ProductTemplateVersionApi {

    @Resource
    private ProductCategoryAttrTplVersionMapper templateVersionMapper;

    @Override
    public List<ProductTemplateVersionRespDTO> getTemplateVersionList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> normalizedIds = ids.stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ProductCategoryAttrTplVersionDO> versions = templateVersionMapper.selectBatchIds(normalizedIds);
        return BeanUtils.toBean(versions, ProductTemplateVersionRespDTO.class);
    }
}
