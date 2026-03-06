package cn.iocoder.yudao.module.trade.dal.dataobject.order;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 交易订单套餐子项台账 DO
 */
@TableName("hxy_trade_order_item_bundle_child")
@KeySequence("hxy_trade_order_item_bundle_child_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeOrderItemBundleChildDO extends BaseDO {

    private Long id;

    /**
     * 交易订单 ID
     */
    private Long orderId;
    /**
     * 交易订单项 ID
     */
    private Long orderItemId;
    /**
     * 商品 SPU ID
     */
    private Long spuId;
    /**
     * 商品 SKU ID
     */
    private Long skuId;
    /**
     * 子项标识
     */
    private String childCode;
    /**
     * 子项名称
     */
    private String skuName;
    /**
     * 子项数量
     */
    private Integer quantity;
    /**
     * 子项可退基准金额（分）
     */
    private Integer payPrice;
    /**
     * 子项已退款金额（分）
     */
    private Integer refundedPrice;
    /**
     * 子项履约状态（对齐服务履约状态）
     */
    private Integer fulfillmentStatus;
    /**
     * 子项快照
     */
    private String snapshotJson;
}
