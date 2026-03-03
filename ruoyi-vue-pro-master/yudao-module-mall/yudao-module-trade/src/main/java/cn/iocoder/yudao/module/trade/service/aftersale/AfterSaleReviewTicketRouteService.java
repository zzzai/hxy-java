package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRoutePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRouteUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 售后复核工单路由规则 Service
 */
public interface AfterSaleReviewTicketRouteService {

    Long createRoute(@Valid AfterSaleReviewTicketRouteCreateReqVO createReqVO);

    void updateRoute(@Valid AfterSaleReviewTicketRouteUpdateReqVO updateReqVO);

    void deleteRoute(Long id);

    AfterSaleReviewTicketRouteDO getRoute(Long id);

    PageResult<AfterSaleReviewTicketRouteDO> getRoutePage(AfterSaleReviewTicketRoutePageReqVO pageReqVO);

    List<AfterSaleReviewTicketRouteDO> getEnabledRouteList();

}
