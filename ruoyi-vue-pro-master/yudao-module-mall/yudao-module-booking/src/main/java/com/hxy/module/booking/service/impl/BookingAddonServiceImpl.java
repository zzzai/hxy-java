package com.hxy.module.booking.service.impl;

import cn.hutool.core.util.IdUtil;
import com.hxy.module.booking.dal.dataobject.BookingOrderDO;
import com.hxy.module.booking.dal.dataobject.TimeSlotDO;
import com.hxy.module.booking.dal.mysql.BookingOrderMapper;
import com.hxy.module.booking.enums.AddonTypeEnum;
import com.hxy.module.booking.enums.BookingOrderStatusEnum;
import com.hxy.module.booking.service.BookingAddonService;
import com.hxy.module.booking.service.TimeSlotService;
import cn.iocoder.yudao.module.product.api.sku.ProductSkuApi;
import cn.iocoder.yudao.module.product.api.sku.dto.ProductSkuRespDTO;
import cn.iocoder.yudao.module.product.api.spu.ProductSpuApi;
import cn.iocoder.yudao.module.product.api.spu.dto.ProductSpuRespDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.*;

@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class BookingAddonServiceImpl implements BookingAddonService {

    private final BookingOrderMapper bookingOrderMapper;
    private final TimeSlotService timeSlotService;
    private final ProductSpuApi productSpuApi;
    private final ProductSkuApi productSkuApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExtendOrder(Long parentOrderId, Long userId, Long spuId, Long skuId) {
        BookingOrderDO parentOrder = validateParentOrder(parentOrderId, userId);

        // 查找当前技师下一个可用时间槽
        TimeSlotDO nextSlot = findNextAvailableSlot(
                parentOrder.getTechnicianId(), parentOrder.getBookingDate(), parentOrder.getBookingEndTime());
        if (nextSlot == null) {
            throw exception(TIME_SLOT_NOT_AVAILABLE);
        }

        // 锁定时间槽
        boolean locked = timeSlotService.lockTimeSlot(nextSlot.getId(), userId);
        if (!locked) {
            throw exception(TIME_SLOT_NOT_AVAILABLE);
        }

        // 获取价格
        Integer price = resolveSkuPrice(spuId, skuId);
        String serviceName = resolveServiceName(spuId, "加钟服务");

        BookingOrderDO addonOrder = buildAddonOrder(parentOrder, nextSlot, spuId, skuId,
                serviceName, price, AddonTypeEnum.EXTEND);
        bookingOrderMapper.insert(addonOrder);

        log.info("创建加钟订单，addonOrderId={}, parentOrderId={}, userId={}",
                addonOrder.getId(), parentOrderId, userId);
        return addonOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUpgradeOrder(Long parentOrderId, Long userId, Long newSkuId) {
        BookingOrderDO parentOrder = validateParentOrder(parentOrderId, userId);

        // 获取新SKU价格
        ProductSkuRespDTO newSku = productSkuApi.getSku(newSkuId);
        if (newSku == null) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        Integer newPrice = newSku.getPrice() != null ? newSku.getPrice() : 0;
        // 差价计算：新价格 - 原价格，最低为0
        Integer diffPrice = Math.max(newPrice - parentOrder.getPayPrice(), 0);

        BookingOrderDO addonOrder = BookingOrderDO.builder()
                .orderNo("BK" + IdUtil.getSnowflakeNextIdStr())
                .userId(userId)
                .storeId(parentOrder.getStoreId())
                .technicianId(parentOrder.getTechnicianId())
                .timeSlotId(parentOrder.getTimeSlotId())
                .spuId(parentOrder.getSpuId())
                .skuId(newSkuId)
                .serviceName("升级服务")
                .bookingDate(parentOrder.getBookingDate())
                .bookingStartTime(parentOrder.getBookingStartTime())
                .bookingEndTime(parentOrder.getBookingEndTime())
                .duration(parentOrder.getDuration())
                .originalPrice(newPrice)
                .discountPrice(0)
                .payPrice(diffPrice)
                .isOffpeak(parentOrder.getIsOffpeak())
                .status(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus())
                .parentOrderId(parentOrderId)
                .isAddon(1)
                .addonType(AddonTypeEnum.UPGRADE.getType())
                .dispatchMode(parentOrder.getDispatchMode())
                .build();
        bookingOrderMapper.insert(addonOrder);

        log.info("创建升级订单，addonOrderId={}, parentOrderId={}, diffPrice={}",
                addonOrder.getId(), parentOrderId, diffPrice);
        return addonOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAddItemOrder(Long parentOrderId, Long userId, Long addonSpuId, Long addonSkuId) {
        BookingOrderDO parentOrder = validateParentOrder(parentOrderId, userId);

        Integer price = resolveSkuPrice(addonSpuId, addonSkuId);
        String serviceName = resolveServiceName(addonSpuId, "加项目");

        BookingOrderDO addonOrder = BookingOrderDO.builder()
                .orderNo("BK" + IdUtil.getSnowflakeNextIdStr())
                .userId(userId)
                .storeId(parentOrder.getStoreId())
                .technicianId(parentOrder.getTechnicianId())
                .timeSlotId(parentOrder.getTimeSlotId())
                .spuId(addonSpuId)
                .skuId(addonSkuId)
                .serviceName(serviceName)
                .bookingDate(parentOrder.getBookingDate())
                .bookingStartTime(parentOrder.getBookingStartTime())
                .bookingEndTime(parentOrder.getBookingEndTime())
                .duration(parentOrder.getDuration())
                .originalPrice(price)
                .discountPrice(0)
                .payPrice(price)
                .isOffpeak(parentOrder.getIsOffpeak())
                .status(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus())
                .parentOrderId(parentOrderId)
                .isAddon(1)
                .addonType(AddonTypeEnum.ADD_ITEM.getType())
                .dispatchMode(parentOrder.getDispatchMode())
                .build();
        bookingOrderMapper.insert(addonOrder);

        log.info("创建加项目订单，addonOrderId={}, parentOrderId={}", addonOrder.getId(), parentOrderId);
        return addonOrder.getId();
    }

    private BookingOrderDO validateParentOrder(Long parentOrderId, Long userId) {
        BookingOrderDO order = bookingOrderMapper.selectById(parentOrderId);
        if (order == null) {
            throw exception(BOOKING_ORDER_NOT_EXISTS);
        }
        if (!userId.equals(order.getUserId())) {
            throw exception(BOOKING_ORDER_NOT_OWNER);
        }
        // 只有服务中的订单可以加钟/升级/加项目
        if (!BookingOrderStatusEnum.IN_SERVICE.getStatus().equals(order.getStatus())) {
            throw exception(BOOKING_ORDER_STATUS_ERROR);
        }
        return order;
    }

    private TimeSlotDO findNextAvailableSlot(Long technicianId, LocalDate date, LocalTime afterTime) {
        List<TimeSlotDO> slots = timeSlotService.getTimeSlotsByTechnicianAndDate(technicianId, date);
        return slots.stream()
                .filter(s -> s.getStartTime().equals(afterTime)
                        && com.hxy.module.booking.enums.TimeSlotStatusEnum.AVAILABLE.getStatus().equals(s.getStatus()))
                .findFirst()
                .orElse(null);
    }

    private BookingOrderDO buildAddonOrder(BookingOrderDO parent, TimeSlotDO slot,
                                           Long spuId, Long skuId, String serviceName,
                                           Integer price, AddonTypeEnum addonType) {
        return BookingOrderDO.builder()
                .orderNo("BK" + IdUtil.getSnowflakeNextIdStr())
                .userId(parent.getUserId())
                .storeId(parent.getStoreId())
                .technicianId(parent.getTechnicianId())
                .timeSlotId(slot.getId())
                .spuId(spuId)
                .skuId(skuId)
                .serviceName(serviceName)
                .bookingDate(slot.getSlotDate())
                .bookingStartTime(slot.getStartTime())
                .bookingEndTime(slot.getEndTime())
                .duration(slot.getDuration())
                .originalPrice(price)
                .discountPrice(0)
                .payPrice(price)
                .isOffpeak(slot.getIsOffpeak())
                .status(BookingOrderStatusEnum.PENDING_PAYMENT.getStatus())
                .parentOrderId(parent.getId())
                .isAddon(1)
                .addonType(addonType.getType())
                .dispatchMode(parent.getDispatchMode())
                .build();
    }

    private Integer resolveSkuPrice(Long spuId, Long skuId) {
        if (skuId != null) {
            ProductSkuRespDTO sku = productSkuApi.getSku(skuId);
            if (sku != null && sku.getPrice() != null && sku.getPrice() > 0) {
                return sku.getPrice();
            }
        }
        if (spuId != null) {
            ProductSpuRespDTO spu = productSpuApi.getSpu(spuId);
            if (spu != null && spu.getPrice() != null) {
                return spu.getPrice();
            }
        }
        return 0;
    }

    private String resolveServiceName(Long spuId, String defaultName) {
        if (spuId != null) {
            ProductSpuRespDTO spu = productSpuApi.getSpu(spuId);
            if (spu != null && spu.getName() != null) {
                return spu.getName();
            }
        }
        return defaultName;
    }

}
