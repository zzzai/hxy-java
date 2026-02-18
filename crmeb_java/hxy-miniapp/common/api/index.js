/**
 * API接口统一管理
 */
import { get, post, put, del } from './request.js'

/**
 * 门店相关接口
 */
export const storeApi = {
  // 获取附近门店列表
  getNearbyStores: (data) => get('/store/nearby', data),
  
  // 获取门店详情
  getStoreDetail: (id) => get(`/store/detail/${id}`),
  
  // 获取门店服务列表
  getStoreServices: (storeId) => get('/store/services', { storeId })
}

/**
 * 预约相关接口
 */
export const bookingApi = {
  // 获取可预约时间
  getAvailableTimes: (data) => get('/booking/times', data),
  
  // 获取技师列表
  getTechnicians: (storeId) => get('/booking/technicians', { storeId }),
  
  // 创建预约订单
  createBooking: (data) => post('/booking/create', data),
  
  // 取消预约
  cancelBooking: (id) => post(`/booking/cancel/${id}`)
}

/**
 * 订单相关接口
 */
export const orderApi = {
  // 获取订单列表
  getOrderList: (data) => get('/order/list', data),
  
  // 获取订单详情
  getOrderDetail: (id) => get(`/order/detail/${id}`),
  
  // 订单支付
  payOrder: (id) => post(`/order/pay/${id}`),
  
  // 核销订单
  verifyOrder: (code) => post('/order/verify', { code })
}

/**
 * 会员相关接口
 */
export const memberApi = {
  // 获取会员信息
  getMemberInfo: () => get('/member/info'),
  
  // 获取会员权益
  getMemberBenefits: () => get('/member/benefits'),
  
  // 获取成长值记录
  getGrowthLog: (data) => get('/member/growth/log', data)
}

/**
 * 用户相关接口
 */
export const userApi = {
  // 微信登录
  wxLogin: (code) => post('/user/wx-login', { code }),
  
  // 获取用户信息
  getUserInfo: () => get('/user/info'),
  
  // 更新用户信息
  updateUserInfo: (data) => put('/user/info', data)
}

export default {
  storeApi,
  bookingApi,
  orderApi,
  memberApi,
  userApi
}

