package com.zbkj.service.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.PayConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.user.UserToken;
import com.zbkj.common.request.ServiceBookingCreateRequest;
import com.zbkj.common.request.ServiceBookingPayRequest;
import com.zbkj.common.request.ServiceBookingVerifyRequest;
import com.zbkj.common.response.OrderPayResultResponse;
import com.zbkj.common.response.ServiceBookingCardCheckResponse;
import com.zbkj.common.response.ServiceBookingOrderResponse;
import com.zbkj.common.response.ServiceBookingSlotResponse;
import com.zbkj.common.response.ServiceBookingVerifyRecordResponse;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.utils.RedisUtil;
import com.zbkj.common.utils.WxPayUtil;
import com.zbkj.common.vo.AttachVo;
import com.zbkj.common.vo.CreateOrderH5SceneInfoDetailVo;
import com.zbkj.common.vo.CreateOrderH5SceneInfoVo;
import com.zbkj.common.vo.CreateOrderRequestVo;
import com.zbkj.common.vo.CreateOrderResponseVo;
import com.zbkj.common.vo.WeChatPayChannelConfig;
import com.zbkj.common.vo.WxPayJsResultVo;
import com.zbkj.service.dao.BookingOrderDao;
import com.zbkj.service.dao.MemberCardDao;
import com.zbkj.service.dao.MemberCardUsageDao;
import com.zbkj.service.dao.TechnicianScheduleDao;
import com.zbkj.service.model.BookingOrder;
import com.zbkj.service.model.MemberCard;
import com.zbkj.service.model.MemberCardUsage;
import com.zbkj.service.model.TechnicianSchedule;
import com.zbkj.service.service.impl.payment.WeChatPayConfigSupport;
import com.zbkj.service.service.impl.payment.WeChatPayV3Crypto;
import com.zbkj.service.service.ServiceBookingService;
import com.zbkj.service.service.SystemConfigService;
import com.zbkj.service.service.UserTokenService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.service.WechatNewService;
import com.zbkj.service.util.DistributedLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 服务预约业务实现
 */
@Service
public class ServiceBookingServiceImpl implements ServiceBookingService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBookingServiceImpl.class);

    private static final String IDEM_PREFIX = "booking:create:idem:";
    private static final String IDEM_PROCESSING = "PROCESSING";
    private static final int LOCK_EXPIRE_SECONDS = 300;
    private static final int ORDER_STATUS_PENDING = 1;
    private static final int ORDER_STATUS_PAID = 2;
    private static final int ORDER_STATUS_VERIFIED = 3;
    private static final int ORDER_STATUS_CANCELLED = 4;
    private static final int MEMBER_CARD_STATUS_NORMAL = 1;
    private static final int MEMBER_CARD_STATUS_FINISHED = 3;
    private static final int MEMBER_CARD_STATUS_EXPIRED = 4;

    @Resource
    private BookingOrderDao bookingOrderDao;

    @Resource
    private TechnicianScheduleDao technicianScheduleDao;

    @Resource
    private MemberCardDao memberCardDao;

    @Resource
    private MemberCardUsageDao memberCardUsageDao;

    @Autowired
    private UserService userService;

    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private WechatNewService wechatNewService;

    @Autowired
    private WeChatPayConfigSupport weChatPayConfigSupport;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private DistributedLockUtil distributedLockUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ServiceBookingOrderResponse create(ServiceBookingCreateRequest request) {
        Integer uid = userService.getUserIdException();
        String idemKey = IDEM_PREFIX + uid + ":" + request.getIdempotentToken();

        RedisTemplate<String, Object> redisTemplate = redisUtil.getRedisTemplate();
        Object idemVal = redisUtil.get(idemKey);
        if (ObjectUtil.isNotEmpty(idemVal) && !IDEM_PROCESSING.equals(String.valueOf(idemVal))) {
            return getOrderAndCheckOwner(String.valueOf(idemVal), uid);
        }

        Boolean lockIdem = redisTemplate.opsForValue().setIfAbsent(idemKey, IDEM_PROCESSING, 60, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(lockIdem)) {
            Object current = redisUtil.get(idemKey);
            if (ObjectUtil.isNotEmpty(current) && !IDEM_PROCESSING.equals(String.valueOf(current))) {
                return getOrderAndCheckOwner(String.valueOf(current), uid);
            }
            throw new CrmebException("请求处理中，请稍后重试");
        }

        String slotLockKey = "booking:slot:lock:" + request.getScheduleId() + ":" + request.getSlotId();
        try {
            ServiceBookingOrderResponse response = distributedLockUtil.executeWithLock(slotLockKey, 12,
                    () -> createInLock(uid, request));
            redisUtil.set(idemKey, response.getOrderNo(), 1800L);
            return response;
        } catch (RuntimeException e) {
            redisUtil.delete(idemKey);
            throw e;
        }
    }

    @Override
    public List<ServiceBookingSlotResponse> listSlots(Integer scheduleId) {
        TechnicianSchedule schedule = technicianScheduleDao.selectById(scheduleId);
        if (ObjectUtil.isNull(schedule)) {
            throw new CrmebException("排班不存在");
        }

        List<ServiceBookingSlotResponse> responseList = new ArrayList<>();
        JSONArray slotArray = extractSlots(schedule.getTimeSlots());
        for (int i = 0; i < slotArray.size(); i++) {
            JSONObject slot = slotArray.getJSONObject(i);
            ServiceBookingSlotResponse resp = new ServiceBookingSlotResponse();
            resp.setScheduleId(scheduleId);
            resp.setSlotId(readSlotId(slot));
            resp.setStartTime(readString(slot, "startTime", "start_time", "start"));
            resp.setEndTime(readString(slot, "endTime", "end_time", "end"));
            resp.setStatus(readStatus(slot));
            resp.setPrice(readDecimal(slot, "price"));
            resp.setOffpeak(readBoolean(slot, "isOffpeak", "is_offpeak"));
            responseList.add(resp);
        }
        return responseList;
    }

    @Override
    public ServiceBookingOrderResponse detail(String orderNo) {
        Integer uid = userService.getUserIdException();
        return getOrderAndCheckOwner(orderNo, uid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayResultResponse pay(ServiceBookingPayRequest request, String ip) {
        Integer uid = userService.getUserIdException();
        BookingOrder order = getByOrderNo(request.getOrderNo());
        if (!uid.equals(order.getUid())) {
            throw new CrmebException("无权支付该预约订单");
        }
        if (ORDER_STATUS_PAID == order.getStatus()) {
            throw new CrmebException("预约订单已支付");
        }
        if (ORDER_STATUS_PENDING != order.getStatus()) {
            throw new CrmebException("当前订单状态不可支付");
        }
        if (!PayConstants.PAY_TYPE_WE_CHAT.equals(request.getPayType())) {
            throw new CrmebException("当前仅支持微信支付");
        }
        Integer channel = parsePayChannel(request.getPayChannel());

        OrderPayResultResponse response = new OrderPayResultResponse();
        response.setOrderNo(order.getOrderNo());
        response.setPayType(PayConstants.PAY_TYPE_WE_CHAT);

        if (order.getActualPrice().compareTo(BigDecimal.ZERO) <= 0) {
            Boolean zeroPayResult = paySuccess(order.getOrderNo());
            response.setStatus(zeroPayResult);
            response.setPayType(PayConstants.PAY_TYPE_ZERO_PAY);
            return response;
        }

        Map<String, String> unifiedorder = unifiedorder(order, channel, ip);
        WxPayJsResultVo vo = new WxPayJsResultVo();
        vo.setAppId(unifiedorder.get("appId"));
        vo.setNonceStr(unifiedorder.get("nonceStr"));
        vo.setPackages(unifiedorder.get("package"));
        vo.setSignType(unifiedorder.get("signType"));
        vo.setTimeStamp(unifiedorder.get("timeStamp"));
        vo.setPaySign(unifiedorder.get("paySign"));
        if (channel == 2) {
            vo.setMwebUrl(unifiedorder.get("mweb_url"));
            response.setPayType(PayConstants.PAY_CHANNEL_WE_CHAT_H5);
        }
        response.setStatus(true);
        response.setJsConfig(vo);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancel(String orderNo) {
        Integer uid = userService.getUserIdException();
        BookingOrder order = getByOrderNo(orderNo);
        if (!uid.equals(order.getUid())) {
            throw new CrmebException("无权取消该预约订单");
        }

        String slotLockKey = "booking:slot:lock:" + order.getScheduleId() + ":" + order.getSlotId();
        return distributedLockUtil.executeWithLock(slotLockKey, 12, () -> cancelInLock(orderNo, true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean paySuccess(String orderNo) {
        BookingOrder order = getByOrderNo(orderNo);
        String slotLockKey = "booking:slot:lock:" + order.getScheduleId() + ":" + order.getSlotId();
        return distributedLockUtil.executeWithLock(slotLockKey, 12, () -> paySuccessInLock(orderNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean verify(ServiceBookingVerifyRequest request) {
        BookingOrder order = getByOrderNo(request.getOrderNo());
        String slotLockKey = "booking:slot:lock:" + order.getScheduleId() + ":" + order.getSlotId();
        return distributedLockUtil.executeWithLock(slotLockKey, 12, () -> verifyInLock(order, request));
    }

    @Override
    public ServiceBookingCardCheckResponse checkMemberCard(String orderNo, Integer usageTimes, BigDecimal usageAmount) {
        BookingOrder order = getByOrderNo(orderNo);
        ServiceBookingCardCheckResponse response = new ServiceBookingCardCheckResponse();
        response.setOrderNo(orderNo);
        response.setMemberCardId(order.getMemberCardId());

        if (ObjectUtil.isNull(order.getMemberCardId()) || order.getMemberCardId() <= 0) {
            response.setAvailable(true);
            response.setReasonCode("NO_MEMBER_CARD");
            response.setReasonMessage("订单未绑定会员卡");
            response.setRequiredValue(BigDecimal.ZERO);
            response.setRemainingValue(BigDecimal.ZERO);
            return response;
        }

        MemberCard card = memberCardDao.selectById(order.getMemberCardId());
        if (ObjectUtil.isNull(card)) {
            response.setAvailable(false);
            response.setReasonCode("CARD_NOT_FOUND");
            response.setReasonMessage("会员卡不存在");
            return response;
        }

        response.setCardType(card.getCardType());
        response.setCardStatus(card.getStatus());
        response.setExpireTime(card.getExpireTime());
        response.setRemainingValue(ObjectUtil.defaultIfNull(card.getRemainingValue(), BigDecimal.ZERO));

        int now = nowUnix();
        if (ObjectUtil.isNotNull(card.getExpireTime()) && card.getExpireTime() > 0 && card.getExpireTime() < now) {
            response.setAvailable(false);
            response.setReasonCode("CARD_EXPIRED");
            response.setReasonMessage("会员卡已过期");
            return response;
        }
        if (ObjectUtil.isNull(card.getStatus()) || card.getStatus() != MEMBER_CARD_STATUS_NORMAL) {
            response.setAvailable(false);
            response.setReasonCode("CARD_STATUS_INVALID");
            response.setReasonMessage("会员卡状态不可用");
            return response;
        }
        if (!order.getUid().equals(card.getUid())) {
            response.setAvailable(false);
            response.setReasonCode("CARD_USER_MISMATCH");
            response.setReasonMessage("会员卡与订单用户不一致");
            return response;
        }

        ConsumeDecision decision = buildConsumeDecision(card, order, usageTimes, usageAmount);
        response.setRequiredValue(decision.getConsumeValue());
        if (decision.getConsumeValue().compareTo(BigDecimal.ZERO) <= 0) {
            response.setAvailable(true);
            response.setReasonCode("NO_CONSUME");
            response.setReasonMessage("本次无需扣减会员卡");
            return response;
        }

        boolean available = response.getRemainingValue().compareTo(decision.getConsumeValue()) >= 0;
        response.setAvailable(available);
        response.setReasonCode(available ? "OK" : "INSUFFICIENT_BALANCE");
        response.setReasonMessage(available ? "会员卡可用" : "会员卡余额不足");
        return response;
    }

    @Override
    public List<ServiceBookingVerifyRecordResponse> listVerifyRecords(String orderNo) {
        BookingOrder order = getByOrderNo(orderNo);
        LambdaQueryWrapper<MemberCardUsage> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MemberCardUsage::getOrderId, order.getId())
                .orderByDesc(MemberCardUsage::getId)
                .last("limit 200");
        List<MemberCardUsage> usageList = memberCardUsageDao.selectList(wrapper);
        List<ServiceBookingVerifyRecordResponse> responseList = new ArrayList<>();
        for (MemberCardUsage usage : usageList) {
            ServiceBookingVerifyRecordResponse response = new ServiceBookingVerifyRecordResponse();
            response.setUsageId(usage.getId());
            response.setOrderNo(orderNo);
            response.setMemberCardId(usage.getUserCardId());
            response.setUsageType(usage.getUsageType());
            response.setUsedTimes(usage.getUsedTimes());
            response.setUsedAmount(usage.getUsedAmount());
            response.setBeforeAmount(usage.getBeforeAmount());
            response.setAfterAmount(usage.getAfterAmount());
            response.setStoreId(usage.getStoreId());
            response.setTechnicianId(usage.getTechnicianId());
            response.setCreatedAt(usage.getCreatedAt());
            responseList.add(response);
        }
        return responseList;
    }

    @Override
    public Integer releaseExpiredLocks(Integer limit) {
        int max = ObjectUtil.isNull(limit) || limit <= 0 ? 200 : Math.min(limit, 1000);
        int now = nowUnix();

        LambdaQueryWrapper<BookingOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(BookingOrder::getStatus, ORDER_STATUS_PENDING)
                .gt(BookingOrder::getLockedExpire, 0)
                .le(BookingOrder::getLockedExpire, now)
                .orderByAsc(BookingOrder::getLockedExpire)
                .last("limit " + max);
        List<BookingOrder> expiredOrders = bookingOrderDao.selectList(wrapper);
        if (ObjectUtil.isNull(expiredOrders) || expiredOrders.isEmpty()) {
            return 0;
        }

        int released = 0;
        for (BookingOrder order : expiredOrders) {
            String slotLockKey = "booking:slot:lock:" + order.getScheduleId() + ":" + order.getSlotId();
            try {
                Boolean done = distributedLockUtil.executeWithLock(slotLockKey, 12,
                        () -> cancelInLock(order.getOrderNo(), false));
                if (Boolean.TRUE.equals(done)) {
                    released++;
                }
            } catch (Exception e) {
                logger.warn("释放超时锁单失败, orderNo={}", order.getOrderNo(), e);
            }
        }
        return released;
    }

    private ServiceBookingOrderResponse createInLock(Integer uid, ServiceBookingCreateRequest request) {
        TechnicianSchedule schedule = technicianScheduleDao.selectById(request.getScheduleId());
        if (ObjectUtil.isNull(schedule)) {
            throw new CrmebException("排班不存在");
        }
        if (ObjectUtil.isNotNull(schedule.getStatus()) && schedule.getStatus() != 1) {
            throw new CrmebException("当前排班不可预约");
        }

        if (ObjectUtil.isNotNull(schedule.getServiceSkuId())
                && schedule.getServiceSkuId() > 0
                && !schedule.getServiceSkuId().equals(request.getServiceSkuId())) {
            throw new CrmebException("服务SKU与排班不匹配");
        }
        SlotMutation mutation = lockSlot(schedule.getTimeSlots(), request.getSlotId());

        String orderNo = CrmebUtil.getOrderNo("bk");
        String checkInCode = "CI" + CrmebUtil.randomCount(100000, 999999);
        int now = nowUnix();
        JSONObject targetSlot = mutation.getTargetSlot();

        BigDecimal actualPrice = readDecimal(targetSlot, "price", "actualPrice", "actual_price");
        if (actualPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new CrmebException("排班价格配置非法");
        }
        if (actualPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw new CrmebException("排班未配置有效价格");
        }
        BigDecimal originalPrice = readDecimal(targetSlot, "originalPrice", "original_price", "marketPrice", "market_price");
        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            originalPrice = actualPrice;
        }
        BigDecimal offpeakDiscount = originalPrice.subtract(actualPrice);
        if (offpeakDiscount.compareTo(BigDecimal.ZERO) < 0) {
            offpeakDiscount = BigDecimal.ZERO;
        }
        String resolvedServiceName = readString(targetSlot, "serviceName", "service_name");
        if (StrUtil.isBlank(resolvedServiceName)) {
            resolvedServiceName = request.getServiceName();
        }
        if (request.getActualPrice().compareTo(actualPrice) != 0 || request.getOriginalPrice().compareTo(originalPrice) != 0) {
            logger.warn("booking create price override requestOriginal={} requestActual={} serverOriginal={} serverActual={} scheduleId={} slotId={}",
                    request.getOriginalPrice(), request.getActualPrice(), originalPrice, actualPrice, request.getScheduleId(), request.getSlotId());
        }

        targetSlot.put("status", "locked");
        targetSlot.put("orderNo", orderNo);
        targetSlot.put("lockedExpire", now + LOCK_EXPIRE_SECONDS);

        schedule.setTimeSlots(JSON.toJSONString(mutation.getRoot()));
        if (ObjectUtil.isNotNull(schedule.getAvailableSlots()) && schedule.getAvailableSlots() > 0) {
            schedule.setAvailableSlots(schedule.getAvailableSlots() - 1);
        }
        schedule.setUpdatedAt(now);
        int scheduleUpdated = technicianScheduleDao.updateById(schedule);
        if (scheduleUpdated <= 0) {
            throw new CrmebException("排班更新失败，请重试");
        }

        BookingOrder order = new BookingOrder();
        order.setOrderNo(orderNo);
        order.setUid(uid);
        order.setStoreId(schedule.getStoreId());
        order.setTechnicianId(schedule.getTechnicianId());
        order.setScheduleId(schedule.getId());
        order.setSlotId(request.getSlotId());
        order.setServiceSkuId(request.getServiceSkuId());
        order.setServiceName(resolvedServiceName);
        order.setReserveDate(schedule.getWorkDate());
        order.setReserveTime(mutation.getReserveTime());
        order.setServiceDuration(mutation.getDurationMinutes());
        order.setOriginalPrice(originalPrice);
        order.setActualPrice(actualPrice);
        order.setOffpeakDiscount(offpeakDiscount);
        order.setPaymentType(ObjectUtil.isNull(request.getPaymentType()) ? 1 : request.getPaymentType());
        order.setMemberCardId(ObjectUtil.isNull(request.getMemberCardId()) ? 0L : request.getMemberCardId());
        order.setStatus(ORDER_STATUS_PENDING);
        order.setCheckInCode(checkInCode);
        order.setCheckInTime(0);
        order.setLockedExpire(now + LOCK_EXPIRE_SECONDS);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        bookingOrderDao.insert(order);

        return toResponse(order);
    }

    private Integer parsePayChannel(String payChannel) {
        if (PayConstants.PAY_CHANNEL_WE_CHAT_H5.equals(payChannel)) {
            return 2;
        }
        if (PayConstants.PAY_CHANNEL_WE_CHAT_PUBLIC.equals(payChannel)) {
            return 0;
        }
        if (PayConstants.PAY_CHANNEL_WE_CHAT_PROGRAM.equals(payChannel)) {
            return 1;
        }
        throw new CrmebException("不支持的支付渠道");
    }

    private Map<String, String> unifiedorder(BookingOrder order, Integer channel, String ip) {
        UserToken userToken = new UserToken();
        if (channel == 0) {
            userToken = userTokenService.getTokenByUserId(order.getUid(), 1);
        }
        if (channel == 1) {
            userToken = userTokenService.getTokenByUserId(order.getUid(), 2);
        }
        if (channel == 2) {
            userToken.setToken("");
        }
        if (channel != 2 && ObjectUtil.isNull(userToken)) {
            throw new CrmebException("该用户没有openId");
        }

        WeChatPayChannelConfig payConfig = weChatPayConfigSupport.resolveByChannel(channel, order.getStoreId());

        CreateOrderRequestVo unifiedorderVo = getUnifiedorderVo(order, userToken.getToken(), ip, channel, payConfig);
        logger.info(
                "wx.unifiedorder.request biz=booking orderNo={} outTradeNo={} channel={} storeId={} serviceProviderMode={} mchId={} subMchId={}",
                order.getOrderNo(),
                unifiedorderVo.getOut_trade_no(),
                channel,
                order.getStoreId(),
                Boolean.TRUE.equals(payConfig.getServiceProviderMode()),
                payConfig.getMchId(),
                StrUtil.blankToDefault(payConfig.getSubMchId(), "-")
        );
        CreateOrderResponseVo responseVo = wechatNewService.payUnifiedorder(unifiedorderVo, payConfig);
        logger.info(
                "wx.unifiedorder.response biz=booking orderNo={} outTradeNo={} channel={} storeId={} serviceProviderMode={} mchId={} subMchId={} prepayId={}",
                order.getOrderNo(),
                unifiedorderVo.getOut_trade_no(),
                channel,
                order.getStoreId(),
                Boolean.TRUE.equals(payConfig.getServiceProviderMode()),
                payConfig.getMchId(),
                StrUtil.blankToDefault(payConfig.getSubMchId(), "-"),
                responseVo.getPrepayId()
        );

        Map<String, String> map = new HashMap<>();
        String clientAppId = StrUtil.isNotBlank(payConfig.getClientAppId()) ? payConfig.getClientAppId() : unifiedorderVo.getAppid();
        String packageValue = "prepay_id=".concat(responseVo.getPrepayId());
        map.put("appId", clientAppId);
        map.put("package", packageValue);
        Long currentTimestamp = WxPayUtil.getCurrentTimestamp();
        String timeStamp = Long.toString(currentTimestamp);
        map.put("timeStamp", timeStamp);
        if (isV3PayConfig(payConfig)) {
            String nonceStr = WxPayUtil.getNonceStr();
            map.put("nonceStr", nonceStr);
            map.put("signType", "RSA");
            map.put("paySign", WeChatPayV3Crypto.signMiniProgramPay(
                    clientAppId,
                    timeStamp,
                    nonceStr,
                    packageValue,
                    payConfig.getPrivateKeyPath()
            ));
        } else {
            map.put("nonceStr", unifiedorderVo.getNonce_str());
            map.put("signType", unifiedorderVo.getSign_type());
            map.put("paySign", WxPayUtil.getSign(map, payConfig.getSignKey()));
        }
        map.put("prepayId", responseVo.getPrepayId());
        map.put("prepayTime", CrmebDateUtil.nowDateTimeStr());
        map.put("outTradeNo", unifiedorderVo.getOut_trade_no());
        if (channel == 2) {
            map.put("mweb_url", responseVo.getMWebUrl());
        }
        return map;
    }

    private CreateOrderRequestVo getUnifiedorderVo(BookingOrder order, String openid, String ip, Integer channel,
                                                   WeChatPayChannelConfig payConfig) {
        String domain = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_SITE_URL);
        String apiDomain = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_API_URL);

        AttachVo attachVo = new AttachVo(Constants.SERVICE_PAY_TYPE_BOOKING, order.getUid());
        CreateOrderRequestVo vo = new CreateOrderRequestVo();

        vo.setAppid(payConfig.getAppId());
        vo.setMch_id(payConfig.getMchId());
        vo.setNonce_str(WxPayUtil.getNonceStr());
        vo.setSign_type(PayConstants.WX_PAY_SIGN_TYPE_MD5);
        String siteName = systemConfigService.getValueByKeyException(Constants.CONFIG_KEY_SITE_NAME);
        vo.setBody(siteName);
        vo.setAttach(JSONObject.toJSONString(attachVo));
        vo.setOut_trade_no(order.getOrderNo());
        vo.setTotal_fee(order.getActualPrice().multiply(BigDecimal.TEN).multiply(BigDecimal.TEN).intValue());
        vo.setSpbill_create_ip(ip);
        vo.setNotify_url(apiDomain + PayConstants.WX_PAY_NOTIFY_API_URI);
        vo.setTrade_type(PayConstants.WX_PAY_TRADE_TYPE_JS);
        String payOpenId = openid;
        if (channel == 2) {
            vo.setTrade_type(PayConstants.WX_PAY_TRADE_TYPE_H5);
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

    private boolean isV3PayConfig(WeChatPayChannelConfig payConfig) {
        return ObjectUtil.isNotNull(payConfig) && "v3".equalsIgnoreCase(StrUtil.blankToDefault(payConfig.getApiVersion(), "v2"));
    }

    private Boolean paySuccessInLock(String orderNo) {
        BookingOrder order = getByOrderNo(orderNo);
        if (ORDER_STATUS_PAID == order.getStatus()) {
            return true;
        }
        if (ORDER_STATUS_PENDING != order.getStatus()) {
            throw new CrmebException("当前订单状态不可支付");
        }

        TechnicianSchedule schedule = technicianScheduleDao.selectById(order.getScheduleId());
        if (ObjectUtil.isNull(schedule)) {
            throw new CrmebException("排班不存在");
        }

        SlotMutation slotMutation = findSlot(schedule.getTimeSlots(), order.getSlotId());
        JSONObject slot = slotMutation.getTargetSlot();
        String slotOrderNo = readString(slot, "orderNo", "order_no");
        if (StrUtil.isNotBlank(slotOrderNo) && !orderNo.equals(slotOrderNo)) {
            throw new CrmebException("时间槽已被占用");
        }
        String slotStatus = readStatus(slot).toLowerCase();
        if ("available".equals(slotStatus)) {
            throw new CrmebException("时间槽锁定已失效，请重新下单");
        }

        slot.put("status", "booked");
        slot.put("orderNo", orderNo);
        slot.remove("lockedExpire");
        schedule.setTimeSlots(JSON.toJSONString(slotMutation.getRoot()));
        schedule.setUpdatedAt(nowUnix());
        int scheduleUpdated = technicianScheduleDao.updateById(schedule);
        if (scheduleUpdated <= 0) {
            throw new CrmebException("排班更新失败");
        }

        int now = nowUnix();
        LambdaUpdateWrapper<BookingOrder> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(BookingOrder::getOrderNo, orderNo)
                .eq(BookingOrder::getStatus, ORDER_STATUS_PENDING)
                .set(BookingOrder::getStatus, ORDER_STATUS_PAID)
                .set(BookingOrder::getLockedExpire, 0)
                .set(BookingOrder::getUpdatedAt, now);
        int updated = bookingOrderDao.update(null, updateWrapper);
        if (updated <= 0) {
            BookingOrder latest = getByOrderNo(orderNo);
            if (ORDER_STATUS_PAID == latest.getStatus()) {
                return true;
            }
            throw new CrmebException("订单状态更新失败");
        }
        return true;
    }

    private Boolean cancelInLock(String orderNo, boolean strictStatus) {
        BookingOrder order = getByOrderNo(orderNo);
        if (ORDER_STATUS_CANCELLED == order.getStatus()) {
            return true;
        }
        if (ORDER_STATUS_PENDING != order.getStatus()) {
            if (strictStatus) {
                throw new CrmebException("当前订单状态不可取消");
            }
            return false;
        }

        TechnicianSchedule schedule = technicianScheduleDao.selectById(order.getScheduleId());
        if (ObjectUtil.isNotNull(schedule)) {
            releaseSlot(schedule, order.getSlotId(), orderNo);
        }

        int now = nowUnix();
        LambdaUpdateWrapper<BookingOrder> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(BookingOrder::getOrderNo, orderNo)
                .eq(BookingOrder::getStatus, ORDER_STATUS_PENDING)
                .set(BookingOrder::getStatus, ORDER_STATUS_CANCELLED)
                .set(BookingOrder::getLockedExpire, 0)
                .set(BookingOrder::getUpdatedAt, now);
        int updated = bookingOrderDao.update(null, updateWrapper);
        if (updated <= 0) {
            BookingOrder latest = getByOrderNo(orderNo);
            if (ORDER_STATUS_CANCELLED == latest.getStatus()) {
                return true;
            }
            if (strictStatus) {
                throw new CrmebException("订单状态更新失败");
            }
            return false;
        }
        return true;
    }

    private Boolean verifyInLock(BookingOrder order, ServiceBookingVerifyRequest request) {
        if (ORDER_STATUS_VERIFIED == order.getStatus()) {
            return true;
        }
        if (ORDER_STATUS_PAID != order.getStatus()) {
            throw new CrmebException("当前订单状态不可核销");
        }
        if (StrUtil.isNotBlank(request.getCheckInCode()) && !request.getCheckInCode().equals(order.getCheckInCode())) {
            throw new CrmebException("核销码不匹配");
        }

        TechnicianSchedule schedule = technicianScheduleDao.selectById(order.getScheduleId());
        if (ObjectUtil.isNotNull(schedule)) {
            SlotMutation slotMutation = findSlot(schedule.getTimeSlots(), order.getSlotId());
            JSONObject slot = slotMutation.getTargetSlot();
            String slotOrderNo = readString(slot, "orderNo", "order_no");
            if (StrUtil.isNotBlank(slotOrderNo) && !order.getOrderNo().equals(slotOrderNo)) {
                throw new CrmebException("时间槽占用订单不一致");
            }
            slot.put("status", "verified");
            slot.put("orderNo", order.getOrderNo());
            slot.remove("lockedExpire");
            schedule.setTimeSlots(JSON.toJSONString(slotMutation.getRoot()));
            schedule.setUpdatedAt(nowUnix());
            technicianScheduleDao.updateById(schedule);
        }

        consumeMemberCardIfNeeded(order, request);

        int now = nowUnix();
        LambdaUpdateWrapper<BookingOrder> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(BookingOrder::getOrderNo, order.getOrderNo())
                .eq(BookingOrder::getStatus, ORDER_STATUS_PAID)
                .set(BookingOrder::getStatus, ORDER_STATUS_VERIFIED)
                .set(BookingOrder::getCheckInTime, now)
                .set(BookingOrder::getUpdatedAt, now);
        int updated = bookingOrderDao.update(null, updateWrapper);
        if (updated <= 0) {
            BookingOrder latest = getByOrderNo(order.getOrderNo());
            if (ORDER_STATUS_VERIFIED == latest.getStatus()) {
                return true;
            }
            throw new CrmebException("预约订单核销状态更新失败");
        }
        return true;
    }

    private void consumeMemberCardIfNeeded(BookingOrder order, ServiceBookingVerifyRequest request) {
        if (ObjectUtil.isNull(order.getMemberCardId()) || order.getMemberCardId() <= 0) {
            return;
        }
        String cardLockKey = "booking:member_card:lock:" + order.getMemberCardId();
        distributedLockUtil.executeWithLock(cardLockKey, 8, () -> {
            consumeMemberCardInLock(order, request);
            return Boolean.TRUE;
        });
    }

    private void consumeMemberCardInLock(BookingOrder order, ServiceBookingVerifyRequest request) {
        MemberCard card = memberCardDao.selectById(order.getMemberCardId());
        if (ObjectUtil.isNull(card)) {
            throw new CrmebException("会员卡不存在");
        }
        if (!order.getUid().equals(card.getUid())) {
            throw new CrmebException("会员卡与订单用户不一致");
        }
        if (ObjectUtil.isNull(card.getStatus()) || card.getStatus() != MEMBER_CARD_STATUS_NORMAL) {
            throw new CrmebException("会员卡状态不可用");
        }
        int now = nowUnix();
        if (ObjectUtil.isNotNull(card.getExpireTime()) && card.getExpireTime() > 0 && card.getExpireTime() < now) {
            LambdaUpdateWrapper<MemberCard> expireWrapper = Wrappers.lambdaUpdate();
            expireWrapper.eq(MemberCard::getId, card.getId())
                    .set(MemberCard::getStatus, MEMBER_CARD_STATUS_EXPIRED)
                    .set(MemberCard::getUpdatedAt, now);
            memberCardDao.update(null, expireWrapper);
            throw new CrmebException("会员卡已过期");
        }

        BigDecimal beforeValue = ObjectUtil.defaultIfNull(card.getRemainingValue(), BigDecimal.ZERO);
        ConsumeDecision decision = buildConsumeDecision(card, order, request.getUsageTimes(), request.getUsageAmount());
        BigDecimal consumeValue = decision.getConsumeValue();
        Integer usageType = decision.getUsageType();
        Integer usageTimes = decision.getUsageTimes();
        BigDecimal usageAmount = decision.getUsageAmount();
        if (consumeValue.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (beforeValue.compareTo(consumeValue) < 0) {
            throw new CrmebException("会员卡余额不足");
        }

        BigDecimal afterValue = beforeValue.subtract(consumeValue);
        int nextStatus = afterValue.compareTo(BigDecimal.ZERO) == 0 ? MEMBER_CARD_STATUS_FINISHED : MEMBER_CARD_STATUS_NORMAL;

        LambdaUpdateWrapper<MemberCard> updateWrapper = Wrappers.lambdaUpdate();
        updateWrapper.eq(MemberCard::getId, card.getId())
                .eq(MemberCard::getStatus, MEMBER_CARD_STATUS_NORMAL)
                .set(MemberCard::getRemainingValue, afterValue)
                .set(MemberCard::getStatus, nextStatus)
                .set(MemberCard::getUpdatedAt, now);
        int updated = memberCardDao.update(null, updateWrapper);
        if (updated <= 0) {
            throw new CrmebException("会员卡核销扣减失败");
        }

        MemberCardUsage usage = new MemberCardUsage();
        usage.setUserCardId(card.getId());
        usage.setUserId(order.getUid());
        usage.setOrderId(order.getId());
        usage.setUsageType(usageType);
        usage.setUsedTimes(usageTimes);
        usage.setUsedAmount(usageAmount);
        usage.setBeforeTimes(beforeValue.intValue());
        usage.setAfterTimes(afterValue.intValue());
        usage.setBeforeAmount(beforeValue);
        usage.setAfterAmount(afterValue);
        usage.setStoreId(ObjectUtil.defaultIfNull(request.getStoreId(), order.getStoreId()));
        usage.setTechnicianId(ObjectUtil.defaultIfNull(request.getTechnicianId(), order.getTechnicianId()));
        usage.setCreatedAt(now);
        memberCardUsageDao.insert(usage);
    }

    private ConsumeDecision buildConsumeDecision(MemberCard card, BookingOrder order, Integer requestUsageTimes,
                                                 BigDecimal requestUsageAmount) {
        ConsumeDecision decision = new ConsumeDecision();
        if (card.getCardType() == 1 || card.getCardType() == 3) {
            int times = ObjectUtil.isNull(requestUsageTimes) || requestUsageTimes <= 0 ? 1 : requestUsageTimes;
            decision.setUsageType(1);
            decision.setUsageTimes(times);
            decision.setUsageAmount(BigDecimal.ZERO);
            decision.setConsumeValue(BigDecimal.valueOf(times));
            return decision;
        }
        BigDecimal amount = requestUsageAmount;
        if (ObjectUtil.isNull(amount) || amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = ObjectUtil.defaultIfNull(order.getActualPrice(), BigDecimal.ZERO);
        }
        decision.setUsageType(2);
        decision.setUsageTimes(0);
        decision.setUsageAmount(amount);
        decision.setConsumeValue(amount);
        return decision;
    }

    private void releaseSlot(TechnicianSchedule schedule, String slotId, String orderNo) {
        SlotMutation mutation = findSlot(schedule.getTimeSlots(), slotId);
        JSONObject slot = mutation.getTargetSlot();
        String slotOrderNo = readString(slot, "orderNo", "order_no");
        if (StrUtil.isNotBlank(slotOrderNo) && !orderNo.equals(slotOrderNo)) {
            return;
        }

        slot.put("status", "available");
        slot.remove("orderNo");
        slot.remove("lockedExpire");

        Integer availableSlots = schedule.getAvailableSlots();
        if (ObjectUtil.isNotNull(availableSlots)) {
            Integer totalSlots = schedule.getTotalSlots();
            if (ObjectUtil.isNotNull(totalSlots)) {
                schedule.setAvailableSlots(Math.min(availableSlots + 1, totalSlots));
            } else {
                schedule.setAvailableSlots(availableSlots + 1);
            }
        }
        schedule.setTimeSlots(JSON.toJSONString(mutation.getRoot()));
        schedule.setUpdatedAt(nowUnix());
        technicianScheduleDao.updateById(schedule);
    }

    private BookingOrder getByOrderNo(String orderNo) {
        LambdaQueryWrapper<BookingOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(BookingOrder::getOrderNo, orderNo);
        BookingOrder order = bookingOrderDao.selectOne(lqw);
        if (ObjectUtil.isNull(order)) {
            throw new CrmebException("预约订单不存在");
        }
        return order;
    }

    private ServiceBookingOrderResponse getOrderAndCheckOwner(String orderNo, Integer uid) {
        BookingOrder order = getByOrderNo(orderNo);
        if (!uid.equals(order.getUid())) {
            throw new CrmebException("无权查看该预约订单");
        }
        return toResponse(order);
    }

    private ServiceBookingOrderResponse toResponse(BookingOrder order) {
        ServiceBookingOrderResponse response = new ServiceBookingOrderResponse();
        response.setOrderNo(order.getOrderNo());
        response.setStatus(order.getStatus());
        response.setScheduleId(order.getScheduleId());
        response.setSlotId(order.getSlotId());
        response.setReserveDate(ObjectUtil.isNull(order.getReserveDate()) ? "" : DateUtil.formatDate(order.getReserveDate()));
        response.setReserveTime(order.getReserveTime());
        response.setActualPrice(order.getActualPrice());
        response.setCheckInCode(order.getCheckInCode());
        return response;
    }

    private SlotMutation lockSlot(String timeSlotsJson, String slotId) {
        SlotMutation mutation = findSlot(timeSlotsJson, slotId);
        if (!isAvailable(mutation.getTargetSlot())) {
            throw new CrmebException("该时间槽不可预约");
        }
        return mutation;
    }

    private SlotMutation findSlot(String timeSlotsJson, String slotId) {
        if (StrUtil.isBlank(timeSlotsJson)) {
            throw new CrmebException("排班时间槽为空");
        }

        Object root = JSON.parse(timeSlotsJson);
        JSONArray slots = extractSlots(root);
        if (ObjectUtil.isNull(slots) || slots.isEmpty()) {
            throw new CrmebException("排班时间槽为空");
        }

        JSONObject target = null;
        for (int i = 0; i < slots.size(); i++) {
            JSONObject slot = slots.getJSONObject(i);
            if (slotId.equals(readSlotId(slot))) {
                target = slot;
                break;
            }
        }

        if (ObjectUtil.isNull(target)) {
            throw new CrmebException("时间槽不存在");
        }

        String start = readString(target, "startTime", "start_time", "start");
        String end = readString(target, "endTime", "end_time", "end");
        int duration = 60;
        if (StrUtil.isNotBlank(start) && StrUtil.isNotBlank(end)) {
            Date startDate = DateUtil.parse(DateUtil.today() + " " + start, "yyyy-MM-dd HH:mm");
            Date endDate = DateUtil.parse(DateUtil.today() + " " + end, "yyyy-MM-dd HH:mm");
            long diffMin = (endDate.getTime() - startDate.getTime()) / 60000;
            if (diffMin > 0) {
                duration = (int) diffMin;
            }
        }

        SlotMutation mutation = new SlotMutation();
        mutation.setRoot(root);
        mutation.setTargetSlot(target);
        mutation.setDurationMinutes(duration);
        mutation.setReserveTime(StrUtil.isBlank(start) || StrUtil.isBlank(end) ? "" : start + "-" + end);
        return mutation;
    }

    private JSONArray extractSlots(String timeSlotsJson) {
        if (StrUtil.isBlank(timeSlotsJson)) {
            return new JSONArray();
        }
        try {
            return extractSlots(JSON.parse(timeSlotsJson));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private JSONArray extractSlots(Object root) {
        if (root instanceof JSONArray) {
            return (JSONArray) root;
        }
        if (root instanceof JSONObject) {
            JSONObject obj = (JSONObject) root;
            Object slots = obj.get("slots");
            if (slots instanceof JSONArray) {
                return (JSONArray) slots;
            }
            if (slots instanceof List) {
                return JSON.parseArray(JSON.toJSONString(slots));
            }
        }
        return new JSONArray();
    }

    private String readSlotId(JSONObject slot) {
        return readString(slot, "id", "slotId", "slot_id");
    }

    private String readStatus(JSONObject slot) {
        String status = readString(slot, "status");
        return StrUtil.isBlank(status) ? "available" : status;
    }

    private boolean readBoolean(JSONObject slot, String... keys) {
        for (String key : keys) {
            Object val = slot.get(key);
            if (ObjectUtil.isNull(val)) {
                continue;
            }
            if (val instanceof Boolean) {
                return (Boolean) val;
            }
            return "1".equals(val.toString()) || "true".equalsIgnoreCase(val.toString());
        }
        return false;
    }

    private BigDecimal readDecimal(JSONObject slot, String... keys) {
        for (String key : keys) {
            Object val = slot.get(key);
            if (ObjectUtil.isNotNull(val) && StrUtil.isNotBlank(val.toString())) {
                return new BigDecimal(val.toString());
            }
        }
        return BigDecimal.ZERO;
    }

    private String readString(JSONObject slot, String... keys) {
        for (String key : keys) {
            Object val = slot.get(key);
            if (ObjectUtil.isNotNull(val) && StrUtil.isNotBlank(val.toString())) {
                return val.toString();
            }
        }
        return "";
    }

    private boolean isAvailable(JSONObject slot) {
        String status = readStatus(slot).toLowerCase();
        if ("booked".equals(status) || "3".equals(status)) {
            return false;
        }
        String orderNo = readString(slot, "orderNo", "order_no");
        if ("locked".equals(status) || "2".equals(status) || StrUtil.isNotBlank(orderNo)) {
            Object lockedExpire = slot.get("lockedExpire");
            if (ObjectUtil.isNull(lockedExpire)) {
                return false;
            }
            long expire;
            try {
                expire = Long.parseLong(lockedExpire.toString());
            } catch (Exception e) {
                return false;
            }
            return expire < (System.currentTimeMillis() / 1000);
        }
        return true;
    }

    private int nowUnix() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    private static class SlotMutation {
        private Object root;
        private JSONObject targetSlot;
        private Integer durationMinutes;
        private String reserveTime;

        public Object getRoot() {
            return root;
        }

        public void setRoot(Object root) {
            this.root = root;
        }

        public JSONObject getTargetSlot() {
            return targetSlot;
        }

        public void setTargetSlot(JSONObject targetSlot) {
            this.targetSlot = targetSlot;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public String getReserveTime() {
            return reserveTime;
        }

        public void setReserveTime(String reserveTime) {
            this.reserveTime = reserveTime;
        }
    }

    private static class ConsumeDecision {
        private Integer usageType;
        private Integer usageTimes;
        private BigDecimal usageAmount;
        private BigDecimal consumeValue;

        public Integer getUsageType() {
            return usageType;
        }

        public void setUsageType(Integer usageType) {
            this.usageType = usageType;
        }

        public Integer getUsageTimes() {
            return usageTimes;
        }

        public void setUsageTimes(Integer usageTimes) {
            this.usageTimes = usageTimes;
        }

        public BigDecimal getUsageAmount() {
            return usageAmount;
        }

        public void setUsageAmount(BigDecimal usageAmount) {
            this.usageAmount = usageAmount;
        }

        public BigDecimal getConsumeValue() {
            return consumeValue;
        }

        public void setConsumeValue(BigDecimal consumeValue) {
            this.consumeValue = consumeValue;
        }
    }
}
