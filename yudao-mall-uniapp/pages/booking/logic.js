export async function loadTechnicianList(api, storeId) {
  return api.getTechnicianList(storeId);
}

export async function loadTechnicianDetail(api, technicianId) {
  return api.getTechnician(technicianId);
}

export async function loadTimeSlots(api, technicianId, date) {
  return api.getTimeSlots(technicianId, date);
}

export async function submitBookingOrder(api, payload) {
  return api.createOrder({
    ...payload,
    dispatchMode: 1,
  });
}

export async function cancelBookingOrder(api, id, reason = '用户主动取消') {
  return api.cancelOrder(id, reason);
}

export async function submitAddonOrder(api, payload) {
  return api.createAddonOrder(payload);
}

export function goToTechnicianDetail(router, id, storeId) {
  return router.go('/pages/booking/technician-detail', { id, storeId });
}

export function goToOrderConfirm(router, payload) {
  return router.go('/pages/booking/order-confirm', payload);
}

export function goToOrderDetail(router, id) {
  return router.go('/pages/booking/order-detail', { id });
}
