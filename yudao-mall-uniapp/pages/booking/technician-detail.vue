<!-- 技师详情 + 时段选择 -->
<template>
  <s-layout title="技师详情">
    <!-- 技师信息 -->
    <view class="tech-header bg-white ss-p-30">
      <view class="ss-flex">
        <image class="avatar" :src="state.technician.avatar || '/static/default-avatar.png'" mode="aspectFill" />
        <view class="ss-flex-1 ss-m-l-20">
          <view class="ss-font-32 ss-font-bold">{{ state.technician.name }}</view>
          <view class="ss-font-24 text-gray ss-m-t-8">{{ state.technician.title || '高级技师' }}</view>
          <view class="ss-flex ss-col-center ss-m-t-10">
            <view class="ss-font-24 text-orange">评分 {{ state.technician.rating || '5.0' }}</view>
            <view class="ss-font-22 text-gray ss-m-l-20">服务 {{ state.technician.serviceCount || 0 }} 次</view>
          </view>
        </view>
      </view>
      <view v-if="state.technician.introduction" class="ss-font-24 text-gray ss-m-t-20">
        {{ state.technician.introduction }}
      </view>
    </view>

    <!-- 日期选择 -->
    <view class="date-section bg-white ss-m-t-14 ss-p-30">
      <view class="ss-font-28 ss-font-bold ss-m-b-20">选择日期</view>
      <scroll-view scroll-x class="date-scroll">
        <view class="ss-flex">
          <view
            v-for="(d, idx) in state.dates"
            :key="idx"
            class="date-item ss-m-r-16"
            :class="{ active: state.selectedDateIdx === idx }"
            @tap="onSelectDate(idx)"
          >
            <view class="ss-font-22">{{ d.weekDay }}</view>
            <view class="ss-font-26 ss-m-t-4">{{ d.label }}</view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 时段选择 -->
    <view class="slot-section bg-white ss-m-t-14 ss-p-30">
      <view class="ss-font-28 ss-font-bold ss-m-b-20">选择时段</view>
      <s-empty v-if="state.slots.length === 0 && !state.slotsLoading" text="该日期暂无可用时段" />
      <view class="slot-grid">
        <view
          v-for="slot in state.slots"
          :key="slot.id"
          class="slot-item"
          :class="{ active: state.selectedSlotId === slot.id, disabled: slot.status !== 0 }"
          @tap="onSelectSlot(slot)"
        >
          <view class="ss-font-24">{{ slot.startTime }} - {{ slot.endTime }}</view>
          <view v-if="slot.isOffpeak" class="offpeak-tag ss-font-20">闲时优惠</view>
        </view>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <su-fixed bottom>
      <view class="footer-bar ss-flex ss-col-center ss-row-between ss-p-x-30">
        <view v-if="state.selectedSlotId" class="ss-font-24 text-gray">
          已选：{{ state.selectedSlotLabel }}
        </view>
        <view v-else class="ss-font-24 text-gray">请选择时段</view>
        <button
          class="confirm-btn ss-reset-button"
          :disabled="!state.selectedSlotId"
          @tap="onConfirm"
        >
          立即预约
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
  import { goToOrderConfirm, loadTechnicianDetail, loadTimeSlots } from './logic';

  const WEEK_DAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

  const state = reactive({
    technicianId: 0,
    storeId: 0,
    technician: {},
    dates: [],
    selectedDateIdx: 0,
    slots: [],
    slotsLoading: false,
    selectedSlotId: null,
    selectedSlotLabel: '',
  });

  function initDates() {
    const dates = [];
    const today = new Date();
    for (let i = 0; i < 7; i++) {
      const d = new Date(today);
      d.setDate(today.getDate() + i);
      const month = d.getMonth() + 1;
      const day = d.getDate();
      dates.push({
        date: `${d.getFullYear()}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`,
        label: i === 0 ? '今天' : i === 1 ? '明天' : `${month}/${day}`,
        weekDay: i === 0 ? '今天' : WEEK_DAYS[d.getDay()],
      });
    }
    state.dates = dates;
  }

  async function getTechnician() {
    const { code, data } = await loadTechnicianDetail(BookingApi, state.technicianId);
    if (code === 0) {
      state.technician = data || {};
    }
  }

  async function getSlots() {
    state.slotsLoading = true;
    state.selectedSlotId = null;
    state.selectedSlotLabel = '';
    const date = state.dates[state.selectedDateIdx]?.date;
    const { code, data } = await loadTimeSlots(BookingApi, state.technicianId, date);
    if (code === 0) {
      state.slots = data || [];
    }
    state.slotsLoading = false;
  }

  function onSelectDate(idx) {
    state.selectedDateIdx = idx;
    getSlots();
  }

  function onSelectSlot(slot) {
    if (slot.status !== 0) return;
    state.selectedSlotId = slot.id;
    state.selectedSlotLabel = `${state.dates[state.selectedDateIdx].label} ${slot.startTime}-${slot.endTime}`;
  }

  function onConfirm() {
    if (!state.selectedSlotId) return;
    goToOrderConfirm(sheep.$router, {
      timeSlotId: state.selectedSlotId,
      technicianId: state.technicianId,
      storeId: state.storeId,
    });
  }

  onLoad((options) => {
    state.technicianId = options.id;
    state.storeId = options.storeId || 0;
    initDates();
    getTechnician();
    getSlots();
  });
</script>

<style lang="scss" scoped>
  .tech-header {
    .avatar {
      width: 140rpx;
      height: 140rpx;
      border-radius: 50%;
    }
  }
  .text-gray { color: #999; }
  .text-orange { color: #ff6600; }
  .date-scroll {
    white-space: nowrap;
  }
  .date-item {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    padding: 16rpx 24rpx;
    border-radius: 12rpx;
    background: #f5f5f5;
    &.active {
      background: var(--ui-BG-Main, #ff6600);
      color: #fff;
    }
  }
  .slot-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
  }
  .slot-item {
    width: calc(33.33% - 12rpx);
    padding: 20rpx 10rpx;
    text-align: center;
    border-radius: 12rpx;
    background: #f5f5f5;
    position: relative;
    &.active {
      background: var(--ui-BG-Main, #ff6600);
      color: #fff;
      .offpeak-tag { color: #ffe0b2; }
    }
    &.disabled {
      opacity: 0.4;
      pointer-events: none;
    }
  }
  .offpeak-tag {
    color: #ff6600;
    margin-top: 4rpx;
  }
  .footer-bar {
    height: 100rpx;
    background: #fff;
  }
  .confirm-btn {
    width: 240rpx;
    height: 72rpx;
    background: var(--ui-BG-Main, #ff6600);
    color: #fff;
    border-radius: 36rpx;
    font-size: 28rpx;
    &[disabled] {
      opacity: 0.5;
    }
  }
</style>
