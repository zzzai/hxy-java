package cn.iocoder.yudao.module.trade.dal.dataobject.aftersale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 售后人工复核工单 DO
 *
 * @author HXY
 */
@TableName("trade_after_sale_review_ticket")
@KeySequence("trade_after_sale_review_ticket_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
public class AfterSaleReviewTicketDO extends BaseDO {

    /**
     * 主键
     */
    private Long id;
    /**
     * 工单类型：10售后复核 20服务履约 30提成争议
     */
    private Integer ticketType;
    /**
     * 售后单 ID（唯一）
     */
    private Long afterSaleId;
    /**
     * 来源业务单号
     */
    private String sourceBizNo;
    /**
     * 订单 ID
     */
    private Long orderId;
    /**
     * 订单项 ID
     */
    private Long orderItemId;
    /**
     * 用户 ID
     */
    private Long userId;
    /**
     * 命中规则编码
     */
    private String ruleCode;
    /**
     * 命中原因
     */
    private String decisionReason;
    /**
     * 严重级别：P0 / P1 / P2
     */
    private String severity;
    /**
     * 升级对象
     */
    private String escalateTo;
    /**
     * SLA 截止时间
     */
    private LocalDateTime slaDeadlineTime;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 首次触发时间
     */
    private LocalDateTime firstTriggerTime;
    /**
     * 最近触发时间
     */
    private LocalDateTime lastTriggerTime;
    /**
     * 触发次数
     */
    private Integer triggerCount;
    /**
     * 收口时间
     */
    private LocalDateTime resolvedTime;
    /**
     * 收口人
     */
    private Long resolverId;
    /**
     * 收口人类型
     */
    private Integer resolverType;
    /**
     * 收口动作编码
     *
     * 例如：MANUAL_RESOLVE、AUTO_RESOLVE
     */
    private String resolveActionCode;
    /**
     * 收口来源业务号
     *
     * 例如：后台操作单号、重试任务号
     */
    private String resolveBizNo;
    /**
     * 备注
     */
    private String remark;

}
