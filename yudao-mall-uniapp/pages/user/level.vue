<template>
  <s-layout class="member-level-page" title="会员等级" navbar="inner">
    <view class="hero-card">
      <view class="hero-title">当前等级</view>
      <view class="hero-level">{{ currentLevelName }}</view>
      <view class="hero-meta">
        <text>当前经验 {{ userInfo.experience || 0 }}</text>
        <text v-if="nextLevel">，距 {{ nextLevel.name }} 还差 {{ nextLevelGap }}</text>
      </view>
      <view class="progress-track">
        <view class="progress-bar" :style="{ width: `${progressPercent}%` }" />
      </view>
    </view>

    <view class="section-card">
      <view class="section-title">等级权益</view>
      <view class="level-list">
        <view
          v-for="item in state.levelList"
          :key="`${item.level}-${item.name}`"
          class="level-item"
          :class="{ active: item.id && item.id === userInfo.level?.id }"
        >
          <view class="level-main">
            <view class="level-name">{{ item.name }}</view>
            <view class="level-desc">Lv.{{ item.level }} / {{ item.experience }} 经验解锁</view>
          </view>
          <view class="level-badge">{{ item.discountPercent || 100 }} 折</view>
        </view>
      </view>
      <s-empty v-if="!state.levelList.length" text="暂无等级配置" icon="/static/data-empty.png" />
    </view>

    <view class="section-card record-card">
      <view class="section-title">成长记录</view>
      <view v-if="state.pagination.total > 0">
        <view
          v-for="item in state.pagination.list"
          :key="`${item.createTime}-${item.title}-${item.experience}`"
          class="record-item"
        >
          <view class="record-main">
            <view class="record-title">{{ item.title }}</view>
            <view class="record-desc">{{ item.description || '经验变动记录' }}</view>
            <view class="record-time">{{
              sheep.$helper.timeFormat(item.createTime, 'yyyy-mm-dd hh:MM:ss')
            }}</view>
          </view>
          <view class="record-value" :class="{ minus: item.experience < 0 }">
            {{ item.experience > 0 ? `+${item.experience}` : item.experience }}
          </view>
        </view>
      </view>
      <s-empty v-else text="暂无成长记录" icon="/static/data-empty.png" />
      <uni-load-more
        v-if="state.pagination.total > 0"
        :status="state.loadStatus"
        :content-text="{ contentdown: '上拉加载更多' }"
        @tap="onLoadMore"
      />
    </view>
  </s-layout>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import { onLoad, onReachBottom } from '@dcloudio/uni-app';
  import { concat } from 'lodash-es';
  import sheep from '@/sheep';
  import MemberLevelApi from '@/sheep/api/member/level';
  import { resetPagination } from '@/sheep/helper/utils';

  const userInfo = computed(() => sheep.$store('user').userInfo || {});

  const state = reactive({
    levelList: [],
    pagination: {
      list: [],
      total: 0,
      pageNo: 1,
      pageSize: 10,
    },
    loadStatus: '',
  });

  const currentLevel = computed(() => {
    if (!state.levelList.length) {
      return null;
    }
    if (userInfo.value.level?.id) {
      return state.levelList.find((item) => item.id === userInfo.value.level.id) || null;
    }
    const currentExperience = userInfo.value.experience || 0;
    return (
      [...state.levelList]
        .sort((a, b) => (a.experience || 0) - (b.experience || 0))
        .filter((item) => (item.experience || 0) <= currentExperience)
        .pop() || null
    );
  });

  const nextLevel = computed(() => {
    if (!currentLevel.value) {
      return state.levelList[0] || null;
    }
    const currentLevelValue = currentLevel.value.level || 0;
    return (
      state.levelList
        .filter((item) => (item.level || 0) > currentLevelValue)
        .sort((a, b) => (a.level || 0) - (b.level || 0))[0] || null
    );
  });

  const currentLevelName = computed(() => userInfo.value.level?.name || currentLevel.value?.name || '普通会员');

  const nextLevelGap = computed(() => {
    if (!nextLevel.value) {
      return 0;
    }
    return Math.max((nextLevel.value.experience || 0) - (userInfo.value.experience || 0), 0);
  });

  const progressPercent = computed(() => {
    if (!currentLevel.value || !nextLevel.value) {
      return currentLevel.value ? 100 : 0;
    }
    const start = currentLevel.value.experience || 0;
    const end = nextLevel.value.experience || start;
    if (end <= start) {
      return 100;
    }
    const progress = ((userInfo.value.experience || 0) - start) / (end - start);
    return Math.min(Math.max(Math.round(progress * 100), 0), 100);
  });

  async function loadLevelList() {
    await sheep.$store('user').getInfo();
    const { code, data } = await MemberLevelApi.getLevelList();
    if (code !== 0) {
      return;
    }
    state.levelList = Array.isArray(data) ? data : [];
  }

  async function loadExperienceRecords() {
    state.loadStatus = 'loading';
    const { code, data } = await MemberLevelApi.getExperienceRecordPage({
      pageNo: state.pagination.pageNo,
      pageSize: state.pagination.pageSize,
    });
    if (code !== 0) {
      state.loadStatus = 'more';
      return;
    }
    state.pagination.list = concat(state.pagination.list, data.list || []);
    state.pagination.total = data.total || 0;
    state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
  }

  function onLoadMore() {
    if (state.loadStatus === 'noMore') {
      return;
    }
    state.pagination.pageNo += 1;
    loadExperienceRecords();
  }

  onLoad(async () => {
    resetPagination(state.pagination);
    await loadLevelList();
    await loadExperienceRecords();
  });

  onReachBottom(() => {
    onLoadMore();
  });
</script>

<style lang="scss" scoped>
  .member-level-page {
    background: #f7f7f7;
    min-height: 100vh;
  }

  .hero-card,
  .section-card {
    margin: 24rpx;
    padding: 28rpx;
    border-radius: 24rpx;
    background: #ffffff;
    box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.05);
  }

  .hero-card {
    background: linear-gradient(135deg, #1f7a8c 0%, #4d908e 100%);
    color: #ffffff;
  }

  .hero-title,
  .section-title {
    font-size: 26rpx;
    font-weight: 600;
  }

  .hero-level {
    margin-top: 18rpx;
    font-size: 52rpx;
    font-weight: 700;
  }

  .hero-meta {
    margin-top: 16rpx;
    font-size: 24rpx;
    opacity: 0.92;
  }

  .progress-track {
    margin-top: 24rpx;
    height: 14rpx;
    border-radius: 999rpx;
    background: rgba(255, 255, 255, 0.22);
    overflow: hidden;
  }

  .progress-bar {
    height: 100%;
    border-radius: 999rpx;
    background: #f4f1de;
  }

  .level-list {
    margin-top: 18rpx;
  }

  .level-item,
  .record-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 22rpx 0;
    border-bottom: 1rpx solid #f0f0f0;
  }

  .level-item:last-child,
  .record-item:last-child {
    border-bottom: none;
  }

  .level-item.active {
    background: rgba(77, 144, 142, 0.08);
    margin: 0 -12rpx;
    padding: 22rpx 12rpx;
    border-radius: 18rpx;
    border-bottom: none;
  }

  .level-name,
  .record-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #243b53;
  }

  .level-desc,
  .record-desc,
  .record-time {
    margin-top: 10rpx;
    font-size: 24rpx;
    color: #66788a;
  }

  .level-badge {
    padding: 10rpx 18rpx;
    border-radius: 999rpx;
    background: #f4f1de;
    color: #1f7a8c;
    font-size: 24rpx;
    font-weight: 600;
  }

  .record-value {
    min-width: 120rpx;
    text-align: right;
    font-size: 30rpx;
    font-weight: 700;
    color: #2a9d8f;
  }

  .record-value.minus {
    color: #c1121f;
  }

  .record-card {
    padding-bottom: 12rpx;
  }
</style>
