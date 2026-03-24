package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "用户 App - 礼品卡模板 Response VO")
@Data
@Accessors(chain = true)
public class AppGiftCardTemplateRespVO {

    @Schema(description = "模板编号", example = "101")
    private Long templateId;

    @Schema(description = "模板标题", example = "春日疗愈礼卡")
    private String title;

    @Schema(description = "面值（分）", example = "9900")
    private Integer faceValue;

    @Schema(description = "库存", example = "20")
    private Integer stock;

    @Schema(description = "有效天数", example = "30")
    private Integer validDays;
}
