package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 门店标签组新增/更新 Request VO")
@Data
public class ProductStoreTagGroupSaveReqVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "标签组编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "STORE_ATTR")
    @NotBlank(message = "标签组编码不能为空")
    private String code;

    @Schema(description = "标签组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "门店属性")
    @NotBlank(message = "标签组名称不能为空")
    private String name;

    @Schema(description = "是否必选：0 否 1 是", example = "1")
    private Integer required;

    @Schema(description = "是否互斥：0 否 1 是", example = "1")
    private Integer mutex;

    @Schema(description = "门店是否可编辑：0 否 1 是", example = "0")
    private Integer editableByStore;

    @Schema(description = "状态：0 停用 1 启用", example = "1")
    private Integer status;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "备注", example = "总部维护，门店不可修改")
    private String remark;
}
