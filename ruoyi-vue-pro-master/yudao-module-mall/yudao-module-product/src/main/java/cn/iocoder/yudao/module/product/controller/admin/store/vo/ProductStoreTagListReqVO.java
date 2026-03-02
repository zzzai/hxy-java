package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 门店标签列表 Request VO")
@Data
public class ProductStoreTagListReqVO {

    @Schema(description = "标签编码", example = "BUSINESS_AREA")
    private String code;

    @Schema(description = "标签名称", example = "商圈店")
    private String name;
    @Schema(description = "标签组编号", example = "1")
    private Long groupId;

    @Schema(description = "标签组", example = "门店属性")
    private String groupName;

    @Schema(description = "状态：0 停用 1 启用", example = "1")
    private Integer status;
}
