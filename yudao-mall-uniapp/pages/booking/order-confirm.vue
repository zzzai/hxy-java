<!-- 预约确认 -->
<template>
  <s-layout title="确认预约">
    <!-- 技师信息 -->
    <view class="section bg-white ss-m-t-14 ss-p-30">
      <view class="ss-font-28 ss-font-bold ss-m-b-20">技师信息</view>
      <view class="ss-flex ss-col-center">
        <image class="avatar-sm" :src="state.technician.avatar || '/static/default-avatar.png'" mode="aspectFill" />
        <view class="ss-m-l-16">
          <view class="ss-font-28">{{ state.technician.name }}</view>
          <view class="ss-font-22 text-gray">{{ state.technician.title || '高级技师' }}</view>
        </view>
      </view>
    </view>

    <!-- 预约信息 -->
    <view class="section bg-white ss-m-t-14 ss-p-30">
      <view class="ss-font-28 ss-font-bold ss-m-b-20">预约信息</view>
      <view class="info-row ss-flex ss-row-between ss-m-b-16">
        <text class="text-gray">预约日期</text>
        <text>{{ state.slot.slotDate }}</text>
      </view>
      <view class="info-row ss-flex ss-row-between ss-m-b-16">
        <text class="text-gray">预约时段</text>
        <text>{{ state.slot.startTime }} - {{ state.slot.endTime }}</text>
      </view>
      <view class="info-row ss-flex ss-row-between ss-m-b-16">
        <text class="text-gray">服务时长</text>
        <text>{{ state.slot.duration }}分钟</text>
      </view>
      <view v-if="state.slot.isOffpeak" class="info-row ss-flex ss-row-between">
        <text class="text-gray">闲时优惠</text>
        <text class="text-orange">享受闲时价</text>
      </view>
    </view>

    <!-- 备注 -->
    <view class="section bg-white ss-m-t-14 ss-p-30">
      <view class="ss-font-28 ss-font-bold ss-m-b-20">备注</view>
      <textarea
        v-model="state.userRemark"
        class="remark-input"
        placeholder="请输入备注信息（选填）"
        :maxlength="200"
      />
    </view>

    <!-- 底部提交 -->
    <su-fixed bottom>
      <view class="footer-bar ss-flex ss-col-center ss-row-between ss-p-x-30">
        <view class="ss-flex ss-col-center" v-if="state.slot.offpeakPrice > 0">
          <text class="ss-font-24 text-gray ss-m-r-8">闲时价</text>
          <text class="ss-font-32 ss-font-bold text-orange">¥{{ fen2yuan(state.slot.offpeakPrice) }}</text>
        </view>
        <view v-else />
        <button class="submit-btn ss-reset-button" :loading="state.submitting" @tap="onSubmit">
          提交预约
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
  import {
    loadTechnicianDetail,
    loadTimeSlotDetail,
    submitBookingOrderAndGo,
  } from './logic';

  function fen2yuan(fen) {
    return (fen / 100).toFixed(2);
  }

  const state = reactive({
    timeSlotId: 0,
    technicianId: 0,
    storeId: 0,
    technician: {},
    slot: {},
    userRemark: '',
    submitting: false,
  });

  async function loadData() {
    const [techRes, slotRes] = await Promise.all([
      loadTechnicianDetail(BookingApi, state.technicianId),
      loadTimeSlotDetail(BookingApi, state.timeSlotId),
    ]);
    if (techRes.code === 0) {
      state.technician = techRes.data || {};
    }
    if (slotRes.code === 0) {
      state.slot = slotRes.data || {};
    }
  }

  async function onSubmit() {
    if (state.submitting) return;
    state.submitting = true;
    await submitBookingOrderAndGo(BookingApi, sheep.$router, {
      timeSlotId: state.timeSlotId,
      spuId: state.slot.spuId || undefined,
      skuId: state.slot.skuId || undefined,
      userRemark: state.userRemark,
    });
    state.submitting = false;
  }

  onLoad((options) => {
    state.timeSlotId = options.timeSlotId;
    state.technicianId = options.technicianId;
    state.storeId = options.storeId || 0;
    loadData();
  });
</script>

<style lang="scss" scoped>
  .avatar-sm {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
  }
  .text-gray { color: #999; }
  .text-orange { color: #ff6600; }
  .info-row {
    font-size: 26rpx;
  }
  .remark-input {
    width: 100%;
    height: 160rpx;
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
  }
</style>
