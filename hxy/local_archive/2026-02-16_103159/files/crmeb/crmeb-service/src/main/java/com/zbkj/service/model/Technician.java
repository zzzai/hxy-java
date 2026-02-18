package com.zbkj.service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 技师基础信息表
 * 
 * @author CRMEB
 * @since 2026-02-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_technician")
public class Technician implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 技师ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 所属门店ID
     */
    private Integer storeId;

    /**
     * 技师姓名
     */
    private String name;

    /**
     * 技师头像URL
     */
    private String avatar;

    /**
     * 技师等级：1=初级 2=中级 3=高级 4=首席
     */
    private Integer level;

    /**
     * 从业年限
     */
    private BigDecimal serviceYears;

    /**
     * 技能标签（逗号分隔）
     */
    private String skillTags;

    /**
     * 技师介绍
     */
    private String intro;

    /**
     * 评分（5.00满分）
     */
    private BigDecimal rating;

    /**
     * 累计服务单数
     */
    private Integer orderCount;

    /**
     * 状态：1=在职 2=离职
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Integer createdAt;

    /**
     * 更新时间
     */
    private Integer updatedAt;
}


