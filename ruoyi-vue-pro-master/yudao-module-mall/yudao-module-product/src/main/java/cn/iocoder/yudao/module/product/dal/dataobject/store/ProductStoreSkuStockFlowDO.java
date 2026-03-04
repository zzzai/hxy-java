package cn.iocoder.yudao.module.product.dal.dataobject.store;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 门店 SKU 库存流水 DO
 */
@TableName("hxy_store_product_sku_stock_flow")
@KeySequence("hxy_store_product_sku_stock_flow_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStoreSkuStockFlowDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 业务类型，例如 TRADE_ORDER_RESERVE
     */
    private String bizType;
    /**
     * 业务单号，例如订单号
     */
    private String bizNo;
    /**
     * 门店编号
     */
    private Long storeId;
    /**
     * SKU 编号
     */
    private Long skuId;
    /**
     * 库存变化值：正数加库存，负数减库存
     */
    private Integer incrCount;
    /**
     * 状态：0待执行 1成功 2失败 3执行中
     */
    private Integer status;
    /**
     * 重试次数
     */
    private Integer retryCount;
    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;
    /**
     * 最后错误信息
     */
    private String lastErrorMsg;
    /**
     * 最后执行时间
     */
    private LocalDateTime executeTime;
    /**
     * 最近重试操作人
     */
    private String lastRetryOperator;
    /**
     * 最近重试来源
     */
    private String lastRetrySource;

}
