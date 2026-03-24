package com.hxy.module.booking.dal.dataobject.feed;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("technician_feed_comment")
@KeySequence("technician_feed_comment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianFeedCommentDO extends BaseDO {

    @TableId
    private Long id;

    private Long postId;

    private Long storeId;

    private Long technicianId;

    private Long memberId;

    private String content;

    private String clientToken;

    /** REVIEWING / APPROVED / REJECTED */
    private String status;

    private Boolean degraded;

    private LocalDateTime submittedAt;
}
