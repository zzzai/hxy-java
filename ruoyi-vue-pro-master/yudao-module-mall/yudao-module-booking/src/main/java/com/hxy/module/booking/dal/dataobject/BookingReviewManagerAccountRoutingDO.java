package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 预约评价店长账号路由 DO
 */
@TableName("booking_review_manager_account_routing")
@KeySequence("booking_review_manager_account_routing_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BookingReviewManagerAccountRoutingDO extends BaseDO {

    @TableId
    private Long id;

    /** 门店ID */
    private Long storeId;

    /** 店长后台账号ID */
    private Long managerAdminUserId;

    /** 绑定状态 */
    private String bindingStatus;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 失效时间 */
    private LocalDateTime expireTime;

    /** 来源 */
    private String source;

    /** 最近核验时间 */
    private LocalDateTime lastVerifiedTime;
}
