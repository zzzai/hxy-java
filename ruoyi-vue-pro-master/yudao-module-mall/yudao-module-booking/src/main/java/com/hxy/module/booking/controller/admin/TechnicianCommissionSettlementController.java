package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import com.hxy.module.booking.controller.admin.vo.*;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementLogDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementNotifyOutboxDO;
import com.hxy.module.booking.enums.CommissionSettlementStatusEnum;
import com.hxy.module.booking.service.TechnicianCommissionSettlementService;
import com.hxy.module.booking.service.dto.TechnicianCommissionNotifyBatchRetryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 技师佣金结算单")
@RestController
@RequestMapping("/booking/commission-settlement")
@Validated
public class TechnicianCommissionSettlementController {

    @Resource
    private TechnicianCommissionSettlementService settlementService;

    @PostMapping("/create")
    @Operation(summary = "创建技师佣金结算单")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<Long> create(@Valid @RequestBody TechnicianCommissionSettlementCreateReqVO reqVO) {
        return success(settlementService.createSettlement(reqVO.getCommissionIds(), reqVO.getRemark()));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交技师佣金结算单审核")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<Boolean> submit(@Valid @RequestBody TechnicianCommissionSettlementSubmitReqVO reqVO) {
        settlementService.submitForReview(reqVO.getId(), reqVO.getSlaMinutes(), reqVO.getRemark());
        return success(true);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批通过")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<Boolean> approve(@Valid @RequestBody TechnicianCommissionSettlementApproveReqVO reqVO) {
        settlementService.approve(reqVO.getId(), getLoginUserId(), reqVO.getRemark());
        return success(true);
    }

    @PostMapping("/reject")
    @Operation(summary = "审批驳回")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<Boolean> reject(@Valid @RequestBody TechnicianCommissionSettlementRejectReqVO reqVO) {
        settlementService.reject(reqVO.getId(), getLoginUserId(), reqVO.getRejectReason());
        return success(true);
    }

    @PostMapping("/pay")
    @Operation(summary = "确认打款")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<Boolean> pay(@Valid @RequestBody TechnicianCommissionSettlementPayReqVO reqVO) {
        settlementService.markPaid(reqVO.getId(), getLoginUserId(), reqVO.getPayVoucherNo(), reqVO.getPayRemark());
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得结算单详情")
    @Parameter(name = "id", required = true, description = "结算单ID")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<TechnicianCommissionSettlementRespVO> get(@RequestParam("id") Long id) {
        TechnicianCommissionSettlementDO settlement = settlementService.getSettlement(id);
        return success(fillSlaStatus(BeanUtils.toBean(settlement, TechnicianCommissionSettlementRespVO.class)));
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得结算单")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<PageResult<TechnicianCommissionSettlementRespVO>> page(
            @Valid TechnicianCommissionSettlementPageReqVO pageReqVO) {
        PageResult<TechnicianCommissionSettlementDO> pageResult = settlementService.getSettlementPage(pageReqVO);
        PageResult<TechnicianCommissionSettlementRespVO> respPage =
                BeanUtils.toBean(pageResult, TechnicianCommissionSettlementRespVO.class);
        fillSlaStatus(respPage.getList());
        return success(respPage);
    }

    @GetMapping("/list")
    @Operation(summary = "获得结算单列表")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<List<TechnicianCommissionSettlementRespVO>> list(
            @RequestParam(value = "technicianId", required = false) Long technicianId,
            @RequestParam(value = "status", required = false) Integer status) {
        List<TechnicianCommissionSettlementDO> list = settlementService.getSettlementList(technicianId, status);
        return success(fillSlaStatus(BeanUtils.toBean(list, TechnicianCommissionSettlementRespVO.class)));
    }

    @GetMapping("/sla-overdue-list")
    @Operation(summary = "获得审核SLA超时的待审核结算单")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<List<TechnicianCommissionSettlementRespVO>> slaOverdueList(
            @RequestParam(value = "limit", required = false) Integer limit) {
        List<TechnicianCommissionSettlementDO> list = settlementService.getSlaOverduePendingList(limit);
        return success(fillSlaStatus(BeanUtils.toBean(list, TechnicianCommissionSettlementRespVO.class)));
    }

    @GetMapping("/log-list")
    @Operation(summary = "获得结算单操作日志")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<List<TechnicianCommissionSettlementLogRespVO>> logList(
            @RequestParam("settlementId") Long settlementId) {
        List<TechnicianCommissionSettlementLogDO> list = settlementService.getOperationLogList(settlementId);
        return success(BeanUtils.toBean(list, TechnicianCommissionSettlementLogRespVO.class));
    }

    @GetMapping("/notify-outbox-list")
    @Operation(summary = "获得结算单通知出站记录")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<List<TechnicianCommissionSettlementNotifyOutboxRespVO>> notifyOutboxList(
            @RequestParam(value = "settlementId", required = false) Long settlementId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "limit", required = false) Integer limit) {
        List<TechnicianCommissionSettlementNotifyOutboxDO> list =
                settlementService.getNotifyOutboxList(settlementId, status, limit);
        return success(BeanUtils.toBean(list, TechnicianCommissionSettlementNotifyOutboxRespVO.class));
    }

    @GetMapping("/notify-outbox-page")
    @Operation(summary = "分页获得结算单通知出站记录")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<PageResult<TechnicianCommissionSettlementNotifyOutboxRespVO>> notifyOutboxPage(
            @Valid TechnicianCommissionSettlementNotifyOutboxPageReqVO pageReqVO) {
        PageResult<TechnicianCommissionSettlementNotifyOutboxDO> pageResult =
                settlementService.getNotifyOutboxPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, TechnicianCommissionSettlementNotifyOutboxRespVO.class));
    }

    @PostMapping("/notify-outbox-retry")
    @Operation(summary = "人工重试通知出站记录")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<Integer> retryNotifyOutbox(
            @Valid @RequestBody TechnicianCommissionSettlementNotifyOutboxRetryReqVO reqVO) {
        return success(settlementService.retryNotifyOutbox(reqVO.getIds(), getLoginUserId(), reqVO.getReason()));
    }

    @PostMapping("/notify-outbox-batch-retry")
    @Operation(summary = "人工批量重试通知出站记录（返回明细）")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<TechnicianCommissionSettlementNotifyOutboxBatchRetryRespVO> batchRetryNotifyOutbox(
            @Valid @RequestBody TechnicianCommissionSettlementNotifyOutboxRetryReqVO reqVO) {
        TechnicianCommissionNotifyBatchRetryResult result =
                settlementService.retryNotifyOutboxBatch(reqVO.getIds(), getLoginUserId(), reqVO.getReason());
        return success(BeanUtils.toBean(result, TechnicianCommissionSettlementNotifyOutboxBatchRetryRespVO.class));
    }

    private List<TechnicianCommissionSettlementRespVO> fillSlaStatus(List<TechnicianCommissionSettlementRespVO> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        list.forEach(this::fillSlaStatus);
        return list;
    }

    private TechnicianCommissionSettlementRespVO fillSlaStatus(TechnicianCommissionSettlementRespVO vo) {
        if (vo == null) {
            return null;
        }
        vo.setOverdue(isOverdue(vo.getStatus(), vo.getReviewDeadlineTime()));
        return vo;
    }

    private boolean isOverdue(Integer status, LocalDateTime reviewDeadlineTime) {
        return CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus().equals(status)
                && reviewDeadlineTime != null
                && !reviewDeadlineTime.isAfter(LocalDateTime.now());
    }
}
