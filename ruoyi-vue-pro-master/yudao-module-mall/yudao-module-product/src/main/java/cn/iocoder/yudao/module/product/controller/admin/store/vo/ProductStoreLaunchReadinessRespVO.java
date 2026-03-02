package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 门店上线门禁检查 Response VO")
@Data
public class ProductStoreLaunchReadinessRespVO {

    @Schema(description = "门店编号", example = "1001")
    private Long storeId;

    @Schema(description = "是否可上线", example = "true")
    private Boolean ready;

    @Schema(description = "不通过原因列表")
    private List<String> reasons;
}
