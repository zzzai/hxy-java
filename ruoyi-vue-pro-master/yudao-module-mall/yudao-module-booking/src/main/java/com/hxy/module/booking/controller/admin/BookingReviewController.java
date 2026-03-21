package com.hxy.module.booking.controller.admin;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import com.hxy.module.booking.controller.admin.vo.BookingReviewDashboardRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewFollowUpdateReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanItemRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewHistoryScanRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerTodoClaimReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerTodoCloseReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerTodoFirstActionReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewReplyReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewDO;
import com.hxy.module.booking.dal.dataobject.BookingReviewNotifyOutboxDO;
import com.hxy.module.booking.dal.mysql.BookingReviewNotifyOutboxMapper;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.service.BookingReviewService;
import com.hxy.module.booking.service.TechnicianService;
import com.hxy.module.booking.service.support.BookingReviewAdminPrioritySupport;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 预约服务评价")
@RestController
@RequestMapping("/booking/review")
@Validated
public class BookingReviewController {

    @Resource
    private BookingReviewService bookingReviewService;
    @Resource
    private ProductStoreService productStoreService;
    @Resource
    private TechnicianService technicianService;
    @Resource
    private MemberUserApi memberUserApi;
    @Resource
    private BookingReviewNotifyOutboxMapper bookingReviewNotifyOutboxMapper;

    @GetMapping("/page")
    @Operation(summary = "分页获得预约服务评价")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<PageResult<BookingReviewRespVO>> page(@Valid BookingReviewPageReqVO reqVO) {
        PageResult<BookingReviewDO> pageResult = bookingReviewService.getAdminReviewPage(reqVO);
        PageResult<BookingReviewRespVO> result = BeanUtils.toBean(pageResult, BookingReviewRespVO.class);
        enrichReadableFields(result.getList());
        return success(result);
    }

    @GetMapping("/history-scan")
    @Operation(summary = "获得预约服务评价历史治理扫描结果")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<BookingReviewHistoryScanRespVO> historyScan(@Valid BookingReviewHistoryScanReqVO reqVO) {
        BookingReviewHistoryScanRespVO respVO = bookingReviewService.scanAdminHistoryCandidates(reqVO);
        if (respVO != null) {
            enrichHistoryScanFields(respVO.getList());
        }
        return success(respVO);
    }

    @GetMapping("/get")
    @Operation(summary = "获得预约服务评价详情")
    @Parameter(name = "id", required = true, description = "评价ID")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<BookingReviewRespVO> get(@RequestParam("id") Long id) {
        BookingReviewRespVO review = BeanUtils.toBean(bookingReviewService.getAdminReview(id), BookingReviewRespVO.class);
        enrichReadableField(review);
        return success(review);
    }

    @PostMapping("/reply")
    @Operation(summary = "回复预约服务评价")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> reply(@Valid @RequestBody BookingReviewReplyReqVO reqVO) {
        bookingReviewService.replyReview(reqVO.getReviewId(), getLoginUserId(), reqVO.getReplyContent());
        return success(true);
    }

    @PostMapping("/follow-status")
    @Operation(summary = "更新预约服务评价跟进状态")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> updateFollowStatus(@Valid @RequestBody BookingReviewFollowUpdateReqVO reqVO) {
        bookingReviewService.updateFollowStatus(reqVO.getReviewId(), getLoginUserId(), reqVO);
        return success(true);
    }

    @PostMapping("/manager-todo/claim")
    @Operation(summary = "认领预约服务评价店长待办")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> claimManagerTodo(@Valid @RequestBody BookingReviewManagerTodoClaimReqVO reqVO) {
        bookingReviewService.claimManagerTodo(reqVO.getReviewId(), getLoginUserId());
        return success(true);
    }

    @PostMapping("/manager-todo/first-action")
    @Operation(summary = "记录预约服务评价店长待办首次处理")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> recordManagerFirstAction(
            @Valid @RequestBody BookingReviewManagerTodoFirstActionReqVO reqVO) {
        bookingReviewService.recordManagerFirstAction(reqVO.getReviewId(), getLoginUserId(), reqVO.getRemark());
        return success(true);
    }

    @PostMapping("/manager-todo/close")
    @Operation(summary = "关闭预约服务评价店长待办")
    @PreAuthorize("@ss.hasPermission('booking:review:update')")
    public CommonResult<Boolean> closeManagerTodo(@Valid @RequestBody BookingReviewManagerTodoCloseReqVO reqVO) {
        bookingReviewService.closeManagerTodo(reqVO.getReviewId(), getLoginUserId(), reqVO.getRemark());
        return success(true);
    }

    @GetMapping("/dashboard-summary")
    @Operation(summary = "获得预约服务评价看板汇总")
    @PreAuthorize("@ss.hasPermission('booking:review:query')")
    public CommonResult<BookingReviewDashboardRespVO> dashboardSummary() {
        return success(bookingReviewService.getDashboardSummary());
    }

    private void enrichReadableFields(List<BookingReviewRespVO> reviews) {
        if (CollUtil.isEmpty(reviews)) {
            return;
        }
        Map<Long, ProductStoreDO> storeMap = productStoreService.getStoreMap(convertSet(reviews, BookingReviewRespVO::getStoreId));
        Map<Long, MemberUserRespDTO> memberMap = memberUserApi.getUserMap(convertSet(reviews, BookingReviewRespVO::getMemberId));
        Map<Long, TechnicianDO> technicianMap = buildTechnicianMap(convertSet(reviews, BookingReviewRespVO::getTechnicianId));
        Map<Long, List<BookingReviewNotifyOutboxDO>> notifyOutboxMap = buildNotifyOutboxMap(convertSet(reviews, BookingReviewRespVO::getId));
        reviews.forEach(review -> enrichReadableField(review, storeMap, memberMap, technicianMap, notifyOutboxMap));
    }

    private void enrichHistoryScanFields(List<BookingReviewHistoryScanItemRespVO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Map<Long, ProductStoreDO> storeMap = productStoreService.getStoreMap(convertSet(items, BookingReviewHistoryScanItemRespVO::getStoreId));
        Map<Long, MemberUserRespDTO> memberMap = memberUserApi.getUserMap(convertSet(items, BookingReviewHistoryScanItemRespVO::getMemberId));
        Map<Long, TechnicianDO> technicianMap = buildTechnicianMap(convertSet(items, BookingReviewHistoryScanItemRespVO::getTechnicianId));
        items.forEach(item -> enrichHistoryScanField(item, storeMap, memberMap, technicianMap));
    }

    private void enrichReadableField(BookingReviewRespVO review) {
        if (review == null) {
            return;
        }
        Map<Long, ProductStoreDO> storeMap = productStoreService.getStoreMap(CollUtil.newHashSet(review.getStoreId()));
        Map<Long, MemberUserRespDTO> memberMap = memberUserApi.getUserMap(CollUtil.newHashSet(review.getMemberId()));
        Map<Long, TechnicianDO> technicianMap = buildTechnicianMap(CollUtil.newHashSet(review.getTechnicianId()));
        Map<Long, List<BookingReviewNotifyOutboxDO>> notifyOutboxMap = buildNotifyOutboxMap(CollUtil.newHashSet(review.getId()));
        enrichReadableField(review, storeMap, memberMap, technicianMap, notifyOutboxMap);
    }

    private void enrichReadableField(BookingReviewRespVO review, Map<Long, ProductStoreDO> storeMap,
                                     Map<Long, MemberUserRespDTO> memberMap, Map<Long, TechnicianDO> technicianMap,
                                     Map<Long, List<BookingReviewNotifyOutboxDO>> notifyOutboxMap) {
        if (review == null) {
            return;
        }
        ProductStoreDO store = review.getStoreId() == null ? null : storeMap.get(review.getStoreId());
        if (store != null) {
            review.setStoreName(store.getName());
        }
        TechnicianDO technician = review.getTechnicianId() == null ? null : technicianMap.get(review.getTechnicianId());
        if (technician != null) {
            review.setTechnicianName(technician.getName());
        }
        MemberUserRespDTO member = review.getMemberId() == null ? null : memberMap.get(review.getMemberId());
        if (member != null) {
            review.setMemberNickname(member.getNickname());
        }
        LocalDateTime now = LocalDateTime.now().withNano(0);
        BookingReviewDO reviewDO = BeanUtils.toBean(review, BookingReviewDO.class);
        String managerSlaStage = BookingReviewAdminPrioritySupport.resolveManagerSlaStage(reviewDO, now);
        BookingReviewAdminPrioritySupport.NotifyRiskSnapshot notifyRiskSnapshot =
                BookingReviewAdminPrioritySupport.resolveNotifyRisk(review.getId() == null
                        ? Collections.emptyList()
                        : notifyOutboxMap.getOrDefault(review.getId(), Collections.emptyList()));
        BookingReviewAdminPrioritySupport.PrioritySnapshot prioritySnapshot =
                BookingReviewAdminPrioritySupport.resolvePriority(managerSlaStage, notifyRiskSnapshot);
        review.setManagerSlaStage(managerSlaStage);
        review.setPriorityLevel(prioritySnapshot.getLevel());
        review.setPriorityReason(prioritySnapshot.getReason());
        review.setNotifyRiskSummary(notifyRiskSnapshot.getSummary());
    }

    private void enrichHistoryScanField(BookingReviewHistoryScanItemRespVO item, Map<Long, ProductStoreDO> storeMap,
                                        Map<Long, MemberUserRespDTO> memberMap, Map<Long, TechnicianDO> technicianMap) {
        if (item == null) {
            return;
        }
        ProductStoreDO store = item.getStoreId() == null ? null : storeMap.get(item.getStoreId());
        if (store != null) {
            item.setStoreName(store.getName());
        }
        TechnicianDO technician = item.getTechnicianId() == null ? null : technicianMap.get(item.getTechnicianId());
        if (technician != null) {
            item.setTechnicianName(technician.getName());
        }
        MemberUserRespDTO member = item.getMemberId() == null ? null : memberMap.get(item.getMemberId());
        if (member != null) {
            item.setMemberNickname(member.getNickname());
        }
    }

    private Map<Long, TechnicianDO> buildTechnicianMap(Set<Long> technicianIds) {
        Map<Long, TechnicianDO> technicianMap = new HashMap<>();
        if (CollUtil.isEmpty(technicianIds)) {
            return technicianMap;
        }
        technicianIds.forEach(technicianId -> {
            TechnicianDO technician = technicianService.getTechnician(technicianId);
            if (technician != null) {
                technicianMap.put(technicianId, technician);
            }
        });
        return technicianMap;
    }

    private Map<Long, List<BookingReviewNotifyOutboxDO>> buildNotifyOutboxMap(Set<Long> reviewIds) {
        if (CollUtil.isEmpty(reviewIds)) {
            return Collections.emptyMap();
        }
        List<BookingReviewNotifyOutboxDO> outboxes = bookingReviewNotifyOutboxMapper.selectListByBizIds(new ArrayList<>(reviewIds));
        if (CollUtil.isEmpty(outboxes)) {
            return Collections.emptyMap();
        }
        return outboxes.stream()
                .filter(outbox -> outbox.getBizId() != null)
                .collect(Collectors.groupingBy(BookingReviewNotifyOutboxDO::getBizId));
    }
}
