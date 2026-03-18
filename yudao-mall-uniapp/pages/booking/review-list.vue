<template>
  <s-layout title="我的评价">
    <view class="ss-p-24">
      <view class="summary-card bg-white ss-r-16 ss-p-24 ss-m-b-20">
        <view class="ss-flex ss-row-between ss-col-center ss-m-b-12">
          <view class="ss-font-30 ss-font-bold">评价概览</view>
          <view class="ss-font-24 text-gray">总计 {{ state.summary.totalCount || 0 }} 条</view>
        </view>
        <view class="ss-flex ss-row-between metric-row">
          <view>好评 {{ state.summary.positiveCount || 0 }}</view>
          <view>中评 {{ state.summary.neutralCount || 0 }}</view>
          <view>差评 {{ state.summary.negativeCount || 0 }}</view>
        </view>
      </view>

      <su-tabs :list="state.tabs" :scrollable="false" :current="state.currentTab" @change="onTabsChange" />

      <s-empty v-if="!state.loading && state.pagination.total === 0" icon="/static/data-empty.png" text="暂无评价" />

      <view
        v-for="item in state.pagination.list"
        :key="item.id"
        class="review-card bg-white ss-r-16 ss-p-24 ss-m-t-20"
        @tap="goReviewDetail(item.id)"
      >
        <view class="ss-flex ss-row-between ss-col-center ss-m-b-12">
          <view class="ss-font-28 ss-font-bold">{{ formatReviewLevel(item.reviewLevel) }}</view>
          <view class="ss-font-22 text-gray">{{ item.submitTime || '--' }}</view>
        </view>
        <view class="ss-font-24 ss-m-b-8">订单号：{{ item.bookingOrderId || '--' }}</view>
        <view class="ss-font-24 text-gray ss-m-b-8">总体评分：{{ item.overallScore || '--' }}/5</view>
        <view class="ss-font-24 ss-m-b-8">{{ item.content || '用户未填写文字评价' }}</view>
        <view v-if="item.replyContent" class="reply-box ss-m-t-12">回复：{{ item.replyContent }}</view>
        <view class="detail-entry ss-flex ss-row-right ss-m-t-12">
          <text>查看详情</text>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
import { reactive } from 'vue';
import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
import { concat } from 'lodash-es';
import sheep from '@/sheep';
import BookingReviewApi from '@/sheep/api/trade/review';
import { resetPagination } from '@/sheep/helper/utils';

const state = reactive({
  loading: false,
  currentTab: 0,
  tabs: [
    { name: '全部', value: undefined },
    { name: '好评', value: 1 },
    { name: '中评', value: 2 },
    { name: '差评', value: 3 },
  ],
  summary: {},
  pagination: {
    list: [],
    total: 0,
    pageNo: 1,
    pageSize: 10,
  },
  loadStatus: '',
});

function formatReviewLevel(level) {
  return {
    1: '好评',
    2: '中评',
    3: '差评',
  }[level] || '评价';
}

function goReviewDetail(id) {
  if (!id) {
    return;
  }
  sheep.$router.go('/pages/booking/review-detail', { id });
}

async function loadSummary() {
  const { code, data } = await BookingReviewApi.getSummary();
  if (code === 0) {
    state.summary = data || {};
  }
}

async function getList() {
  state.loading = true;
  state.loadStatus = 'loading';
  const { code, data } = await BookingReviewApi.getReviewPage({
    pageNo: state.pagination.pageNo,
    pageSize: state.pagination.pageSize,
    reviewLevel: state.tabs[state.currentTab].value,
  });
  if (code === 0) {
    state.pagination.list = concat(state.pagination.list, data?.list || []);
    state.pagination.total = data?.total || 0;
    state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
  }
  state.loading = false;
}

function onTabsChange(e) {
  state.currentTab = e.index;
  resetPagination(state.pagination);
  getList();
}

onLoad(async () => {
  await loadSummary();
  await getList();
});

onPullDownRefresh(async () => {
  resetPagination(state.pagination);
  await loadSummary();
  await getList();
  uni.stopPullDownRefresh();
});

onReachBottom(() => {
  if (state.loadStatus === 'noMore') {
    return;
  }
  state.pagination.pageNo += 1;
  getList();
});
</script>

<style lang="scss" scoped>
.summary-card,
.review-card {
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}
.metric-row {
  font-size: 24rpx;
}
.text-gray {
  color: #8c8c8c;
}
.reply-box {
  padding: 16rpx 20rpx;
  border-radius: 16rpx;
  background: #fff7e6;
  color: #ad6800;
  font-size: 24rpx;
}
.detail-entry {
  color: #ff6600;
  font-size: 24rpx;
}
</style>
