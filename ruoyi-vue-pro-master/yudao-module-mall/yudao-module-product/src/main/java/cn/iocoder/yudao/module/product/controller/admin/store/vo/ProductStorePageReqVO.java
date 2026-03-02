package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 门店分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProductStorePageReqVO extends PageParam {

    @Schema(description = "门店编码", example = "SH-001")
    private String code;

    @Schema(description = "门店名称", example = "荷小悦-上海徐汇店")
    private String name;

    @Schema(description = "门店简称", example = "徐汇店")
    private String shortName;

    @Schema(description = "分类编号", example = "10")
    private Long categoryId;

    @Schema(description = "门店状态：0 停用 1 启用", example = "1")
    private Integer status;
    @Schema(description = "生命周期状态：10筹备中 20试营业 30营业中 35停业 40闭店", example = "30")
    private Integer lifecycleStatus;

    @Schema(description = "联系电话", example = "13900000000")
    private String contactMobile;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;
}
