package cn.iocoder.yudao.module.product.controller.admin.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - SKU 生成提交 Request VO")
@Data
public class ProductSkuGenerateCommitReqVO {

    @NotBlank(message = "预览任务号不能为空")
    private String taskNo;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
