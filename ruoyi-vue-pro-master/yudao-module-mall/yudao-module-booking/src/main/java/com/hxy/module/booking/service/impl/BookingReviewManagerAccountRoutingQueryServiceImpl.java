package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStorePageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreService;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingPageReqVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingRespVO;
import com.hxy.module.booking.controller.admin.vo.BookingReviewManagerAccountRoutingSummaryRespVO;
import com.hxy.module.booking.dal.dataobject.BookingReviewManagerAccountRoutingDO;
import com.hxy.module.booking.dal.mysql.BookingReviewManagerAccountRoutingMapper;
import com.hxy.module.booking.service.BookingReviewManagerAccountRoutingQueryService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Validated
public class BookingReviewManagerAccountRoutingQueryServiceImpl implements BookingReviewManagerAccountRoutingQueryService {

    private static final String BINDING_STATUS_ACTIVE = "ACTIVE";
    private static final String ROUTING_STATUS_ACTIVE = "ACTIVE_ROUTE";
    private static final String APP_ROUTING_MISSING = "APP_MISSING";
    private static final String WECOM_ROUTING_MISSING = "WECOM_MISSING";
    private static final int STORE_BATCH_SIZE = 200;

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
        List<BookingReviewManagerAccountRoutingRespVO> filteredList = buildFilteredRoutingList(reqVO);
        int pageNo = reqVO.getPageNo() == null || reqVO.getPageNo() < 1 ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null || reqVO.getPageSize() < 1 ? 10 : reqVO.getPageSize();
        int fromIndex = Math.min((pageNo - 1) * pageSize, filteredList.size());
        int toIndex = Math.min(fromIndex + pageSize, filteredList.size());
        return new PageResult<>(filteredList.subList(fromIndex, toIndex), (long) filteredList.size());
    }

    @Override
    public BookingReviewManagerAccountRoutingSummaryRespVO getRoutingCoverageSummary(
            BookingReviewManagerAccountRoutingPageReqVO reqVO) {
        List<BookingReviewManagerAccountRoutingRespVO> filteredList = buildFilteredRoutingList(reqVO);
        BookingReviewManagerAccountRoutingSummaryRespVO summary = new BookingReviewManagerAccountRoutingSummaryRespVO();
        summary.setTotalStoreCount((long) filteredList.size());
        summary.setDualReadyCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getRoutingStatus(), ROUTING_STATUS_ACTIVE))
                .count());
        summary.setAppReadyCount(filteredList.stream()
                .filter(item -> !StrUtil.equalsIgnoreCase(item.getAppRoutingStatus(), APP_ROUTING_MISSING))
                .count());
        summary.setWecomReadyCount(filteredList.stream()
                .filter(item -> !StrUtil.equalsIgnoreCase(item.getWecomRoutingStatus(), WECOM_ROUTING_MISSING))
                .count());
        summary.setMissingAnyCount(filteredList.stream()
                .filter(this::isMissingAny)
                .count());
        summary.setMissingAppCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getAppRoutingStatus(), APP_ROUTING_MISSING))
                .count());
        summary.setMissingWecomCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getWecomRoutingStatus(), WECOM_ROUTING_MISSING))
                .count());
        summary.setMissingBothCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getAppRoutingStatus(), APP_ROUTING_MISSING))
                .filter(item -> StrUtil.equalsIgnoreCase(item.getWecomRoutingStatus(), WECOM_ROUTING_MISSING))
                .count());
        return summary;
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

    private boolean matchesRoutingFilters(BookingReviewManagerAccountRoutingRespVO respVO,
                                          BookingReviewManagerAccountRoutingPageReqVO reqVO) {
        if (Boolean.TRUE.equals(reqVO.getOnlyMissingAny()) && !isMissingAny(respVO)) {
            return false;
        }
        if (StrUtil.isNotBlank(reqVO.getRoutingStatus())
                && !StrUtil.equalsIgnoreCase(respVO.getRoutingStatus(), reqVO.getRoutingStatus())) {
            return false;
        }
        if (StrUtil.isNotBlank(reqVO.getAppRoutingStatus())
                && !StrUtil.equalsIgnoreCase(respVO.getAppRoutingStatus(), reqVO.getAppRoutingStatus())) {
            return false;
        }
        if (StrUtil.isNotBlank(reqVO.getWecomRoutingStatus())
                && !StrUtil.equalsIgnoreCase(respVO.getWecomRoutingStatus(), reqVO.getWecomRoutingStatus())) {
            return false;
        }
        return true;
    }

    private boolean isMissingAny(BookingReviewManagerAccountRoutingRespVO respVO) {
        return StrUtil.equalsIgnoreCase(respVO.getAppRoutingStatus(), APP_ROUTING_MISSING)
                || StrUtil.equalsIgnoreCase(respVO.getWecomRoutingStatus(), WECOM_ROUTING_MISSING);
    }

    private String toNullable(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private List<BookingReviewManagerAccountRoutingRespVO> buildFilteredRoutingList(
            BookingReviewManagerAccountRoutingPageReqVO reqVO) {
        List<ProductStoreDO> matchedStores = loadMatchedStores(reqVO);
        if (matchedStores.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, BookingReviewManagerAccountRoutingDO> routingMap = loadLatestRoutingMap(matchedStores);
        return matchedStores.stream()
                .map(store -> buildResp(store, routingMap.get(store.getId())))
                .filter(respVO -> matchesStoreFilters(respVO, reqVO))
                .filter(respVO -> matchesRoutingFilters(respVO, reqVO))
                .collect(Collectors.toList());
    }

    private List<ProductStoreDO> loadMatchedStores(BookingReviewManagerAccountRoutingPageReqVO reqVO) {
        if (reqVO.getStoreId() != null) {
            ProductStoreDO store = productStoreService.getStore(reqVO.getStoreId());
            if (store == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(store);
        }
        List<ProductStoreDO> stores = new ArrayList<>();
        int pageNo = 1;
        long total = Long.MAX_VALUE;
        while (stores.size() < total) {
            ProductStorePageReqVO storeReqVO = new ProductStorePageReqVO();
            storeReqVO.setPageNo(pageNo);
            storeReqVO.setPageSize(STORE_BATCH_SIZE);
            storeReqVO.setName(toNullable(reqVO.getStoreName()));
            storeReqVO.setContactMobile(toNullable(reqVO.getContactMobile()));
            PageResult<ProductStoreDO> storePage = productStoreService.getStorePage(storeReqVO);
            if (storePage == null || storePage.getList() == null || storePage.getList().isEmpty()) {
                break;
            }
            stores.addAll(storePage.getList());
            total = storePage.getTotal() == null ? stores.size() : storePage.getTotal();
            pageNo++;
        }
        return stores;
    }

    private Map<Long, BookingReviewManagerAccountRoutingDO> loadLatestRoutingMap(List<ProductStoreDO> stores) {
        List<Long> storeIds = stores.stream().map(ProductStoreDO::getId).collect(Collectors.toList());
        List<BookingReviewManagerAccountRoutingDO> routingList =
                bookingReviewManagerAccountRoutingMapper.selectLatestListByStoreIds(storeIds);
        Map<Long, BookingReviewManagerAccountRoutingDO> latestMap = new LinkedHashMap<>();
        for (BookingReviewManagerAccountRoutingDO routing : routingList) {
            latestMap.putIfAbsent(routing.getStoreId(), routing);
        }
        return latestMap;
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
