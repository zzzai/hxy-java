package cn.iocoder.yudao.module.trade.dal.mysql.ticketsla;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRulePageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.ticketsla.TicketSlaRuleDO;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleScopeTypeEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleTicketTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * SLA 工单规则 Mapper
 */
@Mapper
public interface TicketSlaRuleMapper extends BaseMapperX<TicketSlaRuleDO> {

    default PageResult<TicketSlaRuleDO> selectPage(TicketSlaRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TicketSlaRuleDO>()
                .eqIfPresent(TicketSlaRuleDO::getTicketType, reqVO.getTicketType())
                .eqIfPresent(TicketSlaRuleDO::getRuleCode, reqVO.getRuleCode())
                .eqIfPresent(TicketSlaRuleDO::getSeverity, reqVO.getSeverity())
                .eqIfPresent(TicketSlaRuleDO::getScopeType, reqVO.getScopeType())
                .eqIfPresent(TicketSlaRuleDO::getScopeStoreId, reqVO.getScopeStoreId())
                .eqIfPresent(TicketSlaRuleDO::getEnabled, reqVO.getEnabled())
                .orderByDesc(TicketSlaRuleDO::getPriority)
                .orderByDesc(TicketSlaRuleDO::getId));
    }

    default TicketSlaRuleDO selectByScope(Integer ticketType, String ruleCode, String severity,
                                          Integer scopeType, Long scopeStoreId, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<TicketSlaRuleDO>()
                .eq(TicketSlaRuleDO::getTicketType, ticketType)
                .eq(TicketSlaRuleDO::getRuleCode, ruleCode)
                .eq(TicketSlaRuleDO::getSeverity, severity)
                .eq(TicketSlaRuleDO::getScopeType, scopeType)
                .eq(TicketSlaRuleDO::getScopeStoreId, scopeStoreId)
                .neIfPresent(TicketSlaRuleDO::getId, excludeId));
    }

    default List<TicketSlaRuleDO> selectListByTicketTypeAndScope(Integer ticketType, Long storeId) {
        Integer safeTicketType = ObjUtil.defaultIfNull(ticketType, TicketSlaRuleTicketTypeEnum.GLOBAL_DEFAULT.getType());
        long safeStoreId = storeId == null ? 0L : Math.max(storeId, 0L);

        LambdaQueryWrapper<TicketSlaRuleDO> query = new LambdaQueryWrapperX<TicketSlaRuleDO>()
                .in(TicketSlaRuleDO::getTicketType,
                        safeTicketType,
                        TicketSlaRuleTicketTypeEnum.GLOBAL_DEFAULT.getType())
                .orderByDesc(TicketSlaRuleDO::getPriority)
                .orderByDesc(TicketSlaRuleDO::getId);

        if (safeStoreId > 0) {
            query.apply("(scope_type = {0} OR (scope_type = {1} AND scope_store_id = {2}))",
                    TicketSlaRuleScopeTypeEnum.GLOBAL.getCode(),
                    TicketSlaRuleScopeTypeEnum.STORE.getCode(),
                    safeStoreId);
        } else {
            query.eq(TicketSlaRuleDO::getScopeType, TicketSlaRuleScopeTypeEnum.GLOBAL.getCode());
        }

        return selectList(query);
    }

}
