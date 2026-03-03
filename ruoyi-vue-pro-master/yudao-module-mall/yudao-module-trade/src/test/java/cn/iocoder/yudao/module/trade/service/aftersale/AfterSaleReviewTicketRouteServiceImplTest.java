package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteCreateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketRouteMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
import cn.iocoder.yudao.module.trade.service.aftersale.bo.AfterSaleReviewTicketRouteResolveRespBO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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

    @Test
    void shouldBatchUpdateEnabledAndInvalidateCache() {
        AfterSaleReviewTicketRouteDO row1 = new AfterSaleReviewTicketRouteDO();
        row1.setId(1L);
        AfterSaleReviewTicketRouteDO row2 = new AfterSaleReviewTicketRouteDO();
        row2.setId(2L);
        when(routeMapper.selectById(1L)).thenReturn(row1);
        when(routeMapper.selectById(2L)).thenReturn(row2);
        when(routeMapper.updateById(any(AfterSaleReviewTicketRouteDO.class))).thenReturn(1);

        int count = service.batchUpdateRouteEnabled(Arrays.asList(1L, 2L, 2L, null, -1L), true);

        assertEquals(2, count);
        verify(routeMapper, times(2)).updateById(any(AfterSaleReviewTicketRouteDO.class));
        verify(routeProvider).invalidateCache();
    }

    @Test
    void shouldResolveByDecisionOrder() {
        AfterSaleReviewTicketRouteDO rule = buildRoute(11L, "RULE", "BLACKLIST_USER", 0, "P0", "HQ_RISK", 30, 1, true);
        AfterSaleReviewTicketRouteDO typeSeverity = buildRoute(12L, "TYPE_SEVERITY", "", 10, "P1", "HQ_OPS", 60, 2, true);
        AfterSaleReviewTicketRouteDO typeDefault = buildRoute(13L, "TYPE_DEFAULT", "", 10, "P1", "HQ_SERVICE", 120, 3, true);
        AfterSaleReviewTicketRouteDO global = buildRoute(14L, "GLOBAL_DEFAULT", "", 0, "P1", "HQ_AFTER_SALE", 180, 4, true);
        when(routeMapper.selectListByEnabled(Boolean.TRUE)).thenReturn(Arrays.asList(rule, typeSeverity, typeDefault, global));

        AfterSaleReviewTicketRouteResolveRespBO byRule = service.resolveRoute(10, "P1", "blacklist_user");
        assertEquals("RULE", byRule.getMatchedScope());
        assertEquals(11L, byRule.getRouteId());

        AfterSaleReviewTicketRouteResolveRespBO byTypeSeverity = service.resolveRoute(10, "P1", "");
        assertEquals("TYPE_SEVERITY", byTypeSeverity.getMatchedScope());
        assertEquals(12L, byTypeSeverity.getRouteId());

        AfterSaleReviewTicketRouteResolveRespBO byTypeDefault = service.resolveRoute(10, "P0", "");
        assertEquals("TYPE_DEFAULT", byTypeDefault.getMatchedScope());
        assertEquals(13L, byTypeDefault.getRouteId());

        AfterSaleReviewTicketRouteResolveRespBO byGlobal = service.resolveRoute(99, "P0", "");
        assertEquals("GLOBAL_DEFAULT", byGlobal.getMatchedScope());
        assertEquals(14L, byGlobal.getRouteId());
    }

    @Test
    void shouldResolveToFallbackWhenNoEnabledRoute() {
        when(routeMapper.selectListByEnabled(Boolean.TRUE)).thenReturn(Collections.emptyList());

        AfterSaleReviewTicketRouteResolveRespBO resolved = service.resolveRoute(10, "P1", "MISSING");

        assertEquals("GLOBAL_DEFAULT_FALLBACK", resolved.getMatchedScope());
        assertEquals("P1", resolved.getSeverity());
        assertEquals("HQ_AFTER_SALE", resolved.getEscalateTo());
        assertEquals(120, resolved.getSlaMinutes());
    }

    private static AfterSaleReviewTicketRouteDO buildRoute(Long id, String scope, String ruleCode, Integer ticketType,
                                                           String severity, String escalateTo, Integer slaMinutes,
                                                           Integer sort, Boolean enabled) {
        AfterSaleReviewTicketRouteDO route = new AfterSaleReviewTicketRouteDO();
        route.setId(id);
        route.setScope(scope);
        route.setRuleCode(ruleCode);
        route.setTicketType(ticketType);
        route.setSeverity(severity);
        route.setEscalateTo(escalateTo);
        route.setSlaMinutes(slaMinutes);
        route.setSort(sort);
        route.setEnabled(enabled);
        return route;
    }

}
