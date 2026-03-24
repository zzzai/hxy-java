<template>
  <s-layout class="member-tag-page" title="我的标签" navbar="inner">
    <view class="intro-card">
      <view class="intro-title">用户标签中心</view>
      <view class="intro-desc">
        这些标签来自后台会员治理能力，用于帮助门店与运营识别用户偏好和服务阶段。
      </view>
      <view class="intro-count">当前共 {{ state.tags.length }} 个标签</view>
    </view>

    <view class="tag-card">
      <view v-if="state.tags.length" class="tag-list">
        <view v-for="item in state.tags" :key="item.id" class="tag-item">
          {{ item.name }}
        </view>
      </view>
      <s-empty v-else text="当前还没有会员标签" icon="/static/data-empty.png" />
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import MemberTagApi from '@/sheep/api/member/tag';

  const state = reactive({
    tags: [],
  });

  async function loadTags() {
    const { code, data } = await MemberTagApi.getMyTags();
    if (code !== 0) {
      return;
    }
    state.tags = Array.isArray(data) ? data : [];
  }

  onLoad(() => {
    loadTags();
  });
</script>

<style lang="scss" scoped>
  .member-tag-page {
    background: #f7f7f7;
    min-height: 100vh;
  }

  .intro-card,
  .tag-card {
    margin: 24rpx;
    padding: 28rpx;
    border-radius: 24rpx;
    background: #ffffff;
    box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.05);
  }

  .intro-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #243b53;
  }

  .intro-desc,
  .intro-count {
    margin-top: 16rpx;
    font-size: 24rpx;
    color: #6b7c93;
    line-height: 1.7;
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
  }

  .tag-item {
    padding: 14rpx 22rpx;
    border-radius: 999rpx;
    background: #edf6f9;
    color: #1d6f73;
    font-size: 26rpx;
    font-weight: 600;
  }
</style>
