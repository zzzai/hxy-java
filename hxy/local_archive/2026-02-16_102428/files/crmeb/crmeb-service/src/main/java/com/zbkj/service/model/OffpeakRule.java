package com.zbkj.service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 闲时优惠规则表
 * 
 * @author CRMEB
 * @since 2026-02-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_offpeak_rule")
public class OffpeakRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 门店ID（0=全局规则）
     */
    private Integer storeId;

    /**
     * 折扣率（0.8=8折）
     */
    private Double discountRate;

    /**
     * 时间段配置JSON
     * 格式: {"periods": [{"start": "09:00", "end": "12:00"}, {"start": "14:00", "end": "17:00"}]}
     */
    private String timePeriods;

    /**
     * 星期配置（逗号分隔，1-7表示周一到周日）
     */
    private String weekdays;

    /**
     * 是否启用：0=否 1=是
     */
    private Integer isEnabled;

    /**
     * 优先级（数字越大优先级越高）
     */
    private Integer priority;

    /**
     * 创建时间
     */
    private Integer createdAt;

    /**
     * 更新时间
     */
    private Integer updatedAt;
}


