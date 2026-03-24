<template>
  <s-layout title="邀请有礼" navbar="inner">
    <view class="ss-p-24">
      <view class="summary-card ss-p-24 ss-r-16 bg-white ss-m-b-20">
        <view class="ss-flex ss-row-between ss-col-center ss-m-b-12">
          <view class="ss-font-32 ss-font-bold">邀请总览</view>
          <view class="code-tag">邀请码 {{ state.overview.referralCode || '--' }}</view>
        </view>
        <view class="metric-grid">
          <view class="metric-item">
            <view class="metric-value">{{ state.overview.totalInvites || 0 }}</view>
            <view class="metric-label">累计邀请</view>
          </view>
          <view class="metric-item">
            <view class="metric-value">{{ state.overview.effectiveInvites || 0 }}</view>
            <view class="metric-label">有效邀请</view>
          </view>
          <view class="metric-item">
            <view class="metric-value">¥{{ fen2yuan(state.overview.pendingRewardAmount) }}</view>
            <view class="metric-label">待到账</view>
          </view>
          <view class="metric-item">
            <view class="metric-value">¥{{ fen2yuan(state.overview.rewardBalance) }}</view>
            <view class="metric-label">奖励余额</view>
          </view>
        </view>
      </view>

      <view class="bind-card ss-p-24 ss-r-16 bg-white ss-m-b-20">
        <view class="ss-font-30 ss-font-bold ss-m-b-16">绑定邀请人</view>
        <uni-easyinput
          v-model="state.bindForm.inviterMemberId"
          type="number"
          placeholder="请输入邀请人会员 ID"
        />
        <button class="bind-btn" :disabled="state.binding" @tap="onBindInviter">
          {{ state.binding ? '绑定中...' : '立即绑定' }}
        </button>
      </view>

      <view class="ledger-card ss-p-24 ss-r-16 bg-white">
        <view class="ss-flex ss-row-between ss-col-center ss-m-b-16">
          <view class="ss-font-30 ss-font-bold">奖励台账</view>
          <view class="ss-font-24 text-gray">共 {{ state.pagination.total || 0 }} 条</view>
        </view>
        <s-empty
          v-if="!state.loading && state.pagination.total === 0"
          icon="/static/data-empty.png"
          text="暂无奖励台账"
        />
        <view v-for="item in state.pagination.list" :key="item.ledgerId" class="ledger-item">
          <view class="ss-flex ss-row-between ss-col-center ss-m-b-8">
            <view class="ss-font-26 ss-font-bold">订单 {{ item.orderId || '--' }}</view>
            <view class="status-tag">{{ formatStatus(item.status) }}</view>
          </view>
          <view class="ss-font-24 text-gray ss-m-b-6">来源业务号：{{ item.sourceBizNo || '--' }}</view>
          <view class="ss-font-24">奖励金额：¥{{ fen2yuan(item.rewardAmount) }}</view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
  import ReferralApi from '@/sheep/api/promotion/referral';

  const state = reactive({
    loading: false,
    binding: false,
    overview: {},
    bindForm: {
      inviterMemberId: '',
    },
    pagination: {
      list: [],
      total: 0,
      pageNo: 1,
      pageSize: 10,
    },
    loadStatus: '',
  });

  function fen2yuan(value) {
    return ((Number(value) || 0) / 100).toFixed(2);
  }

  function formatStatus(status) {
    return {
      0: '待结算',
      1: '已结算',
      2: '已取消',
    }[status] || '处理中';
  }

  function resetPagination() {
    state.pagination.list = [];
    state.pagination.total = 0;
    state.pagination.pageNo = 1;
    state.loadStatus = '';
  }

  async function getOverview() {
    const { code, data } = await ReferralApi.getOverview();
    if (code === 0) {
      state.overview = data || {};
    }
  }

  async function getRewardLedgerPage() {
    state.loading = true;
    const { code, data } = await ReferralApi.getRewardLedgerPage({
      pageNo: state.pagination.pageNo,
      pageSize: state.pagination.pageSize,
      status: 'SETTLED',
    });
    if (code === 0) {
      const rows = data?.list || [];
      state.pagination.list = state.pagination.list.concat(rows);
      state.pagination.total = data?.total || 0;
      state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
    }
    state.loading = false;
  }

  async function onBindInviter() {
    if (!state.bindForm.inviterMemberId) {
      uni.showToast({ title: '请先输入邀请人 ID', icon: 'none' });
      return;
    }
    state.binding = true;
    const { code, data } = await ReferralApi.bindInviter({
      inviterMemberId: Number(state.bindForm.inviterMemberId),
      clientToken: `ref-bind-${Date.now()}`,
    });
    state.binding = false;
    if (code !== 0) {
      return;
    }
    uni.showToast({
      title: data?.bindStatus === 'BOUND' ? '绑定成功' : '已处理',
      icon: 'none',
    });
    await getOverview();
    resetPagination();
    await getRewardLedgerPage();
  }

  onLoad(async () => {
    await getOverview();
    await getRewardLedgerPage();
  });

  onPullDownRefresh(async () => {
    resetPagination();
    await getOverview();
    await getRewardLedgerPage();
    uni.stopPullDownRefresh();
  });

  onReachBottom(async () => {
    if (state.loadStatus === 'noMore') {
      return;
    }
    state.pagination.pageNo += 1;
    await getRewardLedgerPage();
  });
</script>

<style scoped lang="scss">
  .summary-card,
  .bind-card,
  .ledger-card {
    box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.06);
  }

  .code-tag {
    padding: 8rpx 18rpx;
    border-radius: 999rpx;
    background: #fff7e6;
    color: #ad6800;
    font-size: 22rpx;
  }

  .metric-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18rpx;
  }

  .metric-item {
    padding: 20rpx;
    border-radius: 18rpx;
    background: linear-gradient(135deg, #fff7e6 0%, #fff1b8 100%);
  }

  .metric-value {
    font-size: 34rpx;
    font-weight: 700;
    color: #ad6800;
  }

  .metric-label,
  .text-gray {
    color: #8c8c8c;
    font-size: 24rpx;
  }

  .bind-btn {
    margin-top: 20rpx;
    background: #f59e0b;
    color: #fff;
    border-radius: 999rpx;
  }

  .ledger-item {
    padding: 18rpx 0;
    border-top: 1rpx solid #f0f0f0;
  }

  .ledger-item:first-of-type {
    border-top: none;
    padding-top: 0;
  }

  .status-tag {
    font-size: 22rpx;
    color: #2563eb;
  }
</style>
