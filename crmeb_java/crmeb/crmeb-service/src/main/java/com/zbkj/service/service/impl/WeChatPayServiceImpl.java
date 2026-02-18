package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.PayConstants;
import com.zbkj.common.constants.TaskConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.combination.StoreCombination;
import com.zbkj.common.model.combination.StorePink;
import com.zbkj.common.model.finance.UserRecharge;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.User;
import com.zbkj.common.model.user.UserToken;
import com.zbkj.common.model.wechat.WechatPayInfo;
import com.zbkj.common.utils.*;
import com.zbkj.common.vo.*;
import com.zbkj.service.dao.BookingOrderDao;
import com.zbkj.service.model.BookingOrder;
import com.zbkj.service.service.impl.payment.WeChatPayConfigSupport;
import com.zbkj.service.service.impl.payment.WeChatPayV3Crypto;
import com.zbkj.service.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


/**
 * 微信支付
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
public class WeChatPayServiceImpl implements WeChatPayService {
    private static final Logger logger = LoggerFactory.getLogger(WeChatPayServiceImpl.class);

    @Autowired
    private RestTemplateUtil restTemplateUtil;

    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private StoreOrderInfoService storeOrderInfoService;

    @Autowired
    private BookingOrderDao bookingOrderDao;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRechargeService userRechargeService;

    @Autowired
    private RechargePayService rechargePayService;

    @Autowired
    private StoreCombinationService storeCombinationService;

    @Autowired
    private StorePinkService storePinkService;

//    @Autowired
//    private PayComponentOrderService componentOrderService;

    @Autowired
    private WechatNewService wechatNewService;

    @Autowired
    private WechatPayInfoService wechatPayInfoService;

    @Autowired
    private ServiceBookingService serviceBookingService;

    @Autowired
    private WeChatPayConfigSupport weChatPayConfigSupport;

    /**
     * 查询支付结果
     * @param orderNo 订单编号
     * @return
     */
    @Override
    public Boolean queryPayResult(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new CrmebException("订单编号不能为空");
        }

        BookingOrder bookingOrder = getBookingOrderByOrderNo(orderNo);
        if (ObjectUtil.isNotNull(bookingOrder)) {
            return bookingQueryPayResult(bookingOrder);
        }

        // 切割字符串，判断是支付订单还是充值订单
        String pre = StrUtil.subPre(orderNo, 5);
        if (pre.equals("order")) {// 支付订单
            StoreOrder storeOrder = storeOrderService.getByOderId(orderNo);
            if (ObjectUtil.isNull(storeOrder)) {
                throw new CrmebException("订单不存在");
            }
            if (storeOrder.getIsDel()) {
                throw new CrmebException("订单已被删除");
            }
            if (!storeOrder.getPayType().equals(PayConstants.PAY_TYPE_WE_CHAT)) {
                throw new CrmebException("不是微信支付类型订单，请重新选择支付方式");
            }

            if (storeOrder.getPaid()) {
                return Boolean.TRUE;
            }
            if (StrUtil.isBlank(storeOrder.getOutTradeNo())) {
                throw new CrmebException("未找到对应商户单号");
            }

            WechatPayInfo wechatPayInfo = wechatPayInfoService.getByNo(storeOrder.getOutTradeNo());
            if (ObjectUtil.isNull(wechatPayInfo)) {
                throw new CrmebException("未找到对应微信订单");
            }

            User user = userService.getById(storeOrder.getUid());
            if (ObjectUtil.isNull(user)) throw new CrmebException("用户不存在");


            WeChatPayChannelConfig payConfig = weChatPayConfigSupport.resolveByChannel(storeOrder.getIsChannel(), storeOrder.getStoreId());
            // 查询订单信息
            MyRecord record = wechatNewService.payOrderQuery(wechatPayInfo.getOutTradeNo(), payConfig);
            verifyStoreOrderQueryConsistency(storeOrder, wechatPayInfo, record, payConfig);

            wechatPayInfo.setIsSubscribe(record.getStr("is_subscribe"));
            wechatPayInfo.setTradeState(record.getStr("trade_state"));
            wechatPayInfo.setBankType(record.getStr("bank_type"));
            wechatPayInfo.setCashFee(record.getInt("cash_fee"));
            wechatPayInfo.setCouponFee(record.getInt("coupon_fee"));
            wechatPayInfo.setTransactionId(record.getStr("transaction_id"));
            wechatPayInfo.setTimeEnd(record.getStr("time_end"));
            wechatPayInfo.setTradeStateDesc(record.getStr("trade_state_desc"));

            Boolean updatePaid = transactionTemplate.execute(e -> {
                storeOrderService.updatePaid(orderNo);
                wechatPayInfoService.updateById(wechatPayInfo);
                if (storeOrder.getUseIntegral() > 0) {
                    userService.updateIntegral(user, storeOrder.getUseIntegral(), "sub");
                }
//                if (storeOrder.getType().equals(1)) {
//                    PayComponentOrder componentOrder = componentOrderService.getByOrderNo(orderNo);
//                    componentOrder.setTransactionId(record.getStr("transaction_id"));
//                    componentOrder.setTimeEnd(record.getStr("time_end"));
//                    componentOrderService.updateById(componentOrder);
//                }
                // 处理拼团
                if (storeOrder.getCombinationId() > 0) {
                    // 判断拼团团长是否存在
                    StorePink headPink = new StorePink();
                    Integer pinkId = storeOrder.getPinkId();
                    if (pinkId > 0) {
                        headPink = storePinkService.getById(pinkId);
                        if (ObjectUtil.isNull(headPink) || headPink.getIsRefund().equals(true) || headPink.getStatus() == 3) {
                            pinkId = 0;
                        }
                    }
                    StoreCombination storeCombination = storeCombinationService.getById(storeOrder.getCombinationId());
                    // 如果拼团人数已满，重新开团
                    if (pinkId > 0) {
                        Integer count = storePinkService.getCountByKid(pinkId);
                        if (count >= storeCombination.getPeople()) {
                            pinkId = 0;
                        }
                    }
                    // 生成拼团表数据
                    StorePink storePink = new StorePink();
                    storePink.setUid(user.getUid());
                    storePink.setAvatar(user.getAvatar());
                    storePink.setNickname(user.getNickname());
                    storePink.setOrderId(storeOrder.getOrderId());
                    storePink.setOrderIdKey(storeOrder.getId());
                    storePink.setTotalNum(storeOrder.getTotalNum());
                    storePink.setTotalPrice(storeOrder.getTotalPrice());
                    storePink.setCid(storeCombination.getId());
                    storePink.setPid(storeCombination.getProductId());
                    storePink.setPeople(storeCombination.getPeople());
                    storePink.setPrice(storeCombination.getPrice());
                    Integer effectiveTime = storeCombination.getEffectiveTime();// 有效小时数
                    DateTime dateTime = cn.hutool.core.date.DateUtil.date();
                    storePink.setAddTime(dateTime.getTime());
                    if (pinkId > 0) {
                        storePink.setStopTime(headPink.getStopTime());
                    } else {
                        DateTime hourTime = cn.hutool.core.date.DateUtil.offsetHour(dateTime, effectiveTime);
                        long stopTime =  hourTime.getTime();
                        if (stopTime > storeCombination.getStopTime()) {
                            stopTime = storeCombination.getStopTime();
                        }
                        storePink.setStopTime(stopTime);
                    }
                    storePink.setKId(pinkId);
                    storePink.setIsTpl(false);
                    storePink.setIsRefund(false);
                    storePink.setStatus(1);
                    storePinkService.save(storePink);
                    // 如果是开团，需要更新订单数据
                    storeOrder.setPinkId(storePink.getId());
                    storeOrder.setUpdateTime(DateUtil.date());
                    storeOrderService.updateById(storeOrder);
                }
                return Boolean.TRUE;
            });
            if (!updatePaid) {
                throw new CrmebException("支付成功更新订单失败");
            }
            // 添加支付成功task
            redisUtil.lPush(TaskConstants.ORDER_TASK_PAY_SUCCESS_AFTER, orderNo);
            return Boolean.TRUE;
        }
        // 充值订单
        UserRecharge userRecharge = new UserRecharge();
        userRecharge.setOrderId(orderNo);
        userRecharge = userRechargeService.getInfoByEntity(userRecharge);
        if (ObjectUtil.isNull(userRecharge)) {
            throw new CrmebException("没有找到订单信息");
        }
        if (userRecharge.getPaid()) {
            return Boolean.TRUE;
        }
        // 查询订单
        Integer rechargeChannel = weChatPayConfigSupport.parseRechargeChannel(userRecharge.getRechargeType());
        WeChatPayChannelConfig payConfig = weChatPayConfigSupport.resolveByChannel(rechargeChannel, 0);
        // 查询订单信息
        MyRecord record = wechatNewService.payOrderQuery(userRecharge.getOutTradeNo(), payConfig);
        // 支付成功处理
        Boolean rechargePayAfter = rechargePayService.paySuccess(userRecharge);
        if (!rechargePayAfter) {
            throw new CrmebException("wechat pay error : 数据保存失败==》" + orderNo);
        }
        return rechargePayAfter;
    }

    private BookingOrder getBookingOrderByOrderNo(String orderNo) {
        LambdaQueryWrapper<BookingOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(BookingOrder::getOrderNo, orderNo);
        return bookingOrderDao.selectOne(lqw);
    }

    private Boolean bookingQueryPayResult(BookingOrder bookingOrder) {
        if (bookingOrder.getStatus() == 2) {
            return Boolean.TRUE;
        }
        if (bookingOrder.getStatus() != 1) {
            throw new CrmebException("当前预约订单状态不可查询支付");
        }

        WechatPayInfo wechatPayInfo = wechatPayInfoService.getByNo(bookingOrder.getOrderNo());
        if (ObjectUtil.isNull(wechatPayInfo)) {
            throw new CrmebException("未找到对应微信订单");
        }

        // 预约支付当前仅支持公众号/小程序/H5，按微信订单中的appid反查渠道配置
        WeChatPayChannelConfig payConfig = weChatPayConfigSupport.resolveByAppId(wechatPayInfo.getAppId(), bookingOrder.getStoreId());

        MyRecord record = wechatNewService.payOrderQuery(wechatPayInfo.getOutTradeNo(), payConfig);
        verifyBookingOrderQueryConsistency(bookingOrder, wechatPayInfo, record, payConfig);

        wechatPayInfo.setIsSubscribe(record.getStr("is_subscribe"));
        wechatPayInfo.setTradeState(record.getStr("trade_state"));
        wechatPayInfo.setBankType(record.getStr("bank_type"));
        wechatPayInfo.setCashFee(record.getInt("cash_fee"));
        wechatPayInfo.setCouponFee(record.getInt("coupon_fee"));
        wechatPayInfo.setTransactionId(record.getStr("transaction_id"));
        wechatPayInfo.setTimeEnd(record.getStr("time_end"));
        wechatPayInfo.setTradeStateDesc(record.getStr("trade_state_desc"));

        Boolean execute = transactionTemplate.execute(e -> {
            wechatPayInfoService.updateById(wechatPayInfo);
            serviceBookingService.paySuccess(bookingOrder.getOrderNo());
            return Boolean.TRUE;
        });
        if (!execute) {
            throw new CrmebException("预约支付结果更新失败");
        }
        return Boolean.TRUE;
    }

    private void verifyStoreOrderQueryConsistency(StoreOrder storeOrder, WechatPayInfo payInfo, MyRecord record, WeChatPayChannelConfig payConfig) {
        String queryOutTradeNo = StrUtil.trimToEmpty(record.getStr("out_trade_no"));
        if (StrUtil.isBlank(queryOutTradeNo) || !queryOutTradeNo.equals(payInfo.getOutTradeNo())) {
            throw new CrmebException("支付结果校验失败：out_trade_no不一致");
        }
        int expectedTotalFee = storeOrder.getPayPrice().multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue();
        int queryTotalFee = parsePositiveInt(record.get("total_fee"), "total_fee");
        if (expectedTotalFee != queryTotalFee) {
            throw new CrmebException("支付结果校验失败：total_fee不一致");
        }
        String queryMchId = StrUtil.trimToEmpty(record.getStr("mch_id"));
        if (StrUtil.isNotBlank(payConfig.getMchId()) && !payConfig.getMchId().equals(queryMchId)) {
            throw new CrmebException("支付结果校验失败：mch_id不一致");
        }
        if (Boolean.TRUE.equals(payConfig.getServiceProviderMode()) && StrUtil.isNotBlank(payConfig.getSubMchId())) {
            String querySubMchId = StrUtil.trimToEmpty(record.getStr("sub_mch_id"));
            if (!payConfig.getSubMchId().equals(querySubMchId)) {
                throw new CrmebException("支付结果校验失败：sub_mch_id不一致");
            }
        }
        String existingTransactionId = StrUtil.trimToEmpty(payInfo.getTransactionId());
        String queryTransactionId = StrUtil.trimToEmpty(record.getStr("transaction_id"));
        if (StrUtil.isNotBlank(existingTransactionId) && StrUtil.isNotBlank(queryTransactionId)
                && !existingTransactionId.equals(queryTransactionId)) {
            throw new CrmebException("支付结果校验失败：transaction_id不一致");
        }
    }

    private void verifyBookingOrderQueryConsistency(BookingOrder bookingOrder, WechatPayInfo payInfo, MyRecord record, WeChatPayChannelConfig payConfig) {
        String queryOutTradeNo = StrUtil.trimToEmpty(record.getStr("out_trade_no"));
        if (StrUtil.isBlank(queryOutTradeNo) || !queryOutTradeNo.equals(payInfo.getOutTradeNo())) {
            throw new CrmebException("预约支付结果校验失败：out_trade_no不一致");
        }
        int expectedTotalFee = bookingOrder.getActualPrice().multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue();
        int queryTotalFee = parsePositiveInt(record.get("total_fee"), "total_fee");
        if (expectedTotalFee != queryTotalFee) {
            throw new CrmebException("预约支付结果校验失败：total_fee不一致");
        }
        String queryMchId = StrUtil.trimToEmpty(record.getStr("mch_id"));
        if (StrUtil.isNotBlank(payConfig.getMchId()) && !payConfig.getMchId().equals(queryMchId)) {
            throw new CrmebException("预约支付结果校验失败：mch_id不一致");
        }
        if (Boolean.TRUE.equals(payConfig.getServiceProviderMode()) && StrUtil.isNotBlank(payConfig.getSubMchId())) {
            String querySubMchId = StrUtil.trimToEmpty(record.getStr("sub_mch_id"));
            if (!payConfig.getSubMchId().equals(querySubMchId)) {
                throw new CrmebException("预约支付结果校验失败：sub_mch_id不一致");
            }
        }
        String existingTransactionId = StrUtil.trimToEmpty(payInfo.getTransactionId());
        String queryTransactionId = StrUtil.trimToEmpty(record.getStr("transaction_id"));
        if (StrUtil.isNotBlank(existingTransactionId) && StrUtil.isNotBlank(queryTransactionId)
                && !existingTransactionId.equals(queryTransactionId)) {
            throw new CrmebException("预约支付结果校验失败：transaction_id不一致");
        }
    }

    private int parsePositiveInt(Object rawValue, String fieldName) {
        String value = ObjectUtil.isNull(rawValue) ? "" : String.valueOf(rawValue);
        try {
            return Integer.parseInt(StrUtil.trim(value));
        } catch (Exception e) {
            throw new CrmebException("支付结果校验失败：" + fieldName + "非法");
        }
    }

    /**
     * 微信充值预下单接口
     * @param userRecharge 充值订单
     * @param clientIp      ip
     * @return
     */
    @Override
    public Map<String, String> unifiedRecharge(UserRecharge userRecharge, String clientIp) {
        if (ObjectUtil.isNull(userRecharge)) {
            throw new CrmebException("订单不存在");
        }
        // 获取用户openId
        // 根据订单支付类型来判断获取公众号openId还是小程序openId
        UserToken userToken = new UserToken();
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_PUBLIC)) {// 公众号
            userToken = userTokenService.getTokenByUserId(userRecharge.getUid(), 1);
        }
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_PROGRAM)) {// 小程序
            userToken = userTokenService.getTokenByUserId(userRecharge.getUid(), 2);
        }
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_H5)) {// H5
            userToken.setToken("");
        }

        if (ObjectUtil.isNull(userToken)) {
            throw new CrmebException("该用户没有openId");
        }

        Integer rechargeChannel = weChatPayConfigSupport.parseRechargeChannel(userRecharge.getRechargeType());
        WeChatPayChannelConfig payConfig = weChatPayConfigSupport.resolveByChannel(rechargeChannel, 0);

        // 获取微信预下单对象
        CreateOrderRequestVo unifiedorderVo = getUnifiedorderVo(userRecharge, userToken.getToken(), clientIp, payConfig);
        logger.info(
                "wx.unifiedorder.request biz=recharge orderNo={} outTradeNo={} channel={} storeId={} serviceProviderMode={} mchId={} subMchId={}",
                userRecharge.getOrderId(),
                unifiedorderVo.getOut_trade_no(),
                rechargeChannel,
                0,
                Boolean.TRUE.equals(payConfig.getServiceProviderMode()),
                payConfig.getMchId(),
                StrUtil.blankToDefault(payConfig.getSubMchId(), "-")
        );
        // 预下单
        CreateOrderResponseVo responseVo = wechatNewService.payUnifiedorder(unifiedorderVo, payConfig);
        logger.info(
                "wx.unifiedorder.response biz=recharge orderNo={} outTradeNo={} channel={} storeId={} serviceProviderMode={} mchId={} subMchId={} prepayId={}",
                userRecharge.getOrderId(),
                unifiedorderVo.getOut_trade_no(),
                rechargeChannel,
                0,
                Boolean.TRUE.equals(payConfig.getServiceProviderMode()),
                payConfig.getMchId(),
                StrUtil.blankToDefault(payConfig.getSubMchId(), "-"),
                responseVo.getPrepayId()
        );

        // 组装前端预下单参数
        Map<String, String> map = new HashMap<>();
        String clientAppId = StrUtil.isNotBlank(payConfig.getClientAppId()) ? payConfig.getClientAppId() : unifiedorderVo.getAppid();
        String packageValue = "prepay_id=".concat(responseVo.getPrepayId());
        map.put("appId", clientAppId);
        map.put("package", packageValue);
        Long currentTimestamp = WxPayUtil.getCurrentTimestamp();
        String timeStamp = Long.toString(currentTimestamp);
        map.put("timeStamp", timeStamp);
        String paySign;
        if (isV3PayConfig(payConfig)) {
            String nonceStr = WxPayUtil.getNonceStr();
            map.put("nonceStr", nonceStr);
            map.put("signType", "RSA");
            paySign = WeChatPayV3Crypto.signMiniProgramPay(
                    clientAppId,
                    timeStamp,
                    nonceStr,
                    packageValue,
                    payConfig.getPrivateKeyPath()
            );
        } else {
            map.put("nonceStr", unifiedorderVo.getNonce_str());
            map.put("signType", unifiedorderVo.getSign_type());
            paySign = WxPayUtil.getSign(map, payConfig.getSignKey());
        }
        map.put("outTradeNo", unifiedorderVo.getOut_trade_no());
        map.put("paySign", paySign);
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_H5)) {
            map.put("mweb_url", responseVo.getMWebUrl());
        }

        return map;
    }

    /**
     * 生成微信查询订单对象
     * @return
     */
    private boolean isV3PayConfig(WeChatPayChannelConfig payConfig) {
        return ObjectUtil.isNotNull(payConfig) && "v3".equalsIgnoreCase(StrUtil.blankToDefault(payConfig.getApiVersion(), "v2"));
    }

    private Map<String, String> getWxChantQueryPayVo(String orderNo, WeChatPayChannelConfig payConfig) {
        Map<String, String> map = CollUtil.newHashMap();
        map.put("appid", payConfig.getAppId());
        map.put("mch_id", payConfig.getMchId());
        if (Boolean.TRUE.equals(payConfig.getServiceProviderMode())) {
            map.put("sub_mch_id", payConfig.getSubMchId());
            if (StrUtil.isNotBlank(payConfig.getSubAppId()) && !payConfig.getSubAppId().equals(payConfig.getAppId())) {
                map.put("sub_appid", payConfig.getSubAppId());
            }
        }
        map.put("out_trade_no", orderNo);
        map.put("nonce_str", WxPayUtil.getNonceStr());
        map.put("sign_type", PayConstants.WX_PAY_SIGN_TYPE_MD5);
        map.put("sign", WxPayUtil.getSign(map, payConfig.getSignKey()));
        return map;
    }

    /**
     * 获取微信预下单对象
     * @return
     */
    private CreateOrderRequestVo getUnifiedorderVo(UserRecharge userRecharge, String openid, String ip, WeChatPayChannelConfig payConfig) {

        // 获取域名
        String domain = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_SITE_URL);
        String apiDomain = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_API_URL);

        AttachVo attachVo = new AttachVo(Constants.SERVICE_PAY_TYPE_RECHARGE, userRecharge.getUid());
        CreateOrderRequestVo vo = new CreateOrderRequestVo();

        vo.setAppid(payConfig.getAppId());
        vo.setMch_id(payConfig.getMchId());
        vo.setNonce_str(WxPayUtil.getNonceStr());
        vo.setSign_type(PayConstants.WX_PAY_SIGN_TYPE_MD5);
        vo.setBody(PayConstants.PAY_BODY);
        vo.setAttach(JSONObject.toJSONString(attachVo));
        vo.setOut_trade_no(userRecharge.getOrderId());
        // 订单中使用的是BigDecimal,这里要转为Integer类型
        vo.setTotal_fee(userRecharge.getPrice().multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue());
        vo.setSpbill_create_ip(ip);
        vo.setNotify_url(apiDomain + PayConstants.WX_PAY_NOTIFY_API_URI);
        vo.setTrade_type(PayConstants.WX_PAY_TRADE_TYPE_JS);
        String payOpenId = openid;
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_H5)) {// H5
            vo.setTrade_type(PayConstants.WX_PAY_TRADE_TYPE_H5);
            payOpenId = null;
        }
        if (userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_APP_IOS) || userRecharge.getRechargeType().equals(PayConstants.PAY_CHANNEL_WE_CHAT_APP_ANDROID)) {
            vo.setTrade_type("APP");
            payOpenId = null;
        }
        if (Boolean.TRUE.equals(payConfig.getServiceProviderMode())) {
            vo.setSub_mch_id(payConfig.getSubMchId());
            if (StrUtil.isNotBlank(payConfig.getSubAppId())) {
                vo.setSub_appid(payConfig.getSubAppId());
            }
            if (StrUtil.isNotBlank(payOpenId) && StrUtil.isNotBlank(payConfig.getSubAppId())
                    && !payConfig.getAppId().equals(payConfig.getSubAppId())) {
                vo.setSub_openid(payOpenId);
                vo.setOpenid(null);
            } else {
                vo.setOpenid(payOpenId);
            }
        } else {
            vo.setOpenid(payOpenId);
        }
        CreateOrderH5SceneInfoVo createOrderH5SceneInfoVo = new CreateOrderH5SceneInfoVo(
                new CreateOrderH5SceneInfoDetailVo(
                        domain,
                        systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_SITE_NAME)
                )
        );
        vo.setScene_info(JSONObject.toJSONString(createOrderH5SceneInfoVo));
        String sign = WxPayUtil.getSign(vo, payConfig.getSignKey());
        vo.setSign(sign);
        return vo;
    }

    /**
     * 作用：统一下单<br>
     * 场景：公共号支付、扫码支付、APP支付
     *
     * @param vo 向wxpay post的请求数据
     * @return API返回数据
     */
    private CreateOrderResponseVo unifiedOrder(CreateOrderRequestVo vo) {
        try {
            String url = PayConstants.WX_PAY_API_URL + PayConstants.WX_PAY_API_URI;
            String request = XmlUtil.objectToXml(vo);
            String xml = restTemplateUtil.postXml(url, request);
            HashMap<String, Object> map = XmlUtil.xmlToMap(xml);
            if (null == map) {
                throw new CrmebException("微信下单失败！");
            }
            CreateOrderResponseVo responseVo = CrmebUtil.mapToObj(map, CreateOrderResponseVo.class);
            if (responseVo.getReturnCode().toUpperCase().equals("FAIL")) {
                throw new CrmebException("微信下单失败1！" +  responseVo.getReturnMsg());
            }

            if (responseVo.getResultCode().toUpperCase().equals("FAIL")) {
                throw new CrmebException("微信下单失败2！" + responseVo.getErrCodeDes());
            }

            responseVo.setExtra(vo.getScene_info());
            return responseVo;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CrmebException(e.getMessage());
        }
    }

}
