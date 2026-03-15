<!-- 技师列表 -->
<template>
  <s-layout title="选择技师">
    <s-empty v-if="state.list.length === 0 && !state.loading" icon="/static/data-empty.png" text="暂无可用技师" />
    <view class="technician-list ss-p-20">
      <view
        v-for="item in state.list"
        :key="item.id"
        class="technician-card bg-white ss-r-10 ss-m-b-20 ss-p-20"
        @tap="onDetail(item.id)"
      >
        <view class="ss-flex">
          <image
            class="avatar"
            :src="item.avatar || '/static/default-avatar.png'"
            mode="aspectFill"
          />
          <view class="info ss-flex-1 ss-m-l-20">
            <view class="name ss-font-28 ss-font-bold">{{ item.name }}</view>
            <view class="title ss-font-24 ss-m-t-8 text-gray">{{ item.title || '高级技师' }}</view>
            <view class="ss-flex ss-col-center ss-m-t-10">
              <view class="rating ss-font-24 text-orange">评分 {{ item.rating || '5.0' }}</view>
              <view class="service-count ss-font-22 text-gray ss-m-l-20">
                服务 {{ item.serviceCount || 0 }} 次
              </view>
            </view>
            <view v-if="item.specialties" class="specialties ss-font-22 text-gray ss-m-t-8">
              擅长：{{ item.specialties }}
            </view>
          </view>
          <view class="ss-flex ss-col-center">
            <view class="status-dot" :class="item.status === 1 ? 'online' : 'offline'" />
          </view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import BookingApi from '@/sheep/api/trade/booking';
  import { goToTechnicianDetail, loadTechnicianList } from './logic';

  const state = reactive({
    storeId: 0,
    list: [],
    loading: false,
  });

  async function getList() {
    state.loading = true;
    const { code, data } = await loadTechnicianList(BookingApi, state.storeId);
    if (code === 0) {
      state.list = data || [];
    }
    state.loading = false;
  }

  function onDetail(id) {
    goToTechnicianDetail(sheep.$router, id, state.storeId);
  }

  onLoad((options) => {
    state.storeId = options.storeId || 0;
    getList();
  });
</script>

<style lang="scss" scoped>
  .technician-card {
    box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
  }
  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
  }
  .text-gray {
    color: #999;
  }
  .text-orange {
    color: #ff6600;
  }
  .status-dot {
    width: 16rpx;
    height: 16rpx;
    border-radius: 50%;
    &.online {
      background: #52c41a;
    }
    &.offline {
      background: #ccc;
    }
  }
</style>
