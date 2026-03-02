package cn.iocoder.yudao.module.product.api.template;

import cn.iocoder.yudao.module.product.api.template.dto.ProductTemplateVersionRespDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * 类目模板版本 API
 */
public interface ProductTemplateVersionApi {

    /**
     * 批量查询模板版本
     *
     * @param ids 模板版本编号
     * @return 模板版本列表
     */
    List<ProductTemplateVersionRespDTO> getTemplateVersionList(Collection<Long> ids);

    /**
     * 批量查询模板版本 Map
     *
     * @param ids 模板版本编号
     * @return 模板版本 Map
     */
    default Map<Long, ProductTemplateVersionRespDTO> getTemplateVersionMap(Collection<Long> ids) {
        return convertMap(getTemplateVersionList(ids), ProductTemplateVersionRespDTO::getId);
    }

}
