package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketCreateReqVO;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketBatchResolveReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketBatchResolveRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketPageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketResolveReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketTypeEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketService;
import cn.iocoder.yudao.module.trade.service.aftersale.dto.AfterSaleReviewTicketBatchResolveResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class AfterSaleReviewTicketControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleReviewTicketController controller;

    @Mock
    private AfterSaleReviewTicketService afterSaleReviewTicketService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetReviewTicketPage() {
        AfterSaleReviewTicketPageReqVO reqVO = new AfterSaleReviewTicketPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);

        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(1L);
        ticket.setAfterSaleId(101L);
        ticket.setStatus(AfterSaleReviewTicketStatusEnum.PENDING.getStatus());
        ticket.setSlaDeadlineTime(LocalDateTime.now().minusMinutes(5));
        ticket.setLastActionCode("SLA_AUTO_ESCALATE");
        ticket.setLastActionBizNo("TICKET#1");
        ticket.setLastActionTime(LocalDateTime.now().minusMinutes(1));

        when(afterSaleReviewTicketService.getReviewTicketPage(reqVO))
                .thenReturn(new PageResult<>(Collections.singletonList(ticket), 1L));

        CommonResult<PageResult<AfterSaleReviewTicketRespVO>> result = controller.getReviewTicketPage(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(1L, result.getData().getList().get(0).getId());
        assertTrue(result.getData().getList().get(0).getOverdue());
        assertEquals("SLA_AUTO_ESCALATE", result.getData().getList().get(0).getLastActionCode());
        assertEquals("TICKET#1", result.getData().getList().get(0).getLastActionBizNo());
        assertNotNull(result.getData().getList().get(0).getLastActionTime());
        verify(afterSaleReviewTicketService).getReviewTicketPage(reqVO);
    }

    @Test
    void shouldGetReviewTicket() {
        AfterSaleReviewTicketDO ticket = new AfterSaleReviewTicketDO();
        ticket.setId(9L);
        ticket.setAfterSaleId(9001L);
        ticket.setStatus(AfterSaleReviewTicketStatusEnum.RESOLVED.getStatus());
        ticket.setSlaDeadlineTime(LocalDateTime.now().minusMinutes(10));
        ticket.setLastActionCode("MANUAL_RESOLVE");
        ticket.setLastActionBizNo("OPS-202603020001");
        ticket.setLastActionTime(LocalDateTime.now().minusMinutes(3));

        when(afterSaleReviewTicketService.getReviewTicket(9L)).thenReturn(ticket);

        CommonResult<AfterSaleReviewTicketRespVO> result = controller.getReviewTicket(9L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(9L, result.getData().getId());
        assertEquals(9001L, result.getData().getAfterSaleId());
        assertFalse(result.getData().getOverdue());
        assertEquals("MANUAL_RESOLVE", result.getData().getLastActionCode());
        assertEquals("OPS-202603020001", result.getData().getLastActionBizNo());
        assertNotNull(result.getData().getLastActionTime());
        verify(afterSaleReviewTicketService).getReviewTicket(9L);
    }

    @Test
    void shouldResolveReviewTicket() {
        mockLoginUser(88L);

        AfterSaleReviewTicketResolveReqVO reqVO = new AfterSaleReviewTicketResolveReqVO();
        reqVO.setId(11L);
        reqVO.setResolveRemark("manual-reviewed");
        reqVO.setResolveActionCode("MANUAL_RESOLVE");
        reqVO.setResolveBizNo("OPS-202603020001");

        CommonResult<Boolean> result = controller.resolveReviewTicket(reqVO);

        assertTrue(result.isSuccess());
        assertTrue(Boolean.TRUE.equals(result.getData()));
        verify(afterSaleReviewTicketService).resolveManualReviewTicketById(11L, 88L,
                UserTypeEnum.ADMIN.getValue(), "MANUAL_RESOLVE",
                "OPS-202603020001", "manual-reviewed");
    }

    @Test
    void shouldBatchResolveReviewTicket() {
        mockLoginUser(88L);
        AfterSaleReviewTicketBatchResolveReqVO reqVO = new AfterSaleReviewTicketBatchResolveReqVO();
        reqVO.setIds(List.of(11L, 12L, 13L));
        reqVO.setResolveActionCode("MANUAL_RESOLVE");
        reqVO.setResolveBizNo("OPS-BATCH-20260303");
        reqVO.setResolveRemark("batch-close");

        when(afterSaleReviewTicketService.batchResolveManualReviewTicketByIds(
                List.of(11L, 12L, 13L), 88L, UserTypeEnum.ADMIN.getValue(),
                "MANUAL_RESOLVE", "OPS-BATCH-20260303", "batch-close"))
                .thenReturn(AfterSaleReviewTicketBatchResolveResult.builder()
                        .totalCount(3)
                        .successCount(2)
                        .skippedNotFoundCount(1)
                        .skippedNotPendingCount(0)
                        .successIds(List.of(11L, 12L))
                        .skippedNotFoundIds(List.of(13L))
                        .skippedNotPendingIds(List.of())
                        .build());

        CommonResult<AfterSaleReviewTicketBatchResolveRespVO> result = controller.batchResolveReviewTicket(reqVO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getTotalCount());
        assertEquals(2, result.getData().getSuccessCount());
        assertEquals(1, result.getData().getSkippedNotFoundCount());
        assertEquals(0, result.getData().getSkippedNotPendingCount());
        verify(afterSaleReviewTicketService).batchResolveManualReviewTicketByIds(
                List.of(11L, 12L, 13L), 88L, UserTypeEnum.ADMIN.getValue(),
                "MANUAL_RESOLVE", "OPS-BATCH-20260303", "batch-close");
    }

    @Test
    void shouldCreateReviewTicket() {
        AfterSaleReviewTicketCreateReqVO reqVO = new AfterSaleReviewTicketCreateReqVO();
        reqVO.setTicketType(AfterSaleReviewTicketTypeEnum.SERVICE_FULFILLMENT.getType());
        reqVO.setOrderId(100L);
        reqVO.setOrderItemId(101L);
        reqVO.setUserId(102L);
        reqVO.setSourceBizNo("BK202603010001");
        reqVO.setDecisionReason("服务履约超时");
        when(afterSaleReviewTicketService.createReviewTicket(any())).thenReturn(555L);

        CommonResult<Long> result = controller.createReviewTicket(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(555L, result.getData());
        verify(afterSaleReviewTicketService).createReviewTicket(any());
    }

    private void mockLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
    }

}
