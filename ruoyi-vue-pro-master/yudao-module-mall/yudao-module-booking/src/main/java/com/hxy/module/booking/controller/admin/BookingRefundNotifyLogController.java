package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogReplayReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingRefundNotifyLogRespVO;
import com.hxy.module.booking.dal.dataobject.BookingRefundNotifyLogDO;
import com.hxy.module.booking.service.BookingRefundNotifyLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

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
    public CommonResult<Boolean> replay(@Valid @RequestBody BookingRefundNotifyLogReplayReqVO reqVO) {
        refundNotifyLogService.replayFailedLog(reqVO.getId(), getLoginUserId());
        return success(true);
    }
}
