package com.hxy.module.booking.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementPageReqVO;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementNotifyOutboxPageReqVO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementLogDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementNotifyOutboxDO;
import com.hxy.module.booking.service.dto.TechnicianCommissionNotifyBatchRetryResult;

import java.util.List;

/**
 * 技师佣金结算单 Service
 */
public interface TechnicianCommissionSettlementService {

    Long createSettlement(List<Long> commissionIds, String remark);

    void submitForReview(Long settlementId, Integer slaMinutes, String submitRemark);

    void approve(Long settlementId, Long reviewerId, String reviewRemark);

    void reject(Long settlementId, Long reviewerId, String rejectReason);

    void markPaid(Long settlementId, Long payerId, String payVoucherNo, String payRemark);

    int expireOverduePending(Integer limit);

    int warnNearDeadlinePending(Integer leadMinutes, Integer limit);

    int escalateOverduePendingToP0(Integer delayMinutes, Integer limit);

    int dispatchPendingNotifyOutbox(Integer limit);

    TechnicianCommissionSettlementDO getSettlement(Long id);

    PageResult<TechnicianCommissionSettlementDO> getSettlementPage(TechnicianCommissionSettlementPageReqVO pageReqVO);

    List<TechnicianCommissionSettlementDO> getSettlementList(Long technicianId, Integer status);

    List<TechnicianCommissionSettlementDO> getSlaOverduePendingList(Integer limit);

    List<TechnicianCommissionSettlementLogDO> getOperationLogList(Long settlementId);

    List<TechnicianCommissionSettlementNotifyOutboxDO> getNotifyOutboxList(Long settlementId, Integer status, Integer limit);

    PageResult<TechnicianCommissionSettlementNotifyOutboxDO> getNotifyOutboxPage(
            TechnicianCommissionSettlementNotifyOutboxPageReqVO pageReqVO);

    default int retryNotifyOutbox(List<Long> ids, String reason) {
        return retryNotifyOutbox(ids, null, reason);
    }

    int retryNotifyOutbox(List<Long> ids, Long operatorId, String reason);

    default TechnicianCommissionNotifyBatchRetryResult retryNotifyOutboxBatch(List<Long> ids, String reason) {
        return retryNotifyOutboxBatch(ids, null, reason);
    }

    TechnicianCommissionNotifyBatchRetryResult retryNotifyOutboxBatch(List<Long> ids, Long operatorId, String reason);
}
