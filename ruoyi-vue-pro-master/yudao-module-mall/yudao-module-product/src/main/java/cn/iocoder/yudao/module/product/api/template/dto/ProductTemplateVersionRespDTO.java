package cn.iocoder.yudao.module.product.api.template.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 类目模板版本 DTO
 */
@Data
public class ProductTemplateVersionRespDTO implements Serializable {

    /**
     * 模板版本编号
     */
    private Long id;

    /**
     * 类目编号
     */
    private Long categoryId;

    /**
     * 模板状态
     */
    private Integer status;

    /**
     * 模板快照
     */
    private String snapshotJson;
}
