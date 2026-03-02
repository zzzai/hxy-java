package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.OffpeakRuleDO;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.dal.mysql.BookingOrderMapper;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.enums.DispatchModeEnum;
import com.hxy.module.booking.service.BookingOrderService;
import com.hxy.module.booking.service.OffpeakRuleService;
import com.hxy.module.booking.service.TechnicianCommissionService;
import com.hxy.module.booking.service.TechnicianDispatchService;
import com.hxy.module.booking.service.TimeSlotService;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.api.refund.PayRefundApi;
import cn.iocoder.yudao.module.pay.api.refund.dto.PayRefundCreateReqDTO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class BookingOrderServiceImpl implements BookingOrderService {

    private final BookingOrderMapper bookingOrderMapper;
    private final TimeSlotService timeSlotService;
    private final TechnicianDispatchService technicianDispatchService;
    private final ProductSpuApi productSpuApi;
    private final ProductSkuApi productSkuApi;
    private final OffpeakRuleService offpeakRuleService;
    private final TechnicianCommissionService technicianCommissionService;
    private final PayOrderApi payOrderApi;
    private final PayRefundApi payRefundApi;

    /**
     * 预约支付应用标识（需在支付管理中配置）
     */
    private static final String PAY_APP_KEY = "booking";

    public BookingOrderServiceImpl(
            BookingOrderMapper bookingOrderMapper,
            TimeSlotService timeSlotService,
            TechnicianDispatchService technicianDispatchService,
            ProductSpuApi productSpuApi,
            ProductSkuApi productSkuApi,
            OffpeakRuleService offpeakRuleService,
            @Lazy TechnicianCommissionService technicianCommissionService,
            PayOrderApi payOrderApi,
            PayRefundApi payRefundApi) {
        this.bookingOrderMapper = bookingOrderMapper;
        this.timeSlotService = timeSlotService;
        this.technicianDispatchService = technicianDispatchService;
        this.productSpuApi = productSpuApi;
        this.productSkuApi = productSkuApi;
        this.offpeakRuleService = offpeakRuleService;
        this.technicianCommissionService = technicianCommissionService;
        this.payOrderApi = payOrderApi;
        this.payRefundApi = payRefundApi;
    }

    /**
     * 待支付超时时间（分钟）
     */
    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, Long timeSlotId, Long spuId, Long skuId, String userRemark) {
        // 默认点钟模式
        return createOrder(userId, timeSlotId, spuId, skuId, userRemark,
                DispatchModeEnum.DESIGNATED.getMode(), null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, Long timeSlotId, Long spuId, Long skuId, String userRemark,
                            Integer dispatchMode, Long storeId, LocalDate bookingDate, LocalTime startTime) {
        // 排钟模式：自动分配技师和时间槽
        if (DispatchModeEnum.AUTO_ASSIGN.getMode().equals(dispatchMode)) {
            TechnicianDO technician = technicianDispatchService.autoAssignTechnician(storeId, bookingDate, startTime);
            if (technician == null) {
                throw exception(DISPATCH_NO_AVAILABLE_TECHNICIAN);
            }
            // 查找该技师对应时段的时间槽
            List<TimeSlotDO> slots = timeSlotService.getTimeSlotsByTechnicianAndDate(technician.getId(), bookingDate);
            TimeSlotDO matchedSlot = slots.stream()
                    .filter(s -> s.getStartTime().equals(startTime)
                            && com.hxy.module.booking.enums.TimeSlotStatusEnum.AVAILABLE.getStatus().equals(s.getStatus()))
                    .findFirst()
                    .orElse(null);
            if (matchedSlot == null) {
                throw exception(TIME_SLOT_NOT_AVAILABLE);
            }
            timeSlotId = matchedSlot.getId();
        }

        // 1. 锁定时间槽
        boolean locked = timeSlotService.lockTimeSlot(timeSlotId, userId);
        if (!locked) {
            throw exception(TIME_SLOT_NOT_AVAILABLE);
        }

        // 2. 获取时间槽信息
        TimeSlotDO timeSlot = timeSlotService.getTimeSlot(timeSlotId);
        if (timeSlot == null) {
            throw exception(TIME_SLOT_NOT_EXISTS);
        }

        // 3. 创建订单
        BookingOrderDO order = BookingOrderDO.builder()
                .orderNo(generateOrderNo())
                .userId(userId)
                .storeId(timeSlot.getStoreId())
                .technicianId(timeSlot.getTechnicianId())
                .timeSlotId(timeSlotId)
                .spuId(spuId)
                .skuId(skuId)
                .bookingDate(timeSlot.getSlotDate())
                .bookingStartTime(timeSlot.getStartTime())
                .bookingEndTime(timeSlot.getEndTime())
                .duration(timeSlot.getDuration())
                .isOffpeak(timeSlot.getIsOffpeak())
                .status(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus())
                .userRemark(userRemark)
                .dispatchMode(dispatchMode != null ? dispatchMode : DispatchModeEnum.DESIGNATED.getMode())
                .isAddon(0)
                .build();

        // 从商品服务获取价格信息
        Integer originalPrice = 0;
        String serviceName = "服务预约";
        String servicePic = null;
        if (skuId != null) {
            ProductSkuRespDTO sku = productSkuApi.getSku(skuId);
            if (sku != null) {
                originalPrice = sku.getPrice() != null ? sku.getPrice() : 0;
                servicePic = sku.getPicUrl();
            }
        }
        if (spuId != null) {
            ProductSpuRespDTO spu = productSpuApi.getSpu(spuId);
            if (spu != null) {
                serviceName = spu.getName() != null ? spu.getName() : serviceName;
                if (servicePic == null) {
                    servicePic = spu.getPicUrl();
                }
                // 如果SKU没有价格，使用SPU价格
                if (originalPrice == 0 && spu.getPrice() != null) {
                    originalPrice = spu.getPrice();
                }
            }
        }
        // 闲时优惠价格处理：兼容历史脏数据 offpeakPrice=0 的场景
        Integer payPrice = resolvePayPrice(timeSlot, originalPrice);
        Integer discountPrice = Math.max(originalPrice - payPrice, 0);
        order.setOriginalPrice(originalPrice);
        order.setDiscountPrice(discountPrice);
        order.setPayPrice(payPrice);
        order.setServiceName(serviceName);
        order.setServicePic(servicePic);

        bookingOrderMapper.insert(order);

        // 创建支付单
        if (payPrice > 0) {
            Long payOrderId = payOrderApi.createOrder(new PayOrderCreateReqDTO()
                    .setAppKey(PAY_APP_KEY).setUserIp("127.0.0.1")
                    .setUserId(userId).setUserType(UserTypeEnum.MEMBER.getValue())
                    .setMerchantOrderId(order.getId().toString())
                    .setSubject(serviceName).setBody("").setPrice(payPrice)
                    .setExpireTime(LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES)));
            BookingOrderDO payUpdate = new BookingOrderDO();
            payUpdate.setId(order.getId());
            payUpdate.setPayOrderId(payOrderId);
            bookingOrderMapper.updateById(payUpdate);
        }

        log.info("创建预约订单，orderId={}, orderNo={}, userId={}, timeSlotId={}",
                order.getId(), order.getOrderNo(), userId, timeSlotId);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long payOrderId) {
        BookingOrderDO order = validateOrderExists(orderId);
        if (!BookingOrderStatusEnum.PENDING_PAYMENT.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_ERROR);
        }

        BookingOrderDO update = buildStatusUpdate(orderId, BookingOrderStatusEnum.PAID);
        update.setPayOrderId(payOrderId);
        update.setPayTime(LocalDateTime.now());
        bookingOrderMapper.updateById(update);

        // 确认时间槽预约
        timeSlotService.confirmBooking(order.getTimeSlotId(), orderId);
        log.info("支付预约订单，orderId={}, payOrderId={}", orderId, payOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId, String cancelReason) {
        BookingOrderDO order = validateOrderExists(orderId);
        // 用户端调用时校验归属（userId不为null时校验）
        if (userId != null && !userId.equals(order.getUserId())) {
            throw exception(BOOKING_ORDER_NOT_OWNER);
        }
        // 只有待支付和已支付状态可以取消
        if (!BookingOrderStatusEnum.PENDING_PAYMENT.getStatus().equals(order.getStatus())
                && !BookingOrderStatusEnum.PAID.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_CANNOT_CANCEL);
        }

        BookingOrderDO update = buildStatusUpdate(orderId, BookingOrderStatusEnum.CANCELLED);
        update.setCancelTime(LocalDateTime.now());
        update.setCancelReason(cancelReason);
        bookingOrderMapper.updateById(update);

        // 取消时间槽预约
        timeSlotService.cancelBooking(order.getTimeSlotId());
        log.info("取消预约订单，orderId={}, userId={}, cancelReason={}", orderId, userId, cancelReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startService(Long orderId) {
        BookingOrderDO order = validateOrderExists(orderId);
        if (!BookingOrderStatusEnum.PAID.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_ERROR);
        }

        BookingOrderDO update = buildStatusUpdate(orderId, BookingOrderStatusEnum.IN_SERVICE);
        update.setServiceStartTime(LocalDateTime.now());
        bookingOrderMapper.updateById(update);
        log.info("开始服务，orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeService(Long orderId) {
        BookingOrderDO order = validateOrderExists(orderId);
        if (!BookingOrderStatusEnum.IN_SERVICE.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_ERROR);
        }

        BookingOrderDO update = buildStatusUpdate(orderId, BookingOrderStatusEnum.COMPLETED);
        update.setServiceEndTime(LocalDateTime.now());
        bookingOrderMapper.updateById(update);

        // 完成时间槽服务
        timeSlotService.completeService(order.getTimeSlotId());

        // 计算技师佣金
        technicianCommissionService.calculateCommission(orderId);

        log.info("完成服务，orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId) {
        BookingOrderDO order = validateOrderExists(orderId);
        if (!BookingOrderStatusEnum.PAID.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_ERROR);
        }

        BookingOrderDO update = buildStatusUpdate(orderId, BookingOrderStatusEnum.REFUNDED);
        bookingOrderMapper.updateById(update);

        // 取消时间槽预约
        timeSlotService.cancelBooking(order.getTimeSlotId());

        // 取消佣金记录
        technicianCommissionService.cancelCommission(orderId);

        // 创建退款单
        if (order.getPayOrderId() != null && order.getPayPrice() != null && order.getPayPrice() > 0) {
            payRefundApi.createRefund(new PayRefundCreateReqDTO()
                    .setAppKey(PAY_APP_KEY).setUserIp("127.0.0.1")
                    .setUserId(order.getUserId()).setUserType(UserTypeEnum.MEMBER.getValue())
                    .setMerchantOrderId(order.getId().toString())
                    .setMerchantRefundId(order.getId() + "-refund")
                    .setReason("预约订单退款").setPrice(order.getPayPrice()));
        }

        log.info("退款预约订单，orderId={}", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderPaid(Long id, Long payOrderId) {
        // 1. 校验订单存在
        BookingOrderDO order = validateOrderExists(id);
        // 2. 校验订单状态
        if (!BookingOrderStatusEnum.PENDING_PAYMENT.getStatus().equals(order.getStatus())) {
            // 重复回调：支付单号相同直接返回
            if (payOrderId.equals(order.getPayOrderId())) {
                log.warn("[updateOrderPaid][order({}) 已支付，且支付单号相同({})，直接返回]", id, payOrderId);
                return;
            }
            log.error("[updateOrderPaid][order({}) 支付单不匹配({})，请进行处理！]", id, payOrderId);
            throw exception(BOOKING_ORDER_STATUS_ERROR);
        }
        // 3. 校验支付单合法性
        PayOrderRespDTO payOrder = payOrderApi.getOrder(payOrderId);
        if (payOrder == null || !PayOrderStatusEnum.isSuccess(payOrder.getStatus())) {
            log.error("[updateOrderPaid][order({}) payOrder({}) 未支付或不存在]", id, payOrderId);
            throw exception(BOOKING_ORDER_NOT_PAID);
        }
        // 4. 更新订单状态
        payOrder(id, payOrderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderRefunded(Long id, Long payRefundId) {
        BookingOrderDO order = validateOrderExists(id);
        // 已退款则幂等返回
        if (BookingOrderStatusEnum.REFUNDED.getStatus().equals(order.getStatus())) {
            log.warn("[updateOrderRefunded][order({}) 已退款，直接返回]", id);
            return;
        }
        log.info("退款回调完成，orderId={}, payRefundId={}", id, payRefundId);
    }

    @Override
    public BookingOrderDO getOrderByUser(Long id, Long userId) {
        BookingOrderDO order = bookingOrderMapper.selectById(id);
        if (order == null) {
            return null;
        }
        // 校验归属
        if (!userId.equals(order.getUserId())) {
            throw exception(BOOKING_ORDER_NOT_OWNER);
        }
        return order;
    }

    @Override
    public BookingOrderDO getOrderByOrderNoAndUser(String orderNo, Long userId) {
        BookingOrderDO order = bookingOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return null;
        }
        // 校验归属
        if (!userId.equals(order.getUserId())) {
            throw exception(BOOKING_ORDER_NOT_OWNER);
        }
        return order;
    }

    @Override
    public BookingOrderDO getOrder(Long id) {
        return bookingOrderMapper.selectById(id);
    }

    @Override
    public BookingOrderDO getOrderByOrderNo(String orderNo) {
        return bookingOrderMapper.selectByOrderNo(orderNo);
    }

    @Override
    public BookingOrderDO getOrderByPayOrderId(Long payOrderId) {
        return bookingOrderMapper.selectByPayOrderId(payOrderId);
    }

    @Override
    public List<BookingOrderDO> getOrderListByUserId(Long userId) {
        return bookingOrderMapper.selectListByUserId(userId);
    }

    @Override
    public List<BookingOrderDO> getOrderListByUserIdAndStatus(Long userId, Integer status) {
        return bookingOrderMapper.selectListByUserIdAndStatus(userId, status);
    }

    @Override
    public List<BookingOrderDO> getOrderListByTechnicianAndDate(Long technicianId, LocalDate date) {
        return bookingOrderMapper.selectListByTechnicianIdAndDate(technicianId, date);
    }

    @Override
    public List<BookingOrderDO> getOrderListByStoreAndDate(Long storeId, LocalDate date) {
        return bookingOrderMapper.selectListByStoreIdAndDate(storeId, date);
    }

    @Override
    public List<BookingOrderDO> getPendingPaymentTimeoutOrders() {
        return bookingOrderMapper.selectListByStatus(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoCancelTimeoutOrders() {
        List<BookingOrderDO> orders = getPendingPaymentTimeoutOrders();
        int count = 0;
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        for (BookingOrderDO order : orders) {
            if (order.getCreateTime().isBefore(timeoutThreshold)) {
                try {
                    cancelOrder(order.getId(), null, "支付超时自动取消");
                    count++;
                } catch (Exception e) {
                    log.error("自动取消超时订单失败，orderId={}", order.getId(), e);
                }
            }
        }
        if (count > 0) {
            log.info("自动取消超时未支付订单，数量={}", count);
        }
        return count;
    }

    private BookingOrderDO validateOrderExists(Long id) {
        BookingOrderDO order = bookingOrderMapper.selectById(id);
        if (order == null) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private BookingOrderDO buildStatusUpdate(Long orderId, BookingOrderStatusEnum status) {
        BookingOrderDO update = new BookingOrderDO();
        update.setId(orderId);
        update.setStatus(status.getStatus());
        return update;
    }

    private Integer resolvePayPrice(TimeSlotDO timeSlot, Integer originalPrice) {
        if (!Boolean.TRUE.equals(timeSlot.getIsOffpeak()) || originalPrice <= 0) {
            return originalPrice;
        }
        Integer slotOffpeakPrice = timeSlot.getOffpeakPrice();
        if (isValidOffpeakPrice(slotOffpeakPrice, originalPrice)) {
            return slotOffpeakPrice;
        }
        if (timeSlot.getStoreId() == null || timeSlot.getSlotDate() == null || timeSlot.getStartTime() == null) {
            return originalPrice;
        }
        Integer weekDay = timeSlot.getSlotDate().getDayOfWeek().getValue();
        OffpeakRuleDO rule = offpeakRuleService.matchOffpeakRule(timeSlot.getStoreId(), weekDay, timeSlot.getStartTime());
        Integer recalculatedPrice = offpeakRuleService.calculateOffpeakPrice(rule, originalPrice);
        return isValidOffpeakPrice(recalculatedPrice, originalPrice) ? recalculatedPrice : originalPrice;
    }

    private boolean isValidOffpeakPrice(Integer offpeakPrice, Integer originalPrice) {
        return offpeakPrice != null && offpeakPrice > 0 && offpeakPrice <= originalPrice;
    }

    private String generateOrderNo() {
        return "BK" + IdUtil.getSnowflakeNextIdStr();
    }

}
