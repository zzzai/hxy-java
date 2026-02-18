package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.PayConstants;
import com.zbkj.common.request.StoreOrderRefundRequest;
import com.zbkj.common.utils.RestTemplateUtil;
import com.zbkj.common.utils.WxPayUtil;
import com.zbkj.common.vo.WeChatPayChannelConfig;
import com.zbkj.common.vo.WxRefundVo;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.service.dao.StoreOrderDao;
import com.zbkj.service.service.impl.payment.WeChatPayConfigSupport;
import com.zbkj.service.service.StoreOrderRefundService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.WechatNewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * StoreOrderServiceImpl 接口实现
 * +----------------------------------------------------------------------
 * | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 * +----------------------------------------------------------------------
 * | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 * +----------------------------------------------------------------------
 * | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 * +----------------------------------------------------------------------
 * | Author: CRMEB Team <admin@crmeb.com>
 * +----------------------------------------------------------------------
 */
@Service
public class StoreOrderRefundServiceImpl extends ServiceImpl<StoreOrderDao, StoreOrder> implements StoreOrderRefundService {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private RestTemplateUtil restTemplateUtil;
    @Autowired
    private WechatNewService wechatNewService;

    @Autowired
    private WeChatPayConfigSupport weChatPayConfigSupport;

    /**
     * 退款
     */
    @Override
    public void refund(StoreOrderRefundRequest request, StoreOrder storeOrder) {
        refundWx(request, storeOrder);
    }

    /**
     * 公众号退款
     * @param request
     * @param storeOrder
     */
    private void refundWx(StoreOrderRefundRequest request, StoreOrder storeOrder) {
        WeChatPayChannelConfig payConfig = weChatPayConfigSupport.resolveByChannel(storeOrder.getIsChannel(), storeOrder.getStoreId());
        String path = payConfig.getCertificatePath();
        if (cn.hutool.core.util.StrUtil.isBlank(path)) {
            throw new RuntimeException("未配置微信退款证书路径，请检查支付配置");
        }

        String apiDomain = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_API_URL);

        //统一下单数据
        WxRefundVo wxRefundVo = new WxRefundVo();
        wxRefundVo.setAppid(payConfig.getAppId());
        wxRefundVo.setMch_id(payConfig.getMchId());
        if (Boolean.TRUE.equals(payConfig.getServiceProviderMode())) {
            wxRefundVo.setSub_mch_id(payConfig.getSubMchId());
            if (cn.hutool.core.util.StrUtil.isNotBlank(payConfig.getSubAppId())) {
                wxRefundVo.setSub_appid(payConfig.getSubAppId());
            }
        }
        wxRefundVo.setNonce_str(WxPayUtil.getNonceStr());
        wxRefundVo.setOut_trade_no(storeOrder.getOutTradeNo());
        wxRefundVo.setOut_refund_no(storeOrder.getOrderId());
        wxRefundVo.setTotal_fee(storeOrder.getPayPrice().multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue());
        wxRefundVo.setRefund_fee(request.getAmount().multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue());
        wxRefundVo.setNotify_url(apiDomain + PayConstants.WX_PAY_REFUND_NOTIFY_API_URI);
        String sign = WxPayUtil.getSign(wxRefundVo, payConfig.getSignKey());
        wxRefundVo.setSign(sign);

        wechatNewService.payRefund(wxRefundVo, path);
    }

}
