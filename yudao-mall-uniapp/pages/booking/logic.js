export async function loadTechnicianList(api, storeId) {
  return api.getTechnicianList(storeId);
}

export async function loadTechnicianDetail(api, technicianId) {
  return api.getTechnician(technicianId);
}

export async function loadTimeSlots(api, technicianId, date) {
  return api.getTimeSlots(technicianId, date);
}

export async function loadTimeSlotDetail(api, timeSlotId) {
  return api.getTimeSlot(timeSlotId);
}

export async function loadReviewEligibility(api, bookingOrderId) {
  return api.getEligibility(bookingOrderId);
}

function hasValue(value) {
  return value !== undefined && value !== null && value !== '';
}

function hasExplicitProductSource(payload = {}) {
  return hasValue(payload.spuId) && hasValue(payload.skuId);
}

export async function submitBookingOrder(api, payload) {
  return api.createOrder({
    ...payload,
    dispatchMode: 1,
  });
}

export async function submitBookingOrderAndGo(api, router, payload) {
  const result = await submitBookingOrder(api, payload);
  if (result.code === 0) {
    goToOrderDetail(router, result.data);
  }
  return result;
}

export async function cancelBookingOrder(api, id, reason = '用户主动取消') {
  return api.cancelOrder(id, reason);
}

export async function cancelBookingOrderAndRefresh(api, id, onSuccess, reason = '用户主动取消') {
  const result = await cancelBookingOrder(api, id, reason);
  if (result.code === 0 && onSuccess) {
    await onSuccess();
  }
  return result;
}

export async function submitAddonOrder(api, payload) {
  return api.createAddonOrder(payload);
}

export async function submitAddonOrderAndGo(api, router, payload) {
  const result = await submitAddonOrder(api, payload);
  if (result.code === 0) {
    goToOrderDetail(router, result.data);
  }
  return result;
}

export function goToTechnicianDetail(router, id, storeId) {
  return router.go('/pages/booking/technician-detail', { id, storeId });
}

export function goToBookingServiceSelect(router, payload) {
  return router.go('/pages/booking/service-select', payload);
}

export function goToOrderConfirm(router, payload) {
  return router.go('/pages/booking/order-confirm', payload);
}

export function goToOrderDetail(router, id) {
  return router.go('/pages/booking/order-detail', { id });
}

export function goToReviewAdd(router, bookingOrderId) {
  return router.go('/pages/booking/review-add', { bookingOrderId });
}

export function buildBookingCreatePayload(payload = {}) {
  if (!hasValue(payload.timeSlotId) || !hasExplicitProductSource(payload)) {
    return null;
  }

  return {
    timeSlotId: payload.timeSlotId,
    spuId: payload.spuId,
    skuId: payload.skuId,
    userRemark: payload.userRemark,
  };
}

export function buildBookingAddonPayload(payload = {}) {
  if (!hasValue(payload.parentOrderId) || !hasValue(payload.addonType)) {
    return null;
  }

  if (payload.addonType === 1) {
    const spuId = hasValue(payload.spuId) ? payload.spuId : payload.parentOrderSpuId;
    const skuId = hasValue(payload.skuId) ? payload.skuId : payload.parentOrderSkuId;
    if (!hasValue(spuId) || !hasValue(skuId)) {
      return null;
    }
    return {
      parentOrderId: payload.parentOrderId,
      addonType: payload.addonType,
      spuId,
      skuId,
    };
  }

  if (!hasExplicitProductSource(payload)) {
    return null;
  }

  return {
    parentOrderId: payload.parentOrderId,
    addonType: payload.addonType,
    spuId: payload.spuId,
    skuId: payload.skuId,
  };
}
