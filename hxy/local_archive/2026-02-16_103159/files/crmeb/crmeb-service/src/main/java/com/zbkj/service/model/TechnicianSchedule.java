package com.zbkj.service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 技师排班表
 * 
 * @author CRMEB
 * @since 2026-02-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_technician_schedule")
public class TechnicianSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 排班ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 门店ID
     */
    private Integer storeId;

    /**
     * 技师ID
     */
    private Integer technicianId;

    /**
     * 服务SKU ID
     */
    private Integer serviceSkuId;

    /**
     * 排班日期
     */
    private Date workDate;

    /**
     * 时间槽JSON（核心字段）
     * 格式: {"slots": [{"slot_id": "20250212_0900", "status": 1, "price": 128.00, ...}]}
     */
    private String timeSlots;

    /**
     * 总时间槽数
     */
    private Integer totalSlots;

    /**
     * 可预约时间槽数
     */
    private Integer availableSlots;

    /**
     * 排班状态：1=正常 2=请假 3=已完成
     */
    private Integer status;

    /**
     * 是否启用闲时优惠
     */
    private Integer isOffpeakEnabled;

    /**
     * 创建时间
     */
    private Integer createdAt;

    /**
     * 更新时间
     */
    private Integer updatedAt;
}


