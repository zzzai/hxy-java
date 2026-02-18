package com.zbkj.service.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.PayConstants;
import com.zbkj.common.constants.TaskConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.combination.StoreCombination;
import com.zbkj.common.model.combination.StorePink;
import com.zbkj.common.model.finance.UserRecharge;
import com.zbkj.common.model.order.StoreOrder;
import com.zbkj.common.model.user.User;
import com.zbkj.common.model.wechat.WechatExceptions;
import com.zbkj.common.model.wechat.WechatPayInfo;

import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.common.utils.WxPayUtil;
import com.zbkj.common.vo.AttachVo;
import com.zbkj.common.vo.CallbackVo;
import com.zbkj.common.vo.MyRecord;
import com.zbkj.common.vo.WeChatPayChannelConfig;
import com.zbkj.service.dao.BookingOrderDao;
import com.zbkj.service.model.BookingOrder;
import com.zbkj.service.service.impl.payment.OrderRefundStateMachine;
import com.zbkj.service.service.impl.payment.WeChatPayConfigSupport;
import com.zbkj.service.service.impl.payment.WeChatPayV3Crypto;
import com.zbkj.service.service.impl.payment.WeChatPaySignKeyResolver;
import com.zbkj.service.service.impl.payment.WeChatRefundSignKeyResolver;
import com.zbkj.service.service.*;
import com.zbkj.service.util.DistributedLockUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.*;


/**
 * 订单支付回调 CallbackService 实现类
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
public class CallbackServiceImpl implements CallbackService {

    private static final Logger logger = LoggerFactory.getLogger(CallbackServiceImpl.class);
    private static final String REFUND_CALLBACK_DEDUP_CHANGE_TYPE = "refund_callback_dedup";

    @Autowired
    private RechargePayService rechargePayService;

    @Autowired
    private StoreOrderService storeOrderService;

    @Autowired
    private ServiceBookingService serviceBookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRechargeService userRechargeService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private StoreCombinationService storeCombinationService;

    @Autowired
    private StorePinkService storePinkService;

    @Autowired
    private WechatPayInfoService wechatPayInfoService;

    @Autowired
    private StoreOrderStatusService storeOrderStatusService;

    @Autowired
    private WeChatRefundSignKeyResolver weChatRefundSignKeyResolver;

    @Autowired
    private WeChatPaySignKeyResolver weChatPaySignKeyResolver;

    @Autowired
    private WeChatPayConfigSupport weChatPayConfigSupport;

    @Autowired
    private WechatNewService wechatNewService;

    @Autowired
    private WechatExceptionsService wechatExceptionsService;

    @Autowired
    private DistributedLockUtil distributedLockUtil;

    @Autowired
    private BookingOrderDao bookingOrderDao;

    /**
     * 微信支付回调
     */
    @Override
    public String weChat(String xmlInfo) {
        return weChat(xmlInfo, Collections.emptyMap());
    }

    @Override
    public String weChat(String body, Map<String, String> headers) {
        if (isV3PayCallback(body, headers)) {
            return weChatV3(body, headers);
        }
        return weChatV2(body);
    }

    private boolean isV3PayCallback(String body, Map<String, String> headers) {
        if (StrUtil.isBlank(body)) {
            return false;
        }
        String trimBody = StrUtil.trim(body);
        if (!trimBody.startsWith("{")) {
            return false;
        }
        String signature = headerIgnoreCase(headers, "Wechatpay-Signature");
        return StrUtil.isNotBlank(signature);
    }

    private String weChatV2(String xmlInfo) {
        StringBuffer sb = new StringBuffer();
        sb.append("<xml>");
        if(StrUtil.isBlank(xmlInfo)){
            sb.append("<return_code><![CDATA[FAIL]]></return_code>");
            sb.append("<return_msg><![CDATA[xmlInfo is blank]]></return_msg>");
            sb.append("</xml>");
            logger.error("wechat callback error : " + sb.toString());
            return sb.toString();
        }

        try{
            Map<String, String> callbackRawMap = WxPayUtil.xmlToMap(xmlInfo);
            String signKey = weChatPaySignKeyResolver.resolve(callbackRawMap.get("appid"), callbackRawMap.get("mch_id"));
            HashMap<String, Object> map = WxPayUtil.processResponseXml(xmlInfo, signKey);
            // 通信是否成功
            String returnCode = (String) map.get("return_code");
            if (!returnCode.equals(Constants.SUCCESS)) {
                sb.append("<return_code><![CDATA[SUCCESS]]></return_code>");
                sb.append("<return_msg><![CDATA[OK]]></return_msg>");
                sb.append("</xml>");
                logger.error("wechat callback error : wx pay return code is fail returnMsg : " + map.get("return_msg"));
                return sb.toString();
            }
            // 交易是否成功
            String resultCode = (String) map.get("result_code");
            if (!resultCode.equals(Constants.SUCCESS)) {
                sb.append("<return_code><![CDATA[SUCCESS]]></return_code>");
                sb.append("<return_msg><![CDATA[OK]]></return_msg>");
                sb.append("</xml>");
                logger.error("wechat callback error : wx pay result code is fail");
                return sb.toString();
            }

            //解析xml
            CallbackVo callbackVo = CrmebUtil.mapToObj(map, CallbackVo.class);
            processPaySuccess(callbackVo);
            sb.append("<return_code><![CDATA[SUCCESS]]></return_code>");
            sb.append("<return_msg><![CDATA[OK]]></return_msg>");
        }catch (Exception e){
            sb.append("<return_code><![CDATA[FAIL]]></return_code>");
            sb.append("<return_msg><![CDATA[").append(e.getMessage()).append("]]></return_msg>");
            logger.error("wechat pay error : 业务异常==》" + e.getMessage());
        }
        sb.append("</xml>");
        logger.error("wechat callback response : " + sb.toString());
        return sb.toString();
    }

    private String weChatV3(String body, Map<String, String> headers) {
        try {
            String timestamp = headerIgnoreCase(headers, "Wechatpay-Timestamp");
            String nonce = headerIgnoreCase(headers, "Wechatpay-Nonce");
            String signature = headerIgnoreCase(headers, "Wechatpay-Signature");
            if (StrUtil.hasBlank(timestamp, nonce, signature)) {
                throw new CrmebException("微信v3回调头缺失");
            }

            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            if (!verifyV3CallbackSignature(message, signature)) {
                saveCallbackAlert("-V3SIGN", "微信v3回调验签失败", body, headers);
                throw new CrmebException("微信v3回调验签失败");
            }

            JSONObject callbackJson = JSONObject.parseObject(body);
            if (ObjectUtil.isNull(callbackJson)) {
                throw new CrmebException("微信v3回调报文解析失败");
            }
            String eventType = callbackJson.getString("event_type");
            if (!"TRANSACTION.SUCCESS".equalsIgnoreCase(eventType)) {
                return v3AckSuccess();
            }

            JSONObject resource = callbackJson.getJSONObject("resource");
            String decryptContent = decryptV3Resource(resource);
            JSONObject transaction = JSONObject.parseObject(decryptContent);
            if (ObjectUtil.isNull(transaction)) {
                throw new CrmebException("微信v3回调解密报文为空");
            }
            CallbackVo callbackVo = convertV3Transaction(transaction);
            callbackVo.setEventId(StrUtil.trimToEmpty(callbackJson.getString("id")));
            processPaySuccess(callbackVo);
            return v3AckSuccess();
        } catch (Exception e) {
            logger.error("wechat v3 callback error: {}", e.getMessage(), e);
            return v3AckFail(e.getMessage());
        }
    }

    private CallbackVo convertV3Transaction(JSONObject transaction) {
        CallbackVo callbackVo = new CallbackVo();
        callbackVo.setAppId(StrUtil.blankToDefault(transaction.getString("appid"), transaction.getString("sp_appid")));
        callbackVo.setMchId(StrUtil.blankToDefault(transaction.getString("mchid"), transaction.getString("sp_mchid")));
        callbackVo.setOutTradeNo(transaction.getString("out_trade_no"));
        callbackVo.setTransactionId(transaction.getString("transaction_id"));
        callbackVo.setAttach(transaction.getString("attach"));
        callbackVo.setTimeEnd(transaction.getString("success_time"));
        JSONObject payer = transaction.getJSONObject("payer");
        callbackVo.setBankType(ObjectUtil.isNull(payer) ? "" : payer.getString("bank_type"));
        callbackVo.setIsSubscribe("Y");
        JSONObject amount = transaction.getJSONObject("amount");
        Integer cashFee = ObjectUtil.isNull(amount) ? null : amount.getInteger("payer_total");
        if (ObjectUtil.isNull(cashFee)) {
            cashFee = ObjectUtil.isNull(amount) ? null : amount.getInteger("total");
        }
        callbackVo.setCashFee(ObjectUtil.isNull(cashFee) ? 0 : cashFee);
        callbackVo.setCouponFee(0);
        return callbackVo;
    }

    private boolean verifyV3CallbackSignature(String message, String signature) {
        List<String> certPaths = getV3PlatformCertPathCandidates();
        for (String certPath : certPaths) {
            if (WeChatPayV3Crypto.verifySignature(message, signature, certPath)) {
                return true;
            }
        }
        return false;
    }

    private String decryptV3Resource(JSONObject resource) {
        if (ObjectUtil.isNull(resource)) {
            throw new CrmebException("微信v3回调resource为空");
        }
        String associatedData = resource.getString("associated_data");
        String nonce = resource.getString("nonce");
        String cipherText = resource.getString("ciphertext");
        if (StrUtil.hasBlank(nonce, cipherText)) {
            throw new CrmebException("微信v3回调密文不完整");
        }
        List<String> apiV3Keys = getV3ApiV3KeyCandidates();
        for (String key : apiV3Keys) {
            try {
                return WeChatPayV3Crypto.decryptAesGcm(key, associatedData, nonce, cipherText);
            } catch (Exception ignore) {
                // continue
            }
        }
        throw new CrmebException("微信v3回调解密失败：未匹配到可用APIv3密钥");
    }

    private List<String> getV3PlatformCertPathCandidates() {
        return distinctNonBlank(
                systemConfigService.getValueByKey("pay_routine_sp_platform_cert_path"),
                systemConfigService.getValueByKey("pay_routine_platform_cert_path"),
                systemConfigService.getValueByKey("pay_weixin_sp_platform_cert_path"),
                systemConfigService.getValueByKey("pay_weixin_platform_cert_path"),
                systemConfigService.getValueByKey("pay_weixin_app_sp_platform_cert_path"),
                systemConfigService.getValueByKey("pay_weixin_app_platform_cert_path")
        );
    }

    private List<String> getV3ApiV3KeyCandidates() {
        return distinctNonBlank(
                systemConfigService.getValueByKey("pay_routine_sp_apiv3_key"),
                systemConfigService.getValueByKey("pay_routine_apiv3_key"),
                systemConfigService.getValueByKey("pay_weixin_sp_apiv3_key"),
                systemConfigService.getValueByKey("pay_weixin_apiv3_key"),
                systemConfigService.getValueByKey("pay_weixin_app_sp_apiv3_key"),
                systemConfigService.getValueByKey("pay_weixin_app_apiv3_key")
        );
    }

    private List<String> distinctNonBlank(String... values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                set.add(StrUtil.trim(value));
            }
        }
        return new ArrayList<>(set);
    }

    private String headerIgnoreCase(Map<String, String> headers, String key) {
        if (headers == null || headers.isEmpty() || StrUtil.isBlank(key)) {
            return "";
        }
        String direct = headers.get(key);
        if (StrUtil.isNotBlank(direct)) {
            return direct;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return StrUtil.trimToEmpty(entry.getValue());
            }
        }
        return "";
    }

    private String v3AckSuccess() {
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }

    private String v3AckFail(String message) {
        String safeMsg = StrUtil.blankToDefault(message, "处理失败").replace("\"", "'");
        return "{\"code\":\"FAIL\",\"message\":\"" + safeMsg + "\"}";
    }

    private void processPaySuccess(CallbackVo callbackVo) {
        if (ObjectUtil.isNull(callbackVo) || StrUtil.isBlank(callbackVo.getOutTradeNo())) {
            throw new CrmebException("回调订单号为空");
        }
        if (StrUtil.isBlank(callbackVo.getAttach())) {
            throw new CrmebException("回调attach为空");
        }
        String lockKey = "pay:callback:lock:" + callbackVo.getOutTradeNo();
        distributedLockUtil.executeWithLock(lockKey, 15, () -> {
            processPaySuccessInLock(callbackVo);
            return Boolean.TRUE;
        });
    }

    private void processPaySuccessInLock(CallbackVo callbackVo) {
        AttachVo attachVo = JSONObject.toJavaObject(JSONObject.parseObject(callbackVo.getAttach()), AttachVo.class);
        if (ObjectUtil.isNull(attachVo) || ObjectUtil.isNull(attachVo.getUserId())) {
            throw new CrmebException("回调attach非法");
        }

        User user = userService.getById(attachVo.getUserId());
        if (ObjectUtil.isNull(user)) {
            throw new CrmebException("用户信息错误！");
        }

        if (!Constants.SERVICE_PAY_TYPE_ORDER.equals(attachVo.getType())
                && !Constants.SERVICE_PAY_TYPE_RECHARGE.equals(attachVo.getType())
                && !Constants.SERVICE_PAY_TYPE_BOOKING.equals(attachVo.getType())) {
            logger.error("wechat pay err : 未知的支付类型==》{}", callbackVo.getOutTradeNo());
            throw new CrmebException("未知的支付类型！");
        }

        if (Constants.SERVICE_PAY_TYPE_ORDER.equals(attachVo.getType())) {
            StoreOrder orderParam = new StoreOrder();
            orderParam.setOutTradeNo(callbackVo.getOutTradeNo());
            orderParam.setUid(attachVo.getUserId());

            StoreOrder storeOrder = storeOrderService.getInfoByEntity(orderParam);
            if (ObjectUtil.isNull(storeOrder)) {
                logger.error("wechat pay error : 订单信息不存在==》{}", callbackVo.getOutTradeNo());
                throw new CrmebException("wechat pay error : 订单信息不存在==》" + callbackVo.getOutTradeNo());
            }
            String callbackDedupKey = buildCallbackDedupKey(callbackVo);
            if (hasCallbackConsumed(storeOrder.getId(), callbackDedupKey)) {
                logger.warn("wechat pay warn : 回调幂等命中==》{}", callbackDedupKey);
                return;
            }
            if (storeOrder.getPaid()) {
                logger.warn("wechat pay warn : 订单已处理==》{}", callbackVo.getOutTradeNo());
                return;
            }
            WechatPayInfo wechatPayInfo = wechatPayInfoService.getByNo(storeOrder.getOutTradeNo());
            if (ObjectUtil.isNull(wechatPayInfo)) {
                logger.error("wechat pay error : 微信订单信息不存在==》{}", callbackVo.getOutTradeNo());
                throw new CrmebException("wechat pay error : 微信订单信息不存在==》" + callbackVo.getOutTradeNo());
            }
            wechatPayInfo.setIsSubscribe(callbackVo.getIsSubscribe());
            wechatPayInfo.setBankType(callbackVo.getBankType());
            wechatPayInfo.setCashFee(callbackVo.getCashFee());
            wechatPayInfo.setCouponFee(callbackVo.getCouponFee());
            wechatPayInfo.setTransactionId(callbackVo.getTransactionId());
            wechatPayInfo.setTimeEnd(callbackVo.getTimeEnd());

            verifyOrderByQuery(storeOrder, callbackVo);

            Boolean execute = transactionTemplate.execute(e -> {
                storeOrder.setPaid(true);
                storeOrder.setPayTime(CrmebDateUtil.nowDateTime());
                storeOrder.setUpdateTime(DateUtil.date());
                storeOrderService.updateById(storeOrder);
                if (storeOrder.getUseIntegral() > 0) {
                    userService.updateIntegral(user, storeOrder.getUseIntegral(), "sub");
                }
                wechatPayInfoService.updateById(wechatPayInfo);

                if (storeOrder.getCombinationId() > 0) {
                    StorePink headPink = new StorePink();
                    Integer pinkId = storeOrder.getPinkId();
                    if (pinkId > 0) {
                        headPink = storePinkService.getById(pinkId);
                        if (ObjectUtil.isNull(headPink) || headPink.getIsRefund().equals(true) || headPink.getStatus() == 3) {
                            pinkId = 0;
                        }
                    }
                    StoreCombination storeCombination = storeCombinationService.getById(storeOrder.getCombinationId());
                    if (pinkId > 0) {
                        Integer count = storePinkService.getCountByKid(pinkId);
                        if (count >= storeCombination.getPeople()) {
                            pinkId = 0;
                        }
                    }
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
                    Integer effectiveTime = storeCombination.getEffectiveTime();
                    DateTime dateTime = cn.hutool.core.date.DateUtil.date();
                    storePink.setAddTime(dateTime.getTime());
                    if (pinkId > 0) {
                        storePink.setStopTime(headPink.getStopTime());
                    } else {
                        DateTime hourTime = cn.hutool.core.date.DateUtil.offsetHour(dateTime, effectiveTime);
                        long stopTime = hourTime.getTime();
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
                    storeOrder.setPinkId(storePink.getId());
                    storeOrder.setUpdateTime(DateUtil.date());
                    storeOrderService.updateById(storeOrder);
                }
                return Boolean.TRUE;
            });
            if (!execute) {
                logger.error("wechat pay error : 订单更新失败==》{}", callbackVo.getOutTradeNo());
                throw new CrmebException("订单支付更新失败");
            }
            storeOrderStatusService.createLog(storeOrder.getId(), "pay_callback_dedup", callbackDedupKey);
            redisUtil.lPush(TaskConstants.ORDER_TASK_PAY_SUCCESS_AFTER, storeOrder.getOrderId());
            return;
        }

        if (Constants.SERVICE_PAY_TYPE_BOOKING.equals(attachVo.getType())) {
            BookingOrder bookingOrder = getBookingOrderByOutTradeNo(callbackVo.getOutTradeNo(), attachVo.getUserId());
            verifyBookingOrderByQuery(bookingOrder, callbackVo);
            Boolean bookingPaySuccess = serviceBookingService.paySuccess(callbackVo.getOutTradeNo());
            if (!bookingPaySuccess) {
                logger.error("wechat pay error : 预约订单更新失败==》{}", callbackVo.getOutTradeNo());
                throw new CrmebException("wechat pay error : 预约订单更新失败==》" + callbackVo.getOutTradeNo());
            }
            return;
        }

        if (Constants.SERVICE_PAY_TYPE_RECHARGE.equals(attachVo.getType())) {
            UserRecharge userRecharge = userRechargeService.getByOutTradeNo(callbackVo.getOutTradeNo());
            if (ObjectUtil.isNull(userRecharge)) {
                throw new CrmebException("没有找到订单信息");
            }
            if (userRecharge.getPaid()) {
                return;
            }
            Boolean rechargePayAfter = rechargePayService.paySuccess(userRecharge);
            if (!rechargePayAfter) {
                logger.error("wechat pay error : 数据保存失败==》{}", callbackVo.getOutTradeNo());
                throw new CrmebException("wechat pay error : 数据保存失败==》" + callbackVo.getOutTradeNo());
            }
        }
    }

    private BookingOrder getBookingOrderByOutTradeNo(String outTradeNo, Integer uid) {
        LambdaQueryWrapper<BookingOrder> lqw = new LambdaQueryWrapper<>();
        lqw.eq(BookingOrder::getOrderNo, outTradeNo)
                .eq(BookingOrder::getUid, uid)
                .last("limit 1");
        BookingOrder bookingOrder = bookingOrderDao.selectOne(lqw);
        if (ObjectUtil.isNull(bookingOrder)) {
            throw new CrmebException("wechat pay error : 预约订单信息不存在==》" + outTradeNo);
        }
        return bookingOrder;
    }

    /**
     * 预约订单回调落库前做主动查单与关键字段一致性校验。
     */
    private void verifyBookingOrderByQuery(BookingOrder bookingOrder, CallbackVo callbackVo) {
        WeChatPayChannelConfig payConfig = resolveBookingPayConfig(bookingOrder, callbackVo);
        MyRecord queryRecord = wechatNewService.payOrderQuery(bookingOrder.getOrderNo(), payConfig);

        String queryOutTradeNo = queryRecord.getStr("out_trade_no");
        if (StrUtil.isBlank(queryOutTradeNo) || !queryOutTradeNo.equals(bookingOrder.getOrderNo())) {
            throw new CrmebException("预约查单校验失败：out_trade_no不一致");
        }

        BigDecimal expectedAmount = ObjectUtil.defaultIfNull(bookingOrder.getActualPrice(), BigDecimal.ZERO);
        int expectTotalFee = expectedAmount.multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue();
        int queryTotalFee = parsePositiveInt(queryRecord.getStr("total_fee"), "total_fee");
        if (expectTotalFee != queryTotalFee) {
            throw new CrmebException("预约查单校验失败：total_fee不一致");
        }

        String queryMchId = StrUtil.trimToEmpty(queryRecord.getStr("mch_id"));
        if (StrUtil.isNotBlank(payConfig.getMchId()) && !payConfig.getMchId().equals(queryMchId)) {
            throw new CrmebException("预约查单校验失败：mch_id不一致");
        }
        if (Boolean.TRUE.equals(payConfig.getServiceProviderMode()) && StrUtil.isNotBlank(payConfig.getSubMchId())) {
            String querySubMchId = StrUtil.trimToEmpty(queryRecord.getStr("sub_mch_id"));
            if (!payConfig.getSubMchId().equals(querySubMchId)) {
                throw new CrmebException("预约查单校验失败：sub_mch_id不一致");
            }
        }

        if (StrUtil.isNotBlank(callbackVo.getTransactionId())) {
            String queryTransactionId = StrUtil.trimToEmpty(queryRecord.getStr("transaction_id"));
            if (StrUtil.isNotBlank(queryTransactionId) && !callbackVo.getTransactionId().equals(queryTransactionId)) {
                throw new CrmebException("预约查单校验失败：transaction_id不一致");
            }
        }
    }

    private WeChatPayChannelConfig resolveBookingPayConfig(BookingOrder bookingOrder, CallbackVo callbackVo) {
        if (StrUtil.isNotBlank(callbackVo.getAppId())) {
            return weChatPayConfigSupport.resolveByAppId(callbackVo.getAppId(), bookingOrder.getStoreId());
        }
        return weChatPayConfigSupport.resolveByChannel(1, bookingOrder.getStoreId());
    }

    /**
     * 回调落库前做主动查单与关键字段一致性校验。
     */
    private void verifyOrderByQuery(StoreOrder storeOrder, CallbackVo callbackVo) {
        WeChatPayChannelConfig payConfig = weChatPayConfigSupport.resolveByChannel(storeOrder.getIsChannel(), storeOrder.getStoreId());
        MyRecord queryRecord = wechatNewService.payOrderQuery(storeOrder.getOutTradeNo(), payConfig);

        String queryOutTradeNo = queryRecord.getStr("out_trade_no");
        if (StrUtil.isBlank(queryOutTradeNo) || !queryOutTradeNo.equals(storeOrder.getOutTradeNo())) {
            throw new CrmebException("查单校验失败：out_trade_no不一致");
        }

        int expectTotalFee = storeOrder.getPayPrice().multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue();
        int queryTotalFee = parsePositiveInt(queryRecord.getStr("total_fee"), "total_fee");
        if (expectTotalFee != queryTotalFee) {
            throw new CrmebException("查单校验失败：total_fee不一致");
        }

        String queryMchId = StrUtil.trimToEmpty(queryRecord.getStr("mch_id"));
        if (StrUtil.isNotBlank(payConfig.getMchId()) && !payConfig.getMchId().equals(queryMchId)) {
            throw new CrmebException("查单校验失败：mch_id不一致");
        }
        if (Boolean.TRUE.equals(payConfig.getServiceProviderMode()) && StrUtil.isNotBlank(payConfig.getSubMchId())) {
            String querySubMchId = StrUtil.trimToEmpty(queryRecord.getStr("sub_mch_id"));
            if (!payConfig.getSubMchId().equals(querySubMchId)) {
                throw new CrmebException("查单校验失败：sub_mch_id不一致");
            }
        }

        if (StrUtil.isNotBlank(callbackVo.getTransactionId())) {
            String queryTransactionId = StrUtil.trimToEmpty(queryRecord.getStr("transaction_id"));
            if (StrUtil.isNotBlank(queryTransactionId) && !callbackVo.getTransactionId().equals(queryTransactionId)) {
                throw new CrmebException("查单校验失败：transaction_id不一致");
            }
        }
    }

    private int parsePositiveInt(String value, String fieldName) {
        try {
            return Integer.parseInt(StrUtil.trim(value));
        } catch (Exception e) {
            throw new CrmebException("查单校验失败：" + fieldName + "非法");
        }
    }

    private String buildCallbackDedupKey(CallbackVo callbackVo) {
        String eventId = StrUtil.blankToDefault(StrUtil.trimToEmpty(callbackVo.getEventId()), "v2");
        String transactionId = StrUtil.blankToDefault(StrUtil.trimToEmpty(callbackVo.getTransactionId()), "na");
        return "out_trade_no=" + callbackVo.getOutTradeNo()
                + "|transaction_id=" + transactionId
                + "|event_id=" + eventId;
    }

    private boolean hasCallbackConsumed(Integer orderId, String callbackDedupKey) {
        LambdaQueryWrapper<com.zbkj.common.model.order.StoreOrderStatus> lqw = new LambdaQueryWrapper<>();
        lqw.eq(com.zbkj.common.model.order.StoreOrderStatus::getOid, orderId)
                .eq(com.zbkj.common.model.order.StoreOrderStatus::getChangeType, "pay_callback_dedup")
                .eq(com.zbkj.common.model.order.StoreOrderStatus::getChangeMessage, callbackDedupKey)
                .last("limit 1");
        return storeOrderStatusService.count(lqw) > 0;
    }

    private void saveCallbackAlert(String errCode, String errMsg, String body, Map<String, String> headers) {
        try {
            WechatExceptions wechatExceptions = new WechatExceptions();
            wechatExceptions.setErrcode(StrUtil.blankToDefault(errCode, "-CALLBACK"));
            wechatExceptions.setErrmsg(StrUtil.blankToDefault(errMsg, "微信支付回调异常"));
            JSONObject detail = new JSONObject();
            detail.put("headers", headers);
            detail.put("body", body);
            wechatExceptions.setData(detail.toJSONString());
            wechatExceptions.setRemark("支付回调告警");
            wechatExceptions.setCreateTime(DateUtil.date());
            wechatExceptions.setUpdateTime(DateUtil.date());
            wechatExceptionsService.save(wechatExceptions);
        } catch (Exception e) {
            logger.error("saveCallbackAlert error: {}", e.getMessage(), e);
        }
    }


    /**
     * 将request中的参数转换成Map
     * @param request
     * @return
     */
    private Map<String, String> convertRequestParamsToMap(HttpServletRequest request) {
        Map<String, String> retMap = new HashMap<String, String>();
        Map requestParams = request.getParameterMap();
        for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
            //valueStr = new String(valueStr.getBytes("ISO-8859-1"), "gbk");
            retMap.put(name, valueStr);
        }
        return retMap;
    }

    /**
     * 微信退款回调
     * @param xmlInfo 微信回调json
     * @return MyRecord
     */
    @Override
    public String weChatRefund(String xmlInfo) {
        return weChatRefund(xmlInfo, Collections.emptyMap());
    }

    @Override
    public String weChatRefund(String body, Map<String, String> headers) {
        if (isV3PayCallback(body, headers)) {
            return weChatRefundV3(body, headers);
        }
        return weChatRefundV2(body);
    }

    private String weChatRefundV2(String xmlInfo) {
        MyRecord notifyRecord = new MyRecord();
        MyRecord refundRecord = refundNotify(xmlInfo, notifyRecord);
        if (refundRecord.getStr("status").equals("fail")) {
            logger.error("微信退款回调失败==>" + refundRecord.getColumns() + ", rawData==>" + xmlInfo + ", data==>" + notifyRecord);
            return refundRecord.getStr("returnXml");
        }

        if (!refundRecord.getBoolean("isRefund")) {
            logger.error("微信退款回调失败==>" + refundRecord.getColumns() + ", rawData==>" + xmlInfo + ", data==>" + notifyRecord);
            return refundRecord.getStr("returnXml");
        }
        String outRefundNo = notifyRecord.getStr("out_refund_no");
        String transactionId = StrUtil.trimToEmpty(notifyRecord.getStr("transaction_id"));
        String callbackDedupKey = buildRefundCallbackDedupKey(outRefundNo, transactionId, "v2");
        settleRefundSuccess(outRefundNo, xmlInfo, callbackDedupKey);
        return refundRecord.getStr("returnXml");
    }

    private String weChatRefundV3(String body, Map<String, String> headers) {
        try {
            String timestamp = headerIgnoreCase(headers, "Wechatpay-Timestamp");
            String nonce = headerIgnoreCase(headers, "Wechatpay-Nonce");
            String signature = headerIgnoreCase(headers, "Wechatpay-Signature");
            if (StrUtil.hasBlank(timestamp, nonce, signature)) {
                throw new CrmebException("微信v3退款回调头缺失");
            }

            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            if (!verifyV3CallbackSignature(message, signature)) {
                saveCallbackAlert("-V3REFUNDSIGN", "微信v3退款回调验签失败", body, headers);
                throw new CrmebException("微信v3退款回调验签失败");
            }

            JSONObject callbackJson = JSONObject.parseObject(body);
            if (ObjectUtil.isNull(callbackJson)) {
                throw new CrmebException("微信v3退款回调报文解析失败");
            }
            String eventId = StrUtil.trimToEmpty(callbackJson.getString("id"));
            String eventType = callbackJson.getString("event_type");
            if (!"REFUND.SUCCESS".equalsIgnoreCase(eventType)) {
                return v3AckSuccess();
            }

            JSONObject resource = callbackJson.getJSONObject("resource");
            String decryptContent = decryptV3Resource(resource);
            JSONObject refund = JSONObject.parseObject(decryptContent);
            if (ObjectUtil.isNull(refund)) {
                throw new CrmebException("微信v3退款回调解密报文为空");
            }
            String outRefundNo = StrUtil.trimToEmpty(refund.getString("out_refund_no"));
            if (StrUtil.isBlank(outRefundNo)) {
                throw new CrmebException("微信v3退款回调缺少out_refund_no");
            }
            String refundStatus = StrUtil.trimToEmpty(refund.getString("refund_status"));
            if (!"SUCCESS".equalsIgnoreCase(refundStatus)) {
                return v3AckSuccess();
            }
            String transactionId = StrUtil.trimToEmpty(refund.getString("transaction_id"));
            String callbackDedupKey = buildRefundCallbackDedupKey(outRefundNo, transactionId, eventId);

            settleRefundSuccess(outRefundNo, body, callbackDedupKey);
            return v3AckSuccess();
        } catch (Exception e) {
            logger.error("wechat v3 refund callback error: {}", e.getMessage(), e);
            return v3AckFail(e.getMessage());
        }
    }

    private void settleRefundSuccess(String outRefundNo, String rawData) {
        String callbackDedupKey = buildRefundCallbackDedupKey(outRefundNo, "", "legacy");
        settleRefundSuccess(outRefundNo, rawData, callbackDedupKey);
    }

    private void settleRefundSuccess(String outRefundNo, String rawData, String callbackDedupKey) {
        String dedupKey = StrUtil.blankToDefault(callbackDedupKey, buildRefundCallbackDedupKey(outRefundNo, "", "legacy"));
        String lockKey = "pay:refund:callback:dedup:" + SecureUtil.md5(dedupKey);
        distributedLockUtil.executeWithLock(lockKey, 20, () -> {
            settleRefundSuccessCore(outRefundNo, rawData, dedupKey);
            return Boolean.TRUE;
        });
    }

    private void settleRefundSuccessCore(String outRefundNo, String rawData, String callbackDedupKey) {
        StoreOrder storeOrder = resolveRefundTargetOrder(outRefundNo);
        if (ObjectUtil.isNull(storeOrder)) {
            logger.error("微信退款订单查询失败==> orderNo={}, rawData==>{}", outRefundNo, rawData);
            throw new CrmebException("微信退款订单不存在：" + outRefundNo);
        }
        if (hasRefundCallbackConsumed(storeOrder.getId(), callbackDedupKey)) {
            logger.warn("微信退款回调重复通知，幂等跳过==> orderNo={}, callbackDedupKey={}", outRefundNo, callbackDedupKey);
            return;
        }
        if (OrderRefundStateMachine.isFinalSuccess(storeOrder.getRefundStatus())) {
            markRefundCallbackConsumed(storeOrder.getId(), callbackDedupKey);
            logger.warn("微信退款订单已确认成功==> orderNo={}, rawData==>{}", outRefundNo, rawData);
            return;
        }
        if (!OrderRefundStateMachine.canSwitchToSuccess(storeOrder.getRefundStatus())) {
            logger.warn("微信退款订单状态非法，拒绝变更==> refundStatus={}, orderNo={}, rawData==>{}",
                    storeOrder.getRefundStatus(), outRefundNo, rawData);
            return;
        }
        boolean update = storeOrderService.update(new LambdaUpdateWrapper<StoreOrder>()
                .set(StoreOrder::getRefundStatus, OrderRefundStateMachine.REFUND_STATUS_SUCCESS)
                .set(StoreOrder::getUpdateTime, DateUtil.date())
                .eq(StoreOrder::getId, storeOrder.getId())
                .in(StoreOrder::getRefundStatus,
                        OrderRefundStateMachine.REFUND_STATUS_APPLYING,
                        OrderRefundStateMachine.REFUND_STATUS_REFUNDING));
        if (update) {
            markRefundCallbackConsumed(storeOrder.getId(), callbackDedupKey);
            redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_REFUND_BY_USER, storeOrder.getId());
            return;
        }
        StoreOrder latestOrder = storeOrderService.getById(storeOrder.getId());
        if (ObjectUtil.isNotNull(latestOrder) && OrderRefundStateMachine.isFinalSuccess(latestOrder.getRefundStatus())) {
            markRefundCallbackConsumed(storeOrder.getId(), callbackDedupKey);
            logger.warn("微信退款订单并发更新已完成==> orderNo={}, rawData==>{}", outRefundNo, rawData);
            return;
        }
        logger.warn("微信退款订单更新失败==> orderNo={}, rawData==>{}", outRefundNo, rawData);
    }

    private StoreOrder resolveRefundTargetOrder(String outRefundNo) {
        StoreOrder byOrderId = storeOrderService.getByOderId(outRefundNo);
        if (ObjectUtil.isNotNull(byOrderId)) {
            return byOrderId;
        }
        LambdaQueryWrapper<StoreOrder> outTradeLqw = new LambdaQueryWrapper<>();
        outTradeLqw.eq(StoreOrder::getOutTradeNo, outRefundNo).last("limit 1");
        return storeOrderService.getOne(outTradeLqw, false);
    }

    private String buildRefundCallbackDedupKey(String outRefundNo, String transactionId, String eventId) {
        String safeOutRefundNo = StrUtil.blankToDefault(StrUtil.trimToEmpty(outRefundNo), "na");
        String safeTransactionId = StrUtil.blankToDefault(StrUtil.trimToEmpty(transactionId), "na");
        String safeEventId = StrUtil.blankToDefault(StrUtil.trimToEmpty(eventId), "na");
        return "out_refund_no=" + safeOutRefundNo + "|transaction_id=" + safeTransactionId + "|event_id=" + safeEventId;
    }

    private boolean hasRefundCallbackConsumed(Integer orderId, String callbackDedupKey) {
        LambdaQueryWrapper<com.zbkj.common.model.order.StoreOrderStatus> lqw = new LambdaQueryWrapper<>();
        lqw.eq(com.zbkj.common.model.order.StoreOrderStatus::getOid, orderId)
                .eq(com.zbkj.common.model.order.StoreOrderStatus::getChangeType, REFUND_CALLBACK_DEDUP_CHANGE_TYPE)
                .eq(com.zbkj.common.model.order.StoreOrderStatus::getChangeMessage, callbackDedupKey)
                .last("limit 1");
        return storeOrderStatusService.count(lqw) > 0;
    }

    private void markRefundCallbackConsumed(Integer orderId, String callbackDedupKey) {
        storeOrderStatusService.createLog(orderId, REFUND_CALLBACK_DEDUP_CHANGE_TYPE, callbackDedupKey);
    }

    /**
     * 支付订单回调通知
     * @return MyRecord
     */
    private MyRecord refundNotify(String xmlInfo, MyRecord notifyRecord) {
        MyRecord refundRecord = new MyRecord();
        refundRecord.set("status", "fail");
        StringBuilder sb = new StringBuilder();
        sb.append("<xml>");
        if(StrUtil.isBlank(xmlInfo)){
            sb.append("<return_code><![CDATA[FAIL]]></return_code>");
            sb.append("<return_msg><![CDATA[xmlInfo is blank]]></return_msg>");
            sb.append("</xml>");
            logger.error("wechat refund callback error : " + sb.toString());
            return refundRecord.set("returnXml", sb.toString()).set("errMsg", "xmlInfo is blank");
        }

        Map<String, String> respMap;
        try {
            respMap = WxPayUtil.xmlToMap(xmlInfo);
        } catch (Exception e) {
            sb.append("<return_code><![CDATA[FAIL]]></return_code>");
            sb.append("<return_msg><![CDATA[").append(e.getMessage()).append("]]></return_msg>");
            sb.append("</xml>");
            logger.error("wechat refund callback error : " + e.getMessage());
            return refundRecord.set("returnXml", sb.toString()).set("errMsg", e.getMessage());
        }

        notifyRecord.setColums(_strMap2ObjMap(respMap));
        // 这里的可以应该根据小程序还是公众号区分
        String return_code = respMap.get("return_code");
        if (return_code.equals(Constants.SUCCESS)) {
            String appid = respMap.get("appid");
            String mchId = respMap.get("mch_id");
            String signKey = weChatRefundSignKeyResolver.resolve(appid, mchId);
            try {
                WxPayUtil.verifySign(respMap, signKey);
                // 解码加密信息
                String reqInfo = respMap.get("req_info");
                if (StrUtil.isBlank(reqInfo)) {
                    throw new CrmebException("微信退款回调缺少req_info");
                }
                String decodeInfo = decryptToStr(reqInfo, signKey);
                Map<String, String> infoMap = WxPayUtil.xmlToMap(decodeInfo);
                notifyRecord.setColums(_strMap2ObjMap(infoMap));

                String refund_status = infoMap.get("refund_status");
                refundRecord.set("isRefund", refund_status.equals(Constants.SUCCESS));
            } catch (Exception e) {
                refundRecord.set("isRefund", false);
                logger.error("微信退款回调异常，e==》" + e.getMessage());
            }
        } else {
            notifyRecord.set("return_msg", respMap.get("return_msg"));
            refundRecord.set("isRefund", false);
        }
        sb.append("<return_code><![CDATA[SUCCESS]]></return_code>");
        sb.append("<return_msg><![CDATA[OK]]></return_msg>");
        sb.append("</xml>");
        return refundRecord.set("returnXml", sb.toString()).set("status", "ok");
    }

    /**
     * java自带的是PKCS5Padding填充，不支持PKCS7Padding填充。
     * 通过BouncyCastle组件来让java里面支持PKCS7Padding填充
     * 在加解密之前加上：Security.addProvider(new BouncyCastleProvider())，
     * 并给Cipher.getInstance方法传入参数来指定Java使用这个库里的加/解密算法。
     * 说明：该实现用于微信支付 v2 退款回调 req_info 的历史解密约束（AES/ECB），
     * 不可作为通用加密方案；后续应优先迁移到 v3 回调（AEAD_AES_256_GCM）。
     */
    public static String decryptToStr(String reqInfo, String signKey) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
//        byte[] decodeReqInfo = Base64.decode(reqInfo);
        byte[] decodeReqInfo = base64DecodeJustForWxPay(reqInfo).getBytes(StandardCharsets.ISO_8859_1);
        SecretKeySpec key = new SecretKeySpec(SecureUtil.md5(signKey).toLowerCase().getBytes(), "AES");
        Cipher cipher;
        cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(decodeReqInfo), StandardCharsets.UTF_8);
    }

    private static final List<String> list = new ArrayList<>();
    static {
        list.add("total_fee");
        list.add("cash_fee");
        list.add("coupon_fee");
        list.add("coupon_count");
        list.add("refund_fee");
        list.add("settlement_refund_fee");
        list.add("settlement_total_fee");
        list.add("cash_refund_fee");
        list.add("coupon_refund_fee");
        list.add("coupon_refund_count");
    }

    private Map<String, Object> _strMap2ObjMap(Map<String, String> params) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (list.contains(entry.getKey())) {
                try {
                    map.put(entry.getKey(), Integer.parseInt(entry.getValue()));
                } catch (NumberFormatException e) {
                    map.put(entry.getKey(), 0);
                    logger.error("字段格式错误，key==》" + entry.getKey() + ", value==》" + entry.getValue());
                }
                continue;
            }

            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /**
     * 仅仅为微信解析密文使用
     * @param source 待解析密文
     * @return 结果
     */
    public static String base64DecodeJustForWxPay(final String source) {
        String result = "";
        final Base64.Decoder decoder = Base64.getDecoder();
        try {
            result = new String(decoder.decode(source), "ISO-8859-1");
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }
}
