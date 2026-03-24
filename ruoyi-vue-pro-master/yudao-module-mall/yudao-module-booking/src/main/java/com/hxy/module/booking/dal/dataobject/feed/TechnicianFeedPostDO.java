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

@TableName("technician_feed_post")
@KeySequence("technician_feed_post_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianFeedPostDO extends BaseDO {

    @TableId
    private Long id;

    private Long storeId;

    private Long technicianId;

    private String title;

    private String content;

    private String coverUrl;

    private Integer likeCount;

    private Integer commentCount;

    /** 1=已发布 0=已关闭 */
    private Integer status;

    private LocalDateTime publishedAt;
}
