package com.zbkj.common.model.member;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 会员持久化对象
 * 
 * @author 荷小悦架构团队
 * @since 2026-02-12
 */
@Data
@TableName("eb_member")
public class MemberPO {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer userId;
    
    private Integer level;
    
    private Integer growthValue;
    
    private Date createTime;
    
    private Date upgradeTime;
}

