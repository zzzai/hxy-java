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
        <text class="text-gray">服务项目</text>
        <text>{{ state.serviceName || '请先选择服务项目' }}</text>
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
  import SpuApi from '@/sheep/api/product/spu';
  import {
    buildBookingCreatePayload,
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
    spuId: 0,
    skuId: 0,
    serviceName: '',
    skuName: '',
    technician: {},
    slot: {},
    userRemark: '',
    submitting: false,
  });

  function formatSkuName(sku) {
    if (!sku) return '';
    return sku.name || sku.goods_sku_text || '';
  }

  async function loadSelectedProduct() {
    if (!state.spuId) return;
    const { code, data } = await SpuApi.getSpuDetail(state.spuId);
    if (code !== 0 || !data) return;
    if (!state.serviceName) {
      state.serviceName = data.name || '';
    }
    if (!state.skuName && state.skuId) {
      const matchedSku = (data.skus || []).find((item) => Number(item.id) === Number(state.skuId));
      state.skuName = formatSkuName(matchedSku);
    }
  }

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
    await loadSelectedProduct();
  }

  async function onSubmit() {
    if (state.submitting) return;
    const payload = buildBookingCreatePayload({
      timeSlotId: state.timeSlotId,
      spuId: state.spuId,
      skuId: state.skuId,
      userRemark: state.userRemark,
    });
    if (!payload) {
      sheep.$helper.toast('请先选择服务项目');
      return;
    }
    state.submitting = true;
    await submitBookingOrderAndGo(BookingApi, sheep.$router, payload);
    state.submitting = false;
  }

  onLoad((options) => {
    state.timeSlotId = Number(options.timeSlotId || 0);
    state.technicianId = Number(options.technicianId || 0);
    state.storeId = Number(options.storeId || 0);
    state.spuId = Number(options.spuId || 0);
    state.skuId = Number(options.skuId || 0);
    state.serviceName = options.spuName || '';
    state.skuName = options.skuName || '';
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
