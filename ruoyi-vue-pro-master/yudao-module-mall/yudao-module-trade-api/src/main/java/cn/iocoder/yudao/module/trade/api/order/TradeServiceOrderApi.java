package cn.iocoder.yudao.module.trade.api.order;

/**
 * 服务履约单 API
 *
 * @author HXY
 */
public interface TradeServiceOrderApi {

    /**
     * 按支付单同步“已预约”状态。
     *
     * @param payOrderId 支付单 ID
     * @param bookingNo 预约单号
     * @param remark 同步备注
     * @return 实际同步数量
     */
    int markBookedByPayOrderId(Long payOrderId, String bookingNo, String remark);

    /**
     * 按支付单同步“服务中”状态。
     *
     * @param payOrderId 支付单 ID
     * @param remark 同步备注
     * @return 实际同步数量
     */
    int startServingByPayOrderId(Long payOrderId, String remark);

    /**
     * 按支付单同步“已完成”状态。
     *
     * @param payOrderId 支付单 ID
     * @param remark 同步备注
     * @return 实际同步数量
     */
    int finishServingByPayOrderId(Long payOrderId, String remark);

    /**
     * 按支付单同步“已取消”状态。
     *
     * @param payOrderId 支付单 ID
     * @param remark 同步备注
     * @return 实际同步数量
     */
    int cancelByPayOrderId(Long payOrderId, String remark);

}
