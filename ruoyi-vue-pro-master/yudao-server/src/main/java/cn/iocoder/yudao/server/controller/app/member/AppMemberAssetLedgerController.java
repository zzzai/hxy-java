package cn.iocoder.yudao.server.controller.app.member;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.server.controller.app.member.vo.AppMemberAssetLedgerPageReqVO;
import cn.iocoder.yudao.server.controller.app.member.vo.AppMemberAssetLedgerPageRespVO;
import cn.iocoder.yudao.server.service.member.AppMemberAssetLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 App - 会员统一资产台账")
@RestController
@RequestMapping("/member/asset-ledger")
@Validated
public class AppMemberAssetLedgerController {

    @Resource
    private AppMemberAssetLedgerService appMemberAssetLedgerService;

    @GetMapping("/page")
    @Operation(summary = "获得统一资产台账分页")
    public CommonResult<AppMemberAssetLedgerPageRespVO> getAssetLedgerPage(@Valid AppMemberAssetLedgerPageReqVO reqVO) {
        return success(appMemberAssetLedgerService.getAssetLedgerPage(getLoginUserId(), reqVO));
    }
}
