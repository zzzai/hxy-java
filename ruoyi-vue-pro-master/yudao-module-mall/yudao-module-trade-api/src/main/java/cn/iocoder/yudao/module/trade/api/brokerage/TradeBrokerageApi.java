package cn.iocoder.yudao.module.trade.api.brokerage;

/**
 * 分销佣金 API 接口
 */
public interface TradeBrokerageApi {

    /**
     * 佣金转入余额
     *
     * @param userId 用户编号
     * @param price  转入金额，单位分
     */
    void transferIn(Long userId, Integer price);

}
