package cn.iocoder.yudao.server.service.member;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.member.controller.app.point.vo.AppMemberPointRecordPageReqVO;
import cn.iocoder.yudao.module.member.dal.dataobject.point.MemberPointRecordDO;
import cn.iocoder.yudao.module.member.service.point.MemberPointRecordService;
import cn.iocoder.yudao.module.pay.controller.app.wallet.vo.transaction.AppPayWalletTransactionPageReqVO;
import cn.iocoder.yudao.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import cn.iocoder.yudao.module.pay.service.wallet.PayWalletTransactionService;
import cn.iocoder.yudao.module.promotion.controller.app.coupon.vo.coupon.AppCouponPageReqVO;
import cn.iocoder.yudao.module.promotion.convert.coupon.CouponConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.coupon.CouponDO;
import cn.iocoder.yudao.module.promotion.service.coupon.CouponService;
import cn.iocoder.yudao.server.controller.app.member.vo.AppMemberAssetLedgerPageReqVO;
import cn.iocoder.yudao.server.controller.app.member.vo.AppMemberAssetLedgerPageRespVO;
import cn.iocoder.yudao.server.controller.app.member.vo.AppMemberAssetLedgerRespVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class AppMemberAssetLedgerService {

    private static final String ASSET_TYPE_WALLET = "WALLET";
    private static final String ASSET_TYPE_POINT = "POINT";
    private static final String ASSET_TYPE_COUPON = "COUPON";

    @Resource
    private MemberPointRecordService memberPointRecordService;
    @Resource
    private PayWalletTransactionService payWalletTransactionService;
    @Resource
    private CouponService couponService;

    public AppMemberAssetLedgerPageRespVO getAssetLedgerPage(Long userId, AppMemberAssetLedgerPageReqVO reqVO) {
        List<AppMemberAssetLedgerRespVO> ledger = new ArrayList<>();
        int fetchSize = Math.min(reqVO.getPageNo() * reqVO.getPageSize(), 200);

        if (matchesAssetType(reqVO.getAssetType(), ASSET_TYPE_WALLET)) {
            ledger.addAll(convertWalletRecords(fetchWalletRecords(userId, fetchSize)));
        }
        if (matchesAssetType(reqVO.getAssetType(), ASSET_TYPE_POINT)) {
            ledger.addAll(convertPointRecords(fetchPointRecords(userId, fetchSize)));
        }
        if (matchesAssetType(reqVO.getAssetType(), ASSET_TYPE_COUPON)) {
            ledger.addAll(convertCouponRecords(fetchCouponRecords(userId, fetchSize)));
        }

        ledger.sort(Comparator.comparing(AppMemberAssetLedgerRespVO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AppMemberAssetLedgerRespVO::getLedgerId, Comparator.nullsLast(Comparator.reverseOrder())));

        int fromIndex = Math.min((reqVO.getPageNo() - 1) * reqVO.getPageSize(), ledger.size());
        int toIndex = Math.min(fromIndex + reqVO.getPageSize(), ledger.size());

        AppMemberAssetLedgerPageRespVO respVO = new AppMemberAssetLedgerPageRespVO();
        respVO.setList(new ArrayList<>(ledger.subList(fromIndex, toIndex)));
        respVO.setTotal((long) ledger.size());
        respVO.setDegraded(Boolean.FALSE);
        respVO.setDegradeReason(null);
        return respVO;
    }

    private boolean matchesAssetType(String requestedAssetType, String candidate) {
        return StrUtil.isBlank(requestedAssetType) || candidate.equalsIgnoreCase(requestedAssetType.trim());
    }

    private List<PayWalletTransactionDO> fetchWalletRecords(Long userId, int fetchSize) {
        AppPayWalletTransactionPageReqVO reqVO = new AppPayWalletTransactionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(fetchSize);
        PageResult<PayWalletTransactionDO> pageResult = payWalletTransactionService.getWalletTransactionPage(
                userId, UserTypeEnum.MEMBER.getValue(), reqVO);
        return pageResult != null && pageResult.getList() != null ? pageResult.getList() : Collections.emptyList();
    }

    private List<MemberPointRecordDO> fetchPointRecords(Long userId, int fetchSize) {
        AppMemberPointRecordPageReqVO reqVO = new AppMemberPointRecordPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(fetchSize);
        PageResult<MemberPointRecordDO> pageResult = memberPointRecordService.getPointRecordPage(userId, reqVO);
        return pageResult != null && pageResult.getList() != null ? pageResult.getList() : Collections.emptyList();
    }

    private List<CouponDO> fetchCouponRecords(Long userId, int fetchSize) {
        AppCouponPageReqVO reqVO = new AppCouponPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(fetchSize);
        PageResult<CouponDO> pageResult = couponService.getCouponPage(
                CouponConvert.INSTANCE.convert(reqVO, Collections.singleton(userId)));
        return pageResult != null && pageResult.getList() != null ? pageResult.getList() : Collections.emptyList();
    }

    private List<AppMemberAssetLedgerRespVO> convertWalletRecords(List<PayWalletTransactionDO> records) {
        List<AppMemberAssetLedgerRespVO> result = new ArrayList<>(records.size());
        for (PayWalletTransactionDO record : records) {
            AppMemberAssetLedgerRespVO respVO = new AppMemberAssetLedgerRespVO();
            respVO.setLedgerId(record.getId());
            respVO.setAssetType(ASSET_TYPE_WALLET);
            respVO.setBizType(String.valueOf(record.getBizType()));
            respVO.setTitle(record.getTitle());
            respVO.setDescription("钱包流水");
            respVO.setAmount(record.getPrice() == null ? null : record.getPrice().longValue());
            respVO.setBalanceAfter(record.getBalance() == null ? null : record.getBalance().longValue());
            respVO.setSourceBizNo(record.getBizId());
            respVO.setRunId(record.getNo());
            respVO.setCreateTime(record.getCreateTime());
            result.add(respVO);
        }
        return result;
    }

    private List<AppMemberAssetLedgerRespVO> convertPointRecords(List<MemberPointRecordDO> records) {
        List<AppMemberAssetLedgerRespVO> result = new ArrayList<>(records.size());
        for (MemberPointRecordDO record : records) {
            AppMemberAssetLedgerRespVO respVO = new AppMemberAssetLedgerRespVO();
            respVO.setLedgerId(record.getId());
            respVO.setAssetType(ASSET_TYPE_POINT);
            respVO.setBizType(String.valueOf(record.getBizType()));
            respVO.setTitle(record.getTitle());
            respVO.setDescription(record.getDescription());
            respVO.setAmount(record.getPoint() == null ? null : record.getPoint().longValue());
            respVO.setBalanceAfter(record.getTotalPoint() == null ? null : record.getTotalPoint().longValue());
            respVO.setSourceBizNo(record.getBizId());
            respVO.setRunId("POINT-" + record.getId());
            respVO.setCreateTime(record.getCreateTime());
            result.add(respVO);
        }
        return result;
    }

    private List<AppMemberAssetLedgerRespVO> convertCouponRecords(List<CouponDO> records) {
        List<AppMemberAssetLedgerRespVO> result = new ArrayList<>(records.size());
        for (CouponDO record : records) {
            AppMemberAssetLedgerRespVO respVO = new AppMemberAssetLedgerRespVO();
            respVO.setLedgerId(record.getId());
            respVO.setAssetType(ASSET_TYPE_COUPON);
            respVO.setBizType(String.valueOf(record.getStatus()));
            respVO.setTitle(record.getName());
            respVO.setDescription("优惠券资产");
            respVO.setAmount(resolveCouponAmount(record));
            respVO.setBalanceAfter(null);
            respVO.setSourceBizNo(record.getUseOrderId() == null ? String.valueOf(record.getTemplateId()) : String.valueOf(record.getUseOrderId()));
            respVO.setRunId("COUPON-" + record.getId());
            respVO.setCreateTime(record.getCreateTime());
            result.add(respVO);
        }
        return result;
    }

    private Long resolveCouponAmount(CouponDO record) {
        if (record.getDiscountPrice() != null) {
            return record.getDiscountPrice().longValue();
        }
        if (record.getDiscountPercent() != null) {
            return record.getDiscountPercent().longValue();
        }
        return 0L;
    }
}
