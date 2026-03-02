package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 门店 SPU 批量铺货 Request VO")
@Data
public class ProductStoreSpuBatchSaveReqVO {

    @Schema(description = "门店编号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1001,1002]")
    @NotEmpty(message = "门店编号列表不能为空")
    private List<Long> storeIds;

    @Schema(description = "总部 SPU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2001")
    @NotNull(message = "SPU 编号不能为空")
    private Long spuId;

    @Schema(description = "销售状态：0 上架 1 下架", example = "0")
    private Integer saleStatus;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "备注", example = "总部批量铺货")
    private String remark;
}

