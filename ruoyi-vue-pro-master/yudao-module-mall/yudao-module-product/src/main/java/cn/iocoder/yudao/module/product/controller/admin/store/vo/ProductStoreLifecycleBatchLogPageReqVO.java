package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 门店生命周期批量执行台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProductStoreLifecycleBatchLogPageReqVO extends PageParam {

    @Schema(description = "批次号", example = "LIFECYCLE-20260304")
    private String batchNo;

    @Schema(description = "目标生命周期状态", example = "35")
    private Integer targetLifecycleStatus;

    @Schema(description = "操作人", example = "运营同学")
    private String operator;

    @Schema(description = "来源", example = "ADMIN_UI")
    private String source;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "创建时间范围")
    private LocalDateTime[] createTime;
}
