package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zbkj.common.config.CrmebConfig;
import com.zbkj.common.constants.PayConstants;
import com.zbkj.common.constants.WeChatConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.wechat.WechatExceptions;
import com.zbkj.common.model.wechat.WechatPayInfo;
import com.zbkj.common.request.SaveConfigRequest;
import com.zbkj.common.response.WeChatJsSdkConfigResponse;
import com.zbkj.common.token.WeChatOauthToken;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.common.utils.RestTemplateUtil;
import com.zbkj.common.utils.WxPayUtil;
import com.zbkj.common.utils.XmlUtil;
import com.zbkj.common.vo.*;
import com.zbkj.service.service.impl.payment.WeChatPayV3Crypto;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.WechatExceptionsService;
import com.zbkj.service.service.WechatNewService;
import com.zbkj.service.service.WechatPayInfoService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *  微信公用服务实现类
 *  +----------------------------------------------------------------------
 *  | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 *  +----------------------------------------------------------------------
 *  | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 *  +----------------------------------------------------------------------
 *  | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 *  +----------------------------------------------------------------------
 *  | Author: CRMEB Team <admin@crmeb.com>
 *  +----------------------------------------------------------------------
 */
@Service
public class WechatNewServiceImpl implements WechatNewService {
    private static final Logger logger = LoggerFactory.getLogger(WechatNewServiceImpl.class);

    @Autowired
    private CrmebConfig crmebConfig;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RestTemplateUtil restTemplateUtil;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private WechatExceptionsService wechatExceptionsService;

    @Autowired
    private WechatPayInfoService wechatPayInfoService;

    /**
     * 获取公众号accessToken
     */
    @Override
    public String getPublicAccessToken() {
        boolean exists = redisUtil.exists(WeChatConstants.REDIS_WECAHT_PUBLIC_ACCESS_TOKEN_KEY);
        if (exists) {
            Object accessToken = redisUtil.get(WeChatConstants.REDIS_WECAHT_PUBLIC_ACCESS_TOKEN_KEY);
            return accessToken.toString();
        }
        String appId = systemConfigService.getValueByKey(WeChatConstants.WECHAT_PUBLIC_APPID);
        if (StrUtil.isBlank(appId)) {
            throw new CrmebException("微信公众号appId未设置");
        }
        String secret = systemConfigService.getValueByKey(WeChatConstants.WECHAT_PUBLIC_APPSECRET);
        if (StrUtil.isBlank(secret)) {
            throw new CrmebException("微信公众号secret未设置");
        }
        WeChatAccessTokenVo accessTokenVo = getAccessToken(appId, secret, "public");
        // 缓存accessToken
        redisUtil.set(WeChatConstants.REDIS_WECAHT_PUBLIC_ACCESS_TOKEN_KEY, accessTokenVo.getAccessToken(),
                accessTokenVo.getExpiresIn().longValue() - 1800L, TimeUnit.SECONDS);
        return accessTokenVo.getAccessToken();
    }


    /**
     * 获取小程序accessToken
     * @return accessToken
     */
    @Override
    public String getMiniAccessToken() {
        boolean exists = redisUtil.exists(WeChatConstants.REDIS_WECAHT_MINI_ACCESS_TOKEN_KEY);
        if (exists) {
            Object accessToken = redisUtil.get(WeChatConstants.REDIS_WECAHT_MINI_ACCESS_TOKEN_KEY);
            return accessToken.toString();
        }
        String appId = systemConfigService.getValueByKey(WeChatConstants.WECHAT_MINI_APPID);
        if (StrUtil.isBlank(appId)) {
            throw new CrmebException("微信小程序appId未设置");
        }
        String secret = systemConfigService.getValueByKey(WeChatConstants.WECHAT_MINI_APPSECRET);
        if (StrUtil.isBlank(secret)) {
            throw new CrmebException("微信小程序secret未设置");
        }
        WeChatAccessTokenVo accessTokenVo = getAccessToken(appId, secret, "mini");
        // 缓存accessToken
        redisUtil.set(WeChatConstants.REDIS_WECAHT_MINI_ACCESS_TOKEN_KEY, accessTokenVo.getAccessToken(),
                accessTokenVo.getExpiresIn().longValue() - 1800L, TimeUnit.SECONDS);
        return accessTokenVo.getAccessToken();
    }

    /**
     * 获取开放平台access_token
     * 通过 code 获取
     * 公众号使用
     * @return 开放平台accessToken对象
     */
    @Override
    public WeChatOauthToken getOauth2AccessToken(String code) {
        String appId = systemConfigService.getValueByKey(WeChatConstants.WECHAT_PUBLIC_APPID);
        if (StrUtil.isBlank(appId)) {
            throw new CrmebException("微信公众号appId未设置");
        }
        String secret = systemConfigService.getValueByKey(WeChatConstants.WECHAT_PUBLIC_APPSECRET);
        if (StrUtil.isBlank(secret)) {
            throw new CrmebException("微信公众号secret未设置");
        }
        String url = StrUtil.format(WeChatConstants.WECHAT_OAUTH2_ACCESS_TOKEN_URL, appId, secret, code);
        JSONObject data = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                // 保存到微信异常表
                wxExceptionDispose(data, "微信获取开放平台access_token异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return JSONObject.parseObject(data.toJSONString(), WeChatOauthToken.class);
    }

    /**
     * 获取开放平台用户信息
     * @param accessToken 调用凭证
     * @param openid 普通用户的标识，对当前开发者帐号唯一
     * 公众号使用
     * @return 开放平台用户信息对象
     */
    @Override
    public WeChatAuthorizeLoginUserInfoVo getSnsUserInfo(String accessToken, String openid) {
        String url = StrUtil.format(WeChatConstants.WECHAT_SNS_USERINFO_URL, accessToken, openid, "zh_CN");
        JSONObject data = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                // 保存到微信异常表
                wxExceptionDispose(data, "微信获取开放平台用户信息异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return JSONObject.parseObject(data.toJSONString(), WeChatAuthorizeLoginUserInfoVo.class);
    }

    /**
     * 小程序登录凭证校验
     * @return 小程序登录校验对象
     */
    @Override
    public WeChatMiniAuthorizeVo miniAuthCode(String code) {
        String appId = systemConfigService.getValueByKey(WeChatConstants.WECHAT_MINI_APPID);
        if (StrUtil.isBlank(appId)) {
            throw new CrmebException("微信小程序appId未设置");
        }
        String secret = systemConfigService.getValueByKey(WeChatConstants.WECHAT_MINI_APPSECRET);
        if (StrUtil.isBlank(secret)) {
            throw new CrmebException("微信小程序secret未设置");
        }
        String url = StrUtil.format(WeChatConstants.WECHAT_MINI_SNS_AUTH_CODE2SESSION_URL, appId, secret, code);
        JSONObject data = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                // 保存到微信异常表
                wxExceptionDispose(data, "微信小程序登录凭证校验异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return JSONObject.parseObject(data.toJSONString(), WeChatMiniAuthorizeVo.class);
    }

    /**
     * 获取微信公众号js配置参数
     * @return WeChatJsSdkConfigResponse
     */
    @Override
    public WeChatJsSdkConfigResponse getJsSdkConfig(String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CrmebException("url无法解析！");
        }

        String appId = systemConfigService.getValueByKey(WeChatConstants.WECHAT_PUBLIC_APPID);
        if (StrUtil.isBlank(appId)) {
            throw new CrmebException("微信公众号appId未设置");
        }
        String ticket = getJsApiTicket();
        String nonceStr = CrmebUtil.getUuid();
        Long timestamp = DateUtil.currentSeconds();
        String signature = getJsSDKSignature(nonceStr, ticket, timestamp , url);

        WeChatJsSdkConfigResponse response = new WeChatJsSdkConfigResponse();
        response.setUrl(url);
        response.setAppId(appId);
        response.setNonceStr(nonceStr);
        response.setTimestamp(timestamp);
        response.setSignature(signature);
        response.setJsApiList(CrmebUtil.stringToArrayStr(WeChatConstants.PUBLIC_API_JS_API_SDK_LIST));
        response.setDebug(crmebConfig.isWechatJsApiDebug());
        return response;
    }

    /**
     * 生成小程序码
     * @param page 必须是已经发布的小程序存在的页面
     * @param scene 最大32个可见字符，只支持数字，大小写英文以及部分特殊字符：!#$&'()*+,/:;=?@-._~，其它字符请自行编码为合法字符
     * @return 小程序码
     */
    @Override
    public String createQrCode(String page, String scene) {
        String miniAccessToken = getMiniAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_MINI_QRCODE_UNLIMITED_URL, miniAccessToken);
        HashMap<String, Object> map = new HashMap<>();
        map.put("scene", scene);
        map.put("page", page);
        map.put("width", 200);
        byte[] bytes = restTemplateUtil.postJsonDataAndReturnBuffer(url, new JSONObject(map));
        String response = new String(bytes);
        if (StringUtils.contains(response,"errcode")) {
            logger.error("微信生成小程序码异常"+response);
            JSONObject data = JSONObject.parseObject(response);
            // 保存到微信异常表
            wxExceptionDispose(data, "微信小程序生成小程序码异常");
            if (data.getString("errcode").equals("40001")) {
                redisUtil.delete(WeChatConstants.REDIS_WECAHT_MINI_ACCESS_TOKEN_KEY);
                miniAccessToken = getMiniAccessToken();
                url = StrUtil.format(WeChatConstants.WECHAT_MINI_QRCODE_UNLIMITED_URL, miniAccessToken);
                bytes = restTemplateUtil.postJsonDataAndReturnBuffer(url, new JSONObject(map));
                response = new String(bytes);
                if (StringUtils.contains(response,"errcode")) {
                    logger.error("微信生成小程序码重试异常"+response);
                    JSONObject data2 = JSONObject.parseObject(response);
                    // 保存到微信异常表
                    wxExceptionDispose(data2, "微信小程序重试生成小程序码异常");
                } else {
                    try {
                        return CrmebUtil.getBase64Image(Base64.encodeBase64String(bytes));
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new CrmebException("微信小程序码转换Base64异常");
                    }
                }
            }
            throw new CrmebException("微信生成二维码异常");
        }
        try {
            return CrmebUtil.getBase64Image(Base64.encodeBase64String(bytes));
        } catch (Exception e) {
            e.printStackTrace();
            throw new CrmebException("微信小程序码转换Base64异常");
        }
    }

    /**
     * 生成小程序码
     *
     * @param jsonObject  微信端参数
     * @return 小程序码
     */
    @Override
    public String createQrCode(JSONObject jsonObject) {
        String miniAccessToken = getMiniAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_MINI_QRCODE_UNLIMITED_URL, miniAccessToken);
        logger.info("微信小程序码生成参数:{}", jsonObject);
        byte[] bytes = restTemplateUtil.postJsonDataAndReturnBuffer(url, jsonObject);
        String response = new String(bytes);
        if (StringUtils.contains(response, "errcode")) {
            logger.error("微信生成小程序码异常" + response);
            JSONObject data = JSONObject.parseObject(response);
            // 保存到微信异常表
            wxExceptionDispose(data, "微信小程序生成小程序码异常");
            if (data.getString("errcode").equals("40001")) {
                redisUtil.secondDelete(WeChatConstants.REDIS_WECAHT_MINI_ACCESS_TOKEN_KEY);
                miniAccessToken = getMiniAccessToken();
                url = StrUtil.format(WeChatConstants.WECHAT_MINI_QRCODE_UNLIMITED_URL, miniAccessToken);
                bytes = restTemplateUtil.postJsonDataAndReturnBuffer(url, jsonObject);
                response = new String(bytes);
                if (StringUtils.contains(response, "errcode")) {
                    logger.error("微信生成小程序码重试异常" + response);
                    JSONObject data2 = JSONObject.parseObject(response);
                    // 保存到微信异常表
                    wxExceptionDispose(data2, "微信小程序重试生成小程序码异常");
                } else {
                    try {
                        return CrmebUtil.getBase64Image(Base64.encodeBase64String(bytes));
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new CrmebException("微信小程序码转换Base64异常");
                    }
                }
            }
            throw new CrmebException("微信生成二维码异常");
        }
        try {
            return CrmebUtil.getBase64Image(Base64.encodeBase64String(bytes));
        } catch (Exception e) {
            e.printStackTrace();
            throw new CrmebException("微信小程序码转换Base64异常");
        }
    }

    /**
     * 微信预下单接口(统一下单)
     * @param unifiedorderVo 预下单请求对象
     * @return 微信预下单返回对象
     */
    @Override
    public CreateOrderResponseVo payUnifiedorder(CreateOrderRequestVo unifiedorderVo) {
        return payUnifiedorderV2(unifiedorderVo);
    }

    @Override
    public CreateOrderResponseVo payUnifiedorder(CreateOrderRequestVo unifiedorderVo, WeChatPayChannelConfig payConfig) {
        if (ObjectUtil.isNull(payConfig) || !"v3".equalsIgnoreCase(StrUtil.blankToDefault(payConfig.getApiVersion(), "v2"))) {
            return payUnifiedorderV2(unifiedorderVo);
        }
        return payUnifiedorderV3(unifiedorderVo, payConfig);
    }

    private CreateOrderResponseVo payUnifiedorderV2(CreateOrderRequestVo unifiedorderVo) {
        try {
            String url = PayConstants.WX_PAY_API_URL + PayConstants.WX_PAY_API_URI;
            String request = XmlUtil.objectToXml(unifiedorderVo);
            String xml = restTemplateUtil.postXml(url, request);
            HashMap<String, Object> map = XmlUtil.xmlToMap(xml);
            if (null == map) {
                throw new CrmebException("微信下单失败！");
            }
            // 保存微信预下单
            WechatPayInfo wechatPayInfo = createWechatPayInfo(unifiedorderVo);

            CreateOrderResponseVo responseVo = CrmebUtil.mapToObj(map, CreateOrderResponseVo.class);
            if (responseVo.getReturnCode().toUpperCase().equals("FAIL")) {
                // 保存到微信异常表
                wxPayExceptionDispose(map, "微信支付预下单异常");
                wechatPayInfo.setErrCode(map.get("return_code").toString());
                wechatPayInfoService.save(wechatPayInfo);
                throw new CrmebException("微信下单失败1！" +  responseVo.getReturnMsg());
            }

            if (responseVo.getResultCode().toUpperCase().equals("FAIL")) {
                wxPayExceptionDispose(map, "微信支付预下单业务异常");
                wechatPayInfo.setErrCode(map.get("err_code").toString());
                wechatPayInfoService.save(wechatPayInfo);
                throw new CrmebException("微信下单失败2！" + responseVo.getErrCodeDes());
            }
            wechatPayInfo.setErrCode("200");
            wechatPayInfo.setPrepayId(responseVo.getPrepayId());
            wechatPayInfoService.save(wechatPayInfo);
            responseVo.setExtra(unifiedorderVo.getScene_info());
            return responseVo;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CrmebException(e.getMessage());
        }
    }

    private CreateOrderResponseVo payUnifiedorderV3(CreateOrderRequestVo unifiedorderVo, WeChatPayChannelConfig payConfig) {
        try {
            String path = resolveV3UnifiedorderPath(unifiedorderVo.getTrade_type(), Boolean.TRUE.equals(payConfig.getServiceProviderMode()));
            JSONObject requestBody = buildV3UnifiedorderBody(unifiedorderVo, payConfig);
            String responseText = doV3Request("POST", path, requestBody.toJSONString(), payConfig);
            JSONObject responseJson = JSONObject.parseObject(responseText);
            if (ObjectUtil.isNull(responseJson)) {
                throw new CrmebException("微信下单失败：v3响应为空");
            }
            String prepayId = responseJson.getString("prepay_id");
            if (StrUtil.isBlank(prepayId)) {
                throw new CrmebException("微信下单失败：v3响应缺少prepay_id");
            }
            CreateOrderResponseVo responseVo = new CreateOrderResponseVo();
            responseVo.setAppId(unifiedorderVo.getAppid());
            responseVo.setMchId(unifiedorderVo.getMch_id());
            responseVo.setTradeType(unifiedorderVo.getTrade_type());
            responseVo.setReturnCode("SUCCESS");
            responseVo.setResultCode("SUCCESS");
            responseVo.setPrepayId(prepayId);
            responseVo.setMWebUrl(responseJson.getString("h5_url"));
            responseVo.setExtra(unifiedorderVo.getScene_info());

            WechatPayInfo wechatPayInfo = createWechatPayInfo(unifiedorderVo);
            wechatPayInfo.setErrCode("200");
            wechatPayInfo.setPrepayId(prepayId);
            wechatPayInfoService.save(wechatPayInfo);
            return responseVo;
        } catch (CrmebException e) {
            throw e;
        } catch (Exception e) {
            throw new CrmebException("微信下单失败：" + e.getMessage());
        }
    }

    private String resolveV3UnifiedorderPath(String tradeType, boolean serviceProviderMode) {
        String safeType = StrUtil.blankToDefault(StrUtil.trim(tradeType), PayConstants.WX_PAY_TRADE_TYPE_JS).toUpperCase();
        if ("APP".equals(safeType)) {
            return serviceProviderMode ? "/v3/pay/partner/transactions/app" : "/v3/pay/transactions/app";
        }
        if (PayConstants.WX_PAY_TRADE_TYPE_H5.equals(safeType)) {
            return serviceProviderMode ? "/v3/pay/partner/transactions/h5" : "/v3/pay/transactions/h5";
        }
        return serviceProviderMode ? "/v3/pay/partner/transactions/jsapi" : "/v3/pay/transactions/jsapi";
    }

    private JSONObject buildV3UnifiedorderBody(CreateOrderRequestVo vo, WeChatPayChannelConfig payConfig) {
        JSONObject body = new JSONObject();
        boolean serviceProviderMode = Boolean.TRUE.equals(payConfig.getServiceProviderMode());
        if (serviceProviderMode) {
            body.put("sp_appid", payConfig.getAppId());
            body.put("sp_mchid", payConfig.getMchId());
            body.put("sub_mchid", payConfig.getSubMchId());
            if (StrUtil.isNotBlank(payConfig.getSubAppId())) {
                body.put("sub_appid", payConfig.getSubAppId());
            }
        } else {
            body.put("appid", payConfig.getAppId());
            body.put("mchid", payConfig.getMchId());
        }
        body.put("description", vo.getBody());
        body.put("out_trade_no", vo.getOut_trade_no());
        body.put("notify_url", vo.getNotify_url());
        if (StrUtil.isNotBlank(vo.getAttach())) {
            body.put("attach", vo.getAttach());
        }
        if (StrUtil.isNotBlank(vo.getTime_expire())) {
            body.put("time_expire", toV3TimeExpire(vo.getTime_expire()));
        }
        JSONObject amount = new JSONObject();
        amount.put("total", vo.getTotal_fee());
        amount.put("currency", StrUtil.blankToDefault(vo.getFee_type(), "CNY"));
        body.put("amount", amount);

        String tradeType = StrUtil.blankToDefault(vo.getTrade_type(), PayConstants.WX_PAY_TRADE_TYPE_JS).toUpperCase();
        if (PayConstants.WX_PAY_TRADE_TYPE_H5.equals(tradeType)) {
            JSONObject sceneInfo = new JSONObject();
            sceneInfo.put("payer_client_ip", vo.getSpbill_create_ip());
            JSONObject h5Info = new JSONObject();
            h5Info.put("type", "Wap");
            sceneInfo.put("h5_info", h5Info);
            body.put("scene_info", sceneInfo);
        }
        if (PayConstants.WX_PAY_TRADE_TYPE_JS.equals(tradeType)) {
            JSONObject payer = new JSONObject();
            if (serviceProviderMode) {
                if (StrUtil.isNotBlank(vo.getSub_openid())) {
                    payer.put("sub_openid", vo.getSub_openid());
                } else if (StrUtil.isNotBlank(vo.getOpenid())) {
                    payer.put("sp_openid", vo.getOpenid());
                }
            } else if (StrUtil.isNotBlank(vo.getOpenid())) {
                payer.put("openid", vo.getOpenid());
            }
            if (payer.isEmpty()) {
                throw new CrmebException("微信下单失败：JSAPI缺少openid");
            }
            body.put("payer", payer);
        }
        return body;
    }

    private String toV3TimeExpire(String yyyyMMddHHmmss) {
        if (StrUtil.isBlank(yyyyMMddHHmmss) || yyyyMMddHHmmss.length() != 14) {
            return yyyyMMddHHmmss;
        }
        String year = yyyyMMddHHmmss.substring(0, 4);
        String month = yyyyMMddHHmmss.substring(4, 6);
        String day = yyyyMMddHHmmss.substring(6, 8);
        String hour = yyyyMMddHHmmss.substring(8, 10);
        String minute = yyyyMMddHHmmss.substring(10, 12);
        String second = yyyyMMddHHmmss.substring(12, 14);
        return year + "-" + month + "-" + day + "T" + hour + ":" + minute + ":" + second + "+08:00";
    }

    private String doV3Request(String method, String path, String body, WeChatPayChannelConfig payConfig) {
        String requestBody = StrUtil.blankToDefault(body, "");
        String nonceStr = CrmebUtil.getUuid().replace("-", "");
        String timestamp = String.valueOf(DateUtil.currentSeconds());
        String canonical = method + "\n" + path + "\n" + timestamp + "\n" + nonceStr + "\n" + requestBody + "\n";
        String signature = WeChatPayV3Crypto.signMessage(canonical, payConfig.getPrivateKeyPath());
        String authorization = WeChatPayV3Crypto.buildAuthorization(
                payConfig.getMchId(),
                payConfig.getSerialNo(),
                nonceStr,
                timestamp,
                signature
        );

        String url = "https://api.mch.weixin.qq.com" + path;
        HttpRequest request;
        if ("GET".equalsIgnoreCase(method)) {
            request = HttpRequest.get(url);
        } else if ("POST".equalsIgnoreCase(method)) {
            request = HttpRequest.post(url);
        } else {
            throw new CrmebException("不支持的微信v3请求方法: " + method);
        }
        request.header("Authorization", authorization)
                .header("Wechatpay-Serial", payConfig.getSerialNo())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json;charset=UTF-8");
        if (StrUtil.isNotBlank(requestBody)) {
            request.body(requestBody);
        }
        HttpResponse response = request.execute();
        String responseText = response.body();
        if (response.getStatus() < 200 || response.getStatus() >= 300) {
            String errMsg = "微信v3接口失败: httpStatus=" + response.getStatus();
            if (StrUtil.isNotBlank(responseText)) {
                JSONObject errJson = JSONObject.parseObject(responseText);
                if (ObjectUtil.isNotNull(errJson) && errJson.containsKey("message")) {
                    errMsg += ", message=" + errJson.getString("message");
                }
            }
            throw new CrmebException(errMsg);
        }
        return responseText;
    }

    /**
     * 生成微信订单表对象
     * @param vo 预下单数据
     * @return WechatPayInfo
     */
    private WechatPayInfo createWechatPayInfo(CreateOrderRequestVo vo) {
        WechatPayInfo payInfo = new WechatPayInfo();
        payInfo.setAppId(vo.getAppid());
        payInfo.setMchId(vo.getMch_id());
        payInfo.setDeviceInfo(vo.getDevice_info());
        payInfo.setOpenId(vo.getOpenid());
        payInfo.setNonceStr(vo.getNonce_str());
        payInfo.setSign(vo.getSign());
        payInfo.setSignType(vo.getSign_type());
        payInfo.setBody(vo.getBody());
        payInfo.setDetail(vo.getDetail());
        payInfo.setAttach(vo.getAttach());
        payInfo.setOutTradeNo(vo.getOut_trade_no());
        payInfo.setFeeType(vo.getFee_type());
        payInfo.setTotalFee(vo.getTotal_fee());
        payInfo.setSpbillCreateIp(vo.getSpbill_create_ip());
        payInfo.setTimeStart(vo.getTime_start());
        payInfo.setTimeExpire(vo.getTime_expire());
        payInfo.setNotifyUrl(vo.getNotify_url());
        payInfo.setTradeType(vo.getTrade_type());
        payInfo.setProductId(vo.getProduct_id());
        payInfo.setSceneInfo(vo.getScene_info());
        return payInfo;
    }

    /**
     * 微信支付查询订单
     * @return 支付订单查询结果
     */
    @Override
    public MyRecord payOrderQuery(Map<String, String> payVo) {
        return payOrderQueryV2(payVo);
    }

    @Override
    public MyRecord payOrderQuery(String outTradeNo, WeChatPayChannelConfig payConfig) {
        if (ObjectUtil.isNull(payConfig)) {
            throw new CrmebException("微信支付配置为空");
        }
        if (!"v3".equalsIgnoreCase(StrUtil.blankToDefault(payConfig.getApiVersion(), "v2"))) {
            Map<String, String> map = CollUtil.newHashMap();
            map.put("appid", payConfig.getAppId());
            map.put("mch_id", payConfig.getMchId());
            if (Boolean.TRUE.equals(payConfig.getServiceProviderMode())) {
                map.put("sub_mch_id", payConfig.getSubMchId());
                if (StrUtil.isNotBlank(payConfig.getSubAppId()) && !payConfig.getSubAppId().equals(payConfig.getAppId())) {
                    map.put("sub_appid", payConfig.getSubAppId());
                }
            }
            map.put("out_trade_no", outTradeNo);
            map.put("nonce_str", WxPayUtil.getNonceStr());
            map.put("sign_type", PayConstants.WX_PAY_SIGN_TYPE_MD5);
            map.put("sign", WxPayUtil.getSign(map, payConfig.getSignKey()));
            return payOrderQueryV2(map);
        }
        return payOrderQueryV3(outTradeNo, payConfig);
    }

    private MyRecord payOrderQueryV2(Map<String, String> payVo) {
        String url = PayConstants.WX_PAY_API_URL + PayConstants.WX_PAY_ORDER_QUERY_API_URI;
        try {
            String request = XmlUtil.mapToXml(payVo);
            String xml = restTemplateUtil.postXml(url, request);
            HashMap<String, Object> map = XmlUtil.xmlToMap(xml);
            MyRecord record = new MyRecord();
            if (null == map) {
                throw new CrmebException("微信订单查询失败！");
            }
            record.setColums(map);
            if (record.getStr("return_code").toUpperCase().equals("FAIL")) {
                wxPayQueryExceptionDispose(record, "微信支付查询订单通信异常");
                throw new CrmebException("微信订单查询失败1！" +  record.getStr("return_msg"));
            }

            if (record.getStr("result_code").toUpperCase().equals("FAIL")) {
                wxPayQueryExceptionDispose(record, "微信支付查询订单结果异常");
                throw new CrmebException("微信订单查询失败2！" + record.getStr("err_code") + record.getStr("err_code_des"));
            }
            if (!record.getStr("trade_state").toUpperCase().equals("SUCCESS")) {
                wxPayQueryExceptionDispose(record, "微信支付查询订单状态异常");
                throw new CrmebException("微信订单支付失败3！" + record.getStr("trade_state"));
            }

            return record;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CrmebException("查询微信订单mapToXml异常===》" + e.getMessage());
        }
    }

    private MyRecord payOrderQueryV3(String outTradeNo, WeChatPayChannelConfig payConfig) {
        try {
            String encodedTradeNo = URLEncoder.encode(outTradeNo, StandardCharsets.UTF_8.name());
            String path;
            if (Boolean.TRUE.equals(payConfig.getServiceProviderMode())) {
                path = "/v3/pay/partner/transactions/out-trade-no/" + encodedTradeNo
                        + "?sp_mchid=" + payConfig.getMchId()
                        + "&sub_mchid=" + payConfig.getSubMchId();
            } else {
                path = "/v3/pay/transactions/out-trade-no/" + encodedTradeNo
                        + "?mchid=" + payConfig.getMchId();
            }
            String responseText = doV3Request("GET", path, "", payConfig);
            JSONObject responseJson = JSONObject.parseObject(responseText);
            if (ObjectUtil.isNull(responseJson)) {
                throw new CrmebException("微信订单查询失败：v3响应为空");
            }
            String tradeState = responseJson.getString("trade_state");
            if (!"SUCCESS".equalsIgnoreCase(tradeState)) {
                throw new CrmebException("微信订单支付失败3！" + tradeState);
            }
            return convertV3QueryResponseToRecord(responseJson);
        } catch (CrmebException e) {
            throw e;
        } catch (Exception e) {
            throw new CrmebException("查询微信订单v3异常===》" + e.getMessage());
        }
    }

    private MyRecord convertV3QueryResponseToRecord(JSONObject responseJson) {
        MyRecord record = new MyRecord();
        JSONObject amount = responseJson.getJSONObject("amount");
        Integer total = ObjectUtil.isNull(amount) ? null : amount.getInteger("total");
        Integer payerTotal = ObjectUtil.isNull(amount) ? null : amount.getInteger("payer_total");
        record.set("out_trade_no", responseJson.getString("out_trade_no"));
        record.set("transaction_id", responseJson.getString("transaction_id"));
        record.set("trade_state", responseJson.getString("trade_state"));
        record.set("trade_state_desc", responseJson.getString("trade_state_desc"));
        record.set("total_fee", ObjectUtil.isNull(total) ? 0 : total);
        record.set("cash_fee", ObjectUtil.isNull(payerTotal) ? 0 : payerTotal);
        record.set("coupon_fee", 0);
        record.set("appid", StrUtil.blankToDefault(responseJson.getString("appid"), responseJson.getString("sp_appid")));
        record.set("mch_id", StrUtil.blankToDefault(responseJson.getString("mchid"), responseJson.getString("sp_mchid")));
        record.set("sub_mch_id", responseJson.getString("sub_mchid"));
        record.set("time_end", responseJson.getString("success_time"));
        record.set("bank_type", "");
        record.set("is_subscribe", "Y");
        return record;
    }

    /**
     * 微信关闭订单
     */
    @Override
    public Boolean payOrderClose(String outTradeNo, WeChatPayChannelConfig payConfig) {
        if (ObjectUtil.isNull(payConfig)) {
            throw new CrmebException("微信支付配置为空");
        }
        if (StrUtil.isBlank(outTradeNo)) {
            throw new CrmebException("微信关单失败：商户订单号为空");
        }
        if ("v3".equalsIgnoreCase(StrUtil.blankToDefault(payConfig.getApiVersion(), "v2"))) {
            return payOrderCloseV3(outTradeNo, payConfig);
        }
        return payOrderCloseV2(outTradeNo, payConfig);
    }

    private Boolean payOrderCloseV2(String outTradeNo, WeChatPayChannelConfig payConfig) {
        String url = PayConstants.WX_PAY_API_URL + "pay/closeorder";
        try {
            Map<String, String> map = CollUtil.newHashMap();
            map.put("appid", payConfig.getAppId());
            map.put("mch_id", payConfig.getMchId());
            if (Boolean.TRUE.equals(payConfig.getServiceProviderMode())) {
                map.put("sub_mch_id", payConfig.getSubMchId());
                if (StrUtil.isNotBlank(payConfig.getSubAppId()) && !payConfig.getSubAppId().equals(payConfig.getAppId())) {
                    map.put("sub_appid", payConfig.getSubAppId());
                }
            }
            map.put("out_trade_no", outTradeNo);
            map.put("nonce_str", WxPayUtil.getNonceStr());
            map.put("sign_type", PayConstants.WX_PAY_SIGN_TYPE_MD5);
            map.put("sign", WxPayUtil.getSign(map, payConfig.getSignKey()));

            String request = XmlUtil.mapToXml(map);
            String xml = restTemplateUtil.postXml(url, request);
            HashMap<String, Object> resp = XmlUtil.xmlToMap(xml);
            if (null == resp) {
                throw new CrmebException("微信关单失败：响应为空");
            }
            MyRecord record = new MyRecord();
            record.setColums(resp);
            if ("FAIL".equalsIgnoreCase(record.getStr("return_code"))) {
                throw new CrmebException("微信关单失败1！" + record.getStr("return_msg"));
            }
            if ("FAIL".equalsIgnoreCase(record.getStr("result_code"))) {
                String errCode = StrUtil.blankToDefault(record.getStr("err_code"), "").toUpperCase();
                if ("ORDERNOTEXIST".equals(errCode)) {
                    return Boolean.TRUE;
                }
                if ("ORDERPAID".equals(errCode)) {
                    throw new CrmebException("微信关单失败：订单已支付");
                }
                throw new CrmebException("微信关单失败2！" + record.getStr("err_code") + record.getStr("err_code_des"));
            }
            return Boolean.TRUE;
        } catch (CrmebException e) {
            throw e;
        } catch (Exception e) {
            throw new CrmebException("微信关单失败：" + e.getMessage());
        }
    }

    private Boolean payOrderCloseV3(String outTradeNo, WeChatPayChannelConfig payConfig) {
        try {
            String encodedTradeNo = URLEncoder.encode(outTradeNo, StandardCharsets.UTF_8.name());
            JSONObject body = new JSONObject();
            String path;
            if (Boolean.TRUE.equals(payConfig.getServiceProviderMode())) {
                path = "/v3/pay/partner/transactions/out-trade-no/" + encodedTradeNo + "/close";
                body.put("sp_mchid", payConfig.getMchId());
                body.put("sub_mchid", payConfig.getSubMchId());
            } else {
                path = "/v3/pay/transactions/out-trade-no/" + encodedTradeNo + "/close";
                body.put("mchid", payConfig.getMchId());
            }
            doV3Request("POST", path, body.toJSONString(), payConfig);
            return Boolean.TRUE;
        } catch (CrmebException e) {
            throw e;
        } catch (Exception e) {
            throw new CrmebException("微信关单失败：" + e.getMessage());
        }
    }

    /**
     * 微信退款查询
     */
    @Override
    public MyRecord payRefundQuery(Map<String, String> payVo) {
        return payRefundQueryV2(payVo);
    }

    @Override
    public MyRecord payRefundQuery(String outRefundNo, WeChatPayChannelConfig payConfig) {
        if (ObjectUtil.isNull(payConfig)) {
            throw new CrmebException("微信支付配置为空");
        }
        if (StrUtil.isBlank(outRefundNo)) {
            throw new CrmebException("微信退款查询失败：商户退款单号为空");
        }
        if (!"v3".equalsIgnoreCase(StrUtil.blankToDefault(payConfig.getApiVersion(), "v2"))) {
            Map<String, String> map = CollUtil.newHashMap();
            map.put("appid", payConfig.getAppId());
            map.put("mch_id", payConfig.getMchId());
            if (Boolean.TRUE.equals(payConfig.getServiceProviderMode())) {
                map.put("sub_mch_id", payConfig.getSubMchId());
                if (StrUtil.isNotBlank(payConfig.getSubAppId()) && !payConfig.getSubAppId().equals(payConfig.getAppId())) {
                    map.put("sub_appid", payConfig.getSubAppId());
                }
            }
            map.put("out_refund_no", outRefundNo);
            map.put("nonce_str", WxPayUtil.getNonceStr());
            map.put("sign_type", PayConstants.WX_PAY_SIGN_TYPE_MD5);
            map.put("sign", WxPayUtil.getSign(map, payConfig.getSignKey()));
            return payRefundQueryV2(map);
        }
        return payRefundQueryV3(outRefundNo, payConfig);
    }

    private MyRecord payRefundQueryV2(Map<String, String> payVo) {
        String url = PayConstants.WX_PAY_API_URL + PayConstants.WX_PAY_REFUND_QUERY_API_URI;
        try {
            String request = XmlUtil.mapToXml(payVo);
            String xml = restTemplateUtil.postXml(url, request);
            HashMap<String, Object> map = XmlUtil.xmlToMap(xml);
            MyRecord record = new MyRecord();
            if (null == map) {
                throw new CrmebException("微信退款查询失败！");
            }
            record.setColums(map);
            if (record.getStr("return_code").toUpperCase().equals("FAIL")) {
                wxPayQueryExceptionDispose(record, "微信退款查询通信异常");
                throw new CrmebException("微信退款查询失败1！" + record.getStr("return_msg"));
            }
            if (record.getStr("result_code").toUpperCase().equals("FAIL")) {
                wxPayQueryExceptionDispose(record, "微信退款查询结果异常");
                throw new CrmebException("微信退款查询失败2！" + record.getStr("err_code") + record.getStr("err_code_des"));
            }
            return record;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CrmebException("查询微信退款mapToXml异常===》" + e.getMessage());
        }
    }

    private MyRecord payRefundQueryV3(String outRefundNo, WeChatPayChannelConfig payConfig) {
        try {
            String encodedRefundNo = URLEncoder.encode(outRefundNo, StandardCharsets.UTF_8.name());
            String path = "/v3/refund/domestic/refunds/" + encodedRefundNo;
            if (Boolean.TRUE.equals(payConfig.getServiceProviderMode()) && StrUtil.isNotBlank(payConfig.getSubMchId())) {
                path = path + "?sub_mchid=" + URLEncoder.encode(payConfig.getSubMchId(), StandardCharsets.UTF_8.name());
            }
            String responseText = doV3Request("GET", path, "", payConfig);
            JSONObject responseJson = JSONObject.parseObject(responseText);
            if (ObjectUtil.isNull(responseJson)) {
                throw new CrmebException("微信退款查询失败：v3响应为空");
            }
            return convertV3RefundQueryResponseToRecord(responseJson);
        } catch (CrmebException e) {
            throw e;
        } catch (Exception e) {
            throw new CrmebException("查询微信退款v3异常===》" + e.getMessage());
        }
    }

    private MyRecord convertV3RefundQueryResponseToRecord(JSONObject responseJson) {
        MyRecord record = new MyRecord();
        JSONObject firstRefund = firstRefundNode(responseJson);
        if (ObjectUtil.isNotNull(firstRefund) && firstRefund.size() == 1 && firstRefund.containsKey("$ref")) {
            firstRefund = unwrapJSONObject(responseJson.get("amount"));
        }
        JSONObject amount = normalizeAmountNode(responseJson.get("amount"));
        if (ObjectUtil.isNull(amount) && ObjectUtil.isNotNull(firstRefund)) {
            amount = normalizeAmountNode(firstRefund.get("amount"));
        }
        JSONObject firstRefundAmount = ObjectUtil.isNull(firstRefund) ? null : normalizeAmountNode(firstRefund.get("amount"));
        Integer refund = firstNonNullInt(
                ObjectUtil.isNull(amount) ? null : amount.getInteger("refund"),
                firstNonNullInt(
                        ObjectUtil.isNull(firstRefundAmount) ? null : firstRefundAmount.getInteger("refund"),
                        ObjectUtil.isNull(firstRefund) ? null : firstRefund.getInteger("refund")));
        Integer total = firstNonNullInt(
                ObjectUtil.isNull(amount) ? null : amount.getInteger("total"),
                firstNonNullInt(
                        ObjectUtil.isNull(firstRefundAmount) ? null : firstRefundAmount.getInteger("total"),
                        ObjectUtil.isNull(firstRefund) ? null : firstRefund.getInteger("total")));
        JSONObject from = ObjectUtil.isNull(amount) ? null : unwrapJSONObject(amount.get("from"));
        JSONObject firstRefundFrom = ObjectUtil.isNull(firstRefundAmount) ? null : unwrapJSONObject(firstRefundAmount.get("from"));
        Integer payerRefund = firstNonNullInt(
                ObjectUtil.isNull(from) ? null : from.getInteger("payer_refund"),
                firstNonNullInt(
                        ObjectUtil.isNull(firstRefundAmount) ? null : firstRefundAmount.getInteger("payer_refund"),
                        firstNonNullInt(
                                ObjectUtil.isNull(firstRefundFrom) ? null : firstRefundFrom.getInteger("payer_refund"),
                                ObjectUtil.isNull(firstRefund) ? null : firstRefund.getInteger("payer_refund"))));

        String outRefundNo = firstNonBlank(responseJson.getString("out_refund_no"),
                ObjectUtil.isNull(firstRefund) ? "" : firstRefund.getString("out_refund_no"));
        String outTradeNo = firstNonBlank(responseJson.getString("out_trade_no"),
                ObjectUtil.isNull(firstRefund) ? "" : firstRefund.getString("out_trade_no"));
        String transactionId = firstNonBlank(responseJson.getString("transaction_id"),
                ObjectUtil.isNull(firstRefund) ? "" : firstRefund.getString("transaction_id"));
        String refundId = firstNonBlank(responseJson.getString("refund_id"),
                ObjectUtil.isNull(firstRefund) ? "" : firstRefund.getString("refund_id"));
        String refundStatus = firstNonBlank(responseJson.getString("status"),
                ObjectUtil.isNull(firstRefund) ? "" : firstNonBlank(firstRefund.getString("refund_status"), firstRefund.getString("status")));
        String refundSuccessTime = firstNonBlank(responseJson.getString("success_time"),
                ObjectUtil.isNull(firstRefund) ? "" : firstRefund.getString("success_time"));

        record.set("out_refund_no", outRefundNo);
        record.set("out_trade_no", outTradeNo);
        record.set("transaction_id", transactionId);
        record.set("refund_id", refundId);
        record.set("refund_status_0", StrUtil.blankToDefault(refundStatus, ""));
        record.set("refund_fee_0", ObjectUtil.isNull(refund) ? 0 : refund);
        record.set("settlement_refund_fee_0", ObjectUtil.isNull(refund) ? 0 : refund);
        record.set("total_fee", ObjectUtil.isNull(total) ? 0 : total);
        record.set("cash_refund_fee_0", ObjectUtil.isNull(payerRefund) ? 0 : payerRefund);
        record.set("refund_success_time_0", refundSuccessTime);
        return record;
    }

    private JSONObject normalizeAmountNode(Object data) {
        JSONObject node = unwrapJSONObject(data);
        if (ObjectUtil.isNull(node)) {
            return null;
        }
        if (node.containsKey("refund") || node.containsKey("total") || node.containsKey("payer_refund")) {
            return node;
        }
        JSONObject nested = unwrapJSONObject(node.get("amount"));
        if (ObjectUtil.isNotNull(nested) && (nested.containsKey("refund") || nested.containsKey("total") || nested.containsKey("payer_refund"))) {
            return nested;
        }
        return node;
    }

    private JSONObject firstRefundNode(JSONObject responseJson) {
        JSONArray refunds = unwrapJSONArray(responseJson.get("refunds"));
        if (ObjectUtil.isNull(refunds) || refunds.isEmpty()) {
            return null;
        }
        return unwrapJSONObject(refunds.get(0));
    }

    private JSONObject unwrapJSONObject(Object data) {
        if (ObjectUtil.isNull(data)) {
            return null;
        }
        if (data instanceof JSONObject) {
            return (JSONObject) data;
        }
        if (data instanceof JSONArray) {
            JSONArray arr = (JSONArray) data;
            if (arr.isEmpty()) {
                return null;
            }
            Object first = arr.get(0);
            if (first instanceof JSONObject) {
                return (JSONObject) first;
            }
        }
        return null;
    }

    private JSONArray unwrapJSONArray(Object data) {
        if (ObjectUtil.isNull(data)) {
            return null;
        }
        if (data instanceof JSONArray) {
            return (JSONArray) data;
        }
        if (data instanceof JSONObject) {
            JSONArray arr = new JSONArray();
            arr.add(data);
            return arr;
        }
        return null;
    }

    private Integer firstNonNullInt(Integer primary, Integer fallback) {
        return ObjectUtil.isNotNull(primary) ? primary : fallback;
    }

    private String firstNonBlank(String primary, String fallback) {
        return StrUtil.isNotBlank(primary) ? primary : StrUtil.blankToDefault(fallback, "");
    }

    /**
     * 微信公众号发送模板消息
     * @param templateMessage 模板消息对象
     * @return 是否发送成功
     */
    @Override
    public Boolean sendPublicTemplateMessage(TemplateMessageVo templateMessage) {
        String accessToken = getPublicAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_PUBLIC_SEND_TEMPLATE_URL, accessToken);
        JSONObject jsonData = JSONObject.parseObject(JSONObject.toJSONString(templateMessage));
        String result = restTemplateUtil.postJsonData(url, jsonData);
        JSONObject data = JSONObject.parseObject(result);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                wxExceptionDispose(data, "微信公众号发送模板消息异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 微信小程序发送订阅消息
     * @param templateMessage 消息对象
     * @return 是否发送成功
     */
    @Override
    public Boolean sendMiniSubscribeMessage(TemplateMessageVo templateMessage) {
        String accessToken = getMiniAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_MINI_SEND_SUBSCRIBE_URL, accessToken);
        JSONObject messAge = JSONObject.parseObject(JSONObject.toJSONString(templateMessage));
        String result = restTemplateUtil.postJsonData(url, messAge);
        JSONObject data = JSONObject.parseObject(result);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.getString("errcode").equals("40001")) {
                wxExceptionDispose(data, "微信小程序发送订阅消息异常");
                redisUtil.delete(WeChatConstants.REDIS_WECAHT_MINI_ACCESS_TOKEN_KEY);
                accessToken = getMiniAccessToken();
                url = StrUtil.format(WeChatConstants.WECHAT_MINI_SEND_SUBSCRIBE_URL, accessToken);
                result = restTemplateUtil.postJsonData(url, messAge);
                JSONObject data2 = JSONObject.parseObject(result);
                if (data2.containsKey("errcode") && !data2.getString("errcode").equals("0")) {
                    if (data2.containsKey("errmsg")) {
                        wxExceptionDispose(data2, "微信小程序发送订阅消息重试异常");
                        throw new CrmebException("微信接口调用失败：" + data2.getString("errcode") + data2.getString("errmsg"));
                    }
                } else {
                    return Boolean.TRUE;
                }
            }
            if (data.containsKey("errmsg")) {
                wxExceptionDispose(data, "微信小程序发送订阅消息异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 获取微信公众号自定义菜单配置
     * （使用本自定义菜单查询接口可以获取默认菜单和全部个性化菜单信息）
     * @return 公众号自定义菜单
     */
    @Override
    public JSONObject getPublicCustomMenu() {
        String accessToken = getPublicAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_PUBLIC_MENU_GET_URL, accessToken);
        JSONObject result = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(result)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (result.containsKey("errcode") && result.getString("errcode").equals("0")) {
            return result;
        }
        if (result.containsKey("errmsg")) {
            wxExceptionDispose(result, "微信公众号获取自定义菜单配置异常");
            throw new CrmebException("微信接口调用失败：" + result.getString("errcode") + result.getString("errmsg"));
        }
        return result;
    }

    /**
     * 创建微信自定义菜单
     * @param data 菜单json字符串
     * @return 创建结果
     */
    @Override
    public Boolean createPublicCustomMenu(String data) {
        String accessToken = getPublicAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_PUBLIC_MENU_CREATE_URL, accessToken);
        String result = restTemplateUtil.postJsonData(url, JSONObject.parseObject(data));
        logger.info("微信消息发送结果:" + result);
        JSONObject jsonObject = JSONObject.parseObject(result);
        if (ObjectUtil.isNull(jsonObject)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (jsonObject.containsKey("errcode") && jsonObject.getString("errcode").equals("0")) {
            return Boolean.TRUE;
        }
        if (jsonObject.containsKey("errmsg")) {
            wxExceptionDispose(jsonObject, "微信公众号创建自定义菜单异常");
            throw new CrmebException("微信接口调用失败：" + jsonObject.getString("errcode") + jsonObject.getString("errmsg"));
        }
        return Boolean.TRUE;
    }

    /**
     * 删除微信自定义菜单
     * @return 删除结果
     */
    @Override
    public Boolean deletePublicCustomMenu() {
        String accessToken = getPublicAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_PUBLIC_MENU_DELETE_URL, accessToken);
        JSONObject result = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(result)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (result.containsKey("errcode") && result.getString("errcode").equals("0")) {
            return Boolean.TRUE;
        }
        if (result.containsKey("errmsg")) {
            wxExceptionDispose(result, "微信公众号删除自定义菜单异常");
            throw new CrmebException("微信接口调用失败：" + result.getString("errcode") + result.getString("errmsg"));
        }
        return Boolean.TRUE;
    }

    /**
     * 企业号上传其他类型永久素材
     * 获取url
     * @param type 素材类型:图片（image）、语音（voice）、视频（video），普通文件(file)
     */
    @Override
    public String qyapiAddMaterialUrl(String type) {
        String accessToken = getPublicAccessToken();
        return StrUtil.format(WeChatConstants.WECHAT_PUBLIC_QYAPI_ADD_MATERIAL_URL, type, accessToken);
    }

    /**
     * 微信申请退款
     * @param wxRefundVo 微信申请退款对象
     * @param path 商户p12证书绝对路径
     * @return 申请退款结果对象
     */
    @Override
    public WxRefundResponseVo payRefund(WxRefundVo wxRefundVo, String path) {
        return payRefundV2(wxRefundVo, path);
    }

    @Override
    public WxRefundResponseVo payRefund(WxRefundVo wxRefundVo, WeChatPayChannelConfig payConfig) {
        if (ObjectUtil.isNull(payConfig)) {
            throw new CrmebException("微信支付配置为空");
        }
        if ("v3".equalsIgnoreCase(StrUtil.blankToDefault(payConfig.getApiVersion(), "v2"))) {
            return payRefundV3(wxRefundVo, payConfig);
        }
        return payRefundV2(wxRefundVo, payConfig.getCertificatePath());
    }

    private WxRefundResponseVo payRefundV2(WxRefundVo wxRefundVo, String path) {
        if (StrUtil.isBlank(path)) {
            throw new CrmebException("未配置微信退款证书路径，请检查支付配置");
        }
        String xmlStr = XmlUtil.objectToXml(wxRefundVo);
        String url = WeChatConstants.PAY_API_URL + WeChatConstants.PAY_REFUND_API_URI_WECHAT;
        HashMap<String, Object> map = CollUtil.newHashMap();
        String xml = "";
        try {
            xml = restTemplateUtil.postWXRefundXml(url, xmlStr, wxRefundVo.getMch_id(), path);
            map = XmlUtil.xmlToMap(xml);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CrmebException("xmlToMap错误，xml = " + xml);
        }
        if (null == map) {
            throw new CrmebException("微信无信息返回，微信申请退款失败！");
        }

        WxRefundResponseVo responseVo = CrmebUtil.mapToObj(map, WxRefundResponseVo.class);
        if (responseVo.getReturnCode().toUpperCase().equals("FAIL")) {
            wxPayExceptionDispose(map, "微信申请退款异常1");
            throw new CrmebException("微信申请退款失败1！" +  responseVo.getReturnMsg());
        }

        if (responseVo.getResultCode().toUpperCase().equals("FAIL")) {
            wxPayExceptionDispose(map, "微信申请退款业务异常");
            throw new CrmebException("微信申请退款失败2！" + responseVo.getErrCodeDes());
        }
        System.out.println("================微信申请退款结束=========================");
        return responseVo;
    }

    private WxRefundResponseVo payRefundV3(WxRefundVo wxRefundVo, WeChatPayChannelConfig payConfig) {
        try {
            JSONObject body = new JSONObject();
            body.put("out_trade_no", wxRefundVo.getOut_trade_no());
            body.put("out_refund_no", wxRefundVo.getOut_refund_no());
            if (StrUtil.isNotBlank(wxRefundVo.getNotify_url())) {
                body.put("notify_url", wxRefundVo.getNotify_url());
            }
            JSONObject amount = new JSONObject();
            amount.put("refund", wxRefundVo.getRefund_fee());
            amount.put("total", wxRefundVo.getTotal_fee());
            amount.put("currency", StrUtil.blankToDefault(wxRefundVo.getRefund_fee_type(), "CNY"));
            body.put("amount", amount);
            if (Boolean.TRUE.equals(payConfig.getServiceProviderMode())) {
                body.put("sub_mchid", payConfig.getSubMchId());
            }
            String responseText = doV3Request("POST", "/v3/refund/domestic/refunds", body.toJSONString(), payConfig);
            JSONObject responseJson = JSONObject.parseObject(responseText);
            if (ObjectUtil.isNull(responseJson)) {
                throw new CrmebException("微信申请退款失败：v3响应为空");
            }
            WxRefundResponseVo responseVo = new WxRefundResponseVo();
            responseVo.setReturnCode("SUCCESS");
            responseVo.setResultCode("SUCCESS");
            responseVo.setOutTradeNo(responseJson.getString("out_trade_no"));
            responseVo.setOutRefundNo(responseJson.getString("out_refund_no"));
            responseVo.setRefundId(responseJson.getString("refund_id"));
            return responseVo;
        } catch (CrmebException e) {
            throw e;
        } catch (Exception e) {
            throw new CrmebException("微信申请退款失败：" + e.getMessage());
        }
    }

    /**
     * 获取我的公众号模板消息列表
     * @return List
     */
    @Override
    public List<PublicMyTemplateVo> getPublicMyTemplateList() {
        String accessToken = getPublicAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_PUBLIC_GET_ALL_PRIVATE_TEMPLATE_URL, accessToken);
        JSONObject jsonObject = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(jsonObject)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (jsonObject.containsKey("errcode") && !jsonObject.getString("errcode").equals("0")) {
            if (jsonObject.containsKey("errmsg")) {
                // 保存到微信异常表
                wxExceptionDispose(jsonObject, StrUtil.format("获取我的公众号模板消息列表异常"));
                throw new CrmebException("微信接口调用失败：" + jsonObject.getString("errcode") + jsonObject.getString("errmsg"));
            }
        }
        JSONArray templateList = jsonObject.getJSONArray("template_list");
        List<PublicMyTemplateVo> voList = templateList.toJavaList(PublicMyTemplateVo.class);
        return voList;
    }

    /**
     * 删除微信公众号模板消息
     * @param templateId 模板编号
     * @return Boolean
     */
    @Override
    public Boolean delPublicMyTemplate(String templateId) {
        String accessToken = getPublicAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_PUBLIC_DEL_PRIVATE_TEMPLATE_URL, accessToken);
        HashMap<String, String> map = new HashMap<>();
        map.put("template_id", templateId);
        JSONObject jsonData = JSONObject.parseObject(JSONObject.toJSONString(map));
        String result = restTemplateUtil.postJsonData(url, jsonData);
        JSONObject data = JSONObject.parseObject(result);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                wxExceptionDispose(data, "删除微信公众号模板消息异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 添加公众号模板消息
     * @param templateIdShort 模板库中模板的编号，有“TM**”和“OPENTMTM**”等形式
     * @return 公众号模板编号（自己的）
     */
    @Override
    public String apiAddPublicTemplate(String templateIdShort) {
        String accessToken = getPublicAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_PUBLIC_API_ADD_TEMPLATE_URL, accessToken);
        HashMap<String, String> map = new HashMap<>();
        map.put("template_id_short", templateIdShort);
        JSONObject jsonData = JSONObject.parseObject(JSONObject.toJSONString(map));
        String result = restTemplateUtil.postJsonData(url, jsonData);
        JSONObject data = JSONObject.parseObject(result);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                wxExceptionDispose(data, "添加公众号模板消息异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return data.getString("template_id");
    }

    /**
     * 获取当前帐号下的个人模板列表
     * @return List
     */
    @Override
    public List<RoutineMyTemplateVo> getRoutineMyTemplateList() {
        String accessToken = getMiniAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_MINI_GET_ALL_PRIVATE_TEMPLATE_URL, accessToken);
        JSONObject jsonObject = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(jsonObject)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (jsonObject.containsKey("errcode") && !jsonObject.getString("errcode").equals("0")) {
            if (jsonObject.containsKey("errmsg")) {
                // 保存到微信异常表
                wxExceptionDispose(jsonObject, StrUtil.format("获取小程序当前帐号下的个人模板列表异常"));
                throw new CrmebException("微信接口调用失败：" + jsonObject.getString("errcode") + jsonObject.getString("errmsg"));
            }
        }
        JSONArray templateList = jsonObject.getJSONArray("data");
        List<RoutineMyTemplateVo> voList = templateList.toJavaList(RoutineMyTemplateVo.class);
        return voList;
    }

    /**
     * 删除微信小程序订阅消息
     * @return Boolean
     */
    @Override
    public Boolean delRoutineMyTemplate(String priTmplId) {
        String accessToken = getMiniAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_MINI_DEL_PRIVATE_TEMPLATE_URL, accessToken);
        HashMap<String, String> map = new HashMap<>();
        map.put("priTmplId", priTmplId);
        JSONObject jsonData = JSONObject.parseObject(JSONObject.toJSONString(map));
        String result = restTemplateUtil.postJsonData(url, jsonData);
        JSONObject data = JSONObject.parseObject(result);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                wxExceptionDispose(data, "删除微信小程序订阅消息异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 获取小程序平台上的标准模板
     * @param tempKey 模板编号
     * @return List
     */
    @Override
    public List<RoutineTemplateKeyVo> getRoutineTemplateByWechat(String tempKey) {
        String accessToken = getMiniAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_MINI_GET_TEMPLATE_URL, accessToken, tempKey);
        JSONObject jsonObject = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(jsonObject)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (jsonObject.containsKey("errcode") && !jsonObject.getString("errcode").equals("0")) {
            if (jsonObject.containsKey("errmsg")) {
                // 保存到微信异常表
                wxExceptionDispose(jsonObject, "获取小程序平台上的标准模板异常");
                throw new CrmebException("微信接口调用失败：" + jsonObject.getString("errcode") + jsonObject.getString("errmsg"));
            }
        }
        JSONArray templateList = jsonObject.getJSONArray("data");
        List<RoutineTemplateKeyVo> voList = templateList.toJavaList(RoutineTemplateKeyVo.class);
        return voList;
    }

    /**
     * 添加小程序订阅消息
     * @param tempKey 模板编号
     * @param kidList 小程序订阅消息模板kid数组
     * @return 小程序订阅消息模板编号（自己的）
     */
    @Override
    public String apiAddRoutineTemplate(String tempKey, List<Integer> kidList) {
        String accessToken = getMiniAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_MINI_API_ADD_TEMPLATE_URL, accessToken);
        HashMap<String, Object> map = new HashMap<>();
        map.put("tid", tempKey);
        map.put("kidList", kidList);
        map.put("sceneDesc", "接口添加");
        JSONObject jsonData = JSONObject.parseObject(JSONObject.toJSONString(map));
        String result = restTemplateUtil.postJsonData(url, jsonData);
        JSONObject data = JSONObject.parseObject(result);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                wxExceptionDispose(data, "添加小程序订阅消息异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return data.getString("priTmplId");
    }


    /**
     * 获取JS-SDK的签名
     * @param nonceStr 随机字符串
     * @param ticket ticket
     * @param timestamp 时间戳
     * @param url url
     * @return 签名
     */
    private String getJsSDKSignature(String nonceStr, String ticket, Long timestamp, String url) {
        //注意这里参数名必须全部小写，且必须有序
        String paramString = StrUtil.format("jsapi_ticket={}&noncestr={}&timestamp={}&url={}", ticket, nonceStr, timestamp, url);
        return SecureUtil.sha1(paramString);
    }

    /**
     * 获取JS-SDK的ticket
     * 用于计算签名
     * @return ticket
     */
    private String getJsApiTicket() {
        boolean exists = redisUtil.exists(WeChatConstants.REDIS_PUBLIC_JS_API_TICKET);
        if (exists) {
            Object ticket = redisUtil.get(WeChatConstants.REDIS_PUBLIC_JS_API_TICKET);
            return ticket.toString();
        }
        String accessToken = getPublicAccessToken();
        String url = StrUtil.format(WeChatConstants.WECHAT_PUBLIC_JS_TICKET_URL, accessToken);
        JSONObject data = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                // 保存到微信异常表
                wxExceptionDispose(data, "微信获取JS-SDK的ticket异常");
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        String ticket = data.getString("ticket");
        redisUtil.set(WeChatConstants.REDIS_PUBLIC_JS_API_TICKET, ticket, WeChatConstants.REDIS_PUBLIC_JS_API_TICKET_EXPRESS, TimeUnit.SECONDS);
        return ticket;
    }

    /**
     * 获取微信accessToken
     * @param appId appId
     * @param secret secret
     * @param type mini-小程序，public-公众号，app-app
     * @return WeChatAccessTokenVo
     */
    private WeChatAccessTokenVo getAccessToken(String appId, String secret, String type) {
        String url = StrUtil.format(WeChatConstants.WECHAT_ACCESS_TOKEN_URL, appId, secret);
        JSONObject data = restTemplateUtil.getData(url);
        if (ObjectUtil.isNull(data)) {
            throw new CrmebException("微信平台接口异常，没任何数据返回！");
        }
        if (data.containsKey("errcode") && !data.getString("errcode").equals("0")) {
            if (data.containsKey("errmsg")) {
                // 保存到微信异常表
                wxExceptionDispose(data, StrUtil.format("微信获取accessToken异常，{}端", type));
                throw new CrmebException("微信接口调用失败：" + data.getString("errcode") + data.getString("errmsg"));
            }
        }
        return JSONObject.parseObject(data.toJSONString(), WeChatAccessTokenVo.class);
    }

    /**
     * 微信异常处理
     * @param jsonObject 微信返回数据
     * @param remark 备注
     */
    private void wxExceptionDispose(JSONObject jsonObject, String remark) {
        WechatExceptions wechatExceptions = new WechatExceptions();
        wechatExceptions.setErrcode(jsonObject.getString("errcode"));
        wechatExceptions.setErrmsg(StrUtil.isNotBlank(jsonObject.getString("errmsg")) ? jsonObject.getString("errmsg") : "");
        wechatExceptions.setData(jsonObject.toJSONString());
        wechatExceptions.setRemark(remark);
        wechatExceptions.setCreateTime(DateUtil.date());
        wechatExceptions.setUpdateTime(DateUtil.date());
        wechatExceptionsService.save(wechatExceptions);
    }

    /**
     * 微信支付异常处理
     * @param map 微信返回数据
     * @param remark 备注
     */
    private void wxPayExceptionDispose(HashMap<String, Object> map, String remark) {
        WechatExceptions wechatExceptions = new WechatExceptions();
        String returnCode = (String) map.get("return_code");
        if (returnCode.toUpperCase().equals("FAIL")) {
            wechatExceptions.setErrcode("-100");
            wechatExceptions.setErrmsg(map.get("return_msg").toString());
        } else {
            wechatExceptions.setErrcode(map.get("err_code").toString());
            wechatExceptions.setErrmsg(map.get("err_code_des").toString());
        }
        wechatExceptions.setData(JSONObject.toJSONString(map));
        wechatExceptions.setRemark(remark);
        wechatExceptions.setCreateTime(DateUtil.date());
        wechatExceptions.setUpdateTime(DateUtil.date());
        wechatExceptionsService.save(wechatExceptions);
    }

    /**
     * 微信支付查询异常处理
     * @param record 微信返回数据
     * @param remark 备注
     */
    private void wxPayQueryExceptionDispose(MyRecord record, String remark) {
        WechatExceptions wechatExceptions = new WechatExceptions();
        if (record.getStr("return_code").toUpperCase().equals("FAIL")) {
            wechatExceptions.setErrcode("-200");
            wechatExceptions.setErrmsg(record.getStr("return_msg"));
        } else if (record.getStr("result_code").toUpperCase().equals("FAIL")) {
            wechatExceptions.setErrcode(record.getStr("err_code"));
            wechatExceptions.setErrmsg(record.getStr("err_code_des"));
        } else if (!record.getStr("trade_state").toUpperCase().equals("SUCCESS")) {
            wechatExceptions.setErrcode("-201");
            wechatExceptions.setErrmsg(record.getStr("trade_state"));
        }
        wechatExceptions.setData(JSONObject.toJSONString(record.getColumns()));
        wechatExceptions.setRemark(remark);
        wechatExceptions.setCreateTime(DateUtil.date());
        wechatExceptions.setUpdateTime(DateUtil.date());
        wechatExceptionsService.save(wechatExceptions);
    }

}
