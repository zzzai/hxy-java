package com.hxy.module.booking.dal.dataobject;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 技师 DO
 *
 * @author HXY
 */
@TableName("booking_technician")
@KeySequence("booking_technician_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianDO extends BaseDO {

    /**
     * 技师编号
     */
    @TableId
    private Long id;

    /**
     * 门店编号
     */
    private Long storeId;

    /**
     * 关联用户编号（技师登录账号）
     */
    private Long userId;

    /**
     * 技师姓名
     */
    private String name;

    /**
     * 技师头像
     */
    private String avatar;

    /**
     * 技师手机号
     */
    private String phone;

    /**
     * 技师简介
     */
    private String introduction;

    /**
     * 技师标签（如：手法专业、力度适中）
     */
    private String tags;

    /**
     * 评分（1-5分，保留1位小数）
     */
    private BigDecimal rating;

    /**
     * 已服务单数
     */
    private Integer serviceCount;

    /**
     * 排序值
     */
    private Integer sort;

    /**
     * 状态
     *
     * 枚举 {@link CommonStatusEnum}
     */
    private Integer status;

}
