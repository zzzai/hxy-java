package cn.iocoder.yudao.module.trade.dal.dataobject.aftersale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 售后复核工单路由规则 DO
 */
@TableName("trade_after_sale_review_ticket_route")
@KeySequence("trade_after_sale_review_ticket_route_seq")
@Data
public class AfterSaleReviewTicketRouteDO extends BaseDO {

    /**
     * 主键
     */
    private Long id;
    /**
     * 作用域：RULE / TYPE_SEVERITY / TYPE_DEFAULT / GLOBAL_DEFAULT
     */
    private String scope;
    /**
     * 规则编码（RULE 作用域使用）
     */
    private String ruleCode;
    /**
     * 工单类型（TYPE_SEVERITY / TYPE_DEFAULT 使用）
     */
    private Integer ticketType;
    /**
     * 严重级别（TYPE_SEVERITY 使用）
     */
    private String severity;
    /**
     * 升级对象
     */
    private String escalateTo;
    /**
     * SLA 分钟
     */
    private Integer slaMinutes;
    /**
     * 启用状态：true 启用 false 停用
     */
    private Boolean enabled;
    /**
     * 排序（值越小优先级越高）
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;

}
