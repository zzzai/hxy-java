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

@TableName("technician_feed_like")
@KeySequence("technician_feed_like_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianFeedLikeDO extends BaseDO {

    @TableId
    private Long id;

    private Long postId;

    private Long memberId;

    private String clientToken;

    /** 1=已点赞 0=已取消 */
    private Integer status;

    private LocalDateTime likedAt;

    private LocalDateTime canceledAt;
}
