package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import com.hxy.module.booking.controller.admin.vo.TechnicianCommissionSettlementNotifyOutboxPageReqVO;
import com.hxy.module.booking.dal.dataobject.TechnicianCommissionSettlementNotifyOutboxDO;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MapperScan("com.hxy.module.booking.dal.mysql")
class TechnicianCommissionSettlementNotifyOutboxMapperTest extends BaseDbUnitTest {

    @Resource
    private TechnicianCommissionSettlementNotifyOutboxMapper mapper;

    @Test
    void shouldSelectPageByExceptionFiltersAndLastActionTime() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        TechnicianCommissionSettlementNotifyOutboxDO target = insertOutbox(1001L, "P1_WARN", "IN_APP",
                2, "DISPATCH_FAILED", "OUTBOX#A", now.minusMinutes(1));
        insertOutbox(1002L, "P1_WARN", "IN_APP",
                2, "DISPATCH_FAILED", "OUTBOX#B", now.minusMinutes(1));
        insertOutbox(1003L, "P0_ESCALATE", "IN_APP",
                2, "DISPATCH_FAILED", "OUTBOX#A", now.minusMinutes(1));
        insertOutbox(1004L, "P1_WARN", "IN_APP",
                1, "DISPATCH_SUCCESS", "OUTBOX#A", now.minusMinutes(1));
        insertOutbox(1005L, "P1_WARN", "IN_APP",
                2, "DISPATCH_FAILED", "OUTBOX#A", now.minusHours(2));

        TechnicianCommissionSettlementNotifyOutboxPageReqVO reqVO = new TechnicianCommissionSettlementNotifyOutboxPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setStatus(2);
        reqVO.setNotifyType("P1_WARN");
        reqVO.setChannel("IN_APP");
        reqVO.setLastActionCode("DISPATCH_FAILED");
        reqVO.setLastActionBizNo("OUTBOX#A");
        reqVO.setLastActionTime(new LocalDateTime[]{now.minusMinutes(5), now});

        PageResult<TechnicianCommissionSettlementNotifyOutboxDO> page = mapper.selectPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(target.getId(), page.getList().get(0).getId());
    }

    private TechnicianCommissionSettlementNotifyOutboxDO insertOutbox(Long settlementId,
                                                                       String notifyType,
                                                                       String channel,
                                                                       Integer status,
                                                                       String lastActionCode,
                                                                       String lastActionBizNo,
                                                                       LocalDateTime lastActionTime) {
        TechnicianCommissionSettlementNotifyOutboxDO row = new TechnicianCommissionSettlementNotifyOutboxDO();
        row.setSettlementId(settlementId);
        row.setNotifyType(notifyType);
        row.setChannel(channel);
        row.setSeverity("P1");
        row.setBizKey("BIZ#" + settlementId + "#" + notifyType + "#" + status + "#" + lastActionBizNo);
        row.setStatus(status);
        row.setRetryCount(status);
        row.setLastErrorMsg("");
        row.setLastActionCode(lastActionCode);
        row.setLastActionBizNo(lastActionBizNo);
        row.setLastActionTime(lastActionTime);
        mapper.insert(row);
        return row;
    }
}

