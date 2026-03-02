package com.hxy.module.booking.service;

import com.hxy.module.booking.controller.admin.vo.OffpeakRuleCreateReqVO;
import com.hxy.module.booking.controller.admin.vo.OffpeakRuleUpdateReqVO;
import com.hxy.module.booking.dal.dataobject.OffpeakRuleDO;

import java.time.LocalTime;
import java.util.List;

/**
 * 闲时规则 Service 接口
 */
public interface OffpeakRuleService {

    /**
     * 创建闲时规则
     */
    Long createOffpeakRule(OffpeakRuleCreateReqVO reqVO);

    /**
     * 更新闲时规则
     */
    void updateOffpeakRule(OffpeakRuleUpdateReqVO reqVO);

    /**
     * 删除闲时规则
     */
    void deleteOffpeakRule(Long id);

    /**
     * 获取闲时规则
     */
    OffpeakRuleDO getOffpeakRule(Long id);

    /**
     * 获取门店闲时规则列表
     */
    List<OffpeakRuleDO> getOffpeakRuleListByStoreId(Long storeId);

    /**
     * 获取门店启用的闲时规则列表
     */
    List<OffpeakRuleDO> getEnabledOffpeakRuleListByStoreId(Long storeId);

    /**
     * 更新闲时规则状态
     */
    void updateOffpeakRuleStatus(Long id, Integer status);

    /**
     * 判断指定时间是否为闲时
     *
     * @param storeId 门店ID
     * @param weekDay 星期几（1-7）
     * @param time 时间
     * @return 匹配的闲时规则，null表示非闲时
     */
    OffpeakRuleDO matchOffpeakRule(Long storeId, Integer weekDay, LocalTime time);

    /**
     * 计算闲时价格
     *
     * @param rule 闲时规则
     * @param originalPrice 原价（分）
     * @return 闲时价格（分）
     */
    Integer calculateOffpeakPrice(OffpeakRuleDO rule, Integer originalPrice);

}
