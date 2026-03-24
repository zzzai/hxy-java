package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "用户 App - 礼品卡订单详情 Response VO")
@Data
@Accessors(chain = true)
public class AppGiftCardOrderRespVO {

    @Schema(description = "订单编号", example = "9001")
    private Long orderId;

    @Schema(description = "订单状态", example = "ISSUED")
    private String status;

    @Schema(description = "是否降级", example = "false")
    private Boolean degraded;

    @Schema(description = "礼品卡列表")
    private List<CardItem> cards;

    @Data
    @Accessors(chain = true)
    public static class CardItem {

        @Schema(description = "卡号", example = "GC1001")
        private String cardNo;

        @Schema(description = "卡状态", example = "ISSUED")
        private String status;

        @Schema(description = "受赠人会员编号", example = "66")
        private Long receiverMemberId;
    }
}
