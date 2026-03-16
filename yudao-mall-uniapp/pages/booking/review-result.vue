<template>
  <s-layout title="评价结果">
    <view class="result-wrap ss-flex ss-col-center ss-row-center ss-p-40">
      <view class="result-card bg-white ss-r-16 ss-p-40">
        <view class="result-icon ss-m-b-20">{{ state.reviewId ? '✓' : '!' }}</view>
        <view class="ss-font-34 ss-font-bold ss-m-b-12">{{ state.reviewId ? '提交成功' : '提交未完成' }}</view>
        <view class="ss-font-24 text-gray ss-m-b-30">
          {{ state.reviewId ? '感谢你的反馈，我们会持续跟进服务质量。' : '本次评价未成功提交，请返回订单详情后重试。' }}
        </view>

        <button class="primary-btn ss-reset-button ss-m-b-16" @tap="goReviewList">查看我的评价</button>
        <button class="secondary-btn ss-reset-button" @tap="goOrderDetail">返回订单详情</button>
        <view class="ss-font-22 text-link ss-m-t-20" @tap="goReviewList">继续查看评价</view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
import { reactive } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import sheep from '@/sheep';

const state = reactive({
  reviewId: null,
  bookingOrderId: null,
});

function goReviewList() {
  sheep.$router.go('/pages/booking/review-list');
}

function goOrderDetail() {
  if (!state.bookingOrderId) {
    sheep.$router.back();
    return;
  }
  sheep.$router.go('/pages/booking/order-detail', { id: state.bookingOrderId });
}

onLoad((options) => {
  state.reviewId = options.reviewId ? Number(options.reviewId) : null;
  state.bookingOrderId = options.bookingOrderId ? Number(options.bookingOrderId) : null;
});
</script>

<style lang="scss" scoped>
.result-wrap {
  min-height: calc(100vh - 120rpx);
}
.result-card {
  width: 100%;
  text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}
.result-icon {
  font-size: 72rpx;
  color: #ff6600;
}
.text-gray {
  color: #8c8c8c;
}
.text-link {
  color: #ff6600;
}
.primary-btn,
.secondary-btn {
  height: 84rpx;
  border-radius: 42rpx;
  line-height: 84rpx;
  font-size: 28rpx;
}
.primary-btn {
  background: #ff6600;
  color: #fff;
}
.secondary-btn {
  background: #fff3e8;
  color: #ff6600;
}
</style>
