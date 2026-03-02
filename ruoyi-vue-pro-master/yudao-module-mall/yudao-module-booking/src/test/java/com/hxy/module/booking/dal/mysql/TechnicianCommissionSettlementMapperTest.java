package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementPageReqVO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementDO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementLogDO;
import com.hxy.module.booking.enums.CommissionSettlementStatusEnum;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MapperScan("com.hxy.module.booking.dal.mysql")
class TechnicianCommissionSettlementMapperTest extends BaseDbUnitTest {

    @Resource
    private TechnicianCommissionSettlementMapper settlementMapper;
    @Resource
    private TechnicianCommissionSettlementLogMapper settlementLogMapper;

    @Test
    void shouldSelectPageBySlaFlagsAndDeadlineRange() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        TechnicianCommissionSettlementDO target = insertSettlement("SET-SLA-TARGET",
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), true, false, now.plusMinutes(10));
        insertSettlement("SET-SLA-WARN-MISS",
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), false, false, now.plusMinutes(10));
        insertSettlement("SET-SLA-ESCALATED",
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), true, true, now.plusMinutes(10));
        insertSettlement("SET-SLA-TIME-OUTSIDE",
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), true, false, now.plusHours(2));

        TechnicianCommissionSettlementPageReqVO reqVO = new TechnicianCommissionSettlementPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setStatus(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        reqVO.setReviewWarned(true);
        reqVO.setReviewEscalated(false);
        reqVO.setReviewDeadlineTime(new LocalDateTime[]{now.minusMinutes(1), now.plusMinutes(30)});

        PageResult<TechnicianCommissionSettlementDO> page = settlementMapper.selectPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(target.getSettlementNo(), page.getList().get(0).getSettlementNo());
    }

    @Test
    void shouldSelectOnlyOverdueWhenOverdueIsTrue() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        TechnicianCommissionSettlementDO overdue = insertSettlement("SET-OVERDUE-YES",
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), false, false, now.minusMinutes(5));
        insertSettlement("SET-OVERDUE-NO",
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), false, false, now.plusMinutes(5));
        insertSettlement("SET-OVERDUE-PAID",
                CommissionSettlementStatusEnum.PAID.getStatus(), false, false, now.minusMinutes(30));

        TechnicianCommissionSettlementPageReqVO reqVO = new TechnicianCommissionSettlementPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setOverdue(true);

        PageResult<TechnicianCommissionSettlementDO> page = settlementMapper.selectPage(reqVO);

        List<String> nos = page.getList().stream().map(TechnicianCommissionSettlementDO::getSettlementNo).toList();
        assertEquals(1, nos.size());
        assertTrue(nos.contains(overdue.getSettlementNo()));
    }

    @Test
    void shouldSelectPageByLastActionDimensions() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        TechnicianCommissionSettlementDO target = insertSettlement("SET-AUDIT-TARGET",
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), true, false, now.plusMinutes(20));
        TechnicianCommissionSettlementDO nonLatestMatch = insertSettlement("SET-AUDIT-NON-LATEST",
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), true, false, now.plusMinutes(20));
        TechnicianCommissionSettlementDO wrongBizNo = insertSettlement("SET-AUDIT-WRONG-BIZ",
                CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus(), true, false, now.plusMinutes(20));

        insertLog(target.getId(), "SUBMIT_REVIEW", "BIZ#SET-AUDIT-TARGET", now.minusMinutes(1));

        insertLog(nonLatestMatch.getId(), "SUBMIT_REVIEW", "BIZ#SET-AUDIT-NON-LATEST", now.minusMinutes(2));
        insertLog(nonLatestMatch.getId(), "APPROVE", "BIZ#SET-AUDIT-NON-LATEST", now.minusMinutes(1));

        insertLog(wrongBizNo.getId(), "SUBMIT_REVIEW", "BIZ#SET-AUDIT-WRONG-BIZ", now.minusMinutes(1));

        TechnicianCommissionSettlementPageReqVO reqVO = new TechnicianCommissionSettlementPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setStatus(CommissionSettlementStatusEnum.PENDING_REVIEW.getStatus());
        reqVO.setLastActionCode("SUBMIT_REVIEW");
        reqVO.setLastActionBizNo("BIZ#SET-AUDIT-TARGET");
        reqVO.setLastActionTime(new LocalDateTime[]{now.minusMinutes(3), now});

        PageResult<TechnicianCommissionSettlementDO> page = settlementMapper.selectPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(target.getSettlementNo(), page.getList().get(0).getSettlementNo());
    }

    private TechnicianCommissionSettlementDO insertSettlement(String settlementNo,
                                                              Integer status,
                                                              Boolean reviewWarned,
                                                              Boolean reviewEscalated,
                                                              LocalDateTime deadline) {
        TechnicianCommissionSettlementDO row = TechnicianCommissionSettlementDO.builder()
                .settlementNo(settlementNo)
                .storeId(2001L)
                .technicianId(1001L)
                .status(status)
                .commissionCount(3)
                .totalCommissionAmount(3000)
                .reviewSubmitTime(LocalDateTime.now().minusMinutes(20))
                .reviewDeadlineTime(deadline)
                .reviewWarned(reviewWarned)
                .reviewEscalated(reviewEscalated)
                .reviewRemark("")
                .rejectReason("")
                .payVoucherNo("")
                .payRemark("")
                .remark("")
                .build();
        settlementMapper.insert(row);
        return row;
    }

    private void insertLog(Long settlementId, String action, String remark, LocalDateTime actionTime) {
        TechnicianCommissionSettlementLogDO log = new TechnicianCommissionSettlementLogDO();
        log.setSettlementId(settlementId);
        log.setAction(action);
        log.setOperateRemark(remark);
        log.setActionTime(actionTime);
        settlementLogMapper.insert(log);
    }
}
