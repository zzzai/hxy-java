package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 门店生命周期变更单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProductStoreLifecycleChangeOrderPageReqVO extends PageParam {

    @Schema(description = "变更单号", example = "LCO-20260305183000-ABCD1234")
    private String orderNo;

    @Schema(description = "门店 ID", example = "1001")
    private Long storeId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "变更前生命周期状态", example = "30")
    private Integer fromLifecycleStatus;

    @Schema(description = "目标生命周期状态", example = "35")
    private Integer toLifecycleStatus;

    @Schema(description = "申请人", example = "运营同学")
    private String applyOperator;

    @Schema(description = "是否超时（仅待审批且 slaDeadlineTime < now 生效）", example = "true")
    private Boolean overdue;

    @Schema(description = "最后动作编码", example = "SUBMIT")
    private String lastActionCode;

    @Schema(description = "最后动作操作人", example = "运营同学")
    private String lastActionOperator;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "创建时间范围")
    private LocalDateTime[] createTime;
}
