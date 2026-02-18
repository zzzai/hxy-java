# UniApp前端开发指南

> **基于CRMEB足疗预约系统后端API**

---

## 📋 项目概述

### 技术栈
- **框架**: UniApp
- **UI库**: uView UI 2.0
- **状态管理**: Vuex
- **HTTP库**: uni.request
- **开发工具**: HBuilderX

### 页面结构
```
pages/
├── index/                    # 首页
│   └── index.vue
├── technician/               # 技师模块
│   ├── list.vue             # 技师列表
│   └── detail.vue           # 技师详情
├── booking/                  # 预约模块
│   ├── calendar.vue         # 排班日历
│   ├── timeslot.vue         # 时间槽选择
│   └── confirm.vue          # 确认预约
├── order/                    # 订单模块
│   ├── list.vue             # 订单列表
│   ├── detail.vue           # 订单详情
│   └── verify.vue           # 核销页面
├── member/                   # 会员模块
│   ├── card-list.vue        # 会员卡列表
│   └── card-detail.vue      # 会员卡详情
└── user/                     # 用户模块
    ├── index.vue            # 个人中心
    └── login.vue            # 登录页面
```

---

## 🚀 快速开始

### 1. 创建项目

```bash
# 使用HBuilderX创建UniApp项目
# 或使用命令行
vue create -p dcloudio/uni-preset-vue crmeb-booking-uniapp

cd crmeb-booking-uniapp
```

### 2. 安装依赖

```bash
# 安装uView UI
npm install uview-ui@2.0.36

# 安装其他依赖
npm install dayjs
npm install @escook/request-miniprogram
```

### 3. 配置uView UI

**main.js**:
```javascript
import Vue from 'vue'
import App from './App'
import uView from 'uview-ui'

Vue.use(uView)

Vue.config.productionTip = false

App.mpType = 'app'

const app = new Vue({
    ...App
})
app.$mount()
```

**App.vue**:
```vue
<style lang="scss">
@import "uview-ui/index.scss";
</style>
```

**uni.scss**:
```scss
@import "uview-ui/theme.scss";
```

### 4. 配置API基础地址

**config/api.js**:
```javascript
// 开发环境
const DEV_BASE_URL = 'http://localhost:8080'

// 生产环境
const PROD_BASE_URL = 'https://api.your-domain.com'

export const BASE_URL = process.env.NODE_ENV === 'development' 
  ? DEV_BASE_URL 
  : PROD_BASE_URL

export const API = {
  // 技师管理
  TECHNICIAN_LIST: '/api/admin/technician/list',
  TECHNICIAN_DETAIL: '/api/admin/technician/detail',
  
  // 排班管理
  SCHEDULE_STORE: '/api/admin/schedule/store',
  SCHEDULE_TECHNICIAN: '/api/admin/schedule/technician',
  
  // 预约管理
  BOOKING_BOOK: '/api/admin/booking/book',
  BOOKING_CONFIRM: '/api/admin/booking/confirm',
  
  // 订单管理
  ORDER_CREATE: '/api/admin/booking-order/create',
  ORDER_PAY: '/api/admin/booking-order/pay',
  ORDER_DETAIL: '/api/admin/booking-order/detail',
  ORDER_VERIFY: '/api/admin/booking-order/verify',
  
  // 会员卡管理
  MEMBER_CARD_LIST: '/api/admin/member-card/list',
  MEMBER_CARD_DETAIL: '/api/admin/member-card/detail',
  MEMBER_CARD_USE: '/api/admin/member-card/use-times'
}
```

---

## 📱 页面开发

### 1. 技师列表页

**pages/technician/list.vue**:
```vue
<template>
  <view class="technician-list">
    <u-navbar title="选择技师" :border="false"></u-navbar>
    
    <!-- 搜索栏 -->
    <view class="search-bar">
      <u-search 
        v-model="keyword" 
        placeholder="搜索技师姓名"
        @search="handleSearch"
      ></u-search>
    </view>
    
    <!-- 技师列表 -->
    <view class="list-container">
      <view 
        class="technician-item" 
        v-for="item in technicianList" 
        :key="item.id"
        @click="handleSelect(item)"
      >
        <image class="avatar" :src="item.avatar" mode="aspectFill"></image>
        <view class="info">
          <view class="name-row">
            <text class="name">{{ item.name }}</text>
            <u-tag 
              :text="getLevelText(item.level)" 
              type="warning" 
              size="mini"
            ></u-tag>
          </view>
          <view class="tags">
            <text 
              class="tag" 
              v-for="tag in item.skillTags.split(',')" 
              :key="tag"
            >{{ tag }}</text>
          </view>
          <view class="stats">
            <text class="rating">⭐ {{ item.rating }}</text>
            <text class="orders">已服务{{ item.orderCount }}单</text>
          </view>
        </view>
        <u-icon name="arrow-right" color="#999"></u-icon>
      </view>
    </view>
    
    <!-- 加载更多 -->
    <u-loadmore 
      :status="loadStatus" 
      @loadmore="loadMore"
    ></u-loadmore>
  </view>
</template>

<script>
import { getTechnicianList } from '@/api/technician'

export default {
  data() {
    return {
      storeId: 1,
      keyword: '',
      technicianList: [],
      page: 1,
      pageSize: 10,
      loadStatus: 'loadmore'
    }
  },
  
  onLoad() {
    this.loadTechnicianList()
  },
  
  methods: {
    async loadTechnicianList() {
      try {
        this.loadStatus = 'loading'
        
        const res = await getTechnicianList({
          storeId: this.storeId,
          keyword: this.keyword,
          page: this.page,
          pageSize: this.pageSize
        })
        
        if (res.code === 200) {
          if (this.page === 1) {
            this.technicianList = res.data
          } else {
            this.technicianList.push(...res.data)
          }
          
          this.loadStatus = res.data.length < this.pageSize ? 'nomore' : 'loadmore'
        }
      } catch (error) {
        this.loadStatus = 'loadmore'
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      }
    },
    
    handleSearch() {
      this.page = 1
      this.technicianList = []
      this.loadTechnicianList()
    },
    
    loadMore() {
      if (this.loadStatus === 'nomore') return
      this.page++
      this.loadTechnicianList()
    },
    
    handleSelect(item) {
      uni.navigateTo({
        url: `/pages/booking/calendar?technicianId=${item.id}`
      })
    },
    
    getLevelText(level) {
      const levelMap = {
        1: '初级',
        2: '中级',
        3: '高级',
        4: '首席'
      }
      return levelMap[level] || '未知'
    }
  }
}
</script>

<style lang="scss" scoped>
.technician-list {
  min-height: 100vh;
  background: #f5f5f5;
}

.search-bar {
  padding: 20rpx;
  background: #fff;
}

.list-container {
  padding: 20rpx;
}

.technician-item {
  display: flex;
  align-items: center;
  padding: 30rpx;
  margin-bottom: 20rpx;
  background: #fff;
  border-radius: 16rpx;
  
  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    margin-right: 20rpx;
  }
  
  .info {
    flex: 1;
    
    .name-row {
      display: flex;
      align-items: center;
      margin-bottom: 10rpx;
      
      .name {
        font-size: 32rpx;
        font-weight: bold;
        margin-right: 10rpx;
      }
    }
    
    .tags {
      margin-bottom: 10rpx;
      
      .tag {
        display: inline-block;
        padding: 4rpx 12rpx;
        margin-right: 10rpx;
        font-size: 24rpx;
        color: #666;
        background: #f0f0f0;
        border-radius: 4rpx;
      }
    }
    
    .stats {
      font-size: 24rpx;
      color: #999;
      
      .rating {
        margin-right: 20rpx;
      }
    }
  }
}
</style>
```

### 2. 排班日历页

**pages/booking/calendar.vue**:
```vue
<template>
  <view class="booking-calendar">
    <u-navbar title="选择日期" :border="false"></u-navbar>
    
    <!-- 日历 -->
    <view class="calendar-container">
      <u-calendar 
        v-model="show" 
        :mode="mode"
        :min-date="minDate"
        :max-date="maxDate"
        @confirm="handleDateConfirm"
      ></u-calendar>
    </view>
    
    <!-- 选中日期 -->
    <view class="selected-date" v-if="selectedDate">
      <text>已选择：{{ selectedDate }}</text>
    </view>
    
    <!-- 时间槽列表 -->
    <view class="timeslot-container" v-if="timeSlots.length > 0">
      <view class="section-title">选择时间</view>
      
      <view class="timeslot-grid">
        <view 
          class="timeslot-item"
          :class="{
            'disabled': slot.status !== 1,
            'selected': selectedSlot && selectedSlot.slot_id === slot.slot_id,
            'offpeak': slot.is_offpeak === 1
          }"
          v-for="slot in timeSlots" 
          :key="slot.slot_id"
          @click="handleSlotSelect(slot)"
        >
          <view class="time">{{ slot.start_time }}</view>
          <view class="price">
            <text v-if="slot.is_offpeak === 1" class="offpeak-price">
              ¥{{ slot.offpeak_price }}
            </text>
            <text v-else>¥{{ slot.price }}</text>
          </view>
          <view class="status" v-if="slot.status !== 1">
            {{ getStatusText(slot.status) }}
          </view>
        </view>
      </view>
    </view>
    
    <!-- 底部按钮 -->
    <view class="bottom-bar">
      <view class="price-info">
        <text class="label">合计：</text>
        <text class="price">¥{{ totalPrice }}</text>
      </view>
      <u-button 
        type="primary" 
        :disabled="!selectedSlot"
        @click="handleConfirm"
      >确认预约</u-button>
    </view>
  </view>
</template>

<script>
import { getSchedule } from '@/api/schedule'
import dayjs from 'dayjs'

export default {
  data() {
    return {
      technicianId: 0,
      show: true,
      mode: 'date',
      minDate: dayjs().format('YYYY-MM-DD'),
      maxDate: dayjs().add(30, 'day').format('YYYY-MM-DD'),
      selectedDate: '',
      timeSlots: [],
      selectedSlot: null,
      scheduleId: 0
    }
  },
  
  computed: {
    totalPrice() {
      if (!this.selectedSlot) return 0
      return this.selectedSlot.is_offpeak === 1 
        ? this.selectedSlot.offpeak_price 
        : this.selectedSlot.price
    }
  },
  
  onLoad(options) {
    this.technicianId = options.technicianId
  },
  
  methods: {
    async handleDateConfirm(date) {
      this.selectedDate = dayjs(date).format('YYYY-MM-DD')
      this.show = false
      await this.loadTimeSlots()
    },
    
    async loadTimeSlots() {
      try {
        uni.showLoading({ title: '加载中...' })
        
        const res = await getSchedule({
          technicianId: this.technicianId,
          workDate: this.selectedDate
        })
        
        if (res.code === 200 && res.data) {
          this.scheduleId = res.data.id
          const timeSlotsData = JSON.parse(res.data.timeSlots)
          this.timeSlots = timeSlotsData.slots
        } else {
          uni.showToast({
            title: '该日期暂无排班',
            icon: 'none'
          })
        }
      } catch (error) {
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    },
    
    handleSlotSelect(slot) {
      if (slot.status !== 1) {
        uni.showToast({
          title: '该时段不可预约',
          icon: 'none'
        })
        return
      }
      
      this.selectedSlot = slot
    },
    
    handleConfirm() {
      if (!this.selectedSlot) {
        uni.showToast({
          title: '请选择时间',
          icon: 'none'
        })
        return
      }
      
      uni.navigateTo({
        url: `/pages/booking/confirm?scheduleId=${this.scheduleId}&slotId=${this.selectedSlot.slot_id}&price=${this.totalPrice}`
      })
    },
    
    getStatusText(status) {
      const statusMap = {
        2: '已锁定',
        3: '已预约',
        4: '已完成'
      }
      return statusMap[status] || '不可用'
    }
  }
}
</script>

<style lang="scss" scoped>
.booking-calendar {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 120rpx;
}

.calendar-container {
  background: #fff;
  padding: 20rpx;
}

.selected-date {
  padding: 20rpx;
  text-align: center;
  font-size: 28rpx;
  color: #666;
}

.timeslot-container {
  padding: 20rpx;
  
  .section-title {
    font-size: 32rpx;
    font-weight: bold;
    margin-bottom: 20rpx;
  }
}

.timeslot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20rpx;
}

.timeslot-item {
  padding: 20rpx;
  background: #fff;
  border-radius: 12rpx;
  text-align: center;
  border: 2rpx solid #e5e5e5;
  
  &.selected {
    border-color: #ff6b6b;
    background: #fff5f5;
  }
  
  &.disabled {
    opacity: 0.5;
    background: #f5f5f5;
  }
  
  &.offpeak {
    border-color: #ffa500;
    
    .offpeak-price {
      color: #ff6b6b;
      font-weight: bold;
    }
  }
  
  .time {
    font-size: 28rpx;
    margin-bottom: 10rpx;
  }
  
  .price {
    font-size: 32rpx;
    font-weight: bold;
    color: #333;
  }
  
  .status {
    font-size: 24rpx;
    color: #999;
    margin-top: 10rpx;
  }
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx;
  background: #fff;
  box-shadow: 0 -2rpx 10rpx rgba(0,0,0,0.1);
  
  .price-info {
    .label {
      font-size: 28rpx;
      color: #666;
    }
    
    .price {
      font-size: 40rpx;
      font-weight: bold;
      color: #ff6b6b;
    }
  }
}
</style>
```

---

## 🔌 API封装

### api/request.js

```javascript
import { BASE_URL } from '@/config/api'

class Request {
  constructor() {
    this.baseURL = BASE_URL
    this.timeout = 30000
  }
  
  request(options) {
    return new Promise((resolve, reject) => {
      uni.request({
        url: this.baseURL + options.url,
        method: options.method || 'GET',
        data: options.data || {},
        header: {
          'Content-Type': 'application/json',
          'Authorization': uni.getStorageSync('token') || ''
        },
        timeout: this.timeout,
        success: (res) => {
          if (res.statusCode === 200) {
            resolve(res.data)
          } else {
            reject(res)
          }
        },
        fail: (err) => {
          reject(err)
        }
      })
    })
  }
  
  get(url, data) {
    return this.request({
      url,
      method: 'GET',
      data
    })
  }
  
  post(url, data) {
    return this.request({
      url,
      method: 'POST',
      data
    })
  }
}

export default new Request()
```

### api/technician.js

```javascript
import request from './request'
import { API } from '@/config/api'

export function getTechnicianList(params) {
  return request.get(API.TECHNICIAN_LIST, params)
}

export function getTechnicianDetail(id) {
  return request.get(`${API.TECHNICIAN_DETAIL}/${id}`)
}
```

### api/schedule.js

```javascript
import request from './request'
import { API } from '@/config/api'

export function getSchedule(params) {
  return request.get(API.SCHEDULE_TECHNICIAN, params)
}

export function getStoreSchedules(params) {
  return request.get(API.SCHEDULE_STORE, params)
}
```

### api/booking.js

```javascript
import request from './request'
import { API } from '@/config/api'

export function bookTimeSlot(data) {
  return request.post(API.BOOKING_BOOK, data)
}

export function confirmBooking(data) {
  return request.post(API.BOOKING_CONFIRM, data)
}
```

### api/order.js

```javascript
import request from './request'
import { API } from '@/config/api'

export function createOrder(data) {
  return request.post(API.ORDER_CREATE, data)
}

export function payOrder(data) {
  return request.post(API.ORDER_PAY, data)
}

export function getOrderDetail(orderNo) {
  return request.get(API.ORDER_DETAIL, { orderNo })
}

export function verifyOrder(data) {
  return request.post(API.ORDER_VERIFY, data)
}
```

---

## 📝 开发清单

### Week 2: UniApp前端开发

**Day 1-2: 基础搭建**
- [ ] 创建UniApp项目
- [ ] 安装uView UI
- [ ] 配置API基础地址
- [ ] 封装HTTP请求
- [ ] 创建页面结构

**Day 3-4: 核心页面**
- [ ] 技师列表页
- [ ] 排班日历页
- [ ] 时间槽选择页
- [ ] 确认预约页

**Day 5-6: 订单模块**
- [ ] 订单列表页
- [ ] 订单详情页
- [ ] 支付页面
- [ ] 核销页面

**Day 7: 会员模块**
- [ ] 会员卡列表
- [ ] 会员卡详情
- [ ] 个人中心

---

## 🎨 UI设计规范

### 颜色规范
```scss
$primary-color: #ff6b6b;
$success-color: #52c41a;
$warning-color: #faad14;
$error-color: #f5222d;
$text-color: #333;
$text-secondary: #666;
$text-disabled: #999;
$border-color: #e5e5e5;
$bg-color: #f5f5f5;
```

### 字体规范
```scss
$font-size-xs: 24rpx;
$font-size-sm: 28rpx;
$font-size-base: 32rpx;
$font-size-lg: 36rpx;
$font-size-xl: 40rpx;
```

### 间距规范
```scss
$spacing-xs: 10rpx;
$spacing-sm: 20rpx;
$spacing-base: 30rpx;
$spacing-lg: 40rpx;
$spacing-xl: 50rpx;
```

---

完整代码示例请参考项目仓库。


