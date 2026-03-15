<!-- 加钟/升级 -->
<template>
  <s-layout title="加钟服务">
    <view v-if="state.parentOrder.id" class="ss-p-20">
      <!-- 当前订单信息 -->
      <view class="section bg-white ss-r-10 ss-p-30 ss-m-b-20">
        <view class="ss-font-28 ss-font-bold ss-m-b-16">当前服务</view>
        <view class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">服务项目</text>
          <text>{{ state.parentOrder.serviceName }}</text>
        </view>
        <view class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">当前时段</text>
          <text>{{ state.parentOrder.bookingStartTime }} - {{ state.parentOrder.bookingEndTime }}</text>
        </view>
        <view class="info-row ss-flex ss-row-between">
          <text class="text-gray">技师</text>
          <text>{{ state.parentOrder.technicianName || '技师' }}</text>
        </view>
      </view>

      <!-- 加钟类型选择 -->
      <view class="section bg-white ss-r-10 ss-p-30 ss-m-b-20">
        <view class="ss-font-28 ss-font-bold ss-m-b-20">选择类型</view>
        <view class="type-grid">
          <view
            v-for="t in addonTypes"
            :key="t.value"
            class="type-item"
            :class="{ active: state.addonType === t.value }"
            @tap="state.addonType = t.value"
          >
            <view class="ss-font-28">{{ t.icon }}</view>
            <view class="ss-font-26 ss-m-t-8">{{ t.label }}</view>
            <view class="ss-font-22 text-gray ss-m-t-4">{{ t.desc }}</view>
          </view>
        </view>
      </view>

      <!-- 备注 -->
      <view class="section bg-white ss-r-10 ss-p-30 ss-m-b-20">
        <textarea
          v-model="state.remark"
          class="remark-input"
          placeholder="备注（选填）"
          :maxlength="200"
        />
      </view>
    </view>

    <s-empty v-if="!state.parentOrder.id && !state.loading" text="订单不存在" />

    <!-- 底部提交 -->
    <su-fixed bottom>
      <view class="footer-bar ss-flex ss-col-center ss-row-right ss-p-x-30">
        <button
          class="submit-btn ss-reset-button"
          :disabled="!state.addonType"
          :loading="state.submitting"
          @tap="onSubmit"
        >
          确认加钟
        </button>
      </view>
    </su-fixed>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import BookingApi from '@/sheep/api/trade/booking';
  import { goToOrderDetail, submitAddonOrder } from './logic';

  const addonTypes = [
    { value: 1, label: '加钟', desc: '延长当前服务', icon: '⏱' },
    { value: 2, label: '升级', desc: '升级服务项目', icon: '⬆' },
    { value: 3, label: '加项目', desc: '增加服务项目', icon: '➕' },
  ];

  const state = reactive({
    parentOrderId: 0,
    parentOrder: {},
    addonType: null,
    remark: '',
    loading: false,
    submitting: false,
  });

  async function loadParentOrder() {
    state.loading = true;
    const { code, data } = await BookingApi.getOrderDetail(state.parentOrderId);
    if (code === 0) {
      state.parentOrder = data || {};
    }
    state.loading = false;
  }

  async function onSubmit() {
    if (!state.addonType || state.submitting) return;
    state.submitting = true;
    const { code, data } = await submitAddonOrder(BookingApi, {
      parentOrderId: state.parentOrderId,
      addonType: state.addonType,
    });
    state.submitting = false;
    if (code === 0) {
      goToOrderDetail(sheep.$router, data);
    }
  }

  onLoad((options) => {
    state.parentOrderId = options.parentOrderId;
    loadParentOrder();
  });
</script>

<style lang="scss" scoped>
  .text-gray { color: #999; }
  .info-row { font-size: 26rpx; }
  .type-grid {
    display: flex;
    gap: 20rpx;
  }
  .type-item {
    flex: 1;
    text-align: center;
    padding: 30rpx 16rpx;
    border-radius: 16rpx;
    background: #f5f5f5;
    &.active {
      background: var(--ui-BG-Main, #ff6600);
      color: #fff;
      .text-gray { color: rgba(255,255,255,0.7); }
    }
  }
  .remark-input {
    width: 100%;
    height: 120rpx;
    background: #f5f5f5;
    border-radius: 12rpx;
    padding: 20rpx;
    font-size: 26rpx;
    box-sizing: border-box;
  }
  .footer-bar {
    height: 100rpx;
    background: #fff;
  }
  .submit-btn {
    width: 240rpx;
    height: 72rpx;
    background: var(--ui-BG-Main, #ff6600);
    color: #fff;
    border-radius: 36rpx;
    font-size: 28rpx;
    &[disabled] { opacity: 0.5; }
  }
</style>
