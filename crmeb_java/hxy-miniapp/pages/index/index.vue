<template>
  <view class="index-page">
    <!-- 顶部定位栏 -->
    <view class="location-bar">
      <view class="location-info" @tap="showStoreSelector">
        <text class="location-icon">📍</text>
        <text class="location-text">{{ currentStore.name || '选择门店' }}</text>
        <text class="location-distance" v-if="currentStore.distance">{{ formatDistance(currentStore.distance) }}</text>
        <text class="arrow">▼</text>
      </view>
    </view>

    <!-- 门店列表 -->
    <scroll-view 
      class="store-list" 
      scroll-y 
      refresher-enabled 
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="store-section">
        <view class="section-title">附近门店</view>
        
        <!-- 门店卡片 -->
        <view 
          v-for="store in storeList" 
          :key="store.id"
          class="store-card"
          @tap="selectStore(store)"
        >
          <view class="store-header">
            <view class="store-info">
              <text class="store-name">{{ store.name }}</text>
              <view class="store-tags">
                <text class="tag" v-if="store.isOpen">营业中</text>
                <text class="tag tag-female" v-if="store.isFemaleFriendly">女性友好</text>
              </view>
            </view>
            <view class="store-rating">
              <text class="rating-score">⭐ {{ store.rating }}</text>
            </view>
          </view>
          
          <view class="store-body">
            <text class="store-address">📍 {{ store.address }}</text>
            <text class="store-distance">{{ formatDistance(store.distance) }}</text>
          </view>
          
          <view class="store-footer">
            <text class="store-hours">⏰ {{ store.businessHours }}</text>
            <text class="store-phone" @tap.stop="callPhone(store.phone)">📞 {{ store.phone }}</text>
          </view>
          
          <view class="store-actions">
            <hxy-button text="查看详情" type="outline" size="small" @click.stop="viewStoreDetail(store)"></hxy-button>
            <hxy-button text="立即预约" type="primary" size="small" @click.stop="goBooking(store)"></hxy-button>
          </view>
        </view>
        
        <!-- 空状态 -->
        <hxy-empty 
          v-if="!loading && storeList.length === 0"
          icon="🏪"
          text="附近暂无门店"
          :showButton="true"
          buttonText="刷新"
          @click="loadStores"
        ></hxy-empty>
        
        <!-- 加载中 -->
        <view v-if="loading" class="loading">
          <text>加载中...</text>
        </view>
      </view>
      
      <!-- 热门服务 -->
      <view class="service-section" v-if="recommendServices.length > 0">
        <view class="section-title">热门服务</view>
        <scroll-view class="service-scroll" scroll-x>
          <view 
            v-for="service in recommendServices" 
            :key="service.id"
            class="service-card"
            @tap="goServiceDetail(service)"
          >
            <image class="service-image" :src="service.image" mode="aspectFill"></image>
            <view class="service-info">
              <text class="service-name">{{ service.name }}</text>
              <text class="service-desc">{{ service.duration }}分钟</text>
              <view class="service-price">
                <text class="price-current">¥{{ service.price }}</text>
                <text class="price-original" v-if="service.originalPrice">¥{{ service.originalPrice }}</text>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>
    </scroll-view>
    
    <!-- 门店选择弹窗 -->
    <view v-if="showSelector" class="store-selector" @tap="hideStoreSelector">
      <view class="selector-content" @tap.stop>
        <view class="selector-header">
          <text class="selector-title">选择门店</text>
          <text class="selector-close" @tap="hideStoreSelector">✕</text>
        </view>
        <scroll-view class="selector-list" scroll-y>
          <view 
            v-for="store in storeList" 
            :key="store.id"
            class="selector-item"
            :class="{ active: store.id === currentStore.id }"
            @tap="selectStore(store)"
          >
            <view class="item-info">
              <text class="item-name">{{ store.name }}</text>
              <text class="item-address">{{ store.address }}</text>
              <text class="item-distance">{{ formatDistance(store.distance) }}</text>
            </view>
            <text v-if="store.id === currentStore.id" class="item-check">✓</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { storeApi } from '@/common/api'
import { getDistanceText, setStorage, getStorage } from '@/common/utils'

export default {
  name: 'IndexPage',
  data() {
    return {
      // 当前门店
      currentStore: {},
      // 门店列表
      storeList: [],
      // 推荐服务
      recommendServices: [],
      // 加载状态
      loading: false,
      refreshing: false,
      // 显示门店选择器
      showSelector: false,
      // 用户位置
      userLocation: {
        latitude: 0,
        longitude: 0
      }
    }
  },
  
  onLoad() {
    this.init()
  },
  
  onShow() {
    // 每次显示页面时刷新数据
    this.loadStores()
  },
  
  methods: {
    /**
     * 初始化
     */
    async init() {
      // 获取用户位置
      await this.getUserLocation()
      
      // 加载门店列表
      await this.loadStores()
      
      // 加载推荐服务
      this.loadRecommendServices()
    },
    
    /**
     * 获取用户位置
     */
    getUserLocation() {
      return new Promise((resolve) => {
        uni.getLocation({
          type: 'gcj02',
          success: (res) => {
            this.userLocation = {
              latitude: res.latitude,
              longitude: res.longitude
            }
            resolve()
          },
          fail: () => {
            uni.showToast({
              title: '定位失败，使用默认位置',
              icon: 'none'
            })
            // 使用默认位置（上海）
            this.userLocation = {
              latitude: 31.230416,
              longitude: 121.473701
            }
            resolve()
          }
        })
      })
    },
    
    /**
     * 加载门店列表
     */
    async loadStores() {
      this.loading = true
      
      try {
        const data = await storeApi.getNearbyStores({
          latitude: this.userLocation.latitude,
          longitude: this.userLocation.longitude,
          radius: 5000 // 5公里范围
        })
        
        this.storeList = data || []
        
        // 如果没有选择门店，默认选择第一个
        if (!this.currentStore.id && this.storeList.length > 0) {
          this.currentStore = this.storeList[0]
          setStorage('currentStore', this.currentStore)
        }
      } catch (error) {
        console.error('加载门店失败', error)
        uni.showToast({
          title: '加载失败，请重试',
          icon: 'none'
        })
      } finally {
        this.loading = false
        this.refreshing = false
      }
    },
    
    /**
     * 加载推荐服务
     */
    async loadRecommendServices() {
      if (!this.currentStore.id) return
      
      try {
        const data = await storeApi.getStoreServices(this.currentStore.id)
        // 取前6个热门服务
        this.recommendServices = (data || []).slice(0, 6)
      } catch (error) {
        console.error('加载推荐服务失败', error)
      }
    },
    
    /**
     * 下拉刷新
     */
    onRefresh() {
      this.refreshing = true
      this.loadStores()
    },
    
    /**
     * 显示门店选择器
     */
    showStoreSelector() {
      this.showSelector = true
    },
    
    /**
     * 隐藏门店选择器
     */
    hideStoreSelector() {
      this.showSelector = false
    },
    
    /**
     * 选择门店
     */
    selectStore(store) {
      this.currentStore = store
      setStorage('currentStore', store)
      this.hideStoreSelector()
      
      // 重新加载推荐服务
      this.loadRecommendServices()
      
      uni.showToast({
        title: `已切换到${store.name}`,
        icon: 'success'
      })
    },
    
    /**
     * 查看门店详情
     */
    viewStoreDetail(store) {
      uni.navigateTo({
        url: `/pages/store/detail?id=${store.id}`
      })
    },
    
    /**
     * 去预约
     */
    goBooking(store) {
      this.selectStore(store)
      uni.switchTab({
        url: '/pages/booking/index'
      })
    },
    
    /**
     * 查看服务详情
     */
    goServiceDetail(service) {
      uni.navigateTo({
        url: `/pages/service/detail?id=${service.id}`
      })
    },
    
    /**
     * 拨打电话
     */
    callPhone(phone) {
      uni.makePhoneCall({
        phoneNumber: phone
      })
    },
    
    /**
     * 格式化距离
     */
    formatDistance(distance) {
      return getDistanceText(distance)
    }
  }
}
</script>

<style scoped>
.index-page {
  min-height: 100vh;
  background: #F8F8F8;
}

/* 定位栏 */
.location-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #FFFFFF;
  padding: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.location-info {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #F5F5F5;
  border-radius: 8rpx;
}

.location-icon {
  font-size: 32rpx;
  margin-right: 12rpx;
}

.location-text {
  flex: 1;
  font-size: 28rpx;
  font-weight: bold;
  color: #333333;
}

.location-distance {
  font-size: 24rpx;
  color: #999999;
  margin-right: 12rpx;
}

.arrow {
  font-size: 20rpx;
  color: #999999;
}

/* 门店列表 */
.store-list {
  height: calc(100vh - 100rpx);
}

.store-section {
  padding: 20rpx;
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333333;
  margin-bottom: 20rpx;
}

/* 门店卡片 */
.store-card {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.store-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.store-info {
  flex: 1;
}

.store-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #333333;
  display: block;
  margin-bottom: 8rpx;
}

.store-tags {
  display: flex;
  gap: 8rpx;
}

.tag {
  padding: 4rpx 12rpx;
  border-radius: 4rpx;
  font-size: 20rpx;
  background: rgba(76, 175, 80, 0.1);
  color: #4CAF50;
}

.tag-female {
  background: rgba(255, 152, 0, 0.1);
  color: #FF9800;
}

.store-rating {
  display: flex;
  align-items: center;
}

.rating-score {
  font-size: 24rpx;
  color: #FF9800;
}

.store-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.store-address {
  flex: 1;
  font-size: 24rpx;
  color: #666666;
}

.store-distance {
  font-size: 24rpx;
  color: #4CAF50;
  margin-left: 16rpx;
}

.store-footer {
  display: flex;
  justify-content: space-between;
  padding-bottom: 16rpx;
  margin-bottom: 16rpx;
  border-bottom: 1rpx solid #F0F0F0;
}

.store-hours,
.store-phone {
  font-size: 24rpx;
  color: #999999;
}

.store-actions {
  display: flex;
  gap: 16rpx;
}

/* 热门服务 */
.service-section {
  padding: 20rpx;
  background: #FFFFFF;
  margin-top: 20rpx;
}

.service-scroll {
  white-space: nowrap;
}

.service-card {
  display: inline-block;
  width: 280rpx;
  margin-right: 20rpx;
  background: #F8F8F8;
  border-radius: 12rpx;
  overflow: hidden;
}

.service-image {
  width: 100%;
  height: 200rpx;
}

.service-info {
  padding: 16rpx;
}

.service-name {
  font-size: 28rpx;
  font-weight: bold;
  color: #333333;
  display: block;
  margin-bottom: 8rpx;
}

.service-desc {
  font-size: 24rpx;
  color: #999999;
  display: block;
  margin-bottom: 12rpx;
}

.service-price {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}

.price-current {
  font-size: 32rpx;
  font-weight: bold;
  color: #4CAF50;
}

.price-original {
  font-size: 24rpx;
  color: #999999;
  text-decoration: line-through;
}

/* 门店选择器 */
.store-selector {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.selector-content {
  width: 100%;
  max-height: 80vh;
  background: #FFFFFF;
  border-radius: 24rpx 24rpx 0 0;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx;
  border-bottom: 1rpx solid #F0F0F0;
}

.selector-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333333;
}

.selector-close {
  font-size: 40rpx;
  color: #999999;
}

.selector-list {
  max-height: 60vh;
}

.selector-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 32rpx;
  border-bottom: 1rpx solid #F5F5F5;
}

.selector-item.active {
  background: rgba(76, 175, 80, 0.05);
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 28rpx;
  font-weight: bold;
  color: #333333;
  display: block;
  margin-bottom: 8rpx;
}

.item-address {
  font-size: 24rpx;
  color: #666666;
  display: block;
  margin-bottom: 4rpx;
}

.item-distance {
  font-size: 24rpx;
  color: #4CAF50;
}

.item-check {
  font-size: 40rpx;
  color: #4CAF50;
}

/* 加载中 */
.loading {
  padding: 40rpx 0;
  text-align: center;
  color: #999999;
}
</style>


