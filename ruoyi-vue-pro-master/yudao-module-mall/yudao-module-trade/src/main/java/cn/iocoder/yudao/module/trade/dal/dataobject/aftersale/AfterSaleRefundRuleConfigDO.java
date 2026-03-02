package cn.iocoder.yudao.module.trade.dal.dataobject.aftersale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;

/**
 * 售后退款风控规则配置 DO
 *
 * DB 配置优先于 YAML 配置，用于实现规则表配置化。
 *
 * @author HXY
 */
@TableName(value = "trade_after_sale_refund_rule", autoResultMap = true)
@KeySequence("trade_after_sale_refund_rule_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfterSaleRefundRuleConfigDO extends BaseDO {

    /**
     * 编号
     */
    private Long id;
    /**
     * 是否启用规则
     */
    private Boolean enabled;
    /**
     * 自动退款金额上限（分）
     */
    private Integer autoRefundMaxPrice;
    /**
     * 用户当日售后申请次数阈值
     */
    private Integer userDailyApplyLimit;
    /**
     * 黑名单用户编号列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> blacklistUserIds;
    /**
     * 可疑订单关键字列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> suspiciousOrderKeywords;
    /**
     * 规则版本
     */
    private String ruleVersion;
    /**
     * 备注
     */
    private String remark;

}
