package cn.iocoder.yudao.module.trade.controller.admin.aftersale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRoutePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteRespVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.AfterSaleReviewTicketRouteService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

}
