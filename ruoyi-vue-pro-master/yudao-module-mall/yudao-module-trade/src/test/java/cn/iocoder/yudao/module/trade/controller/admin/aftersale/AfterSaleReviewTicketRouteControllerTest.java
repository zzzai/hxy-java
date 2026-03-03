package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteBatchDeleteReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteBatchEnabledReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRoutePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteResolveReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteResolveRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketRouteService;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketRouteResolveRespBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleReviewTicketRouteControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleReviewTicketRouteController controller;

    @Mock
    private AfterSaleReviewTicketRouteService routeService;

    @Test
    void shouldCreateRoute() {
        AfterSaleReviewTicketRouteCreateReqVO reqVO = new AfterSaleReviewTicketRouteCreateReqVO();
        reqVO.setScope(AfterSaleReviewTicketRouteScopeEnum.RULE.getScope());
        reqVO.setRuleCode("BLACKLIST_USER");
        reqVO.setSeverity("P0");
        reqVO.setEscalateTo("HQ_RISK_FINANCE");
        reqVO.setSlaMinutes(30);
        reqVO.setEnabled(true);
        reqVO.setSort(0);
        when(routeService.createRoute(any())).thenReturn(66L);

        CommonResult<Long> result = controller.createRoute(reqVO);

        assertTrue(result.isSuccess());
        assertEquals(66L, result.getData());
        verify(routeService).createRoute(any());
    }

    @Test
    void shouldGetRoutePage() {
        AfterSaleReviewTicketRouteDO row = new AfterSaleReviewTicketRouteDO();
        row.setId(10L);
        row.setScope("RULE");
        row.setRuleCode("BLACKLIST_USER");
        row.setSeverity("P0");
        row.setEscalateTo("HQ_RISK_FINANCE");
        row.setSlaMinutes(30);
        row.setEnabled(true);
        row.setSort(0);
        when(routeService.getRoutePage(any(AfterSaleReviewTicketRoutePageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(row), 1L));

        CommonResult<PageResult<AfterSaleReviewTicketRouteRespVO>> result =
                controller.getRoutePage(new AfterSaleReviewTicketRoutePageReqVO());

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(10L, result.getData().getList().get(0).getId());
        verify(routeService).getRoutePage(any(AfterSaleReviewTicketRoutePageReqVO.class));
    }

    @Test
    void shouldUpdateAndDeleteRoute() {
        AfterSaleReviewTicketRouteUpdateReqVO updateReqVO = new AfterSaleReviewTicketRouteUpdateReqVO();
        updateReqVO.setId(22L);
        updateReqVO.setScope("GLOBAL_DEFAULT");
        updateReqVO.setSeverity("P1");
        updateReqVO.setEscalateTo("HQ_AFTER_SALE");
        updateReqVO.setSlaMinutes(120);
        updateReqVO.setEnabled(true);
        updateReqVO.setSort(0);

        CommonResult<Boolean> updateResult = controller.updateRoute(updateReqVO);
        CommonResult<Boolean> deleteResult = controller.deleteRoute(22L);

        assertTrue(updateResult.isSuccess());
        assertTrue(deleteResult.isSuccess());
        verify(routeService).updateRoute(updateReqVO);
        verify(routeService).deleteRoute(22L);
    }

    @Test
    void shouldBatchOperateRoutes() {
        AfterSaleReviewTicketRouteBatchEnabledReqVO batchEnabledReqVO = new AfterSaleReviewTicketRouteBatchEnabledReqVO();
        batchEnabledReqVO.setIds(Arrays.asList(1L, 2L, 3L));
        batchEnabledReqVO.setEnabled(true);
        when(routeService.batchUpdateRouteEnabled(any(), eq(true))).thenReturn(3);

        CommonResult<Integer> enabledResult = controller.batchUpdateEnabled(batchEnabledReqVO);

        assertTrue(enabledResult.isSuccess());
        assertEquals(3, enabledResult.getData());
        verify(routeService).batchUpdateRouteEnabled(batchEnabledReqVO.getIds(), true);

        AfterSaleReviewTicketRouteBatchDeleteReqVO batchDeleteReqVO = new AfterSaleReviewTicketRouteBatchDeleteReqVO();
        batchDeleteReqVO.setIds(Arrays.asList(1L, 2L));
        when(routeService.batchDeleteRoute(any())).thenReturn(2);

        CommonResult<Integer> deleteResult = controller.batchDeleteRoute(batchDeleteReqVO);

        assertTrue(deleteResult.isSuccess());
        assertEquals(2, deleteResult.getData());
        verify(routeService).batchDeleteRoute(batchDeleteReqVO.getIds());
    }

    @Test
    void shouldResolveRoutePreview() {
        AfterSaleReviewTicketRouteResolveReqVO reqVO = new AfterSaleReviewTicketRouteResolveReqVO();
        reqVO.setRuleCode("BLACKLIST_USER");
        reqVO.setTicketType(10);
        reqVO.setSeverity("P1");
        AfterSaleReviewTicketRouteResolveRespBO resolved = new AfterSaleReviewTicketRouteResolveRespBO()
                .setMatchedScope("RULE")
                .setRouteId(88L)
                .setRuleCode("BLACKLIST_USER")
                .setTicketType(10)
                .setSeverity("P1")
                .setEscalateTo("HQ_RISK_FINANCE")
                .setSlaMinutes(30)
                .setSort(0)
                .setDecisionOrder("RULE>TYPE_SEVERITY>TYPE_DEFAULT>GLOBAL_DEFAULT");
        when(routeService.resolveRoute(10, "P1", "BLACKLIST_USER")).thenReturn(resolved);

        CommonResult<AfterSaleReviewTicketRouteResolveRespVO> result = controller.resolveRoute(reqVO);

        assertTrue(result.isSuccess());
        assertEquals("RULE", result.getData().getMatchedScope());
        assertEquals(88L, result.getData().getRouteId());
        verify(routeService).resolveRoute(10, "P1", "BLACKLIST_USER");
    }

}
