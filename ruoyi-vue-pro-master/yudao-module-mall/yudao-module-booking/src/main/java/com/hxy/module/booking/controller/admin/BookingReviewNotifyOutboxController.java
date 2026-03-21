package com.hxy.module.booking.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxRetryReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewNotifyOutboxRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import com.hxy.module.booking.service.BookingReviewNotifyOutboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 预约服务评价通知出站")
@RestController
@RequestMapping("/booking/review/notify-outbox")
@Validated
public class BookingReviewNotifyOutboxController {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_BLOCKED_NO_OWNER = "BLOCKED_NO_OWNER";

    @Resource
    private BookingReviewNotifyOutboxService bookingReviewNotifyOutboxService;

    @GetMapping("/list")
    @Operation(summary = "获得预约服务评价通知出站记录")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<List<BookingReviewNotifyOutboxRespVO>> list(
            @RequestParam("reviewId") Long reviewId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", required = false) Integer limit) {
        List<BookingReviewNotifyOutboxDO> list =
                bookingReviewNotifyOutboxService.getNotifyOutboxList(reviewId, status, limit);
        return success(toRespList(list));
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得预约服务评价通知出站记录")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<PageResult<BookingReviewNotifyOutboxRespVO>> page(
            @Valid BookingReviewNotifyOutboxPageReqVO reqVO) {
        PageResult<BookingReviewNotifyOutboxDO> pageResult =
                bookingReviewNotifyOutboxService.getNotifyOutboxPage(reqVO);
        PageResult<BookingReviewNotifyOutboxRespVO> result = new PageResult<>();
        result.setTotal(pageResult.getTotal());
        result.setList(toRespList(pageResult.getList()));
        return success(result);
    }

    @PostMapping("/retry")
    @Operation(summary = "人工重试预约服务评价通知出站记录")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Integer> retry(@Valid @RequestBody BookingReviewNotifyOutboxRetryReqVO reqVO) {
        return success(bookingReviewNotifyOutboxService.retryNotifyOutbox(
                reqVO.getIds(), getLoginUserId(), reqVO.getReason()));
    }

    private List<BookingReviewNotifyOutboxRespVO> toRespList(List<BookingReviewNotifyOutboxDO> list) {
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toResp).collect(Collectors.toList());
    }

    private BookingReviewNotifyOutboxRespVO toResp(BookingReviewNotifyOutboxDO outbox) {
        BookingReviewNotifyOutboxRespVO respVO = BeanUtils.toBean(outbox, BookingReviewNotifyOutboxRespVO.class);
        respVO.setReviewId(outbox.getBizId());
        DiagnosticSnapshot diagnostic = buildDiagnostic(outbox);
        respVO.setDiagnosticCode(diagnostic.code);
        respVO.setDiagnosticLabel(diagnostic.label);
        respVO.setDiagnosticDetail(diagnostic.detail);
        respVO.setRepairHint(diagnostic.repairHint);
        respVO.setManualRetryAllowed(diagnostic.manualRetryAllowed);
        return respVO;
    }

    private DiagnosticSnapshot buildDiagnostic(BookingReviewNotifyOutboxDO outbox) {
        String status = StrUtil.blankToDefault(outbox.getStatus(), STATUS_PENDING);
        if (STATUS_PENDING.equals(status)) {
            return new DiagnosticSnapshot("READY_TO_DISPATCH", "待派发",
                    "通知意图已生成，等待派发任务处理。", "当前无需人工处理，继续观察派发结果。", Boolean.FALSE);
        }
        if (STATUS_SENT.equals(status)) {
            return new DiagnosticSnapshot("SEND_SUCCESS", "已发送",
                    "当前通知已完成发送。", "当前无需修复动作。", Boolean.FALSE);
        }
        if (STATUS_FAILED.equals(status)) {
            return new DiagnosticSnapshot("ACTIONABLE_FAILED", "发送失败，可人工重试",
                    "通知派发已失败，需要先排查最近错误后再人工重试。", "先修复发送异常，再执行人工重试。", Boolean.TRUE);
        }
        if (STATUS_BLOCKED_NO_OWNER.equals(status)) {
            return buildBlockedDiagnostic(outbox.getLastErrorMsg());
        }
        return new DiagnosticSnapshot("BLOCKED_UNKNOWN", "阻断原因待核",
                "当前通知意图处于阻断态，但尚未识别出明确阻断原因。", "需要先核查路由、账号和通道配置后再处理。", Boolean.FALSE);
    }

    private DiagnosticSnapshot buildBlockedDiagnostic(String lastErrorMsg) {
        String errorText = StrUtil.blankToDefault(lastErrorMsg, "");
        if (StrUtil.containsIgnoreCase(errorText, "NO_APP_ACCOUNT")) {
            return new DiagnosticSnapshot("BLOCKED_NO_APP_ACCOUNT", "缺店长 App 账号",
                    "当前门店已命中路由，但缺少可接收通知的店长 App 账号。", "需要先补齐店长 App 账号，再重新触发通知。", Boolean.FALSE);
        }
        if (StrUtil.containsIgnoreCase(errorText, "NO_WECOM_ACCOUNT")) {
            return new DiagnosticSnapshot("BLOCKED_NO_WECOM_ACCOUNT", "缺店长企微账号",
                    "当前门店已命中路由，但缺少可接收通知的店长企微账号。", "需要先补齐店长企微账号，再重新触发通知。", Boolean.FALSE);
        }
        if (StrUtil.containsIgnoreCase(errorText, "CHANNEL_DISABLED")) {
            return new DiagnosticSnapshot("BLOCKED_CHANNEL_DISABLED", "通知通道未启用",
                    "当前通知通道处于关闭或不可用状态。", "需要先恢复对应通知通道，再重新触发通知。", Boolean.FALSE);
        }
        if (StrUtil.containsIgnoreCase(errorText, "NO_OWNER")) {
            return new DiagnosticSnapshot("BLOCKED_NO_MANAGER_ROUTE", "缺店长路由",
                    "当前门店尚未核出有效店长路由，因此通知无法派发。", "需要先补齐门店到店长账号的有效路由关系", Boolean.FALSE);
        }
        return new DiagnosticSnapshot("BLOCKED_UNKNOWN", "阻断原因待核",
                "当前通知意图已被阻断，但尚未识别出标准化阻断类型。", "需要先核查门店路由、账号映射和通道配置。", Boolean.FALSE);
    }

    private static final class DiagnosticSnapshot {
        private final String code;
        private final String label;
        private final String detail;
        private final String repairHint;
        private final Boolean manualRetryAllowed;

        private DiagnosticSnapshot(String code, String label, String detail, String repairHint,
                                   Boolean manualRetryAllowed) {
            this.code = code;
            this.label = label;
            this.detail = detail;
            this.repairHint = repairHint;
            this.manualRetryAllowed = manualRetryAllowed;
        }
    }
}
