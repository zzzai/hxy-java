<template>
  <s-layout title="评价详情">
    <s-empty
      v-if="state.invalid"
      icon="/static/data-empty.png"
      text="评价不存在或参数异常"
    />

    <s-empty
      v-else-if="state.failed"
      icon="/static/data-empty.png"
      text="评价加载失败，请稍后重试"
    />

    <view v-else-if="state.loading" class="ss-p-24">
      <view class="loading-card bg-white ss-r-16 ss-p-24">正在加载评价详情...</view>
    </view>

    <view v-else class="ss-p-24 detail-page">
      <view class="hero-card bg-white ss-r-16 ss-p-24 ss-m-b-20">
        <view class="ss-flex ss-row-between ss-col-center ss-m-b-12">
          <view class="review-level">{{ formatReviewLevel(state.review.reviewLevel) }}</view>
          <view class="meta-text">提交时间：{{ state.review.submitTime || '--' }}</view>
        </view>
        <view class="ss-font-24 text-gray">订单号：{{ state.review.bookingOrderId || '--' }}</view>
      </view>

      <view class="detail-card bg-white ss-r-16 ss-p-24 ss-m-b-20">
        <view class="section-title ss-m-b-20">评分明细</view>
        <view class="score-panel ss-m-b-20">
          <view class="ss-font-48 ss-font-bold score-value">{{ state.review.overallScore || 0 }}</view>
          <view class="ss-font-24 text-gray ss-m-b-16">总体评分</view>
          <uni-rate v-model="state.review.overallScore" :readonly="true" size="20" />
        </view>

        <view class="score-row ss-flex ss-row-between ss-col-center ss-m-b-16">
          <text>服务体验</text>
          <view class="ss-flex ss-col-center score-inline">
            <uni-rate v-model="state.review.serviceScore" :readonly="true" size="16" />
            <text class="ss-m-l-12">{{ state.review.serviceScore || 0 }}/5</text>
          </view>
        </view>
        <view class="score-row ss-flex ss-row-between ss-col-center ss-m-b-16">
          <text>技师表现</text>
          <view class="ss-flex ss-col-center score-inline">
            <uni-rate v-model="state.review.technicianScore" :readonly="true" size="16" />
            <text class="ss-m-l-12">{{ state.review.technicianScore || 0 }}/5</text>
          </view>
        </view>
        <view class="score-row ss-flex ss-row-between ss-col-center">
          <text>门店环境</text>
          <view class="ss-flex ss-col-center score-inline">
            <uni-rate v-model="state.review.environmentScore" :readonly="true" size="16" />
            <text class="ss-m-l-12">{{ state.review.environmentScore || 0 }}/5</text>
          </view>
        </view>
      </view>

      <view class="detail-card bg-white ss-r-16 ss-p-24 ss-m-b-20">
        <view class="section-title ss-m-b-16">评价内容</view>

        <view v-if="state.review.tags?.length" class="tag-group ss-flex ss-flex-wrap ss-m-b-20">
          <view v-for="tag in state.review.tags" :key="tag" class="tag-item ss-m-r-12 ss-m-b-12">
            {{ tag }}
          </view>
        </view>

        <view class="content-box ss-m-b-20">
          {{ state.review.content || '用户未填写文字评价' }}
        </view>

        <view v-if="state.review.picUrls?.length">
          <view class="ss-font-24 ss-m-b-16">评价图片</view>
          <view class="image-grid ss-flex ss-flex-wrap">
            <image
              v-for="(url, index) in state.review.picUrls"
              :key="url"
              class="image-item ss-m-r-12 ss-m-b-12"
              :src="url"
              mode="aspectFill"
              @tap="previewImages(index)"
            />
          </view>
        </view>
      </view>

      <view class="reply-card ss-r-16 ss-p-24 ss-m-b-20">
        <view class="section-title ss-m-b-16">官方回复</view>
        <view class="reply-content">{{ state.review.replyContent || '商家暂未回复' }}</view>
      </view>

      <view class="action-row ss-flex ss-col-center ss-row-between">
        <button class="secondary-btn ss-reset-button" @tap="goReviewList">返回我的评价</button>
        <button class="primary-btn ss-reset-button" @tap="goOrderDetail">查看订单详情</button>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
import { reactive } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import sheep from '@/sheep';
import BookingReviewApi from '@/sheep/api/trade/review';

const REVIEW_NOT_EXISTS_CODE = 1030008000;

const state = reactive({
  reviewId: 0,
  loading: false,
  invalid: false,
  failed: false,
  review: {
    bookingOrderId: null,
    reviewLevel: null,
    submitTime: '',
    overallScore: 0,
    serviceScore: 0,
    technicianScore: 0,
    environmentScore: 0,
    tags: [],
    content: '',
    picUrls: [],
    replyContent: '',
  },
});

function formatReviewLevel(level) {
  return {
    1: '好评',
    2: '中评',
    3: '差评',
  }[level] || '评价详情';
}

function previewImages(index) {
  if (!state.review.picUrls?.length) {
    return;
  }
  uni.previewImage({
    current: state.review.picUrls[index],
    urls: state.review.picUrls,
  });
}

function goReviewList() {
  sheep.$router.go('/pages/booking/review-list');
}

function goOrderDetail() {
  if (!state.review.bookingOrderId) {
    goReviewList();
    return;
  }
  sheep.$router.go('/pages/booking/order-detail', { id: state.review.bookingOrderId });
}

async function loadReview() {
  state.loading = true;
  state.invalid = false;
  state.failed = false;
  const { code, data } = await BookingReviewApi.getReview(state.reviewId);
  if (code === 0 && data) {
    state.review = {
      bookingOrderId: data.bookingOrderId || null,
      reviewLevel: data.reviewLevel || null,
      submitTime: data.submitTime || '',
      overallScore: data.overallScore || 0,
      serviceScore: data.serviceScore || 0,
      technicianScore: data.technicianScore || 0,
      environmentScore: data.environmentScore || 0,
      tags: data.tags || [],
      content: data.content || '',
      picUrls: data.picUrls || [],
      replyContent: data.replyContent || '',
    };
  } else if (code === REVIEW_NOT_EXISTS_CODE || !data) {
    state.invalid = true;
  } else {
    state.failed = true;
  }
  state.loading = false;
}

onLoad((options) => {
  const reviewId = Number(options.id || 0);
  if (!reviewId) {
    state.invalid = true;
    return;
  }
  state.reviewId = reviewId;
  loadReview();
});
</script>

<style lang="scss" scoped>
.detail-page,
.hero-card,
.detail-card,
.loading-card {
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}
.hero-card,
.detail-card,
.loading-card {
  background: #fff;
}
.review-level {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 120rpx;
  height: 52rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #fff3e8;
  color: #ff6600;
  font-size: 24rpx;
  font-weight: 600;
}
.meta-text,
.text-gray {
  color: #8c8c8c;
}
.section-title {
  font-size: 30rpx;
  font-weight: 600;
}
.score-panel {
  padding: 24rpx;
  border-radius: 20rpx;
  background: linear-gradient(180deg, #fff7ed, #ffffff);
  text-align: center;
}
.score-value {
  color: #ff6600;
  line-height: 1;
}
.score-row {
  font-size: 26rpx;
}
.score-inline {
  color: #666;
}
.tag-group {
  gap: 0;
}
.tag-item {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  background: #f5f5f5;
  color: #666;
  font-size: 24rpx;
}
.content-box {
  padding: 24rpx;
  border-radius: 20rpx;
  background: #f8fafc;
  font-size: 26rpx;
  line-height: 1.7;
  color: #333;
}
.image-grid {
  gap: 0;
}
.image-item {
  width: 200rpx;
  height: 200rpx;
  border-radius: 20rpx;
  background: #f3f4f6;
}
.reply-card {
  background: #fff7e6;
  box-shadow: 0 8rpx 24rpx rgba(255, 120, 0, 0.08);
}
.reply-content {
  font-size: 26rpx;
  line-height: 1.7;
  color: #ad6800;
}
.action-row {
  gap: 24rpx;
}
.primary-btn,
.secondary-btn {
  flex: 1;
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
