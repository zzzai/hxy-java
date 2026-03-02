package cn.iocoder.yudao.module.product.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 门店新增/更新 Request VO")
@Data
public class ProductStoreSaveReqVO {

    @Schema(description = "主键", example = "1001")
    private Long id;

    @Schema(description = "门店编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SH-001")
    @NotBlank(message = "门店编码不能为空")
    private String code;

    @Schema(description = "门店名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "荷小悦-上海徐汇店")
    @NotBlank(message = "门店名称不能为空")
    private String name;

    @Schema(description = "门店简称", example = "徐汇店")
    private String shortName;

    @Schema(description = "分类编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "门店分类不能为空")
    private Long categoryId;

    @Schema(description = "门店状态：0 停用 1 启用", example = "1")
    private Integer status;
    @Schema(description = "生命周期状态：10筹备中 20试营业 30营业中 35停业 40闭店", example = "10")
    private Integer lifecycleStatus;

    @Schema(description = "联系人", example = "王店长")
    private String contactName;

    @Schema(description = "联系电话", example = "13900000000")
    private String contactMobile;

    @Schema(description = "省编码", example = "310000")
    private String provinceCode;

    @Schema(description = "市编码", example = "310100")
    private String cityCode;

    @Schema(description = "区编码", example = "310104")
    private String districtCode;

    @Schema(description = "详细地址", example = "漕溪北路18号")
    private String address;

    @Schema(description = "经度", example = "121.436")
    private Double longitude;

    @Schema(description = "纬度", example = "31.192")
    private Double latitude;

    @Schema(description = "营业开始时间", example = "10:00")
    private String openingTime;

    @Schema(description = "营业结束时间", example = "23:00")
    private String closingTime;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "备注", example = "直营旗舰店")
    private String remark;

    @Schema(description = "标签编号列表", example = "[101, 102]")
    private List<Long> tagIds;
}
