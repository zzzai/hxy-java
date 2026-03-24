package cn.iocoder.yudao.server.controller.app.member.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户 App - 会员统一资产台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppMemberAssetLedgerPageReqVO extends PageParam {

    @Schema(description = "资产类型", example = "POINT")
    private String assetType;
}
