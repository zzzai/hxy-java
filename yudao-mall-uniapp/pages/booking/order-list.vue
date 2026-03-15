<!-- 我的预约列表 -->
<template>
  <s-layout title="我的预约">
    <su-sticky bgColor="#fff">
      <su-tabs :list="tabMaps" :scrollable="false" @change="onTabsChange" :current="state.currentTab" />
    </su-sticky>
    <s-empty v-if="state.pagination.total === 0 && !state.loading" icon="/static/order-empty.png" text="暂无预约" />
    <view v-if="state.pagination.total > 0" class="ss-p-20">
      <view
        v-for="order in state.pagination.list"
        :key="order.id"
        class="order-card bg-white ss-r-10 ss-m-b-20 ss-p-20"
        @tap="onDetail(order.id)"
      >
        <view class="ss-flex ss-row-between ss-col-center ss-m-b-16">
          <text class="ss-font-24 text-gray">{{ order.orderNo }}</text>
          <text class="ss-font-24" :class="'status-text-' + order.status">{{ formatStatus(order.status) }}</text>
        </view>
        <view class="ss-flex ss-col-center">
          <image class="service-pic" :src="order.servicePic || '/static/default-service.png'" mode="aspectFill" />
          <view class="ss-flex-1 ss-m-l-16">
            <view class="ss-font-28">{{ order.serviceName }}</view>
            <view class="ss-font-22 text-gray ss-m-t-8">
              {{ order.bookingDate }} {{ order.bookingStartTime }}-{{ order.bookingEndTime }}
            </view>
            <view class="ss-font-22 text-gray ss-m-t-4">{{ order.duration }}分钟</view>
          </view>
          <view class="ss-font-28 ss-font-bold">¥{{ fen2yuan(order.payPrice) }}</view>
        </view>
        <view class="ss-flex ss-row-right ss-m-t-16">
          <button
            v-if="order.status === 0"
            class="small-btn pay-btn ss-reset-button"
            @tap.stop="onPay(order)"
          >去支付</button>
          <button
            v-if="order.status === 0 || order.status === 1"
            class="small-btn cancel-btn ss-reset-button ss-m-l-12"
            @tap.stop="onCancel(order)"
          >取消</button>
        </view>
      </view>
    </view>
    <uni-load-more v-if="state.pagination.total > 0" :status="state.loadStatus" />
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad, onReachBottom, onPullDownRefresh } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import BookingApi from '@/sheep/api/trade/booking';
  import { concat } from 'lodash-es';
  import { resetPagination } from '@/sheep/helper/utils';

  function fen2yuan(fen) {
    return ((fen || 0) / 100).toFixed(2);
  }

  const STATUS_MAP = {
    0: '待支付', 1: '已支付', 2: '已取消', 3: '服务中', 4: '已完成', 5: '已退款',
  };
  function formatStatus(status) {
    return STATUS_MAP[status] || '未知';
  }

  const tabMaps = [
    { name: '全部' },
    { name: '待支付', value: 0 },
    { name: '已支付', value: 1 },
    { name: '服务中', value: 3 },
    { name: '已完成', value: 4 },
  ];

  const state = reactive({
    currentTab: 0,
    loading: false,
    pagination: { list: [], total: 0, pageNo: 1, pageSize: 10 },
    loadStatus: '',
  });

  function onTabsChange(e) {
    if (state.currentTab === e.index) return;
    resetPagination(state.pagination);
    state.currentTab = e.index;
    getList();
  }

  async function getList() {
    state.loading = true;
    state.loadStatus = 'loading';
    const { code, data } = await BookingApi.getOrderList({
      pageNo: state.pagination.pageNo,
      pageSize: state.pagination.pageSize,
      status: tabMaps[state.currentTab].value,
    });
    if (code === 0) {
      state.pagination.list = concat(state.pagination.list, data.list || []);
      state.pagination.total = data.total || 0;
      state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
    }
    state.loading = false;
  }

  function onDetail(id) {
    sheep.$router.go('/pages/booking/order-detail', { id });
  }

  function onPay(order) {
    if (order.payOrderId) {
      sheep.$router.go('/pages/pay/index', { id: order.payOrderId });
    }
  }

  function onCancel(order) {
    uni.showModal({
      title: '提示',
      content: '确定要取消预约吗？',
      success: async (res) => {
        if (!res.confirm) return;
        const { code } = await BookingApi.cancelOrder(order.id, '用户主动取消');
        if (code === 0) {
          resetPagination(state.pagination);
          await getList();
        }
      },
    });
  }

  onLoad((options) => {
    if (options.type) {
      state.currentTab = parseInt(options.type);
    }
    getList();
  });

  onReachBottom(() => {
    if (state.loadStatus === 'noMore') return;
    state.pagination.pageNo++;
    getList();
  });

  onPullDownRefresh(() => {
    resetPagination(state.pagination);
    getList();
    setTimeout(() => uni.stopPullDownRefresh(), 800);
  });
</script>

<style lang="scss" scoped>
  .text-gray { color: #999; }
  .order-card {
    box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  }
  .service-pic {
    width: 120rpx;
    height: 120rpx;
    border-radius: 12rpx;
  }
  .status-text-0 { color: #faad14; }
  .status-text-1 { color: #ff6600; }
  .status-text-2 { color: #999; }
  .status-text-3 { color: #1890ff; }
  .status-text-4 { color: #52c41a; }
  .status-text-5 { color: #ff4d4f; }
  .small-btn {
    height: 56rpx;
    padding: 0 28rpx;
    border-radius: 28rpx;
    font-size: 24rpx;
    line-height: 56rpx;
  }
  .pay-btn {
    background: var(--ui-BG-Main, #ff6600);
    color: #fff;
  }
  .cancel-btn {
    background: #f5f5f5;
    color: #666;
  }
</style>
