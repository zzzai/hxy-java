package cn.iocoder.yudao.module.pay.framework.pay.core.client.impl.weixin;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.pay.enums.PayChannelEnum;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.order.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.framework.pay.core.client.dto.order.PayOrderUnifiedReqDTO;
import cn.iocoder.yudao.module.pay.framework.pay.core.enums.PayOrderDisplayModeEnum;
import com.github.binarywang.wxpay.bean.order.WxPayMpOrderResult;
import com.github.binarywang.wxpay.bean.request.WxPayPartnerUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.constant.WxPayConstants;
import com.github.binarywang.wxpay.exception.WxPayException;
import lombok.extern.slf4j.Slf4j;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;

/**
 * 微信支付（公众号）的 PayClient 实现类
 *
 * 文档：<a href="https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_1_1.shtml">JSAPI 下单</>
 *
 * @author 芋道源码
 */
@Slf4j
public class WxPubPayClient extends AbstractWxPayClient {

    @SuppressWarnings("unused") // 反射会调用到，所以不能删除
    public WxPubPayClient(Long channelId, WxPayClientConfig config) {
        super(channelId, PayChannelEnum.WX_PUB.getCode(), config);
    }

    protected WxPubPayClient(Long channelId, String channelCode, WxPayClientConfig config) {
        super(channelId, channelCode, config);
    }

    @Override
    protected void doInit() {
        super.doInit(WxPayConstants.TradeType.JSAPI);
    }

    @Override
    protected PayOrderRespDTO doUnifiedOrderV2(PayOrderUnifiedReqDTO reqDTO) throws WxPayException {
        // 构建 WxPayUnifiedOrderRequest 对象
        WxPayUnifiedOrderRequest request = buildPayUnifiedOrderRequestV2(reqDTO)
                .setOpenid(getOpenid(reqDTO));
        // 执行请求
        WxPayMpOrderResult response = client.createOrder(request);

        // 转换结果
        return PayOrderRespDTO.waitingOf(PayOrderDisplayModeEnum.APP.getMode(), toJsonString(response),
                reqDTO.getOutTradeNo(), response);
    }

    @Override
    protected PayOrderRespDTO doUnifiedOrderV3(PayOrderUnifiedReqDTO reqDTO) throws WxPayException {
        WxPayUnifiedOrderV3Result.JsapiResult response;
        if (isPartnerModeV3()) {
            // 构建 WxPayPartnerUnifiedOrderV3Request 对象
            WxPayPartnerUnifiedOrderV3Request request = buildPayPartnerUnifiedOrderRequestV3(reqDTO)
                    .setPayer(buildPartnerPayer(reqDTO));
            // 执行请求
            response = client.createPartnerOrderV3(TradeTypeEnum.JSAPI, request);
        } else {
            // 构建 WxPayUnifiedOrderRequest 对象
            WxPayUnifiedOrderV3Request request = buildPayUnifiedOrderRequestV3(reqDTO)
                    .setPayer(new WxPayUnifiedOrderV3Request.Payer().setOpenid(getOpenid(reqDTO)));
            // 执行请求
            response = client.createOrderV3(TradeTypeEnum.JSAPI, request);
        }

        // 转换结果
        return PayOrderRespDTO.waitingOf(PayOrderDisplayModeEnum.APP.getMode(), toJsonString(response),
                reqDTO.getOutTradeNo(), response);
    }

    // ========== 各种工具方法 ==========

    static String getOpenid(PayOrderUnifiedReqDTO reqDTO) {
        String openid = MapUtil.getStr(reqDTO.getChannelExtras(), "openid");
        if (StrUtil.isEmpty(openid)) {
            throw invalidParamException("支付请求的 openid 不能为空！");
        }
        return openid;
    }

    private WxPayPartnerUnifiedOrderV3Request.Payer buildPartnerPayer(PayOrderUnifiedReqDTO reqDTO) {
        String openid = getOpenid(reqDTO);
        String spOpenid = MapUtil.getStr(reqDTO.getChannelExtras(), "spOpenid");
        String subOpenid = MapUtil.getStr(reqDTO.getChannelExtras(), "subOpenid");
        WxPayPartnerUnifiedOrderV3Request.Payer payer = new WxPayPartnerUnifiedOrderV3Request.Payer();
        if (StrUtil.isNotBlank(config.getSubAppId())) {
            // 子商户小程序调起支付，优先使用 subOpenid
            payer.setSubOpenid(StrUtil.blankToDefault(subOpenid, openid));
        } else {
            // 服务商小程序调起支付，优先使用 spOpenid
            payer.setSpOpenid(StrUtil.blankToDefault(spOpenid, openid));
        }
        return payer;
    }

}
