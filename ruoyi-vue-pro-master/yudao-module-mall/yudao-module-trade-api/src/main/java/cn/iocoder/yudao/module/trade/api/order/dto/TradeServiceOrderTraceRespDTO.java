package cn.iocoder.yudao.module.trade.api.order.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 服务履约单追溯信息 DTO。
 */
@Data
@Accessors(chain = true)
public class TradeServiceOrderTraceRespDTO {

    /**
     * 服务履约单 ID
     */
    private Long serviceOrderId;
    /**
     * 交易订单项 ID
     */
    private Long orderItemId;
    /**
     * 支付单 ID
     */
    private Long payOrderId;
    /**
     * 商品 SPU ID
     */
    private Long spuId;
    /**
     * 商品 SKU ID
     */
    private Long skuId;

}
