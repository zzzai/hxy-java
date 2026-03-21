package com.hxy.module.booking.controller.admin;

import cn.hutool.core.collection.CollUtil;
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
        return respVO;
    }
}
