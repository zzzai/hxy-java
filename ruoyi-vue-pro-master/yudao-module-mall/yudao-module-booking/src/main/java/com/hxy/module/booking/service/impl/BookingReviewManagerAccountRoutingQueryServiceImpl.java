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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Validated
public class BookingReviewManagerAccountRoutingQueryServiceImpl implements BookingReviewManagerAccountRoutingQueryService {

    private static final String BINDING_STATUS_ACTIVE = "ACTIVE";
    private static final String ROUTING_STATUS_NO_ROUTE = "NO_ROUTE";
    private static final String ROUTING_STATUS_ACTIVE = "ACTIVE_ROUTE";
    private static final String ROUTING_STATUS_PARTIAL = "PARTIAL_ROUTE";
    private static final String ROUTING_STATUS_INACTIVE = "INACTIVE_ROUTE";
    private static final String ROUTING_STATUS_PENDING_EFFECTIVE = "PENDING_EFFECTIVE";
    private static final String ROUTING_STATUS_EXPIRED = "EXPIRED_ROUTE";
    private static final String APP_ROUTING_MISSING = "APP_MISSING";
    private static final String WECOM_ROUTING_MISSING = "WECOM_MISSING";
    private static final String APP_ROUTING_READY = "APP_READY";
    private static final String WECOM_ROUTING_READY = "WECOM_READY";
    private static final String GOVERNANCE_STAGE_IMMEDIATE_FIX = "IMMEDIATE_FIX";
    private static final String GOVERNANCE_STAGE_WAIT_EFFECTIVE = "WAIT_EFFECTIVE";
    private static final String GOVERNANCE_STAGE_VERIFY_SOURCE = "VERIFY_SOURCE";
    private static final String GOVERNANCE_STAGE_OBSERVE_READY = "OBSERVE_READY";
    private static final String GOVERNANCE_PRIORITY_P0 = "P0";
    private static final String GOVERNANCE_PRIORITY_P1 = "P1";
    private static final String GOVERNANCE_PRIORITY_P2 = "P2";
    private static final String VERIFICATION_UNVERIFIED = "UNVERIFIED";
    private static final String VERIFICATION_STALE = "STALE_VERIFY";
    private static final String VERIFICATION_RECENT = "RECENT_VERIFY";
    private static final String VERIFICATION_ATTENTION = "ATTENTION_REQUIRED";
    private static final String SOURCE_PENDING = "SOURCE_PENDING";
    private static final String SOURCE_READY = "SOURCE_READY";
    private static final String SOURCE_TRUTH_ROUTE_CONFIRMED = "ROUTE_CONFIRMED";
    private static final String SOURCE_TRUTH_SOURCE_MISSING = "SOURCE_MISSING";
    private static final String SOURCE_TRUTH_CONTACT_ONLY_PENDING_BIND = "CONTACT_ONLY_PENDING_BIND";
    private static final String SOURCE_TRUTH_CONTACT_MISSING = "CONTACT_MISSING";
    private static final String SOURCE_TRUTH_VERIFY_STALE = "VERIFY_STALE";
    private static final int RECENT_VERIFY_DAYS = 7;
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
        summary.setImmediateFixCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getGovernanceStage(), GOVERNANCE_STAGE_IMMEDIATE_FIX))
                .count());
        summary.setVerifySourceCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getGovernanceStage(), GOVERNANCE_STAGE_VERIFY_SOURCE))
                .count());
        summary.setStaleVerifyCount(filteredList.stream()
                .filter(this::needsVerificationAttention)
                .count());
        summary.setSourcePendingCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getSourceClosureStatus(), SOURCE_PENDING))
                .count());
        summary.setObserveReadyCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getGovernanceStage(), GOVERNANCE_STAGE_OBSERVE_READY))
                .count());
        summary.setRouteConfirmedCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getSourceTruthStage(), SOURCE_TRUTH_ROUTE_CONFIRMED))
                .count());
        summary.setSourceMissingCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getSourceTruthStage(), SOURCE_TRUTH_SOURCE_MISSING))
                .count());
        summary.setContactOnlyPendingBindCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getSourceTruthStage(), SOURCE_TRUTH_CONTACT_ONLY_PENDING_BIND))
                .count());
        summary.setContactMissingCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getSourceTruthStage(), SOURCE_TRUTH_CONTACT_MISSING))
                .count());
        summary.setVerifyStaleCount(filteredList.stream()
                .filter(item -> StrUtil.equalsIgnoreCase(item.getSourceTruthStage(), SOURCE_TRUTH_VERIFY_STALE))
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
        if (StrUtil.isNotBlank(reqVO.getGovernanceStage())
                && !StrUtil.equalsIgnoreCase(respVO.getGovernanceStage(), reqVO.getGovernanceStage())) {
            return false;
        }
        if (StrUtil.isNotBlank(reqVO.getVerificationFreshnessStatus())
                && !matchesVerificationFreshness(respVO, reqVO.getVerificationFreshnessStatus())) {
            return false;
        }
        if (StrUtil.isNotBlank(reqVO.getSourceClosureStatus())
                && !StrUtil.equalsIgnoreCase(respVO.getSourceClosureStatus(), reqVO.getSourceClosureStatus())) {
            return false;
        }
        if (StrUtil.isNotBlank(reqVO.getSourceTruthStage())
                && !StrUtil.equalsIgnoreCase(respVO.getSourceTruthStage(), reqVO.getSourceTruthStage())) {
            return false;
        }
        return true;
    }

    private boolean isMissingAny(BookingReviewManagerAccountRoutingRespVO respVO) {
        return StrUtil.equalsIgnoreCase(respVO.getAppRoutingStatus(), APP_ROUTING_MISSING)
                || StrUtil.equalsIgnoreCase(respVO.getWecomRoutingStatus(), WECOM_ROUTING_MISSING);
    }

    private boolean matchesVerificationFreshness(BookingReviewManagerAccountRoutingRespVO respVO, String filterStatus) {
        if (StrUtil.equalsIgnoreCase(filterStatus, VERIFICATION_ATTENTION)) {
            return needsVerificationAttention(respVO);
        }
        return StrUtil.equalsIgnoreCase(respVO.getVerificationFreshnessStatus(), filterStatus);
    }

    private boolean needsVerificationAttention(BookingReviewManagerAccountRoutingRespVO respVO) {
        return !StrUtil.equalsIgnoreCase(respVO.getVerificationFreshnessStatus(), VERIFICATION_RECENT);
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
        populateGovernanceFields(respVO);
        return respVO;
    }

    private void populateGovernanceFields(BookingReviewManagerAccountRoutingRespVO respVO) {
        String verificationFreshnessStatus = buildVerificationFreshnessStatus(respVO.getLastVerifiedTime());
        respVO.setVerificationFreshnessStatus(verificationFreshnessStatus);
        respVO.setVerificationFreshnessLabel(buildVerificationFreshnessLabel(verificationFreshnessStatus));

        String sourceClosureStatus = buildSourceClosureStatus(respVO.getSource());
        respVO.setSourceClosureStatus(sourceClosureStatus);
        respVO.setSourceClosureLabel(buildSourceClosureLabel(sourceClosureStatus));

        SourceTruthSnapshot sourceTruthSnapshot = buildSourceTruthSnapshot(respVO, sourceClosureStatus, verificationFreshnessStatus);
        respVO.setSourceTruthStage(sourceTruthSnapshot.stage);
        respVO.setSourceTruthLabel(sourceTruthSnapshot.label);
        respVO.setSourceTruthDetail(sourceTruthSnapshot.detail);
        respVO.setSourceTruthActionHint(sourceTruthSnapshot.actionHint);

        GovernanceSnapshot governance = buildGovernanceSnapshot(respVO, verificationFreshnessStatus, sourceClosureStatus);
        respVO.setGovernanceStage(governance.stage);
        respVO.setGovernanceStageLabel(governance.stageLabel);
        respVO.setGovernancePriority(governance.priority);
        respVO.setGovernancePriorityLabel(governance.priorityLabel);
        respVO.setGovernanceOwnerLabel(governance.ownerLabel);
        respVO.setGovernanceActionSummary(governance.actionSummary);
    }

    private String buildVerificationFreshnessStatus(LocalDateTime lastVerifiedTime) {
        if (lastVerifiedTime == null) {
            return VERIFICATION_UNVERIFIED;
        }
        LocalDateTime threshold = LocalDateTime.now().withNano(0).minusDays(RECENT_VERIFY_DAYS);
        return lastVerifiedTime.isBefore(threshold) ? VERIFICATION_STALE : VERIFICATION_RECENT;
    }

    private String buildVerificationFreshnessLabel(String status) {
        if (StrUtil.equalsIgnoreCase(status, VERIFICATION_UNVERIFIED)) {
            return "未核验";
        }
        if (StrUtil.equalsIgnoreCase(status, VERIFICATION_STALE)) {
            return "长期未核验";
        }
        return RECENT_VERIFY_DAYS + " 天内已核验";
    }

    private String buildSourceClosureStatus(String source) {
        return StrUtil.isBlank(source) || StrUtil.equalsIgnoreCase(source, "UNKNOWN") ? SOURCE_PENDING : SOURCE_READY;
    }

    private String buildSourceClosureLabel(String status) {
        return StrUtil.equalsIgnoreCase(status, SOURCE_PENDING) ? "来源待闭环" : "来源已登记";
    }

    private SourceTruthSnapshot buildSourceTruthSnapshot(BookingReviewManagerAccountRoutingRespVO respVO,
                                                         String sourceClosureStatus,
                                                         String verificationFreshnessStatus) {
        boolean hasRoutingRecord = respVO.getManagerAdminUserId() != null
                || StrUtil.isNotBlank(respVO.getManagerWecomUserId())
                || StrUtil.isNotBlank(respVO.getBindingStatus())
                || respVO.getEffectiveTime() != null
                || respVO.getExpireTime() != null
                || StrUtil.isNotBlank(respVO.getSource());
        boolean hasContact = StrUtil.isNotBlank(respVO.getContactName()) || StrUtil.isNotBlank(respVO.getContactMobile());
        boolean sourceReady = StrUtil.equalsIgnoreCase(sourceClosureStatus, SOURCE_READY);
        boolean verificationRecent = StrUtil.equalsIgnoreCase(verificationFreshnessStatus, VERIFICATION_RECENT);
        if (hasRoutingRecord) {
            if (!sourceReady) {
                return new SourceTruthSnapshot(SOURCE_TRUTH_SOURCE_MISSING, "来源缺失",
                        "当前已存在店长路由记录，但 source 仍为空或 UNKNOWN，不能写成来源闭环。",
                        "补登记稳定来源，并保留当前 storeId -> managerAdminUserId / managerWecomUserId 的核查依据。");
            }
            if (!verificationRecent) {
                return new SourceTruthSnapshot(SOURCE_TRUTH_VERIFY_STALE, "来源待复核",
                        "当前来源已登记，但 lastVerifiedTime 缺失或超过 7 天，不能直接按最新真值使用。",
                        "复核当前门店双通道路由并更新 lastVerifiedTime，再关闭来源复核项。");
            }
            return new SourceTruthSnapshot(SOURCE_TRUTH_ROUTE_CONFIRMED, "来源已确认",
                    "当前门店已存在可追的店长路由记录，来源与核验信息都已闭环。",
                    "继续观察 App / 企微双通道派发结果，无需额外处理。");
        }
        if (hasContact) {
            return new SourceTruthSnapshot(SOURCE_TRUTH_CONTACT_ONLY_PENDING_BIND, "联系人待转绑定",
                    "当前没有稳定双通道路由，但门店联系人已核出，可作为绑定治理入口。",
                    "先将联系人对应到店长 App / 企微账号，再补登记来源与最近核验时间。");
        }
        return new SourceTruthSnapshot(SOURCE_TRUTH_CONTACT_MISSING, "联系人缺失",
                "当前既没有稳定双通道路由，也没有可追的门店联系人主数据。",
                "先补齐门店联系人主数据，再建立店长 App / 企微绑定并登记来源。");
    }

    private GovernanceSnapshot buildGovernanceSnapshot(BookingReviewManagerAccountRoutingRespVO respVO,
                                                       String verificationFreshnessStatus,
                                                       String sourceClosureStatus) {
        if (StrUtil.equalsIgnoreCase(respVO.getRoutingStatus(), ROUTING_STATUS_PENDING_EFFECTIVE)) {
            return new GovernanceSnapshot(GOVERNANCE_STAGE_WAIT_EFFECTIVE, "等待生效",
                    GOVERNANCE_PRIORITY_P1, "P1 等待生效",
                    "路由配置治理",
                    "等待路由生效时间到达，或调整生效时间后再复核。");
        }
        if (needsImmediateFix(respVO)) {
            return buildImmediateFixSnapshot(respVO);
        }
        if (StrUtil.equalsIgnoreCase(sourceClosureStatus, SOURCE_PENDING)
                || !StrUtil.equalsIgnoreCase(verificationFreshnessStatus, VERIFICATION_RECENT)) {
            String actionSummary;
            if (StrUtil.equalsIgnoreCase(sourceClosureStatus, SOURCE_PENDING)
                    && !StrUtil.equalsIgnoreCase(verificationFreshnessStatus, VERIFICATION_RECENT)) {
                actionSummary = "转绑定来源核验：补登记来源并更新最近核验时间，再关闭治理项。";
            } else if (StrUtil.equalsIgnoreCase(sourceClosureStatus, SOURCE_PENDING)) {
                actionSummary = "转绑定来源核验：补登记稳定来源后再关闭治理项。";
            } else {
                actionSummary = "转绑定来源核验：重新核验当前门店双通道路由并更新时间。";
            }
            return new GovernanceSnapshot(GOVERNANCE_STAGE_VERIFY_SOURCE, "待核来源闭环",
                    GOVERNANCE_PRIORITY_P1, "P1 待核来源",
                    "绑定来源核验",
                    actionSummary);
        }
        return new GovernanceSnapshot(GOVERNANCE_STAGE_OBSERVE_READY, "可观察就绪",
                GOVERNANCE_PRIORITY_P2, "P2 观察",
                "运营观察",
                "当前双通道已就绪，继续观察派发结果与告警即可。");
    }

    private boolean needsImmediateFix(BookingReviewManagerAccountRoutingRespVO respVO) {
        return StrUtil.equalsAnyIgnoreCase(respVO.getRoutingStatus(),
                ROUTING_STATUS_NO_ROUTE, ROUTING_STATUS_PARTIAL, ROUTING_STATUS_INACTIVE, ROUTING_STATUS_EXPIRED);
    }

    private GovernanceSnapshot buildImmediateFixSnapshot(BookingReviewManagerAccountRoutingRespVO respVO) {
        if (StrUtil.equalsIgnoreCase(respVO.getRoutingStatus(), ROUTING_STATUS_INACTIVE)) {
            return new GovernanceSnapshot(GOVERNANCE_STAGE_IMMEDIATE_FIX, "立即治理",
                    GOVERNANCE_PRIORITY_P0, "P0 立即治理",
                    "路由配置治理",
                    "转路由配置治理：先启用 ACTIVE 路由，再重新核验双通道派发。");
        }
        if (StrUtil.equalsIgnoreCase(respVO.getRoutingStatus(), ROUTING_STATUS_EXPIRED)) {
            return new GovernanceSnapshot(GOVERNANCE_STAGE_IMMEDIATE_FIX, "立即治理",
                    GOVERNANCE_PRIORITY_P0, "P0 立即治理",
                    "路由配置治理",
                    "转路由配置治理：先续期或重绑有效路由，再重新核验双通道派发。");
        }
        boolean appMissing = StrUtil.equalsIgnoreCase(respVO.getAppRoutingStatus(), APP_ROUTING_MISSING);
        boolean wecomMissing = StrUtil.equalsIgnoreCase(respVO.getWecomRoutingStatus(), WECOM_ROUTING_MISSING);
        if (appMissing && wecomMissing) {
            String owner = StrUtil.isBlank(respVO.getContactName()) && StrUtil.isBlank(respVO.getContactMobile())
                    ? "门店主数据治理"
                    : "账号绑定治理";
            String action = StrUtil.isBlank(respVO.getContactName()) && StrUtil.isBlank(respVO.getContactMobile())
                    ? "转门店主数据治理：先补联系人，再补 managerAdminUserId / managerWecomUserId。"
                    : "转账号绑定治理：补齐 managerAdminUserId / managerWecomUserId 后重新核验。";
            return new GovernanceSnapshot(GOVERNANCE_STAGE_IMMEDIATE_FIX, "立即治理",
                    GOVERNANCE_PRIORITY_P0, "P0 立即治理",
                    owner, action);
        }
        if (appMissing) {
            return new GovernanceSnapshot(GOVERNANCE_STAGE_IMMEDIATE_FIX, "立即治理",
                    GOVERNANCE_PRIORITY_P0, "P0 立即治理",
                    "App 账号治理",
                    "转 App 账号治理：补齐 managerAdminUserId 后重新核验。");
        }
        if (wecomMissing) {
            return new GovernanceSnapshot(GOVERNANCE_STAGE_IMMEDIATE_FIX, "立即治理",
                    GOVERNANCE_PRIORITY_P0, "P0 立即治理",
                    "企微账号治理",
                    "转企微账号治理：补齐 managerWecomUserId 后重新核验。");
        }
        return new GovernanceSnapshot(GOVERNANCE_STAGE_IMMEDIATE_FIX, "立即治理",
                GOVERNANCE_PRIORITY_P0, "P0 立即治理",
                "账号绑定治理",
                "当前路由阻断双通道派发，需要优先核查并修复。");
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
            return new RoutingSnapshot(ROUTING_STATUS_INACTIVE, "路由未启用",
                    "当前存在双通道路由记录，但 bindingStatus 不是 ACTIVE。",
                    "需要先将路由状态切换为 ACTIVE，再观察通知派发。",
                    "APP_BLOCKED", "App 路由未启用", "需要先启用路由。",
                    "WECOM_BLOCKED", "企微路由未启用", "需要先启用路由。");
        }
        if (routing.getEffectiveTime() != null && routing.getEffectiveTime().isAfter(now)) {
            return new RoutingSnapshot(ROUTING_STATUS_PENDING_EFFECTIVE, "路由未生效",
                    "当前存在双通道路由记录，但生效时间尚未到达。",
                    "需要等待路由生效时间到达，或调整生效时间。",
                    "APP_PENDING", "App 路由未生效", "需要等待或调整生效时间。",
                    "WECOM_PENDING", "企微路由未生效", "需要等待或调整生效时间。");
        }
        if (routing.getExpireTime() != null && !routing.getExpireTime().isAfter(now)) {
            return new RoutingSnapshot(ROUTING_STATUS_EXPIRED, "路由已过期",
                    "当前双通道路由记录已过期。",
                    "需要续期或重新绑定有效路由。",
                    "APP_EXPIRED", "App 路由已过期", "需要续期 App 路由。",
                    "WECOM_EXPIRED", "企微路由已过期", "需要续期企微路由。");
        }

        boolean appReady = routing.getManagerAdminUserId() != null && routing.getManagerAdminUserId() > 0;
        boolean wecomReady = StrUtil.isNotBlank(routing.getManagerWecomUserId());
        if (appReady && wecomReady) {
            return new RoutingSnapshot(ROUTING_STATUS_ACTIVE, "双通道路由有效",
                    "当前 storeId 已命中有效店长 App / 企微双通道路由，可进入可派发状态。",
                    "当前无需修复，可继续观察通知派发结果。",
                    APP_ROUTING_READY, "App 路由有效", "当前无需修复。",
                    WECOM_ROUTING_READY, "企微路由有效", "当前无需修复。");
        }
        if (appReady) {
            return new RoutingSnapshot(ROUTING_STATUS_PARTIAL, "App 已就绪，企微待补齐",
                    "当前 storeId 已命中店长 App 路由，但企微账号仍缺失。",
                    "需要先补齐店长企微账号，再完成双通道派发。",
                    APP_ROUTING_READY, "App 路由有效", "当前无需修复。",
                    "WECOM_MISSING", "缺店长企微账号", "需要先补齐 managerWecomUserId。");
        }
        if (wecomReady) {
            return new RoutingSnapshot(ROUTING_STATUS_PARTIAL, "企微已就绪，App 待补齐",
                    "当前 storeId 已命中店长企微路由，但后台 App 账号仍缺失。",
                    "需要先补齐店长后台账号，再完成双通道派发。",
                    "APP_MISSING", "缺店长 App 账号", "需要先补齐 managerAdminUserId。",
                    WECOM_ROUTING_READY, "企微路由有效", "当前无需修复。");
        }
        return new RoutingSnapshot(ROUTING_STATUS_NO_ROUTE, "未绑定店长双通道路由",
                "当前路由记录存在，但 App / 企微接收账号都还未补齐。",
                "需要先补齐 managerAdminUserId 和 managerWecomUserId。",
                "APP_MISSING", "缺店长 App 账号", "需要先补齐 managerAdminUserId。",
                "WECOM_MISSING", "缺店长企微账号", "需要先补齐 managerWecomUserId。");
    }

    private static final class GovernanceSnapshot {
        private final String stage;
        private final String stageLabel;
        private final String priority;
        private final String priorityLabel;
        private final String ownerLabel;
        private final String actionSummary;

        private GovernanceSnapshot(String stage, String stageLabel, String priority, String priorityLabel,
                                   String ownerLabel, String actionSummary) {
            this.stage = stage;
            this.stageLabel = stageLabel;
            this.priority = priority;
            this.priorityLabel = priorityLabel;
            this.ownerLabel = ownerLabel;
            this.actionSummary = actionSummary;
        }
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

    private static final class SourceTruthSnapshot {
        private final String stage;
        private final String label;
        private final String detail;
        private final String actionHint;

        private SourceTruthSnapshot(String stage, String label, String detail, String actionHint) {
            this.stage = stage;
            this.label = label;
            this.detail = detail;
            this.actionHint = actionHint;
        }
    }
}
