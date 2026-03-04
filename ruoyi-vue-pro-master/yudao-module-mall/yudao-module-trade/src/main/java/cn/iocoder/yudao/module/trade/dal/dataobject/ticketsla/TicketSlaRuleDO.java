package cn.iocoder.yudao.module.trade.dal.dataobject.ticketsla;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * SLA 工单规则配置 DO
 */
@TableName("trade_ticket_sla_rule")
@KeySequence("trade_ticket_sla_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TicketSlaRuleDO extends BaseDO {

    private Long id;

    /**
     * 工单类型
     */
    private Integer ticketType;
    /**
     * 规则编码（RULE 层级）
     */
    private String ruleCode;
    /**
     * 严重级别（TYPE_SEVERITY 层级）
     */
    private String severity;
    /**
     * 作用域类型：1=全局 2=门店
     */
    private Integer scopeType;
    /**
     * 作用域门店 ID
     */
    private Long scopeStoreId;
    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 优先级（同层级按降序）
     */
    private Integer priority;
    /**
     * 升级对象
     */
    private String escalateTo;
    /**
     * SLA 分钟
     */
    private Integer slaMinutes;
    /**
     * 预警提前分钟
     */
    private Integer warnLeadMinutes;
    /**
     * 升级延迟分钟
     */
    private Integer escalateDelayMinutes;
    /**
     * 备注
     */
    private String remark;
    /**
     * 最近操作动作
     */
    private String lastAction;
    /**
     * 最近操作时间
     */
    private LocalDateTime lastActionAt;

}
