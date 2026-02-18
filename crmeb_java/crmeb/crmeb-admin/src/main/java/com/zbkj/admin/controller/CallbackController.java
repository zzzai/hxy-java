package com.zbkj.admin.controller;

import com.zbkj.service.service.CallbackService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调
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
@Slf4j
@RestController
@RequestMapping("api/admin/payment/callback")
@Api(tags = "支付回调")
public class CallbackController {

    @Autowired
    private CallbackService callbackService;

    /**
     * 微信支付回调
     */
    @ApiOperation(value = "微信支付回调")
    @RequestMapping(value = "/wechat", method = RequestMethod.POST)
    public String weChat(HttpServletRequest httpServletRequest, @RequestBody String request) {
        log.info("微信支付回调 request ===> {}", request);
        String response = callbackService.weChat(request, extractHeaders(httpServletRequest));
        log.info("微信支付回调 response ===> {}", response);
        return response;
    }

    /**
     * 微信退款回调
     */
    @ApiOperation(value = "微信退款回调")
    @RequestMapping(value = "/wechat/refund", method = RequestMethod.POST)
    public String weChatRefund(HttpServletRequest httpServletRequest, @RequestBody String request) {
        log.info("微信退款回调 request ===> {}", request);
        String response = callbackService.weChatRefund(request, extractHeaders(httpServletRequest));
        log.info("微信退款回调 response ===> {}", response);
        return response;
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
