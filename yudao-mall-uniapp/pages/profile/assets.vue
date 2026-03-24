<template>
  <s-layout class="member-assets-page" title="资产总览" navbar="inner">
    <view class="summary-card">
      <view class="summary-title">我的资产</view>
      <view class="summary-grid">
        <button class="ss-reset-button summary-item" @tap="goWallet">
          <view class="summary-label">钱包余额</view>
          <view class="summary-value">{{ fen2yuan(state.summary.balance || 0) }}</view>
        </button>
        <button class="ss-reset-button summary-item" @tap="goPoint">
          <view class="summary-label">当前积分</view>
          <view class="summary-value">{{ state.summary.point || 0 }}</view>
        </button>
        <button class="ss-reset-button summary-item" @tap="goCoupon">
          <view class="summary-label">可用优惠券</view>
          <view class="summary-value">{{ state.summary.unusedCouponCount || 0 }}</view>
        </button>
      </view>
    </view>

    <view v-if="state.degraded" class="warning-card">
      台账处于降级展示状态{{ state.degradeReason ? `：${state.degradeReason}` : '' }}
    </view>

    <view class="ledger-card">
      <view class="ledger-header">
        <view class="section-title">统一资产台账</view>
        <view class="section-subtitle">仅作会员侧观察与追溯，不代表发布放量结论</view>
      </view>
      <su-tabs
        :list="tabMaps"
        :scrollable="false"
        :current="state.currentTab"
        @change="onTabChange"
      />

      <view v-if="state.pagination.total > 0" class="ledger-list">
        <view
          v-for="item in state.pagination.list"
          :key="`${item.assetType}-${item.ledgerId}-${item.runId}`"
          class="ledger-item"
        >
          <view class="ledger-main">
            <view class="ledger-top">
              <text class="ledger-type">{{ formatAssetType(item.assetType) }}</text>
              <text class="ledger-title">{{ item.title || '资产记录' }}</text>
            </view>
            <view class="ledger-desc">{{ item.description || '统一资产台账聚合记录' }}</view>
            <view class="ledger-meta">
              <text>{{ sheep.$helper.timeFormat(item.createTime, 'yyyy-mm-dd hh:MM:ss') }}</text>
              <text v-if="item.sourceBizNo">业务号 {{ item.sourceBizNo }}</text>
            </view>
          </view>
          <view class="ledger-side">
            <view class="ledger-amount" :class="{ minus: Number(item.amount) < 0 }">
              {{ formatAmount(item) }}
            </view>
            <view class="ledger-balance">余额 {{ formatBalance(item) }}</view>
          </view>
        </view>
      </view>
      <s-empty v-else text="暂无资产记录" icon="/static/data-empty.png" />

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
  import { reactive } from 'vue';
  import { onLoad, onReachBottom } from '@dcloudio/uni-app';
  import { concat } from 'lodash-es';
  import sheep from '@/sheep';
  import UserApi from '@/sheep/api/member/user';
  import MemberAssetApi from '@/sheep/api/member/asset';
  import PayWalletApi from '@/sheep/api/pay/wallet';
  import CouponApi from '@/sheep/api/promotion/coupon';
  import { fen2yuan } from '@/sheep/hooks/useGoods';
  import { resetPagination } from '@/sheep/helper/utils';

  const state = reactive({
    currentTab: 0,
    summary: {
      balance: 0,
      point: 0,
      unusedCouponCount: 0,
    },
    degraded: false,
    degradeReason: '',
    pagination: {
      list: [],
      total: 0,
      pageNo: 1,
      pageSize: 10,
    },
    loadStatus: '',
  });

  const tabMaps = [
    { name: '全部', value: '' },
    { name: '钱包', value: 'WALLET' },
    { name: '积分', value: 'POINT' },
    { name: '优惠券', value: 'COUPON' },
  ];

  async function loadSummary() {
    const [userResult, walletResult, couponResult] = await Promise.all([
      UserApi.getUserInfo(),
      PayWalletApi.getPayWallet(),
      CouponApi.getUnusedCouponCount(),
    ]);
    if (userResult.code === 0) {
      state.summary.point = userResult.data?.point || 0;
    }
    if (walletResult.code === 0) {
      state.summary.balance = walletResult.data?.balance || 0;
    }
    if (couponResult.code === 0) {
      state.summary.unusedCouponCount = couponResult.data || 0;
    }
  }

  async function loadLedger() {
    state.loadStatus = 'loading';
    const params = {
      pageNo: state.pagination.pageNo,
      pageSize: state.pagination.pageSize,
    };
    const assetType = tabMaps[state.currentTab].value;
    if (assetType) {
      params.assetType = assetType;
    }
    const { code, data } = await MemberAssetApi.getAssetLedgerPage(params);
    if (code !== 0) {
      state.loadStatus = 'more';
      return;
    }
    state.degraded = !!data.degraded;
    state.degradeReason = data.degradeReason || '';
    state.pagination.list = concat(state.pagination.list, data.list || []);
    state.pagination.total = data.total || 0;
    state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
  }

  function onTabChange(e) {
    state.currentTab = e.index;
    resetPagination(state.pagination);
    loadLedger();
  }

  function onLoadMore() {
    if (state.loadStatus === 'noMore') {
      return;
    }
    state.pagination.pageNo += 1;
    loadLedger();
  }

  function formatAssetType(assetType) {
    return {
      WALLET: '钱包',
      POINT: '积分',
      COUPON: '优惠券',
    }[assetType] || '资产';
  }

  function formatAmount(item) {
    if (item.amount === null || item.amount === undefined) {
      return '--';
    }
    if (item.assetType === 'WALLET' || item.assetType === 'COUPON') {
      const amount = fen2yuan(Math.abs(item.amount));
      return Number(item.amount) >= 0 ? `+${amount}` : `-${amount}`;
    }
    return Number(item.amount) >= 0 ? `+${item.amount}` : `${item.amount}`;
  }

  function formatBalance(item) {
    if (item.balanceAfter === null || item.balanceAfter === undefined) {
      return '--';
    }
    if (item.assetType === 'WALLET') {
      return fen2yuan(item.balanceAfter);
    }
    return item.balanceAfter;
  }

  function goWallet() {
    sheep.$router.go('/pages/user/wallet/money');
  }

  function goPoint() {
    sheep.$router.go('/pages/user/wallet/score');
  }

  function goCoupon() {
    sheep.$router.go('/pages/coupon/list', { type: 'geted' });
  }

  onLoad(async () => {
    resetPagination(state.pagination);
    await loadSummary();
    await loadLedger();
  });

  onReachBottom(() => {
    onLoadMore();
  });
</script>

<style lang="scss" scoped>
  .member-assets-page {
    background: #f7f7f7;
    min-height: 100vh;
  }

  .summary-card,
  .warning-card,
  .ledger-card {
    margin: 24rpx;
    padding: 28rpx;
    border-radius: 24rpx;
    background: #ffffff;
    box-shadow: 0 10rpx 30rpx rgba(0, 0, 0, 0.05);
  }

  .summary-card {
    background: linear-gradient(135deg, #264653 0%, #2a9d8f 100%);
    color: #ffffff;
  }

  .summary-title,
  .section-title {
    font-size: 28rpx;
    font-weight: 700;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16rpx;
    margin-top: 24rpx;
  }

  .summary-item {
    padding: 22rpx 16rpx;
    border-radius: 20rpx;
    background: rgba(255, 255, 255, 0.12);
    text-align: left;
  }

  .summary-label {
    font-size: 24rpx;
    opacity: 0.86;
  }

  .summary-value {
    margin-top: 14rpx;
    font-size: 34rpx;
    font-weight: 700;
  }

  .warning-card {
    background: #fff3cd;
    color: #8a6d3b;
    font-size: 24rpx;
    line-height: 1.6;
  }

  .section-subtitle {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: #7a8793;
  }

  .ledger-header {
    margin-bottom: 20rpx;
  }

  .ledger-item {
    display: flex;
    justify-content: space-between;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f0f0f0;
  }

  .ledger-item:last-child {
    border-bottom: none;
  }

  .ledger-main {
    flex: 1;
    min-width: 0;
  }

  .ledger-top {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }

  .ledger-type {
    padding: 6rpx 14rpx;
    border-radius: 999rpx;
    background: #edf6f9;
    color: #1d6f73;
    font-size: 22rpx;
    font-weight: 600;
  }

  .ledger-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #243b53;
  }

  .ledger-desc,
  .ledger-meta,
  .ledger-balance {
    margin-top: 10rpx;
    font-size: 22rpx;
    color: #6b7c93;
  }

  .ledger-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12rpx;
  }

  .ledger-side {
    margin-left: 24rpx;
    text-align: right;
  }

  .ledger-amount {
    font-size: 30rpx;
    font-weight: 700;
    color: #2a9d8f;
  }

  .ledger-amount.minus {
    color: #c1121f;
  }
</style>
