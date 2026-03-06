package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayDueReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundReplayRunLogRespVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunLogDO;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;

@Tag(name = "管理后台 - booking退款回调台账")
@RestController
@RequestMapping("/booking/refund-notify-log")
@Validated
public class BookingRefundNotifyLogController {

    @Resource
    private BookingRefundNotifyLogService refundNotifyLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询退款回调台账")
    @PreAuthorize("@ss.hasPermission('booking:refund-notify-log:query')")
    public CommonResult<PageResult<BookingRefundNotifyLogRespVO>> page(@Valid BookingRefundNotifyLogPageReqVO reqVO) {
        PageResult<BookingRefundNotifyLogDO> pageResult = refundNotifyLogService.getNotifyLogPage(reqVO);
        return success(BeanUtils.toBean(pageResult, BookingRefundNotifyLogRespVO.class));
    }

    @PostMapping("/replay")
    @Operation(summary = "重放失败退款回调台账")
    @PreAuthorize("@ss.hasPermission('booking:refund-notify-log:replay')")
    public CommonResult<BookingRefundNotifyLogReplayRespVO> replay(@Valid @RequestBody BookingRefundNotifyLogReplayReqVO reqVO) {
        BookingRefundNotifyLogReplayRespVO respVO = refundNotifyLogService.replayFailedLogs(
                reqVO.resolveReplayIds(), reqVO.dryRunEnabled(),
                getLoginUserId(), getLoginUserNickname());
        return success(respVO);
    }

    @PostMapping("/replay-due")
    @Operation(summary = "重放到期失败退款回调台账")
    @PreAuthorize("@ss.hasPermission('booking:refund-notify-log:replay')")
    public CommonResult<BookingRefundNotifyLogReplayRespVO> replayDue(
            @RequestBody(required = false) BookingRefundNotifyLogReplayDueReqVO reqVO) {
        BookingRefundNotifyLogReplayDueReqVO safeReqVO = reqVO == null ? new BookingRefundNotifyLogReplayDueReqVO() : reqVO;
        BookingRefundNotifyLogReplayRespVO respVO = refundNotifyLogService.replayDueFailedLogs(
                safeReqVO.getLimit(), safeReqVO.dryRunEnabled(),
                getLoginUserId(), getLoginUserNickname(), "MANUAL");
        return success(respVO);
    }

    @GetMapping("/replay-run-log/page")
    @Operation(summary = "分页查询退款重放批次台账")
    @PreAuthorize("@ss.hasPermission('booking:refund-notify-log:query')")
    public CommonResult<PageResult<BookingRefundReplayRunLogRespVO>> replayRunLogPage(
            @Valid BookingRefundReplayRunLogPageReqVO reqVO) {
        PageResult<BookingRefundReplayRunLogDO> pageResult = refundNotifyLogService.getReplayRunLogPage(reqVO);
        return success(BeanUtils.toBean(pageResult, BookingRefundReplayRunLogRespVO.class));
    }

    @GetMapping("/replay-run-log/get")
    @Operation(summary = "查询退款重放批次详情")
    @Parameter(name = "id", required = true, description = "批次台账ID")
    @PreAuthorize("@ss.hasPermission('booking:refund-notify-log:query')")
    public CommonResult<BookingRefundReplayRunLogRespVO> replayRunLogGet(@RequestParam("id") Long id) {
        BookingRefundReplayRunLogDO runLogDO = refundNotifyLogService.getReplayRunLog(id);
        return success(BeanUtils.toBean(runLogDO, BookingRefundReplayRunLogRespVO.class));
    }
}
