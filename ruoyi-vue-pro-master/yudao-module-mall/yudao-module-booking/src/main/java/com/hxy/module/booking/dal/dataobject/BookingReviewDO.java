package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约服务评价 DO
 */
@TableName(value = "booking_review", autoResultMap = true)
@KeySequence("booking_review_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingReviewDO extends BaseDO {

    @TableId
    private Long id;

    /** 预约订单ID */
    private Long bookingOrderId;

    /** 服务履约单ID */
    private Long serviceOrderId;

    /** 门店ID */
    private Long storeId;

    /** 技师ID */
    private Long technicianId;

    /** 会员ID */
    private Long memberId;

    /** 服务商品SPU ID */
    private Long serviceSpuId;

    /** 服务商品SKU ID */
    private Long serviceSkuId;

    /** 总体评分 */
    private Integer overallScore;

    /** 服务体验评分 */
    private Integer serviceScore;

    /** 技师表现评分 */
    private Integer technicianScore;

    /** 门店环境评分 */
    private Integer environmentScore;

    /** 标签 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 评价内容 */
    private String content;

    /** 评价图片 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> picUrls;

    /** 是否匿名 */
    private Boolean anonymous;

    /** 评价等级 */
    private Integer reviewLevel;

    /** 风险等级 */
    private Integer riskLevel;

    /** 展示状态 */
    private Integer displayStatus;

    /** 服务跟进状态 */
    private Integer followStatus;

    /** 是否已回复 */
    private Boolean replyStatus;

    /** 审核状态 */
    private Integer auditStatus;

    /** 来源 */
    private String source;

    /** 服务完成时间 */
    private LocalDateTime completedTime;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 差评触发类型 */
    private String negativeTriggerType;

    /** 店长联系人姓名快照 */
    private String managerContactName;

    /** 店长联系人手机号快照 */
    private String managerContactMobile;

    /** 店长待办状态 */
    private Integer managerTodoStatus;

    /** 店长待办认领截止时间 */
    private LocalDateTime managerClaimDeadlineAt;

    /** 店长待办首次处理截止时间 */
    private LocalDateTime managerFirstActionDeadlineAt;

    /** 店长待办闭环截止时间 */
    private LocalDateTime managerCloseDeadlineAt;

    /** 店长待办认领操作人 */
    private Long managerClaimedByUserId;

    /** 店长待办认领时间 */
    private LocalDateTime managerClaimedAt;

    /** 店长待办首次处理时间 */
    private LocalDateTime managerFirstActionAt;

    /** 店长待办闭环时间 */
    private LocalDateTime managerClosedAt;

    /** 店长待办最近处理备注 */
    private String managerLatestActionRemark;

    /** 店长待办最近处理人 */
    private Long managerLatestActionByUserId;

    /** 首次响应时间 */
    private LocalDateTime firstResponseAt;

    /** 跟进负责人 */
    private Long followOwnerId;

    /** 跟进结果 */
    private String followResult;

    /** 回复人 */
    private Long replyUserId;

    /** 回复内容 */
    private String replyContent;

    /** 回复时间 */
    private LocalDateTime replyTime;

}
