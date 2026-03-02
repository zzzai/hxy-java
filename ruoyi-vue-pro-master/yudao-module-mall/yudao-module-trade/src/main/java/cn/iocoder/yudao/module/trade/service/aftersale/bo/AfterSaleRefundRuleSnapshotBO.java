package cn.iocoder.yudao.module.trade.service.aftersale.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 售后退款风控规则快照
 *
 * @author HXY
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AfterSaleRefundRuleSnapshotBO {

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
     * 黑名单用户编号
     */
    private Set<Long> blacklistUserIds = new LinkedHashSet<>();
    /**
     * 可疑订单关键字
     */
    private Set<String> suspiciousOrderKeywords = new LinkedHashSet<>();
    /**
     * 规则来源（DB / YAML）
     */
    private String source;

}
