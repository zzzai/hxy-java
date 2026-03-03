package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 门店 SKU 库存流水 Response VO")
@Data
public class ProductStoreSkuStockFlowRespVO {

    @Schema(description = "流水编号", example = "10001")
    private Long id;

    @Schema(description = "门店编号", example = "1001")
    private Long storeId;

    @Schema(description = "门店名称", example = "荷小悦-徐汇店")
    private String storeName;

    @Schema(description = "SKU 编号", example = "3001")
    private Long skuId;

    @Schema(description = "业务类型", example = "MANUAL_REPLENISH_IN")
    private String bizType;

    @Schema(description = "业务单号", example = "SUPPLY-20260303-001")
    private String bizNo;

    @Schema(description = "库存变化值", example = "10")
    private Integer incrCount;

    @Schema(description = "流水状态：0待执行 1成功 2失败 3执行中", example = "1")
    private Integer status;

    @Schema(description = "重试次数", example = "0")
    private Integer retryCount;

    @Schema(description = "下次重试时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "最近错误信息", example = "SKU_STOCK_NOT_ENOUGH")
    private String lastErrorMsg;

    @Schema(description = "最近执行时间")
    private LocalDateTime executeTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}

