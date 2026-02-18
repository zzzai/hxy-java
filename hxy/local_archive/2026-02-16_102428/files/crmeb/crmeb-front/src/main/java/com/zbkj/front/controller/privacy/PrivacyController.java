package com.zbkj.front.controller.privacy;

import com.zbkj.common.model.privacy.DataDeletionTicket;
import com.zbkj.common.model.privacy.UserConsentRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.privacy.ConsentGrantRequest;
import com.zbkj.common.request.privacy.ConsentWithdrawRequest;
import com.zbkj.common.request.privacy.DataDeletionCancelRequest;
import com.zbkj.common.request.privacy.DataDeletionRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.privacy.DataDeletionTicketService;
import com.zbkj.service.service.privacy.UserConsentRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 隐私中心
 */
@RestController("FrontPrivacyController")
@RequestMapping("api/front/privacy")
@Api(tags = "隐私 -- 用户授权与删除")
public class PrivacyController {

    @Resource
    private UserService userService;
    @Resource
    private UserConsentRecordService userConsentRecordService;
    @Resource
    private DataDeletionTicketService dataDeletionTicketService;

    @ApiOperation(value = "授权")
    @RequestMapping(value = "/consent/grant", method = RequestMethod.POST)
    public CommonResult<UserConsentRecord> grantConsent(@RequestBody @Validated ConsentGrantRequest request) {
        Integer userId = userService.getUserId();
        if (userId == null || userId <= 0) {
            return CommonResult.failed("请先登录");
        }
        return CommonResult.success(userConsentRecordService.grant(userId, request));
    }

    @ApiOperation(value = "撤回授权")
    @RequestMapping(value = "/consent/withdraw", method = RequestMethod.POST)
    public CommonResult<String> withdrawConsent(@RequestBody @Validated ConsentWithdrawRequest request) {
        Integer userId = userService.getUserId();
        if (userId == null || userId <= 0) {
            return CommonResult.failed("请先登录");
        }
        if (userConsentRecordService.withdraw(userId, request.getConsentId())) {
            return CommonResult.success();
        }
        return CommonResult.failed("撤回失败或无权限");
    }

    @ApiOperation(value = "授权记录列表")
    @RequestMapping(value = "/consent/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<UserConsentRecord>> consentList(@Validated PageParamRequest pageParamRequest) {
        Integer userId = userService.getUserId();
        if (userId == null || userId <= 0) {
            return CommonResult.failed("请先登录");
        }
        return CommonResult.success(CommonPage.restPage(userConsentRecordService.getByUser(userId, pageParamRequest)));
    }

    @ApiOperation(value = "提交数据删除申请")
    @RequestMapping(value = "/deletion/request", method = RequestMethod.POST)
    public CommonResult<DataDeletionTicket> requestDeletion(@RequestBody @Validated DataDeletionRequest request) {
        Integer userId = userService.getUserId();
        if (userId == null || userId <= 0) {
            return CommonResult.failed("请先登录");
        }
        return CommonResult.success(dataDeletionTicketService.requestDeletion(userId, request));
    }

    @ApiOperation(value = "撤销数据删除申请")
    @RequestMapping(value = "/deletion/cancel", method = RequestMethod.POST)
    public CommonResult<String> cancelDeletion(@RequestBody @Validated DataDeletionCancelRequest request) {
        Integer userId = userService.getUserId();
        if (userId == null || userId <= 0) {
            return CommonResult.failed("请先登录");
        }
        if (dataDeletionTicketService.cancelDeletion(userId, request.getTicketId())) {
            return CommonResult.success();
        }
        return CommonResult.failed("撤销失败或工单不可撤销");
    }

    @ApiOperation(value = "我的数据删除工单")
    @RequestMapping(value = "/deletion/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<DataDeletionTicket>> deletionList(@Validated PageParamRequest pageParamRequest) {
        Integer userId = userService.getUserId();
        if (userId == null || userId <= 0) {
            return CommonResult.failed("请先登录");
        }
        return CommonResult.success(CommonPage.restPage(dataDeletionTicketService.getByUser(userId, pageParamRequest)));
    }
}

