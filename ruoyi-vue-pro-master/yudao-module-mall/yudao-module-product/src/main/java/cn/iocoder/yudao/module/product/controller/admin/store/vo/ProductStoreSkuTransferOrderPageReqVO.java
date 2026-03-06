package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 门店 SKU 跨店调拨单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProductStoreSkuTransferOrderPageReqVO extends PageParam {

    @Schema(description = "调拨单号", example = "STO-20260306")
    private String orderNo;

    @Schema(description = "源门店编号", example = "1001")
    private Long fromStoreId;

    @Schema(description = "目标门店编号", example = "1002")
    private Long toStoreId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "申请人", example = "运营同学")
    private String applyOperator;

    @Schema(description = "最后动作", example = "APPROVE")
    private String lastActionCode;

    @Schema(description = "最后动作操作人", example = "审批同学")
    private String lastActionOperator;

    @Schema(description = "创建时间", example = "[\"2026-03-06 00:00:00\", \"2026-03-06 23:59:59\"]")
    private LocalDateTime[] createTime;
}
