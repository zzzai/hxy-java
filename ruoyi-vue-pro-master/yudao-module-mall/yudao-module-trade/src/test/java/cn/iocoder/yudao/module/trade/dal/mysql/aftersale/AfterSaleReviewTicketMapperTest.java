package cn.iocoder.yudao.module.trade.dal.mysql.aftersale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketPageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfterSaleReviewTicketMapperTest extends BaseDbUnitTest {

    @Resource
    private AfterSaleReviewTicketMapper mapper;

    @Test
    void shouldSelectOnlyOverdueWhenOverdueIsTrue() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        AfterSaleReviewTicketDO overdue = insertTicket("SRC-OVERDUE-YES",
                AfterSaleReviewTicketStatusEnum.PENDING.getStatus(), now.minusMinutes(5),
                "TICKET_CREATE", "SRC-OVERDUE-YES", now.minusMinutes(20));
        insertTicket("SRC-OVERDUE-NO",
                AfterSaleReviewTicketStatusEnum.PENDING.getStatus(), now.plusMinutes(5),
                "TICKET_CREATE", "SRC-OVERDUE-NO", now.minusMinutes(10));
        insertTicket("SRC-OVERDUE-RESOLVED",
                AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus(), now.minusMinutes(30),
                "MANUAL_RESOLVE", "OPS-1", now.minusMinutes(1));

        AfterSaleReviewTicketPageReqVO reqVO = new AfterSaleReviewTicketPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setOverdue(true);

        PageResult<AfterSaleReviewTicketDO> page = mapper.selectPage(reqVO);

        List<String> sourceNos = page.getList().stream().map(AfterSaleReviewTicketDO::getSourceBizNo).toList();
        assertEquals(1, sourceNos.size());
        assertTrue(sourceNos.contains(overdue.getSourceBizNo()));
    }

    @Test
    void shouldSelectByLastActionDimensions() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        AfterSaleReviewTicketDO target = insertTicket("SRC-AUDIT-TARGET",
                AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus(), now.minusMinutes(5),
                "MANUAL_RESOLVE", "OPS-202603020001", now.minusMinutes(2));
        insertTicket("SRC-AUDIT-CODE-MISS",
                AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus(), now.minusMinutes(5),
                "AUTO_RESOLVE", "OPS-202603020001", now.minusMinutes(2));
        insertTicket("SRC-AUDIT-BIZ-MISS",
                AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus(), now.minusMinutes(5),
                "MANUAL_RESOLVE", "OPS-202603020002", now.minusMinutes(2));
        insertTicket("SRC-AUDIT-TIME-MISS",
                AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus(), now.minusMinutes(5),
                "MANUAL_RESOLVE", "OPS-202603020001", now.minusHours(1));

        AfterSaleReviewTicketPageReqVO reqVO = new AfterSaleReviewTicketPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setLastActionCode("MANUAL_RESOLVE");
        reqVO.setLastActionBizNo("OPS-202603020001");
        reqVO.setLastActionTime(new LocalDateTime[]{now.minusMinutes(10), now.plusMinutes(10)});

        PageResult<AfterSaleReviewTicketDO> page = mapper.selectPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(target.getId(), page.getList().get(0).getId());
    }

    private AfterSaleReviewTicketDO insertTicket(String sourceBizNo, Integer status, LocalDateTime deadline,
                                                 String lastActionCode, String lastActionBizNo,
                                                 LocalDateTime lastActionTime) {
        AfterSaleReviewTicketDO row = new AfterSaleReviewTicketDO();
        row.setTicketType(10);
        row.setAfterSaleId(null);
        row.setSourceBizNo(sourceBizNo);
        row.setOrderId(2001L);
        row.setOrderItemId(3001L);
        row.setUserId(4001L);
        row.setRuleCode("AMOUNT_OVER_LIMIT");
        row.setDecisionReason("测试");
        row.setSeverity("P1");
        row.setEscalateTo("HQ_AFTER_SALE");
        row.setSlaDeadlineTime(deadline);
        row.setStatus(status);
        row.setFirstTriggerTime(LocalDateTime.now().minusHours(1));
        row.setLastTriggerTime(LocalDateTime.now().minusMinutes(5));
        row.setTriggerCount(1);
        row.setLastActionCode(lastActionCode);
        row.setLastActionBizNo(lastActionBizNo);
        row.setLastActionTime(lastActionTime);
        row.setResolveActionCode("");
        row.setResolveBizNo("");
        row.setRemark("");
        mapper.insert(row);
        return row;
    }
}
