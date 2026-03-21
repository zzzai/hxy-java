package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStorePageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import com.hxy.module.booking.dal.mysql.BookingReviewManagerAccountRoutingMapper;
import com.hxy.module.booking.service.BookingReviewManagerAccountRoutingQueryService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
public class BookingReviewManagerAccountRoutingQueryServiceImpl implements BookingReviewManagerAccountRoutingQueryService {

    private static final String BINDING_STATUS_ACTIVE = "ACTIVE";

    @Resource
    private ProductStoreService productStoreService;

    @Resource
    private BookingReviewManagerAccountRoutingMapper bookingReviewManagerAccountRoutingMapper;

    @Override
    public BookingReviewManagerAccountRoutingRespVO getRouting(Long storeId) {
        if (storeId == null) {
            return null;
        }
        ProductStoreDO store = productStoreService.getStore(storeId);
        if (store == null) {
            return null;
        }
        return buildResp(store, bookingReviewManagerAccountRoutingMapper.selectLatestByStoreId(storeId));
    }

    @Override
    public PageResult<BookingReviewManagerAccountRoutingRespVO> getRoutingPage(
            BookingReviewManagerAccountRoutingPageReqVO reqVO) {
        if (reqVO.getStoreId() != null) {
            BookingReviewManagerAccountRoutingRespVO respVO = getRouting(reqVO.getStoreId());
            if (respVO == null || !matchesStoreFilters(respVO, reqVO)) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
            return new PageResult<>(Collections.singletonList(respVO), 1L);
        }

        ProductStorePageReqVO storeReqVO = new ProductStorePageReqVO();
        storeReqVO.setPageNo(reqVO.getPageNo());
        storeReqVO.setPageSize(reqVO.getPageSize());
        storeReqVO.setName(toNullable(reqVO.getStoreName()));
        storeReqVO.setContactMobile(toNullable(reqVO.getContactMobile()));
        PageResult<ProductStoreDO> storePage = productStoreService.getStorePage(storeReqVO);
        List<BookingReviewManagerAccountRoutingRespVO> list = storePage.getList().stream()
                .map(store -> buildResp(store, bookingReviewManagerAccountRoutingMapper.selectLatestByStoreId(store.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(list, storePage.getTotal());
    }

    private boolean matchesStoreFilters(BookingReviewManagerAccountRoutingRespVO respVO,
                                        BookingReviewManagerAccountRoutingPageReqVO reqVO) {
        if (StrUtil.isNotBlank(reqVO.getStoreName())
                && !StrUtil.containsIgnoreCase(StrUtil.blankToDefault(respVO.getStoreName(), ""), reqVO.getStoreName())) {
            return false;
        }
        if (StrUtil.isNotBlank(reqVO.getContactMobile())
                && !StrUtil.containsIgnoreCase(StrUtil.blankToDefault(respVO.getContactMobile(), ""), reqVO.getContactMobile())) {
            return false;
        }
        return true;
    }

    private String toNullable(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private BookingReviewManagerAccountRoutingRespVO buildResp(ProductStoreDO store,
                                                               BookingReviewManagerAccountRoutingDO routing) {
        BookingReviewManagerAccountRoutingRespVO respVO = new BookingReviewManagerAccountRoutingRespVO();
        respVO.setStoreId(store.getId());
        respVO.setStoreName(store.getName());
        respVO.setContactName(store.getContactName());
        respVO.setContactMobile(store.getContactMobile());
        if (routing != null) {
            respVO.setManagerAdminUserId(routing.getManagerAdminUserId());
            respVO.setManagerWecomUserId(routing.getManagerWecomUserId());
            respVO.setBindingStatus(routing.getBindingStatus());
            respVO.setEffectiveTime(routing.getEffectiveTime());
            respVO.setExpireTime(routing.getExpireTime());
            respVO.setSource(routing.getSource());
            respVO.setLastVerifiedTime(routing.getLastVerifiedTime());
        }
        RoutingSnapshot snapshot = buildRoutingSnapshot(store, routing);
        respVO.setRoutingStatus(snapshot.status);
        respVO.setRoutingLabel(snapshot.label);
        respVO.setRoutingDetail(snapshot.detail);
        respVO.setRepairHint(snapshot.repairHint);
        respVO.setAppRoutingStatus(snapshot.appRoutingStatus);
        respVO.setAppRoutingLabel(snapshot.appRoutingLabel);
        respVO.setAppRepairHint(snapshot.appRepairHint);
        respVO.setWecomRoutingStatus(snapshot.wecomRoutingStatus);
        respVO.setWecomRoutingLabel(snapshot.wecomRoutingLabel);
        respVO.setWecomRepairHint(snapshot.wecomRepairHint);
        return respVO;
    }

    private RoutingSnapshot buildRoutingSnapshot(ProductStoreDO store, BookingReviewManagerAccountRoutingDO routing) {
        boolean hasContact = StrUtil.isNotBlank(store.getContactName()) || StrUtil.isNotBlank(store.getContactMobile());
        if (routing == null) {
            String repairHint = hasContact
                    ? "需要先补齐 storeId -> managerAdminUserId 和 storeId -> managerWecomUserId 路由关系。"
                    : "需要先补齐门店联系人，再绑定 managerAdminUserId 和 managerWecomUserId。";
            return new RoutingSnapshot("NO_ROUTE", "未绑定店长双通道路由",
                    "当前门店还没有稳定的店长 App / 企微路由记录。",
                    repairHint,
                    "APP_MISSING", "缺店长 App 账号", "需要先补齐 managerAdminUserId。",
                    "WECOM_MISSING", "缺店长企微账号", "需要先补齐 managerWecomUserId。");
        }
        LocalDateTime now = LocalDateTime.now().withNano(0);
        if (!StrUtil.equalsIgnoreCase(BINDING_STATUS_ACTIVE, routing.getBindingStatus())) {
            return new RoutingSnapshot("INACTIVE_ROUTE", "路由未启用",
                    "当前存在双通道路由记录，但 bindingStatus 不是 ACTIVE。",
                    "需要先将路由状态切换为 ACTIVE，再观察通知派发。",
                    "APP_BLOCKED", "App 路由未启用", "需要先启用路由。",
                    "WECOM_BLOCKED", "企微路由未启用", "需要先启用路由。");
        }
        if (routing.getEffectiveTime() != null && routing.getEffectiveTime().isAfter(now)) {
            return new RoutingSnapshot("PENDING_EFFECTIVE", "路由未生效",
                    "当前存在双通道路由记录，但生效时间尚未到达。",
                    "需要等待路由生效时间到达，或调整生效时间。",
                    "APP_PENDING", "App 路由未生效", "需要等待或调整生效时间。",
                    "WECOM_PENDING", "企微路由未生效", "需要等待或调整生效时间。");
        }
        if (routing.getExpireTime() != null && !routing.getExpireTime().isAfter(now)) {
            return new RoutingSnapshot("EXPIRED_ROUTE", "路由已过期",
                    "当前双通道路由记录已过期。",
                    "需要续期或重新绑定有效路由。",
                    "APP_EXPIRED", "App 路由已过期", "需要续期 App 路由。",
                    "WECOM_EXPIRED", "企微路由已过期", "需要续期企微路由。");
        }

        boolean appReady = routing.getManagerAdminUserId() != null && routing.getManagerAdminUserId() > 0;
        boolean wecomReady = StrUtil.isNotBlank(routing.getManagerWecomUserId());
        if (appReady && wecomReady) {
            return new RoutingSnapshot("ACTIVE_ROUTE", "双通道路由有效",
                    "当前 storeId 已命中有效店长 App / 企微双通道路由，可进入可派发状态。",
                    "当前无需修复，可继续观察通知派发结果。",
                    "APP_READY", "App 路由有效", "当前无需修复。",
                    "WECOM_READY", "企微路由有效", "当前无需修复。");
        }
        if (appReady) {
            return new RoutingSnapshot("PARTIAL_ROUTE", "App 已就绪，企微待补齐",
                    "当前 storeId 已命中店长 App 路由，但企微账号仍缺失。",
                    "需要先补齐店长企微账号，再完成双通道派发。",
                    "APP_READY", "App 路由有效", "当前无需修复。",
                    "WECOM_MISSING", "缺店长企微账号", "需要先补齐 managerWecomUserId。");
        }
        if (wecomReady) {
            return new RoutingSnapshot("PARTIAL_ROUTE", "企微已就绪，App 待补齐",
                    "当前 storeId 已命中店长企微路由，但后台 App 账号仍缺失。",
                    "需要先补齐店长后台账号，再完成双通道派发。",
                    "APP_MISSING", "缺店长 App 账号", "需要先补齐 managerAdminUserId。",
                    "WECOM_READY", "企微路由有效", "当前无需修复。");
        }
        return new RoutingSnapshot("NO_ROUTE", "未绑定店长双通道路由",
                "当前路由记录存在，但 App / 企微接收账号都还未补齐。",
                "需要先补齐 managerAdminUserId 和 managerWecomUserId。",
                "APP_MISSING", "缺店长 App 账号", "需要先补齐 managerAdminUserId。",
                "WECOM_MISSING", "缺店长企微账号", "需要先补齐 managerWecomUserId。");
    }

    private static final class RoutingSnapshot {
        private final String status;
        private final String label;
        private final String detail;
        private final String repairHint;
        private final String appRoutingStatus;
        private final String appRoutingLabel;
        private final String appRepairHint;
        private final String wecomRoutingStatus;
        private final String wecomRoutingLabel;
        private final String wecomRepairHint;

        private RoutingSnapshot(String status, String label, String detail, String repairHint,
                                String appRoutingStatus, String appRoutingLabel, String appRepairHint,
                                String wecomRoutingStatus, String wecomRoutingLabel, String wecomRepairHint) {
            this.status = status;
            this.label = label;
            this.detail = detail;
            this.repairHint = repairHint;
            this.appRoutingStatus = appRoutingStatus;
            this.appRoutingLabel = appRoutingLabel;
            this.appRepairHint = appRepairHint;
            this.wecomRoutingStatus = wecomRoutingStatus;
            this.wecomRoutingLabel = wecomRoutingLabel;
            this.wecomRepairHint = wecomRepairHint;
        }
    }
}
