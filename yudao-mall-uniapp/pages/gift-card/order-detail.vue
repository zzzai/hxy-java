<template>
  <s-layout title="礼品卡订单详情" navbar="inner">
    <view class="page-wrap ss-p-24">
      <view class="order-card ss-m-b-20">
        <view class="order-title">礼品卡订单</view>
        <view class="order-meta">订单号 {{ state.order.orderId || '--' }} · 状态 {{ state.order.status || '--' }}</view>
      </view>

      <view class="card-list-card ss-m-b-20">
        <view class="order-title">礼品卡列表</view>
        <view v-for="card in state.order.cards || []" :key="card.cardNo" class="card-item">
          <view class="card-no">卡号 {{ card.cardNo }}</view>
          <view class="card-meta">状态 {{ card.status }} · 受赠人 {{ card.receiverMemberId || '--' }}</view>
        </view>
      </view>

      <view class="action-row">
        <button class="action-btn ss-reset-button" @tap="goRedeem">去核销</button>
        <button class="action-btn ghost ss-reset-button" @tap="goRefund">申请退款</button>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import GiftCardApi from '@/sheep/api/promotion/giftCard';

  const state = reactive({
    orderId: 0,
    order: {
      cards: [],
    },
  });

  async function getOrder() {
    const { code, data } = await GiftCardApi.getOrder({ orderId: state.orderId });
    if (code !== 0) {
      return;
    }
    state.order = data || { cards: [] };
  }

  function goRedeem() {
    const cardNo = state.order.cards?.[0]?.cardNo || '';
    sheep.$router.go('/pages/gift-card/redeem', { cardNo });
  }

  function goRefund() {
    sheep.$router.go('/pages/gift-card/refund', { orderId: state.orderId });
  }

  onLoad((options) => {
    state.orderId = Number(options.orderId || 0);
    getOrder();
  });
</script>

<style scoped lang="scss">
  .page-wrap {
    min-height: 100vh;
    background: #f7f7f7;
  }
  .order-card,
  .card-list-card {
    padding: 28rpx;
    border-radius: 24rpx;
    background: #ffffff;
    box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.06);
  }
  .order-title,
  .card-no {
    font-size: 30rpx;
    font-weight: 700;
    color: #243b53;
  }
  .order-meta,
  .card-meta {
    margin-top: 12rpx;
    font-size: 24rpx;
    color: #6b7c93;
  }
  .card-item {
    padding-top: 18rpx;
    margin-top: 18rpx;
    border-top: 1rpx solid #f0f0f0;
  }
  .action-row {
    display: flex;
    gap: 16rpx;
  }
  .action-btn {
    flex: 1;
    height: 72rpx;
    border-radius: 999rpx;
    background: #f59e0b;
    color: #fff;
    font-size: 24rpx;
  }
  .action-btn.ghost {
    background: #fff7e6;
    color: #ad6800;
  }
</style>
