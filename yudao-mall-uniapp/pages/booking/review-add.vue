<template>
  <s-layout title="服务评价">
    <s-empty
      v-if="!state.loading && !state.eligibility.eligible"
      icon="/static/order-empty.png"
      :text="state.eligibility.reasonText || '暂无可评价订单'"
    />

    <view v-else class="ss-p-24">
      <view class="panel bg-white ss-r-16 ss-p-24 ss-m-b-20">
        <view class="ss-font-30 ss-font-bold ss-m-b-12">请为本次服务打分</view>
        <view class="ss-font-24 text-gray ss-m-b-24">完成后我们会同步到服务恢复台账，差评将优先触发人工跟进。</view>

        <view class="score-row ss-flex ss-row-between ss-col-center ss-m-b-18">
          <text>整体体验</text>
          <uni-rate v-model="state.form.overallScore" />
        </view>
        <view class="score-row ss-flex ss-row-between ss-col-center ss-m-b-18">
          <text>服务体验</text>
          <uni-rate v-model="state.form.serviceScore" />
        </view>
        <view class="score-row ss-flex ss-row-between ss-col-center ss-m-b-18">
          <text>技师表现</text>
          <uni-rate v-model="state.form.technicianScore" />
        </view>
        <view class="score-row ss-flex ss-row-between ss-col-center ss-m-b-24">
          <text>门店环境</text>
          <uni-rate v-model="state.form.environmentScore" />
        </view>

        <view class="ss-font-24 ss-m-b-16">服务标签</view>
        <view class="tag-group ss-flex ss-flex-wrap ss-m-b-24">
          <view
            v-for="tag in state.tagOptions"
            :key="tag"
            class="tag-item ss-m-r-12 ss-m-b-12"
            :class="{ active: state.form.tags.includes(tag) }"
            @tap="toggleTag(tag)"
          >
            {{ tag }}
          </view>
        </view>

        <view class="textarea-box ss-m-b-20">
          <uni-easyinput
            v-model="state.form.content"
            :inputBorder="false"
            type="textarea"
            maxlength="300"
            autoHeight
            placeholder="这次服务哪些地方做得好，哪些地方还需要改进？"
          />
        </view>

        <label class="ss-flex ss-col-center ss-font-24 text-gray">
          <checkbox :checked="state.form.anonymous" @click="toggleAnonymous" />
          <text class="ss-m-l-8">匿名评价</text>
        </label>
      </view>

      <button class="submit-btn ss-reset-button" :loading="state.submitting" @tap="onSubmit">提交评价</button>
    </view>
  </s-layout>
</template>

<script setup>
import { reactive } from 'vue';
import { onLoad } from '@dcloudio/uni-app';
import sheep from '@/sheep';
import BookingReviewApi from '@/sheep/api/trade/review';

const state = reactive({
  bookingOrderId: null,
  loading: false,
  submitting: false,
  eligibility: {
    eligible: false,
    reason: '',
    alreadyReviewed: false,
    reviewId: null,
    reasonText: '',
  },
  form: {
    overallScore: 5,
    serviceScore: 5,
    technicianScore: 5,
    environmentScore: 5,
    tags: [],
    content: '',
    anonymous: false,
  },
  tagOptions: ['服务专业', '沟通清晰', '环境整洁', '响应及时', '体验一般', '需要改进'],
});

const REASON_TEXT_MAP = {
  ORDER_NOT_EXISTS: '暂无可评价订单',
  NOT_OWNER: '当前订单不属于你，暂不可评价',
  ALREADY_REVIEWED: '该订单已评价，可直接查看历史评价',
  ORDER_NOT_COMPLETED: '服务未完成，暂不可评价',
};

function toggleTag(tag) {
  const index = state.form.tags.indexOf(tag);
  if (index >= 0) {
    state.form.tags.splice(index, 1);
    return;
  }
  if (state.form.tags.length >= 8) {
    sheep.$helper.toast('最多选择 8 个标签');
    return;
  }
  state.form.tags.push(tag);
}

function toggleAnonymous() {
  state.form.anonymous = !state.form.anonymous;
}

async function loadEligibility() {
  state.loading = true;
  const { code, data } = await BookingReviewApi.getEligibility(state.bookingOrderId);
  if (code === 0) {
    state.eligibility = {
      eligible: !!data?.eligible,
      reason: data?.reason || '',
      alreadyReviewed: !!data?.alreadyReviewed,
      reviewId: data?.reviewId || null,
      reasonText: REASON_TEXT_MAP[data?.reason] || (data?.eligible ? '' : '暂不可评价'),
    };
  } else {
    state.eligibility.reasonText = '暂不可评价';
  }
  state.loading = false;
}

async function onSubmit() {
  if (!state.eligibility.eligible) {
    sheep.$helper.toast(state.eligibility.reasonText || '暂不可评价');
    return;
  }
  state.submitting = true;
  const { code, data } = await BookingReviewApi.createReview({
    bookingOrderId: state.bookingOrderId,
    overallScore: state.form.overallScore,
    serviceScore: state.form.serviceScore,
    technicianScore: state.form.technicianScore,
    environmentScore: state.form.environmentScore,
    tags: state.form.tags,
    content: state.form.content,
    anonymous: state.form.anonymous,
    source: 'order_detail',
  });
  state.submitting = false;
  if (code !== 0) {
    sheep.$helper.toast('提交失败，请稍后重试');
    return;
  }
  sheep.$router.go('/pages/booking/review-result', {
    reviewId: data,
    bookingOrderId: state.bookingOrderId,
  });
}

onLoad((options) => {
  state.bookingOrderId = Number(options.bookingOrderId || options.orderId || options.id || 0);
  if (!state.bookingOrderId) {
    state.eligibility.reasonText = '暂无可评价订单';
    return;
  }
  loadEligibility();
});
</script>

<style lang="scss" scoped>
.panel {
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}
.text-gray {
  color: #8c8c8c;
}
.score-row {
  font-size: 28rpx;
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
.tag-item.active {
  background: #fff3e8;
  color: #ff6600;
}
.textarea-box {
  min-height: 220rpx;
  padding: 24rpx;
  border-radius: 20rpx;
  background: #f8fafc;
}
.submit-btn {
  height: 88rpx;
  border-radius: 44rpx;
  background: linear-gradient(135deg, #ff7a18, #ff4d4f);
  color: #fff;
  font-size: 28rpx;
  line-height: 88rpx;
}
</style>
