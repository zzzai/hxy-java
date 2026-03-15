<!-- 个人中心：支持装修 -->
<template>
  <s-layout
    title="我的"
    tabbar="/pages/index/user"
    navbar="custom"
    :bgStyle="templatePage"
    :navbarStyle="templateNavbar"
    onShareAppMessage
  >
    <s-block v-for="(item, index) in templateComponents" :key="index" :styles="item.property.style">
      <s-block-item :type="item.id" :data="item.property" :styles="item.property.style" />
    </s-block>
    <view v-if="templateComponents.length === 0" class="empty-placeholder">
      <view class="empty-title">个人页模板未配置</view>
      <view class="empty-desc">请在管理后台完成个人中心装修后刷新小程序</view>
    </view>
  </s-layout>
</template>

<script setup>
  import { computed } from 'vue';
  import { onShow, onPageScroll, onPullDownRefresh } from '@dcloudio/uni-app';
  import sheep from '@/sheep';

  // 隐藏原生tabBar
  uni.hideTabBar({
    fail: () => {},
  });

  const template = computed(() => sheep.$store('app').template?.user || {});
  const templateComponents = computed(() =>
    Array.isArray(template.value?.components) ? template.value.components : [],
  );
  const templatePage = computed(() => template.value?.page || {});
  const templateNavbar = computed(() => template.value?.navigationBar || {});

  onShow(() => {
    sheep.$store('user').updateUserData();
  });

  onPullDownRefresh(() => {
    sheep.$store('user').updateUserData();
    setTimeout(function () {
      uni.stopPullDownRefresh();
    }, 800);
  });

  onPageScroll(() => {});
</script>

<style scoped lang="scss">
  .empty-placeholder {
    margin: 80rpx 24rpx 0;
    padding: 28rpx 24rpx;
    border-radius: 16rpx;
    background: #ffffff;
    text-align: center;
    box-shadow: 0 8rpx 28rpx rgba(0, 0, 0, 0.06);
  }

  .empty-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #303133;
  }

  .empty-desc {
    margin-top: 12rpx;
    font-size: 24rpx;
    color: #606266;
  }
</style>
