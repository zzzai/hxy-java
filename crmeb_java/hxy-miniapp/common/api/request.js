/**
 * API请求封装
 * 基于uni.request封装，统一处理请求和响应
 */

const BASE_URL = 'http://localhost:8081/api/front'

// 请求拦截器
const requestInterceptor = (config) => {
  // 添加token
  const token = uni.getStorageSync('token')
  if (token) {
    config.header = {
      ...config.header,
      'Authorization': `Bearer ${token}`
    }
  }
  
  // 添加时间戳防止缓存
  if (config.method === 'GET') {
    config.data = {
      ...config.data,
      _t: Date.now()
    }
  }
  
  return config
}

// 响应拦截器
const responseInterceptor = (response) => {
  const { statusCode, data } = response
  
  // HTTP状态码检查
  if (statusCode !== 200) {
    uni.showToast({
      title: '网络请求失败',
      icon: 'none'
    })
    return Promise.reject(response)
  }
  
  // 业务状态码检查
  if (data.code !== 200) {
    // 401未登录
    if (data.code === 401) {
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
      uni.reLaunch({
        url: '/pages/login/index'
      })
      return Promise.reject(data)
    }
    
    // 其他错误
    uni.showToast({
      title: data.message || '请求失败',
      icon: 'none'
    })
    return Promise.reject(data)
  }
  
  return data.data
}

/**
 * 统一请求方法
 */
const request = (options) => {
  // 默认配置
  const defaultOptions = {
    url: BASE_URL + options.url,
    method: options.method || 'GET',
    data: options.data || {},
    header: {
      'Content-Type': 'application/json',
      ...options.header
    },
    timeout: 30000
  }
  
  // 请求拦截
  const config = requestInterceptor(defaultOptions)
  
  // 显示loading
  if (options.loading !== false) {
    uni.showLoading({
      title: '加载中...',
      mask: true
    })
  }
  
  return new Promise((resolve, reject) => {
    uni.request({
      ...config,
      success: (res) => {
        uni.hideLoading()
        responseInterceptor(res)
          .then(data => resolve(data))
          .catch(err => reject(err))
      },
      fail: (err) => {
        uni.hideLoading()
        uni.showToast({
          title: '网络连接失败',
          icon: 'none'
        })
        reject(err)
      }
    })
  })
}

/**
 * GET请求
 */
export const get = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'GET',
    data,
    ...options
  })
}

/**
 * POST请求
 */
export const post = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'POST',
    data,
    ...options
  })
}

/**
 * PUT请求
 */
export const put = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'PUT',
    data,
    ...options
  })
}

/**
 * DELETE请求
 */
export const del = (url, data = {}, options = {}) => {
  return request({
    url,
    method: 'DELETE',
    data,
    ...options
  })
}

export default {
  get,
  post,
  put,
  del
}

