package com.zbkj.service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 库存变动流水表
 * 
 * @author CRMEB
 * @since 2026-02-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_stock_flow")
public class StockFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer storeId;
    private Integer skuId;
    private Long orderId;
    private Integer changeType;
    private Integer changeQuantity;
    private Integer beforeAvailable;
    private Integer afterAvailable;
    private Integer beforeLocked;
    private Integer afterLocked;
    private Integer operatorId;
    private Integer operatorType;
    private String remark;
    private Integer createdAt;
}
