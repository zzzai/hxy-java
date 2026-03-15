<!-- 售后日志列表  -->
<template>
  <s-layout title="售后进度">
    <view class="compat-tip ss-m-x-20 ss-m-t-20" v-if="state.compatTip">{{ state.compatTip }}</view>
    <s-empty v-if="state.list.length === 0" icon="/static/data-empty.png" text="暂无进度数据" />
    <view class="log-box" v-if="state.list.length > 0">
      <view v-for="(item, index) in state.list" :key="item.id">
        <log-item :item="item" :index="index" :data="state.list" />
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { onLoad } from '@dcloudio/uni-app';
  import { reactive } from 'vue';
  import logItem from './log-item.vue';
  import AfterSaleApi from '@/sheep/api/trade/afterSale';

  const state = reactive({
    aftersaleId: 0,
    list: [],
    compatTip: '',
  });

  async function getDetail(id) {
    const result = await AfterSaleApi.getAfterSaleLogList(id);
    state.compatTip = result?.compat?.degraded ? result.compat.hint || '' : '';
    state.list = result?.data || [];
  }

  onLoad((options) => {
    state.aftersaleId = options.id;
    getDetail(options.id);
  });
</script>

<style lang="scss" scoped>
  .compat-tip {
    border-radius: 12rpx;
    background: #fff8e6;
    color: #ad6a00;
    font-size: 24rpx;
    line-height: 36rpx;
    padding: 10rpx 16rpx;
  }

  .log-box {
    padding: 24rpx 24rpx 24rpx 40rpx;
    background-color: #fff;
  }
</style>
