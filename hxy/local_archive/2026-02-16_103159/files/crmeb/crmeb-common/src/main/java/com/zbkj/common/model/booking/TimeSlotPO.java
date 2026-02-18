package com.zbkj.common.model.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 时间槽持久化对象
 */
@Data
@TableName("eb_time_slot")
public class TimeSlotPO implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Integer technicianId;
    
    private Date startTime;
    
    private Date endTime;
    
    private Boolean available;
    
    private Integer bookingId;
    
    private Date createTime;
}


