package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 门店分类 Response VO")
@Data
public class ProductStoreCategoryRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long id;

    @Schema(description = "分类编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "DIRECT")
    private String code;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "直营门店")
    private String name;
    @Schema(description = "父分类编号，0 表示一级分类", example = "0")
    private Long parentId;
    @Schema(description = "层级：1 一级分类 2 二级分类", example = "1")
    private Integer level;

    @Schema(description = "状态：0 停用 1 启用", example = "1")
    private Integer status;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "备注", example = "总部直营店")
    private String remark;
}
