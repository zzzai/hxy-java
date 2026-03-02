package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 门店分类列表 Request VO")
@Data
public class ProductStoreCategoryListReqVO {

    @Schema(description = "分类编码", example = "DIRECT")
    private String code;

    @Schema(description = "分类名称", example = "直营门店")
    private String name;
    @Schema(description = "父分类编号", example = "0")
    private Long parentId;
    @Schema(description = "层级", example = "1")
    private Integer level;

    @Schema(description = "状态：0 停用 1 启用", example = "1")
    private Integer status;
}
