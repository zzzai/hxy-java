package cn.iocoder.yudao.module.product.controller.admin.template.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - SKU 生成提交 Response VO")
@Data
public class ProductSkuGenerateCommitRespVO {

    private String taskNo;

    private Integer status;

    private Boolean accepted;

    private Boolean idempotentHit;
}
