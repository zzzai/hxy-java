package com.zbkj.admin.controller.privacy;

import com.zbkj.common.model.privacy.DataAccessTicket;
import com.zbkj.common.model.privacy.DataDeletionTicket;
import com.zbkj.common.model.privacy.FieldGovernanceCatalog;
import com.zbkj.common.model.privacy.LabelPolicy;
import com.zbkj.common.model.privacy.UserConsentRecord;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.privacy.DataAccessTicketApproveRequest;
import com.zbkj.common.request.privacy.DataAccessTicketCloseRequest;
import com.zbkj.common.request.privacy.DataAccessTicketCreateRequest;
import com.zbkj.common.request.privacy.DataAccessTicketRejectRequest;
import com.zbkj.common.request.privacy.LabelPolicyUpdateStatusRequest;
import com.zbkj.common.result.CommonResult;
import com.zbkj.common.utils.SecurityUtil;
import com.zbkj.common.vo.LoginUserVo;
import com.zbkj.service.service.privacy.DataAccessTicketService;
import com.zbkj.service.service.privacy.DataDeletionTicketService;
import com.zbkj.service.service.privacy.FieldGovernanceCatalogService;
import com.zbkj.service.service.privacy.LabelPolicyService;
import com.zbkj.service.service.privacy.UserConsentRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 数据治理与隐私合规控制器
 */
@RestController
@RequestMapping("api/admin/data/governance")
@Api(tags = "合规治理 -- 用户数据与标签上线拦截规则")
public class DataGovernanceController {

    @Resource
    private FieldGovernanceCatalogService fieldGovernanceCatalogService;
    @Resource
    private UserConsentRecordService userConsentRecordService;
    @Resource
    private DataAccessTicketService dataAccessTicketService;
    @Resource
    private DataDeletionTicketService dataDeletionTicketService;
    @Resource
    private LabelPolicyService labelPolicyService;

    @PreAuthorize("hasAuthority('admin:data:governance:field:list')")
    @ApiOperation(value = "字段治理目录")
    @RequestMapping(value = "/field/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<FieldGovernanceCatalog>> fieldList(@RequestParam(required = false) Integer necessityLevel,
                                                                      @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(CommonPage.restPage(
                fieldGovernanceCatalogService.getList(necessityLevel, pageParamRequest)
        ));
    }

    @PreAuthorize("hasAuthority('admin:data:governance:consent:list')")
    @ApiOperation(value = "授权记录列表")
    @RequestMapping(value = "/consent/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<UserConsentRecord>> consentList(@RequestParam(required = false) String scenarioCode,
                                                                   @RequestParam(required = false) Integer status,
                                                                   @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(CommonPage.restPage(
                userConsentRecordService.getAdminList(scenarioCode, status, pageParamRequest)
        ));
    }

    @PreAuthorize("hasAuthority('admin:data:governance:access-ticket:create')")
    @ApiOperation(value = "创建数据访问工单")
    @RequestMapping(value = "/access-ticket/create", method = RequestMethod.POST)
    public CommonResult<DataAccessTicket> createAccessTicket(@RequestBody @Validated DataAccessTicketCreateRequest request) {
        Integer adminId = currentAdminId();
        return CommonResult.success(dataAccessTicketService.createTicket(adminId, request));
    }

    @PreAuthorize("hasAuthority('admin:data:governance:access-ticket:approve')")
    @ApiOperation(value = "审批通过数据访问工单")
    @RequestMapping(value = "/access-ticket/approve", method = RequestMethod.POST)
    public CommonResult<String> approveAccessTicket(@RequestBody @Validated DataAccessTicketApproveRequest request) {
        Integer adminId = currentAdminId();
        if (dataAccessTicketService.approve(request.getTicketId(), adminId)) {
            return CommonResult.success();
        }
        return CommonResult.failed("审批失败");
    }

    @PreAuthorize("hasAuthority('admin:data:governance:access-ticket:reject')")
    @ApiOperation(value = "驳回数据访问工单")
    @RequestMapping(value = "/access-ticket/reject", method = RequestMethod.POST)
    public CommonResult<String> rejectAccessTicket(@RequestBody @Validated DataAccessTicketRejectRequest request) {
        Integer adminId = currentAdminId();
        if (dataAccessTicketService.reject(request.getTicketId(), adminId, request.getRejectReason())) {
            return CommonResult.success();
        }
        return CommonResult.failed("驳回失败");
    }

    @PreAuthorize("hasAuthority('admin:data:governance:access-ticket:close')")
    @ApiOperation(value = "关闭数据访问工单")
    @RequestMapping(value = "/access-ticket/close", method = RequestMethod.POST)
    public CommonResult<String> closeAccessTicket(@RequestBody @Validated DataAccessTicketCloseRequest request) {
        Integer adminId = currentAdminId();
        if (dataAccessTicketService.closeTicket(request.getTicketId(), adminId)) {
            return CommonResult.success();
        }
        return CommonResult.failed("关闭失败");
    }

    @PreAuthorize("hasAuthority('admin:data:governance:access-ticket:list')")
    @ApiOperation(value = "数据访问工单列表")
    @RequestMapping(value = "/access-ticket/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<DataAccessTicket>> accessTicketList(@RequestParam(required = false) Integer status,
                                                                       @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(CommonPage.restPage(
                dataAccessTicketService.getList(status, pageParamRequest)
        ));
    }

    @PreAuthorize("hasAuthority('admin:data:governance:deletion:list')")
    @ApiOperation(value = "数据删除工单列表")
    @RequestMapping(value = "/deletion/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<DataDeletionTicket>> deletionList(@RequestParam(required = false) Integer status,
                                                                     @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(CommonPage.restPage(
                dataDeletionTicketService.getList(status, pageParamRequest)
        ));
    }

    @PreAuthorize("hasAuthority('admin:data:governance:label-policy:list')")
    @ApiOperation(value = "标签策略列表")
    @RequestMapping(value = "/label-policy/list", method = RequestMethod.GET)
    public CommonResult<CommonPage<LabelPolicy>> labelPolicyList(@RequestParam(required = false) Integer riskLevel,
                                                                 @RequestParam(required = false) Integer enabled,
                                                                 @Validated PageParamRequest pageParamRequest) {
        return CommonResult.success(CommonPage.restPage(
                labelPolicyService.getList(riskLevel, enabled, pageParamRequest)
        ));
    }

    @PreAuthorize("hasAuthority('admin:data:governance:label-policy:update-status')")
    @ApiOperation(value = "更新标签策略状态")
    @RequestMapping(value = "/label-policy/update-status", method = RequestMethod.POST)
    public CommonResult<String> updateLabelPolicyStatus(@RequestBody @Validated LabelPolicyUpdateStatusRequest request) {
        if (labelPolicyService.updateStatus(request.getId(), request.getEnabled(), request.getRemarks())) {
            return CommonResult.success();
        }
        return CommonResult.failed("更新失败");
    }

    private Integer currentAdminId() {
        LoginUserVo loginUserVo = SecurityUtil.getLoginUserVo();
        return loginUserVo.getUser().getId();
    }
}

