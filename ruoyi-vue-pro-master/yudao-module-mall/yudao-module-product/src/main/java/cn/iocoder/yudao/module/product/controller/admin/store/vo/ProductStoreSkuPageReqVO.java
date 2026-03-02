package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 门店 SKU 映射分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProductStoreSkuPageReqVO extends PageParam {

    @Schema(description = "门店编号", example = "1001")
    private Long storeId;

    @Schema(description = "总部 SPU 编号", example = "2001")
    private Long spuId;

    @Schema(description = "总部 SKU 编号", example = "3001")
    private Long skuId;

    @Schema(description = "销售状态：0 上架 1 下架", example = "0")
    private Integer saleStatus;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;
}

