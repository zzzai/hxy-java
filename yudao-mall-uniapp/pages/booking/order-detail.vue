<!-- 预约订单详情 -->
<template>
  <s-layout title="预约详情">
    <s-empty v-if="!state.order.id && !state.loading" text="订单不存在" />
    <view v-if="state.order.id">
      <!-- 状态 -->
      <view class="status-bar ss-p-30" :class="'status-' + state.order.status">
        <view class="ss-font-32 ss-font-bold">{{ formatStatus(state.order.status) }}</view>
        <view class="ss-font-24 ss-m-t-8 status-desc">{{ formatStatusDesc(state.order.status) }}</view>
      </view>

      <!-- 技师信息 -->
      <view class="section bg-white ss-m-t-14 ss-p-30">
        <view class="ss-font-26 ss-font-bold ss-m-b-16">技师信息</view>
        <view class="ss-flex ss-col-center">
          <image class="avatar-sm" :src="state.order.technicianAvatar || '/static/default-avatar.png'" mode="aspectFill" />
          <view class="ss-m-l-16">
            <view class="ss-font-28">{{ state.order.technicianName || '技师' }}</view>
          </view>
        </view>
      </view>

      <!-- 预约信息 -->
      <view class="section bg-white ss-m-t-14 ss-p-30">
        <view class="ss-font-26 ss-font-bold ss-m-b-16">预约信息</view>
        <view class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">订单号</text>
          <text>{{ state.order.orderNo }}</text>
        </view>
        <view class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">服务项目</text>
          <text>{{ state.order.serviceName }}</text>
        </view>
        <view class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">预约日期</text>
          <text>{{ state.order.bookingDate }}</text>
        </view>
        <view class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">预约时段</text>
          <text>{{ state.order.bookingStartTime }} - {{ state.order.bookingEndTime }}</text>
        </view>
        <view class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">服务时长</text>
          <text>{{ state.order.duration }}分钟</text>
        </view>
        <view class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">原价</text>
          <text>¥{{ fen2yuan(state.order.originalPrice) }}</text>
        </view>
        <view v-if="state.order.discountPrice > 0" class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">优惠</text>
          <text class="text-orange">-¥{{ fen2yuan(state.order.discountPrice) }}</text>
        </view>
        <view class="info-row ss-flex ss-row-between">
          <text class="ss-font-bold">实付</text>
          <text class="ss-font-bold text-orange">¥{{ fen2yuan(state.order.payPrice) }}</text>
        </view>
      </view>

      <!-- 备注 -->
      <view v-if="state.order.userRemark" class="section bg-white ss-m-t-14 ss-p-30">
        <view class="ss-font-26 ss-font-bold ss-m-b-12">备注</view>
        <view class="ss-font-24 text-gray">{{ state.order.userRemark }}</view>
      </view>

      <!-- 操作按钮 -->
      <view class="section bg-white ss-m-t-14 ss-p-30 ss-flex ss-row-right">
        <button
          v-if="state.order.status === 0"
          class="action-btn pay-btn ss-reset-button"
          @tap="onPay"
        >
          去支付
        </button>
        <button
          v-if="state.order.status === 0 || state.order.status === 1"
          class="action-btn cancel-btn ss-reset-button ss-m-r-16"
          @tap="onCancel"
        >
          取消预约
        </button>
        <button
          v-if="state.order.status === 3"
          class="action-btn addon-btn ss-reset-button"
          @tap="onAddon"
        >
          加钟
        </button>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import BookingApi from '@/sheep/api/trade/booking';
  import { cancelBookingOrder } from './logic';

  function fen2yuan(fen) {
    return ((fen || 0) / 100).toFixed(2);
  }

  const STATUS_MAP = {
    0: '待支付',
    1: '已支付',
    2: '已取消',
    3: '服务中',
    4: '已完成',
    5: '已退款',
  };
  const STATUS_DESC_MAP = {
    0: '请在15分钟内完成支付',
    1: '等待技师开始服务',
    2: '订单已取消',
    3: '技师正在为您服务',
    4: '服务已完成，感谢您的信任',
    5: '退款已处理',
  };

  function formatStatus(status) {
    return STATUS_MAP[status] || '未知';
  }
  function formatStatusDesc(status) {
    return STATUS_DESC_MAP[status] || '';
  }

  const state = reactive({
    orderId: 0,
    order: {},
    loading: false,
  });

  async function loadOrder() {
    state.loading = true;
    const { code, data } = await BookingApi.getOrderDetail(state.orderId);
    if (code === 0) {
      state.order = data || {};
    }
    state.loading = false;
  }

  function onPay() {
    if (state.order.payOrderId) {
      sheep.$router.go('/pages/pay/index', { id: state.order.payOrderId });
    }
  }

  function onCancel() {
    uni.showModal({
      title: '提示',
      content: '确定要取消预约吗？',
      success: async (res) => {
        if (!res.confirm) return;
        const { code } = await cancelBookingOrder(BookingApi, state.orderId);
        if (code === 0) {
          await loadOrder();
        }
      },
    });
  }

  function onAddon() {
    sheep.$router.go('/pages/booking/addon', { parentOrderId: state.orderId });
  }

  onLoad((options) => {
    state.orderId = options.id;
    loadOrder();
  });
</script>

<style lang="scss" scoped>
  .status-bar {
    color: #fff;
    &.status-0 { background: #faad14; }
    &.status-1 { background: var(--ui-BG-Main, #ff6600); }
    &.status-2 { background: #999; }
    &.status-3 { background: #1890ff; }
    &.status-4 { background: #52c41a; }
    &.status-5 { background: #ff4d4f; }
  }
  .avatar-sm {
    width: 80rpx;
    height: 80rpx;
    border-radius: 50%;
  }
  .text-gray { color: #999; }
  .text-orange { color: #ff6600; }
  .info-row { font-size: 26rpx; }
  .action-btn {
    height: 64rpx;
    padding: 0 32rpx;
    border-radius: 32rpx;
    font-size: 26rpx;
    line-height: 64rpx;
  }
  .pay-btn {
    background: var(--ui-BG-Main, #ff6600);
    color: #fff;
  }
  .cancel-btn {
    background: #f5f5f5;
    color: #666;
  }
  .addon-btn {
    background: #1890ff;
    color: #fff;
  }
</style>
