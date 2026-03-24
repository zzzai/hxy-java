<template>
  <s-layout title="礼品卡退款" navbar="inner">
    <view class="page-wrap ss-p-24">
      <view class="form-card">
        <view class="form-title">申请退款</view>
        <view class="order-hint ss-m-b-16">订单号 {{ state.orderId || '--' }}</view>
        <uni-easyinput v-model="state.reason" type="textarea" placeholder="请输入礼品卡退款原因" />
        <button class="submit-btn ss-reset-button" @tap="submitRefund">提交退款</button>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import GiftCardApi from '@/sheep/api/promotion/giftCard';

  const state = reactive({
    orderId: 0,
    reason: '',
  });

  async function submitRefund() {
    const { code, data } = await GiftCardApi.applyRefund({
      orderId: state.orderId,
      reason: state.reason,
      clientToken: `gift-refund-${Date.now()}`,
    });
    if (code !== 0) {
      return;
    }
    uni.showToast({ title: data?.refundStatus || '已提交', icon: 'none' });
  }

  onLoad((options) => {
    state.orderId = Number(options.orderId || 0);
  });
</script>

<style scoped lang="scss">
  .page-wrap {
    min-height: 100vh;
    background: #f7f7f7;
  }
  .form-card {
    padding: 28rpx;
    border-radius: 24rpx;
    background: #ffffff;
    box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.06);
  }
  .form-title {
    margin-bottom: 12rpx;
    font-size: 30rpx;
    font-weight: 700;
    color: #243b53;
  }
  .order-hint {
    font-size: 24rpx;
    color: #6b7c93;
  }
  .submit-btn {
    margin-top: 24rpx;
    height: 72rpx;
    border-radius: 999rpx;
    background: #f59e0b;
    color: #fff;
    font-size: 24rpx;
  }
</style>
