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
        return respVO;
    }

    private RoutingSnapshot buildRoutingSnapshot(ProductStoreDO store, BookingReviewManagerAccountRoutingDO routing) {
        boolean hasContact = StrUtil.isNotBlank(store.getContactName()) || StrUtil.isNotBlank(store.getContactMobile());
        if (routing == null) {
            return hasContact
                    ? new RoutingSnapshot("NO_ROUTE", "未绑定店长账号",
                    "当前门店联系人已存在，但还没有稳定的 storeId -> managerAdminUserId 路由记录。",
                    "需要先补齐 storeId -> managerAdminUserId 路由关系，再重试通知。")
                    : new RoutingSnapshot("NO_ROUTE", "缺联系人且未绑定路由",
                    "当前门店既没有完整联系人快照，也没有稳定的店长账号路由记录。",
                    "需要先补齐门店联系人，再绑定 storeId -> managerAdminUserId 路由关系。");
        }
        LocalDateTime now = LocalDateTime.now().withNano(0);
        if (!StrUtil.equalsIgnoreCase(BINDING_STATUS_ACTIVE, routing.getBindingStatus())) {
            return new RoutingSnapshot("INACTIVE_ROUTE", "路由未启用",
                    "当前存在路由记录，但 bindingStatus 不是 ACTIVE，通知链路不会命中它。",
                    "需要先将路由状态切换为 ACTIVE，再观察通知派发。");
        }
        if (routing.getEffectiveTime() != null && routing.getEffectiveTime().isAfter(now)) {
            return new RoutingSnapshot("PENDING_EFFECTIVE", "路由未生效",
                    "当前已存在路由记录，但生效时间尚未到达。",
                    "需要等待路由生效时间到达，或调整生效时间。");
        }
        if (routing.getExpireTime() != null && !routing.getExpireTime().isAfter(now)) {
            return new RoutingSnapshot("EXPIRED_ROUTE", "路由已过期",
                    "当前存在路由记录，但已经超过失效时间。",
                    "需要续期或重新绑定有效路由后，再重试通知。");
        }
        return new RoutingSnapshot("ACTIVE_ROUTE", "路由有效",
                "当前 storeId 已命中有效 managerAdminUserId，可进入可派发状态。",
                "当前无需修复，可继续观察通知派发结果。");
    }

    private static final class RoutingSnapshot {
        private final String status;
        private final String label;
        private final String detail;
        private final String repairHint;

        private RoutingSnapshot(String status, String label, String detail, String repairHint) {
            this.status = status;
            this.label = label;
            this.detail = detail;
            this.repairHint = repairHint;
        }
    }
}
