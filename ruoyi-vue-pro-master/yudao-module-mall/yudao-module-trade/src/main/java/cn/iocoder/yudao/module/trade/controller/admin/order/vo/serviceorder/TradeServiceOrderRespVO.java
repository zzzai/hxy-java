package cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 服务履约单 Response VO")
@Data
public class TradeServiceOrderRespVO {

    @Schema(description = "服务履约单编号", example = "1")
    private Long id;

    @Schema(description = "交易订单编号", example = "1001")
    private Long orderId;

    @Schema(description = "交易订单号", example = "T202602240001")
    private String orderNo;

    @Schema(description = "交易订单项编号", example = "2001")
    private Long orderItemId;

    @Schema(description = "用户编号", example = "3001")
    private Long userId;

    @Schema(description = "支付单编号", example = "4001")
    private Long payOrderId;

    @Schema(description = "商品 SPU 编号", example = "5001")
    private Long spuId;

    @Schema(description = "商品 SKU 编号", example = "6001")
    private Long skuId;

    @Schema(description = "服务加项类型：1=加钟 2=升级 3=加项目", example = "1")
    private Integer addonType;

    @Schema(description = "服务加项快照（JSON）", example = "{\"addonCode\":\"ADD_ON_HOT_STONE\"}")
    private String addonSnapshotJson;

    @Schema(description = "订单项快照（JSON）")
    private String orderItemSnapshotJson;

    @Schema(description = "履约状态", example = "10")
    private Integer status;

    @Schema(description = "履约状态名称", example = "待预约")
    private String statusName;

    @Schema(description = "来源", example = "PAY_CALLBACK")
    private String source;

    @Schema(description = "预约单号", example = "BOOK202602240001")
    private String bookingNo;

    @Schema(description = "备注", example = "支付成功自动创建，待预约")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
