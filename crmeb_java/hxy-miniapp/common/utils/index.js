/**
 * 工具函数库
 */

/**
 * 日期格式化
 * @param {Date|String|Number} date 日期
 * @param {String} format 格式 yyyy-MM-dd HH:mm:ss
 */
export const formatDate = (date, format = 'yyyy-MM-dd HH:mm:ss') => {
  if (!date) return ''
  
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hour = String(d.getHours()).padStart(2, '0')
  const minute = String(d.getMinutes()).padStart(2, '0')
  const second = String(d.getSeconds()).padStart(2, '0')
  
  return format
    .replace('yyyy', year)
    .replace('MM', month)
    .replace('dd', day)
    .replace('HH', hour)
    .replace('mm', minute)
    .replace('ss', second)
}

/**
 * 价格格式化
 * @param {Number} price 价格
 */
export const formatPrice = (price) => {
  if (!price && price !== 0) return '0.00'
  return Number(price).toFixed(2)
}

/**
 * 手机号脱敏
 * @param {String} phone 手机号
 */
export const maskPhone = (phone) => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

/**
 * 防抖函数
 * @param {Function} fn 函数
 * @param {Number} delay 延迟时间
 */
export const debounce = (fn, delay = 500) => {
  let timer = null
  return function(...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 * @param {Function} fn 函数
 * @param {Number} delay 延迟时间
 */
export const throttle = (fn, delay = 500) => {
  let lastTime = 0
  return function(...args) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      fn.apply(this, args)
      lastTime = now
    }
  }
}

/**
 * 获取距离文本
 * @param {Number} distance 距离（米）
 */
export const getDistanceText = (distance) => {
  if (!distance) return ''
  if (distance < 1000) {
    return `${Math.round(distance)}m`
  }
  return `${(distance / 1000).toFixed(1)}km`
}

/**
 * 获取星期文本
 * @param {Date|String} date 日期
 */
export const getWeekText = (date) => {
  const weekMap = ['日', '一', '二', '三', '四', '五', '六']
  const d = new Date(date)
  return `周${weekMap[d.getDay()]}`
}

/**
 * 判断是否是今天
 * @param {Date|String} date 日期
 */
export const isToday = (date) => {
  const d = new Date(date)
  const today = new Date()
  return d.toDateString() === today.toDateString()
}

/**
 * 判断是否是明天
 * @param {Date|String} date 日期
 */
export const isTomorrow = (date) => {
  const d = new Date(date)
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  return d.toDateString() === tomorrow.toDateString()
}

/**
 * 获取日期显示文本
 * @param {Date|String} date 日期
 */
export const getDateText = (date) => {
  if (isToday(date)) return '今天'
  if (isTomorrow(date)) return '明天'
  return formatDate(date, 'MM-dd')
}

/**
 * 深拷贝
 * @param {Object} obj 对象
 */
export const deepClone = (obj) => {
  if (obj === null || typeof obj !== 'object') return obj
  if (obj instanceof Date) return new Date(obj)
  if (obj instanceof Array) return obj.map(item => deepClone(item))
  
  const cloneObj = {}
  for (let key in obj) {
    if (obj.hasOwnProperty(key)) {
      cloneObj[key] = deepClone(obj[key])
    }
  }
  return cloneObj
}

/**
 * 存储数据
 * @param {String} key 键
 * @param {Any} value 值
 */
export const setStorage = (key, value) => {
  try {
    uni.setStorageSync(key, JSON.stringify(value))
    return true
  } catch (e) {
    console.error('setStorage error:', e)
    return false
  }
}

/**
 * 获取数据
 * @param {String} key 键
 */
export const getStorage = (key) => {
  try {
    const value = uni.getStorageSync(key)
    return value ? JSON.parse(value) : null
  } catch (e) {
    console.error('getStorage error:', e)
    return null
  }
}

/**
 * 删除数据
 * @param {String} key 键
 */
export const removeStorage = (key) => {
  try {
    uni.removeStorageSync(key)
    return true
  } catch (e) {
    console.error('removeStorage error:', e)
    return false
  }
}

export default {
  formatDate,
  formatPrice,
  maskPhone,
  debounce,
  throttle,
  getDistanceText,
  getWeekText,
  isToday,
  isTomorrow,
  getDateText,
  deepClone,
  setStorage,
  getStorage,
  removeStorage
}

