<template>
  <s-layout title="礼品卡核销" navbar="inner">
    <view class="page-wrap ss-p-24">
      <view class="form-card">
        <view class="form-title">核销礼品卡</view>
        <uni-easyinput v-model="state.cardNo" placeholder="请输入礼品卡卡号" />
        <uni-easyinput v-model="state.redeemCode" class="ss-m-t-16" placeholder="请输入核销码" />
        <button class="submit-btn ss-reset-button" @tap="submitRedeem">立即核销</button>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import GiftCardApi from '@/sheep/api/promotion/giftCard';

  const state = reactive({
    cardNo: '',
    redeemCode: '',
  });

  async function submitRedeem() {
    const { code, data } = await GiftCardApi.redeem({
      cardNo: state.cardNo,
      redeemCode: state.redeemCode,
      clientToken: `gift-redeem-${Date.now()}`,
    });
    if (code !== 0) {
      return;
    }
    uni.showToast({ title: data?.cardStatus === 'REDEEMED' ? '核销成功' : '已提交', icon: 'none' });
  }

  onLoad((options) => {
    state.cardNo = options.cardNo || '';
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
    margin-bottom: 20rpx;
    font-size: 30rpx;
    font-weight: 700;
    color: #243b53;
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
