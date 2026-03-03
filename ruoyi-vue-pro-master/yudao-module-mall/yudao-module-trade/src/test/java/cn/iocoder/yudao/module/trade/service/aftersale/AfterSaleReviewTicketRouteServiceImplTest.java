package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteCreateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketRouteMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfterSaleReviewTicketRouteServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AfterSaleReviewTicketRouteServiceImpl service;

    @Mock
    private AfterSaleReviewTicketRouteMapper routeMapper;

    @Mock
    private AfterSaleReviewTicketRouteProvider routeProvider;

    @Test
    void shouldCreateRuleRouteAndInvalidateCache() {
        AfterSaleReviewTicketRouteCreateReqVO reqVO = new AfterSaleReviewTicketRouteCreateReqVO();
        reqVO.setScope(AfterSaleReviewTicketRouteScopeEnum.RULE.getScope());
        reqVO.setRuleCode(" blacklist_user ");
        reqVO.setTicketType(99);
        reqVO.setSeverity("p0");
        reqVO.setEscalateTo("HQ_RISK_FINANCE");
        reqVO.setSlaMinutes(30);
        reqVO.setEnabled(true);
        reqVO.setSort(1);
        reqVO.setRemark("risk");

        when(routeMapper.selectByUniqueKey("RULE", "BLACKLIST_USER", 0, "P0")).thenReturn(null);
        when(routeMapper.insert(any(AfterSaleReviewTicketRouteDO.class))).thenAnswer(invocation -> {
            AfterSaleReviewTicketRouteDO row = invocation.getArgument(0);
            row.setId(101L);
            return 1;
        });

        Long id = service.createRoute(reqVO);

        assertEquals(101L, id);
        ArgumentCaptor<AfterSaleReviewTicketRouteDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketRouteDO.class);
        verify(routeMapper).insert(captor.capture());
        assertEquals("RULE", captor.getValue().getScope());
        assertEquals("BLACKLIST_USER", captor.getValue().getRuleCode());
        assertEquals(0, captor.getValue().getTicketType());
        assertEquals("P0", captor.getValue().getSeverity());
        verify(routeProvider).invalidateCache();
    }

    @Test
    void shouldThrowWhenScopeFieldsInvalid() {
        AfterSaleReviewTicketRouteCreateReqVO reqVO = new AfterSaleReviewTicketRouteCreateReqVO();
        reqVO.setScope(AfterSaleReviewTicketRouteScopeEnum.TYPE_SEVERITY.getScope());
        reqVO.setRuleCode("");
        reqVO.setTicketType(null);
        reqVO.setSeverity("P1");
        reqVO.setEscalateTo("HQ_AFTER_SALE");
        reqVO.setSlaMinutes(120);
        reqVO.setEnabled(true);
        reqVO.setSort(0);

        assertThrows(ServiceException.class, () -> service.createRoute(reqVO));
        verify(routeMapper, never()).insert(any(AfterSaleReviewTicketRouteDO.class));
    }

    @Test
    void shouldThrowWhenUniqueKeyConflict() {
        AfterSaleReviewTicketRouteCreateReqVO reqVO = new AfterSaleReviewTicketRouteCreateReqVO();
        reqVO.setScope(AfterSaleReviewTicketRouteScopeEnum.GLOBAL_DEFAULT.getScope());
        reqVO.setRuleCode("");
        reqVO.setTicketType(0);
        reqVO.setSeverity("P1");
        reqVO.setEscalateTo("HQ_AFTER_SALE");
        reqVO.setSlaMinutes(120);
        reqVO.setEnabled(true);
        reqVO.setSort(0);

        AfterSaleReviewTicketRouteDO existed = new AfterSaleReviewTicketRouteDO();
        existed.setId(8L);
        when(routeMapper.selectByUniqueKey(eq("GLOBAL_DEFAULT"), eq(""), eq(0), eq("P1"))).thenReturn(existed);

        assertThrows(ServiceException.class, () -> service.createRoute(reqVO));
        verify(routeMapper, never()).insert(any(AfterSaleReviewTicketRouteDO.class));
    }

    @Test
    void shouldNormalizeTypeDefaultSeverityToP1() {
        AfterSaleReviewTicketRouteCreateReqVO reqVO = new AfterSaleReviewTicketRouteCreateReqVO();
        reqVO.setScope(AfterSaleReviewTicketRouteScopeEnum.TYPE_DEFAULT.getScope());
        reqVO.setRuleCode("ANY");
        reqVO.setTicketType(20);
        reqVO.setSeverity("P0");
        reqVO.setEscalateTo("HQ_SERVICE_OPS");
        reqVO.setSlaMinutes(90);
        reqVO.setEnabled(true);
        reqVO.setSort(2);

        when(routeMapper.selectByUniqueKey("TYPE_DEFAULT", "", 20, "P1")).thenReturn(null);
        when(routeMapper.insert(any(AfterSaleReviewTicketRouteDO.class))).thenAnswer(invocation -> {
            AfterSaleReviewTicketRouteDO row = invocation.getArgument(0);
            row.setId(102L);
            return 1;
        });

        Long id = service.createRoute(reqVO);

        assertEquals(102L, id);
        ArgumentCaptor<AfterSaleReviewTicketRouteDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketRouteDO.class);
        verify(routeMapper).insert(captor.capture());
        assertEquals("TYPE_DEFAULT", captor.getValue().getScope());
        assertEquals("", captor.getValue().getRuleCode());
        assertEquals(20, captor.getValue().getTicketType());
        assertEquals("P1", captor.getValue().getSeverity());
    }

    @Test
    void shouldNormalizeGlobalDefaultSeverityToP1() {
        AfterSaleReviewTicketRouteCreateReqVO reqVO = new AfterSaleReviewTicketRouteCreateReqVO();
        reqVO.setScope(AfterSaleReviewTicketRouteScopeEnum.GLOBAL_DEFAULT.getScope());
        reqVO.setRuleCode("ANY");
        reqVO.setTicketType(999);
        reqVO.setSeverity("");
        reqVO.setEscalateTo("HQ_AFTER_SALE");
        reqVO.setSlaMinutes(120);
        reqVO.setEnabled(true);
        reqVO.setSort(0);

        when(routeMapper.selectByUniqueKey("GLOBAL_DEFAULT", "", 0, "P1")).thenReturn(null);
        when(routeMapper.insert(any(AfterSaleReviewTicketRouteDO.class))).thenAnswer(invocation -> {
            AfterSaleReviewTicketRouteDO row = invocation.getArgument(0);
            row.setId(103L);
            return 1;
        });

        Long id = service.createRoute(reqVO);

        assertEquals(103L, id);
        ArgumentCaptor<AfterSaleReviewTicketRouteDO> captor = ArgumentCaptor.forClass(AfterSaleReviewTicketRouteDO.class);
        verify(routeMapper).insert(captor.capture());
        assertEquals("GLOBAL_DEFAULT", captor.getValue().getScope());
        assertEquals("", captor.getValue().getRuleCode());
        assertEquals(0, captor.getValue().getTicketType());
        assertEquals("P1", captor.getValue().getSeverity());
    }

}
