package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 门店标签组列表 Request VO")
@Data
public class ProductStoreTagGroupListReqVO {

    @Schema(description = "标签组编码", example = "STORE_ATTR")
    private String code;

    @Schema(description = "标签组名称", example = "门店属性")
    private String name;

    @Schema(description = "状态：0 停用 1 启用", example = "1")
    private Integer status;
}
