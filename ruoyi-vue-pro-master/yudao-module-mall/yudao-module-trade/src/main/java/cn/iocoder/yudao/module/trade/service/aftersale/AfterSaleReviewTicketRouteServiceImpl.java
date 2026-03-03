package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRoutePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleReviewTicketRouteMapper;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketRouteScopeEnum;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_REVIEW_TICKET_ROUTE_KEY_CONFLICT;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_REVIEW_TICKET_ROUTE_NOT_FOUND;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.AFTER_SALE_REVIEW_TICKET_ROUTE_SCOPE_INVALID;

/**
 * 售后复核工单路由规则 Service 实现
 */
@Service
@Validated
public class AfterSaleReviewTicketRouteServiceImpl implements AfterSaleReviewTicketRouteService {

    private static final String DEFAULT_SEVERITY = "P1";

    @Resource
    private AfterSaleReviewTicketRouteMapper routeMapper;
    @Resource
    private AfterSaleReviewTicketRouteProvider routeProvider;

    @Override
    public Long createRoute(AfterSaleReviewTicketRouteCreateReqVO createReqVO) {
        AfterSaleReviewTicketRouteDO route = normalizeAndValidate(createReqVO);
        validateUniqueKey(route, null);
        routeMapper.insert(route);
        routeProvider.invalidateCache();
        return route.getId();
    }

    @Override
    public void updateRoute(AfterSaleReviewTicketRouteUpdateReqVO updateReqVO) {
        validateRouteExists(updateReqVO.getId());
        AfterSaleReviewTicketRouteDO route = normalizeAndValidate(updateReqVO);
        route.setId(updateReqVO.getId());
        validateUniqueKey(route, updateReqVO.getId());
        routeMapper.updateById(route);
        routeProvider.invalidateCache();
    }

    @Override
    public void deleteRoute(Long id) {
        validateRouteExists(id);
        routeMapper.deleteById(id);
        routeProvider.invalidateCache();
    }

    @Override
    public AfterSaleReviewTicketRouteDO getRoute(Long id) {
        return routeMapper.selectById(id);
    }

    @Override
    public PageResult<AfterSaleReviewTicketRouteDO> getRoutePage(AfterSaleReviewTicketRoutePageReqVO pageReqVO) {
        return routeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AfterSaleReviewTicketRouteDO> getEnabledRouteList() {
        return routeMapper.selectListByEnabled(Boolean.TRUE);
    }

    private void validateRouteExists(Long id) {
        if (routeMapper.selectById(id) == null) {
            throw exception(AFTER_SALE_REVIEW_TICKET_ROUTE_NOT_FOUND);
        }
    }

    private void validateUniqueKey(AfterSaleReviewTicketRouteDO route, Long id) {
        AfterSaleReviewTicketRouteDO existed = routeMapper.selectByUniqueKey(route.getScope(), route.getRuleCode(),
                route.getTicketType(), route.getSeverity());
        if (existed == null) {
            return;
        }
        if (id == null || !ObjUtil.equal(id, existed.getId())) {
            throw exception(AFTER_SALE_REVIEW_TICKET_ROUTE_KEY_CONFLICT);
        }
    }

    private AfterSaleReviewTicketRouteDO normalizeAndValidate(AfterSaleReviewTicketRouteCreateReqVO reqVO) {
        AfterSaleReviewTicketRouteDO route = new AfterSaleReviewTicketRouteDO();
        route.setScope(normalizeUpper(reqVO.getScope()));
        route.setRuleCode(normalizeUpper(reqVO.getRuleCode()));
        route.setTicketType(ObjUtil.defaultIfNull(reqVO.getTicketType(), 0));
        route.setSeverity(normalizeUpper(reqVO.getSeverity()));
        route.setEscalateTo(StrUtil.trim(reqVO.getEscalateTo()));
        route.setSlaMinutes(reqVO.getSlaMinutes());
        route.setEnabled(reqVO.getEnabled());
        route.setSort(ObjUtil.defaultIfNull(reqVO.getSort(), 0));
        route.setRemark(StrUtil.blankToDefault(StrUtil.trim(reqVO.getRemark()), ""));
        normalizeByScope(route);
        return route;
    }

    private AfterSaleReviewTicketRouteDO normalizeAndValidate(AfterSaleReviewTicketRouteUpdateReqVO reqVO) {
        AfterSaleReviewTicketRouteCreateReqVO convert = new AfterSaleReviewTicketRouteCreateReqVO();
        convert.setScope(reqVO.getScope());
        convert.setRuleCode(reqVO.getRuleCode());
        convert.setTicketType(reqVO.getTicketType());
        convert.setSeverity(reqVO.getSeverity());
        convert.setEscalateTo(reqVO.getEscalateTo());
        convert.setSlaMinutes(reqVO.getSlaMinutes());
        convert.setEnabled(reqVO.getEnabled());
        convert.setSort(reqVO.getSort());
        convert.setRemark(reqVO.getRemark());
        return normalizeAndValidate(convert);
    }

    private void normalizeByScope(AfterSaleReviewTicketRouteDO route) {
        String scope = route.getScope();
        if (StrUtil.equals(scope, AfterSaleReviewTicketRouteScopeEnum.RULE.getScope())) {
            if (StrUtil.isBlank(route.getRuleCode()) || StrUtil.isBlank(route.getSeverity())) {
                throw exception(AFTER_SALE_REVIEW_TICKET_ROUTE_SCOPE_INVALID);
            }
            route.setTicketType(0);
            return;
        }
        if (StrUtil.equals(scope, AfterSaleReviewTicketRouteScopeEnum.TYPE_SEVERITY.getScope())) {
            if (route.getTicketType() == null || route.getTicketType() <= 0 || StrUtil.isBlank(route.getSeverity())) {
                throw exception(AFTER_SALE_REVIEW_TICKET_ROUTE_SCOPE_INVALID);
            }
            route.setRuleCode("");
            return;
        }
        if (StrUtil.equals(scope, AfterSaleReviewTicketRouteScopeEnum.TYPE_DEFAULT.getScope())) {
            if (route.getTicketType() == null || route.getTicketType() <= 0) {
                throw exception(AFTER_SALE_REVIEW_TICKET_ROUTE_SCOPE_INVALID);
            }
            route.setRuleCode("");
            route.setSeverity(DEFAULT_SEVERITY);
            return;
        }
        if (StrUtil.equals(scope, AfterSaleReviewTicketRouteScopeEnum.GLOBAL_DEFAULT.getScope())) {
            route.setRuleCode("");
            route.setTicketType(0);
            route.setSeverity(DEFAULT_SEVERITY);
            return;
        }
        throw exception(AFTER_SALE_REVIEW_TICKET_ROUTE_SCOPE_INVALID);
    }

    private String normalizeUpper(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "").toUpperCase(Locale.ROOT);
    }

}
