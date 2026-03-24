package cn.iocoder.yudao.module.trade.controller.app.referral;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.brokerage.vo.record.BrokerageRecordPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.brokerage.BrokerageRecordDO;
import cn.iocoder.yudao.module.trade.dal.dataobject.brokerage.BrokerageUserDO;
import cn.iocoder.yudao.module.trade.enums.brokerage.BrokerageRecordStatusEnum;
import cn.iocoder.yudao.module.trade.service.brokerage.BrokerageRecordService;
import cn.iocoder.yudao.module.trade.service.brokerage.BrokerageUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.member.enums.ErrorCodeConstants.USER_NOT_EXISTS;

@Tag(name = "用户 App - 邀请有礼")
@RestController
@RequestMapping("/promotion/referral")
@Validated
@RequiredArgsConstructor
public class AppReferralController {

    private final BrokerageUserService brokerageUserService;
    private final BrokerageRecordService brokerageRecordService;

    @PostMapping("/bind-inviter")
    @Operation(summary = "绑定邀请人")
    public CommonResult<AppReferralBindRespVO> bindInviter(@Valid @RequestBody AppReferralBindReqVO reqVO) {
        Long refereeMemberId = getLoginUserId();
        Long inviterMemberId = resolveInviterMemberId(reqVO);
        boolean bound = brokerageUserService.bindBrokerageUser(refereeMemberId, inviterMemberId);
        return success(new AppReferralBindRespVO()
                .setRefereeMemberId(refereeMemberId)
                .setInviterMemberId(inviterMemberId)
                .setBindStatus(bound ? "BOUND" : "UNCHANGED")
                .setIdempotentHit(!bound));
    }

    @GetMapping("/overview")
    @Operation(summary = "获取邀请总览")
    public CommonResult<AppReferralOverviewRespVO> getOverview() {
        Long userId = getLoginUserId();
        BrokerageUserDO brokerageUser = brokerageUserService.getOrCreateBrokerageUser(userId);
        long level1Count = Convert.toLong(brokerageUserService.getBrokerageUserCountByBindUserId(userId, 1), 0L);
        long level2Count = Convert.toLong(brokerageUserService.getBrokerageUserCountByBindUserId(userId, 2), 0L);
        return success(new AppReferralOverviewRespVO()
                .setReferralCode(String.valueOf(userId))
                .setTotalInvites(level1Count + level2Count)
                .setEffectiveInvites(level1Count)
                .setPendingRewardAmount(Convert.toInt(brokerageUser.getFrozenPrice(), 0))
                .setRewardBalance(Convert.toInt(brokerageUser.getBrokeragePrice(), 0))
                .setDegraded(Boolean.FALSE));
    }

    @GetMapping("/reward-ledger/page")
    @Operation(summary = "分页获取奖励台账")
    public CommonResult<PageResult<AppReferralRewardLedgerRespVO>> getRewardLedgerPage(@Valid AppReferralRewardLedgerPageReqVO reqVO) {
        BrokerageRecordPageReqVO pageReqVO = new BrokerageRecordPageReqVO();
        pageReqVO.setPageNo(reqVO.getPageNo());
        pageReqVO.setPageSize(reqVO.getPageSize());
        pageReqVO.setUserId(getLoginUserId());
        pageReqVO.setStatus(parseStatus(reqVO.getStatus()));

        PageResult<BrokerageRecordDO> pageResult = brokerageRecordService.getBrokerageRecordPage(pageReqVO);
        List<AppReferralRewardLedgerRespVO> resultList = pageResult.getList().stream()
                .map(this::convertLedger)
                .collect(Collectors.toList());
        return success(new PageResult<>(resultList, pageResult.getTotal()));
    }

    private Long resolveInviterMemberId(AppReferralBindReqVO reqVO) {
        if (reqVO.getInviterMemberId() != null) {
            return reqVO.getInviterMemberId();
        }
        if (StrUtil.isNotBlank(reqVO.getInviteCode())) {
            return Convert.toLong(reqVO.getInviteCode());
        }
        throw exception(USER_NOT_EXISTS);
    }

    private Integer parseStatus(String status) {
        if (StrUtil.isBlank(status)) {
            return null;
        }
        if (StrUtil.isNumeric(status)) {
            return Convert.toInt(status);
        }
        if (StrUtil.equalsAnyIgnoreCase(status, "SETTLED", "SETTLEMENT")) {
            return BrokerageRecordStatusEnum.SETTLEMENT.getStatus();
        }
        if (StrUtil.equalsAnyIgnoreCase(status, "PENDING", "WAIT_SETTLEMENT")) {
            return BrokerageRecordStatusEnum.WAIT_SETTLEMENT.getStatus();
        }
        if (StrUtil.equalsAnyIgnoreCase(status, "CANCEL", "CANCELLED")) {
            return BrokerageRecordStatusEnum.CANCEL.getStatus();
        }
        return null;
    }

    private AppReferralRewardLedgerRespVO convertLedger(BrokerageRecordDO record) {
        return new AppReferralRewardLedgerRespVO()
                .setLedgerId(record.getId())
                .setOrderId(record.getBizId())
                .setSourceBizNo(record.getBizId())
                .setRewardAmount(Convert.toInt(record.getPrice(), 0))
                .setStatus(record.getStatus())
                .setRunId("0")
                .setPayRefundId("0");
    }
}
