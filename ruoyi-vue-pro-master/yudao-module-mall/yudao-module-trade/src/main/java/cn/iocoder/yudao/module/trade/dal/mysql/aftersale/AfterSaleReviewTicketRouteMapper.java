package cn.iocoder.yudao.module.trade.dal.mysql.aftersale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.route.AfterSaleReviewTicketRoutePageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketRouteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 售后复核工单路由规则 Mapper
 */
@Mapper
public interface AfterSaleReviewTicketRouteMapper extends BaseMapperX<AfterSaleReviewTicketRouteDO> {

    default PageResult<AfterSaleReviewTicketRouteDO> selectPage(AfterSaleReviewTicketRoutePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AfterSaleReviewTicketRouteDO>()
                .eqIfPresent(AfterSaleReviewTicketRouteDO::getScope, reqVO.getScope())
                .eqIfPresent(AfterSaleReviewTicketRouteDO::getRuleCode, reqVO.getRuleCode())
                .eqIfPresent(AfterSaleReviewTicketRouteDO::getTicketType, reqVO.getTicketType())
                .eqIfPresent(AfterSaleReviewTicketRouteDO::getSeverity, reqVO.getSeverity())
                .eqIfPresent(AfterSaleReviewTicketRouteDO::getEnabled, reqVO.getEnabled())
                .orderByAsc(AfterSaleReviewTicketRouteDO::getSort)
                .orderByAsc(AfterSaleReviewTicketRouteDO::getId));
    }

    default List<AfterSaleReviewTicketRouteDO> selectListByEnabled(Boolean enabled) {
        return selectList(new LambdaQueryWrapperX<AfterSaleReviewTicketRouteDO>()
                .eq(AfterSaleReviewTicketRouteDO::getEnabled, enabled)
                .orderByAsc(AfterSaleReviewTicketRouteDO::getSort)
                .orderByAsc(AfterSaleReviewTicketRouteDO::getId));
    }

    default AfterSaleReviewTicketRouteDO selectByUniqueKey(String scope, String ruleCode, Integer ticketType, String severity) {
        return selectOne(new LambdaQueryWrapperX<AfterSaleReviewTicketRouteDO>()
                .eq(AfterSaleReviewTicketRouteDO::getScope, scope)
                .eq(AfterSaleReviewTicketRouteDO::getRuleCode, ruleCode)
                .eq(AfterSaleReviewTicketRouteDO::getTicketType, ticketType)
                .eq(AfterSaleReviewTicketRouteDO::getSeverity, severity));
    }

}
