import request from '@/sheep/request';

const BookingApi = {
  // 获取门店技师列表
  getTechnicianList: (storeId) => {
    return request({
      url: '/booking/technician/list',
      method: 'GET',
      params: { storeId },
      custom: { showLoading: true },
    });
  },
  // 获取技师详情
  getTechnician: (id) => {
    return request({
      url: '/booking/technician/get',
      method: 'GET',
      params: { id },
    });
  },
  // 获取技师可预约时间段
  getTimeSlots: (technicianId, date) => {
    return request({
      url: '/booking/slot/list-by-technician',
      method: 'GET',
      params: { technicianId, date },
      custom: { showLoading: false },
    });
  },
  // 获取时间槽详情
  getTimeSlot: (id) => {
    return request({
      url: '/booking/slot/get',
      method: 'GET',
      params: { id },
      custom: { showLoading: false },
    });
  },
  // 创建预约订单
  createOrder: (data) => {
    return request({
      url: '/booking/order/create',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: true },
    });
  },
  // 获取预约订单详情
  getOrderDetail: (id) => {
    return request({
      url: '/booking/order/get',
      method: 'GET',
      params: { id },
      custom: { showLoading: false },
    });
  },
  // 获取我的预约列表
  getOrderList: (params) => {
    return request({
      url: '/booking/order/list',
      method: 'GET',
      params,
      custom: { showLoading: false, auth: true },
    });
  },
  // 取消预约订单
  cancelOrder: (id, cancelReason) => {
    return request({
      url: '/booking/order/cancel',
      method: 'POST',
      params: { id, reason: cancelReason },
      custom: { auth: true },
    });
  },
  // 创建加钟/升级订单
  createAddonOrder: (data) => {
    return request({
      url: '/app-api/booking/addon/create',
      method: 'POST',
      data,
      custom: { auth: true, showLoading: true },
    });
  },
};

export default BookingApi;
