package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.trade.api.reviewticket.TradeReviewTicketApi;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryQueryReqDTO;
import cn.iocoder.yudao.module.trade.api.reviewticket.dto.TradeReviewTicketSummaryRespDTO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcilePageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileRunReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditPageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditSyncReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountRefundCommissionAuditSyncRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;
import com.hxy.module.booking.service.FourAccountReconcileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;

@Tag(name = "管理后台 - 四账对账")
@RestController
@RequestMapping("/booking/four-account-reconcile")
@Validated
@Slf4j
public class FourAccountReconcileController {

    private static final Integer REVIEW_TICKET_TYPE = 40;
    private static final String REVIEW_TICKET_SOURCE_PREFIX = "FOUR_ACCOUNT_RECONCILE:";

    @Resource
    private FourAccountReconcileService reconcileService;
    @Resource
    private TradeReviewTicketApi tradeReviewTicketApi;

    @GetMapping("/page")
    @Operation(summary = "分页查询四账对账记录")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<PageResult<FourAccountReconcileRespVO>> page(@Valid FourAccountReconcilePageReqVO reqVO) {
        PageResult<FourAccountReconcileDO> pageResult = reconcileService.getReconcilePage(reqVO);
        if (pageResult == null) {
            return success(new PageResult<>(Collections.emptyList(), 0L));
        }
        List<FourAccountReconcileDO> pageList = pageResult.getList() == null
                ? Collections.emptyList() : pageResult.getList();
        List<FourAccountReconcileRespVO> records = BeanUtils.toBean(pageList, FourAccountReconcileRespVO.class);
        bindRelatedTicketSummary(records);
        return success(new PageResult<>(records, pageResult.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获得四账对账详情")
    @Parameter(name = "id", required = true, description = "对账记录ID")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<FourAccountReconcileRespVO> get(@RequestParam("id") Long id) {
        FourAccountReconcileDO reconcile = reconcileService.getReconcile(id);
        if (reconcile == null) {
            return success(null);
        }
        FourAccountReconcileRespVO respVO = BeanUtils.toBean(reconcile, FourAccountReconcileRespVO.class);
        bindRelatedTicketSummary(Collections.singletonList(respVO));
        return success(respVO);
    }

    @PostMapping("/run")
    @Operation(summary = "手工触发四账对账")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<Long> run(@Valid @RequestBody FourAccountReconcileRunReqVO reqVO) {
        return success(reconcileService.runReconcile(reqVO.getBizDate(), reqVO.getSource(), resolveOperator()));
    }

    @GetMapping("/summary")
    @Operation(summary = "四账对账汇总")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<FourAccountReconcileSummaryRespVO> summary(@Valid FourAccountReconcileSummaryReqVO reqVO) {
        return success(reconcileService.getReconcileSummary(reqVO));
    }

    @GetMapping("/refund-commission-audit-page")
    @Operation(summary = "退款-提成联调巡检分页")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<PageResult<FourAccountRefundCommissionAuditRespVO>> refundCommissionAuditPage(
            @Valid FourAccountRefundCommissionAuditPageReqVO reqVO) {
        return success(reconcileService.getRefundCommissionAuditPage(reqVO));
    }

    @PostMapping("/refund-commission-audit/sync-tickets")
    @Operation(summary = "退款-提成巡检异常同步统一工单")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<FourAccountRefundCommissionAuditSyncRespVO> syncRefundCommissionAuditTickets(
            @Valid @RequestBody FourAccountRefundCommissionAuditSyncReqVO reqVO) {
        return success(reconcileService.syncRefundCommissionAuditTickets(reqVO));
    }

    private String resolveOperator() {
        String nickname = getLoginUserNickname();
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname.trim();
        }
        Long userId = getLoginUserId();
        return userId == null ? null : String.valueOf(userId);
    }

    private void bindRelatedTicketSummary(List<FourAccountReconcileRespVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        records.forEach(this::fillSourceBizNo);
        List<String> sourceBizNos = records.stream()
                .map(FourAccountReconcileRespVO::getSourceBizNo)
                .filter(sourceBizNo -> sourceBizNo != null && !sourceBizNo.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (sourceBizNos.isEmpty()) {
            return;
        }
        List<TradeReviewTicketSummaryRespDTO> ticketSummaries;
        try {
            ticketSummaries = tradeReviewTicketApi.listLatestTicketSummaryBySourceBizNos(
                    new TradeReviewTicketSummaryQueryReqDTO()
                            .setTicketType(REVIEW_TICKET_TYPE)
                            .setSourceBizNos(sourceBizNos));
        } catch (Exception ex) {
            log.warn("[bindRelatedTicketSummary][trade summary degrade, sourceBizNos={}]", sourceBizNos, ex);
            return;
        }
        Map<String, TradeReviewTicketSummaryRespDTO> ticketMap = ticketSummaries == null ? Collections.emptyMap()
                : ticketSummaries.stream().filter(item -> item.getSourceBizNo() != null)
                .collect(Collectors.toMap(TradeReviewTicketSummaryRespDTO::getSourceBizNo,
                        Function.identity(), (left, right) -> left));
        records.forEach(record -> {
            TradeReviewTicketSummaryRespDTO ticket = ticketMap.get(record.getSourceBizNo());
            if (ticket == null) {
                return;
            }
            record.setRelatedTicketId(ticket.getId());
            record.setRelatedTicketStatus(ticket.getStatus());
            record.setRelatedTicketSeverity(ticket.getSeverity());
        });
    }

    private void fillSourceBizNo(FourAccountReconcileRespVO record) {
        if (record == null) {
            return;
        }
        record.setSourceBizNo(buildReviewTicketSourceBizNo(record.getBizDate()));
    }

    private String buildReviewTicketSourceBizNo(LocalDate bizDate) {
        return bizDate == null ? null : REVIEW_TICKET_SOURCE_PREFIX + bizDate;
    }
}
