package cn.iocoder.yudao.module.pay.api.channel;

/**
 * 支付渠道 API 接口
 */
public interface PayChannelApi {

    /**
     * 判断指定前缀渠道是否存在已启用记录
     *
     * @param codePrefix 渠道编码前缀，例如 wx_ / alipay_ / wallet
     * @return 是否存在
     */
    boolean existsEnabledChannelByCodePrefix(String codePrefix);

}
