package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 门店 SKU 库存流水分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProductStoreSkuStockFlowPageReqVO extends PageParam {

    @Schema(description = "门店编号", example = "1001")
    private Long storeId;

    @Schema(description = "SKU 编号", example = "3001")
    private Long skuId;

    @Schema(description = "业务类型", example = "MANUAL_REPLENISH_IN")
    private String bizType;

    @Schema(description = "业务单号", example = "SUPPLY-20260303-001")
    private String bizNo;

    @Schema(description = "流水状态：0待执行 1成功 2失败 3执行中", example = "2")
    private Integer status;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "执行时间")
    private LocalDateTime[] executeTime;
}

