package cn.iocoder.yudao.module.trade.service.ticketsla;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchReqDTO;
import cn.iocoder.yudao.module.trade.api.ticketsla.dto.TradeTicketSlaRuleMatchRespDTO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleCreateReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRulePageReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRulePreviewReqVO;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticketsla.TicketSlaRuleUpdateReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.ticketsla.TicketSlaRuleDO;
import cn.iocoder.yudao.module.trade.dal.mysql.ticketsla.TicketSlaRuleMapper;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleMatchLevelEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleScopeTypeEnum;
import cn.iocoder.yudao.module.trade.enums.ticketsla.TicketSlaRuleTicketTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.TICKET_SLA_RULE_NOT_EXISTS;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.TICKET_SLA_RULE_SCOPE_DUPLICATE;
import static cn.iocoder.yudao.module.trade.enums.ErrorCodeConstants.TICKET_SLA_RULE_SCOPE_STORE_REQUIRED;

/**
 * SLA 工单规则 Service 实现
 */
@Service
@Validated
@Slf4j
public class TicketSlaRuleServiceImpl implements TicketSlaRuleService {

    private static final String RULE_CODE_GLOBAL_DEFAULT = "GLOBAL_DEFAULT";

    @Resource
    private TicketSlaRuleMapper ticketSlaRuleMapper;

    @Override
    public PageResult<TicketSlaRuleDO> getRulePage(TicketSlaRulePageReqVO pageReqVO) {
        return ticketSlaRuleMapper.selectPage(pageReqVO);
    }

    @Override
    public TicketSlaRuleDO getRule(Long id) {
        return ticketSlaRuleMapper.selectById(id);
    }

    @Override
    public Long createRule(TicketSlaRuleCreateReqVO reqVO) {
        TicketSlaRuleDO createObj = BeanUtils.toBean(reqVO, TicketSlaRuleDO.class);
        normalizeRule(createObj);
        validateScopeConflict(createObj, null);

        createObj.setLastAction("CREATE");
        createObj.setLastActionAt(LocalDateTime.now());
        ticketSlaRuleMapper.insert(createObj);
        return createObj.getId();
    }

    @Override
    public void updateRule(TicketSlaRuleUpdateReqVO reqVO) {
        validateRuleExists(reqVO.getId());

        TicketSlaRuleDO updateObj = BeanUtils.toBean(reqVO, TicketSlaRuleDO.class);
        normalizeRule(updateObj);
        validateScopeConflict(updateObj, reqVO.getId());

        updateObj.setLastAction("UPDATE");
        updateObj.setLastActionAt(LocalDateTime.now());
        ticketSlaRuleMapper.updateById(updateObj);
    }

    @Override
    public void updateRuleStatus(Long id, Boolean enabled) {
        validateRuleExists(id);
        TicketSlaRuleDO updateObj = new TicketSlaRuleDO();
        updateObj.setId(id);
        updateObj.setEnabled(Boolean.TRUE.equals(enabled));
        updateObj.setLastAction(Boolean.TRUE.equals(enabled) ? "ENABLE" : "DISABLE");
        updateObj.setLastActionAt(LocalDateTime.now());
        ticketSlaRuleMapper.updateById(updateObj);
    }

    @Override
    public TradeTicketSlaRuleMatchRespDTO previewMatch(TicketSlaRulePreviewReqVO reqVO) {
        TradeTicketSlaRuleMatchReqDTO reqDTO = new TradeTicketSlaRuleMatchReqDTO();
        reqDTO.setTicketType(reqVO.getTicketType());
        reqDTO.setRuleCode(reqVO.getRuleCode());
        reqDTO.setSeverity(reqVO.getSeverity());
        reqDTO.setStoreId(reqVO.getStoreId());
        return matchRule(reqDTO);
    }

    @Override
    public TradeTicketSlaRuleMatchRespDTO matchRule(TradeTicketSlaRuleMatchReqDTO reqDTO) {
        if (reqDTO == null) {
            return buildUnmatched();
        }

        Integer ticketType = ObjUtil.defaultIfNull(reqDTO.getTicketType(), TicketSlaRuleTicketTypeEnum.GLOBAL_DEFAULT.getType());
        String ruleCode = normalizeCode(reqDTO.getRuleCode());
        String severity = normalizeCode(reqDTO.getSeverity());
        long storeId = normalizeStoreId(reqDTO.getStoreId(), TicketSlaRuleScopeTypeEnum.GLOBAL.getCode());

        List<TicketSlaRuleDO> ruleList = ticketSlaRuleMapper.selectListByTicketTypeAndScope(ticketType, storeId);
        if (ruleList == null || ruleList.isEmpty()) {
            return buildUnmatched();
        }

        List<MatchCandidate> candidates = new ArrayList<>();
        for (TicketSlaRuleDO rule : ruleList) {
            if (!Boolean.TRUE.equals(rule.getEnabled())) {
                continue;
            }
            TicketSlaRuleMatchLevelEnum matchLevel = resolveMatchLevel(rule, ticketType, ruleCode, severity);
            if (matchLevel == TicketSlaRuleMatchLevelEnum.NONE) {
                continue;
            }
            candidates.add(new MatchCandidate(rule, matchLevel));
        }
        if (candidates.isEmpty()) {
            return buildUnmatched();
        }

        MatchCandidate best = candidates.stream()
                .sorted(Comparator
                        .comparing((MatchCandidate c) -> c.getMatchLevel().getCode())
                        .thenComparing((MatchCandidate c) -> scopeSortRank(c.getRule().getScopeType()))
                        .thenComparing((MatchCandidate c) -> ObjUtil.defaultIfNull(c.getRule().getPriority(), 0), Comparator.reverseOrder())
                        .thenComparing((MatchCandidate c) -> ObjUtil.defaultIfNull(c.getRule().getId(), 0L), Comparator.reverseOrder()))
                .findFirst()
                .orElse(null);
        if (best == null) {
            return buildUnmatched();
        }
        return buildMatched(best.getRule(), best.getMatchLevel());
    }

    private void validateRuleExists(Long id) {
        if (ticketSlaRuleMapper.selectById(id) == null) {
            throw exception(TICKET_SLA_RULE_NOT_EXISTS);
        }
    }

    private void validateScopeConflict(TicketSlaRuleDO rule, Long excludeId) {
        TicketSlaRuleDO conflict = ticketSlaRuleMapper.selectByScope(
                rule.getTicketType(),
                rule.getRuleCode(),
                rule.getSeverity(),
                rule.getScopeType(),
                rule.getScopeStoreId(),
                excludeId);
        if (conflict != null) {
            throw exception(TICKET_SLA_RULE_SCOPE_DUPLICATE);
        }
    }

    private void normalizeRule(TicketSlaRuleDO rule) {
        Integer scopeType = ObjUtil.defaultIfNull(rule.getScopeType(), TicketSlaRuleScopeTypeEnum.GLOBAL.getCode());
        rule.setScopeType(scopeType);
        rule.setScopeStoreId(normalizeStoreId(rule.getScopeStoreId(), scopeType));

        Integer ticketType = ObjUtil.defaultIfNull(rule.getTicketType(), TicketSlaRuleTicketTypeEnum.GLOBAL_DEFAULT.getType());
        rule.setTicketType(ticketType);

        rule.setRuleCode(normalizeCode(rule.getRuleCode()));
        rule.setSeverity(normalizeCode(rule.getSeverity()));
        rule.setEscalateTo(StrUtil.maxLength(StrUtil.blankToDefault(StrUtil.trim(rule.getEscalateTo()), ""), 64));
        rule.setRemark(StrUtil.maxLength(StrUtil.blankToDefault(StrUtil.trim(rule.getRemark()), ""), 255));

        rule.setEnabled(Boolean.TRUE.equals(rule.getEnabled()));
        rule.setPriority(Math.max(0, ObjUtil.defaultIfNull(rule.getPriority(), 0)));
        rule.setSlaMinutes(normalizePositive(rule.getSlaMinutes()));
        rule.setWarnLeadMinutes(normalizePositive(rule.getWarnLeadMinutes()));
        rule.setEscalateDelayMinutes(normalizePositive(rule.getEscalateDelayMinutes()));

        // 全局默认建议固定编码，便于查阅和初始化幂等
        if (TicketSlaRuleTicketTypeEnum.GLOBAL_DEFAULT.getType().equals(ticketType)
                && StrUtil.isBlank(rule.getRuleCode())) {
            rule.setRuleCode(RULE_CODE_GLOBAL_DEFAULT);
        }
    }

    private Integer normalizePositive(Integer value) {
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    private String normalizeCode(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "").toUpperCase(Locale.ROOT);
    }

    private long normalizeStoreId(Long storeId, Integer scopeType) {
        long safeStoreId = storeId == null ? 0L : Math.max(storeId, 0L);
        if (TicketSlaRuleScopeTypeEnum.isStore(scopeType) && safeStoreId <= 0) {
            throw exception(TICKET_SLA_RULE_SCOPE_STORE_REQUIRED);
        }
        return TicketSlaRuleScopeTypeEnum.isStore(scopeType) ? safeStoreId : 0L;
    }

    private TicketSlaRuleMatchLevelEnum resolveMatchLevel(TicketSlaRuleDO rule, Integer ticketType,
                                                          String ruleCode, String severity) {
        Integer ruleTicketType = ObjUtil.defaultIfNull(rule.getTicketType(), TicketSlaRuleTicketTypeEnum.GLOBAL_DEFAULT.getType());
        String ruleRuleCode = normalizeCode(rule.getRuleCode());
        String ruleSeverity = normalizeCode(rule.getSeverity());

        if (ObjUtil.equal(ruleTicketType, ticketType)) {
            if (StrUtil.isNotBlank(ruleRuleCode) && StrUtil.isNotBlank(ruleCode)
                    && StrUtil.equals(ruleRuleCode, ruleCode)) {
                return TicketSlaRuleMatchLevelEnum.RULE;
            }
            if (StrUtil.isBlank(ruleRuleCode)
                    && StrUtil.isNotBlank(ruleSeverity)
                    && StrUtil.isNotBlank(severity)
                    && StrUtil.equals(ruleSeverity, severity)) {
                return TicketSlaRuleMatchLevelEnum.TYPE_SEVERITY;
            }
            if (StrUtil.isBlank(ruleRuleCode) && StrUtil.isBlank(ruleSeverity)) {
                return TicketSlaRuleMatchLevelEnum.TYPE_DEFAULT;
            }
        }

        if (ObjUtil.equal(ruleTicketType, TicketSlaRuleTicketTypeEnum.GLOBAL_DEFAULT.getType())) {
            if (StrUtil.equals(ruleRuleCode, RULE_CODE_GLOBAL_DEFAULT)
                    || (StrUtil.isBlank(ruleRuleCode) && StrUtil.isBlank(ruleSeverity))) {
                return TicketSlaRuleMatchLevelEnum.GLOBAL_DEFAULT;
            }
        }

        return TicketSlaRuleMatchLevelEnum.NONE;
    }

    private int scopeSortRank(Integer scopeType) {
        if (TicketSlaRuleScopeTypeEnum.isStore(scopeType)) {
            return 0;
        }
        return 1;
    }

    private TradeTicketSlaRuleMatchRespDTO buildMatched(TicketSlaRuleDO rule,
                                                        TicketSlaRuleMatchLevelEnum matchLevel) {
        TradeTicketSlaRuleMatchRespDTO respDTO = new TradeTicketSlaRuleMatchRespDTO();
        respDTO.setMatched(true);
        respDTO.setMatchLevel(matchLevel.getCode());
        respDTO.setRuleId(rule.getId());
        respDTO.setTicketType(rule.getTicketType());
        respDTO.setRuleCode(StrUtil.blankToDefault(rule.getRuleCode(), ""));
        respDTO.setSeverity(StrUtil.blankToDefault(rule.getSeverity(), ""));
        respDTO.setEscalateTo(StrUtil.blankToDefault(rule.getEscalateTo(), ""));
        respDTO.setSlaMinutes(rule.getSlaMinutes());
        respDTO.setWarnLeadMinutes(rule.getWarnLeadMinutes());
        respDTO.setEscalateDelayMinutes(rule.getEscalateDelayMinutes());
        respDTO.setPriority(rule.getPriority());
        return respDTO;
    }

    private TradeTicketSlaRuleMatchRespDTO buildUnmatched() {
        TradeTicketSlaRuleMatchRespDTO respDTO = new TradeTicketSlaRuleMatchRespDTO();
        respDTO.setMatched(false);
        respDTO.setMatchLevel(TicketSlaRuleMatchLevelEnum.NONE.getCode());
        return respDTO;
    }

    private static class MatchCandidate {
        private final TicketSlaRuleDO rule;
        private final TicketSlaRuleMatchLevelEnum matchLevel;

        private MatchCandidate(TicketSlaRuleDO rule, TicketSlaRuleMatchLevelEnum matchLevel) {
            this.rule = rule;
            this.matchLevel = matchLevel;
        }

        public TicketSlaRuleDO getRule() {
            return rule;
        }

        public TicketSlaRuleMatchLevelEnum getMatchLevel() {
            return matchLevel;
        }
    }

}
