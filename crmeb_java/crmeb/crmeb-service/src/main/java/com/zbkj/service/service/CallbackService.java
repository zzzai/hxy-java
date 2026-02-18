package com.zbkj.service.service;

import javax.servlet.http.HttpServletRequest;

/**
 * 订单支付回调 service
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
public interface CallbackService {
    /**
     * 微信支付回调
     * @param xmlInfo 微信回调json
     * @return String
     */
    String weChat(String xmlInfo);

    /**
     * 微信支付回调(兼容v3头部验签信息)
     * @param body 微信回调原始报文
     * @param headers 请求头
     * @return String
     */
    String weChat(String body, java.util.Map<String, String> headers);

    /**
     * 微信退款回调
     * @param request 微信回调json
     * @return String
     */
    String weChatRefund(String request);

    /**
     * 微信退款回调(兼容v3头部验签信息)
     * @param body 微信回调原始报文
     * @param headers 请求头
     * @return String
     */
    String weChatRefund(String body, java.util.Map<String, String> headers);
}
