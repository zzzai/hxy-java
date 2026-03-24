package cn.iocoder.yudao.module.promotion.controller.app.giftcard;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户 App - 礼品卡模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppGiftCardTemplatePageReqVO extends PageParam {

    @Schema(description = "模板状态", example = "ENABLE")
    private String status;
}
