package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import com.hxy.module.booking.controller.admin.vo.OffpeakRuleCreateReqVO;
import com.hxy.module.booking.controller.admin.vo.OffpeakRuleUpdateReqVO;
import com.hxy.module.booking.convert.OffpeakRuleConvert;
import com.hxy.module.booking.dal.dataobject.OffpeakRuleDO;
import com.hxy.module.booking.dal.mysql.OffpeakRuleMapper;
import com.hxy.module.booking.service.OffpeakRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.OFFPEAK_RULE_NOT_EXISTS;

@Service
@Validated
@RequiredArgsConstructor
public class OffpeakRuleServiceImpl implements OffpeakRuleService {

    private final OffpeakRuleMapper offpeakRuleMapper;

    @Override
    public Long createOffpeakRule(OffpeakRuleCreateReqVO reqVO) {
        OffpeakRuleDO rule = OffpeakRuleConvert.INSTANCE.convert(reqVO);
        offpeakRuleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public void updateOffpeakRule(OffpeakRuleUpdateReqVO reqVO) {
        validateOffpeakRuleExists(reqVO.getId());
        OffpeakRuleDO rule = OffpeakRuleConvert.INSTANCE.convert(reqVO);
        offpeakRuleMapper.updateById(rule);
    }

    @Override
    public void deleteOffpeakRule(Long id) {
        validateOffpeakRuleExists(id);
        offpeakRuleMapper.deleteById(id);
    }

    @Override
    public OffpeakRuleDO getOffpeakRule(Long id) {
        return offpeakRuleMapper.selectById(id);
    }

    @Override
    public List<OffpeakRuleDO> getOffpeakRuleListByStoreId(Long storeId) {
        return offpeakRuleMapper.selectListByStoreId(storeId);
    }

    @Override
    public List<OffpeakRuleDO> getEnabledOffpeakRuleListByStoreId(Long storeId) {
        return offpeakRuleMapper.selectEnabledByStoreId(storeId);
    }

    @Override
    public void updateOffpeakRuleStatus(Long id, Integer status) {
        validateOffpeakRuleExists(id);
        OffpeakRuleDO update = new OffpeakRuleDO();
        update.setId(id);
        update.setStatus(status);
        offpeakRuleMapper.updateById(update);
    }

    @Override
    public OffpeakRuleDO matchOffpeakRule(Long storeId, Integer weekDay, LocalTime time) {
        List<OffpeakRuleDO> rules = getEnabledOffpeakRuleListByStoreId(storeId);
        for (OffpeakRuleDO rule : rules) {
            // 检查星期是否匹配
            if (rule.getWeekDays() != null && !rule.getWeekDays().isEmpty()) {
                List<String> weekDays = Arrays.asList(rule.getWeekDays().split(","));
                if (!weekDays.contains(String.valueOf(weekDay))) {
                    continue;
                }
            }
            // 检查时间是否在范围内
            if (!time.isBefore(rule.getStartTime()) && !time.isAfter(rule.getEndTime())) {
                return rule;
            }
        }
        return null;
    }

    @Override
    public Integer calculateOffpeakPrice(OffpeakRuleDO rule, Integer originalPrice) {
        if (rule == null) {
            return originalPrice;
        }
        // 优先使用固定价格
        if (rule.getFixedPrice() != null && rule.getFixedPrice() > 0) {
            return rule.getFixedPrice();
        }
        // 使用折扣比例
        if (rule.getDiscountRate() != null && rule.getDiscountRate() > 0) {
            return originalPrice * rule.getDiscountRate() / 100;
        }
        return originalPrice;
    }

    private void validateOffpeakRuleExists(Long id) {
        if (offpeakRuleMapper.selectById(id) == null) {
            throw exception(OFFPEAK_RULE_NOT_EXISTS);
        }
    }

}
