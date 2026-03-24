<!-- 预约服务项目选择 -->
<template>
  <s-layout title="选择服务项目">
    <view class="ss-p-20">
      <view class="section bg-white ss-r-10 ss-p-30 ss-m-b-20">
        <view class="ss-font-28 ss-font-bold ss-m-b-12">当前流程</view>
        <view class="info-row ss-flex ss-row-between ss-m-b-12">
          <text class="text-gray">业务场景</text>
          <text>{{ state.flow === 'addon' ? '加钟 / 升级 / 加项目' : '预约下单' }}</text>
        </view>
        <view v-if="state.flow === 'create'" class="info-row ss-flex ss-row-between">
          <text class="text-gray">预约时段</text>
          <text>{{ state.timeSlotId || '-' }}</text>
        </view>
        <view v-else class="info-row ss-flex ss-row-between">
          <text class="text-gray">母单编号</text>
          <text>{{ state.parentOrderId || '-' }}</text>
        </view>
      </view>

      <view class="section bg-white ss-r-10 ss-p-30">
        <view class="ss-font-28 ss-font-bold ss-m-b-20">服务目录</view>
        <s-empty v-if="!state.loading && state.pagination.list.length === 0" text="暂无可选服务项目" />
        <view
          v-for="item in state.pagination.list"
          :key="item.id"
          class="service-card ss-flex ss-col-center ss-m-b-20"
          @tap="openSkuSelector(item)"
        >
          <image class="service-pic" :src="item.picUrl || '/static/default-service.png'" mode="aspectFill" />
          <view class="ss-flex-1 ss-m-l-16">
            <view class="ss-font-28 ss-line-2">{{ item.name }}</view>
            <view class="ss-font-22 text-gray ss-m-t-8 ss-line-2">
              {{ item.introduction || '请选择规格后确认服务项目' }}
            </view>
            <view class="ss-font-24 text-orange ss-m-t-8">¥{{ fen2yuan(item.price || 0) }}</view>
          </view>
          <button class="select-btn ss-reset-button">选择规格</button>
        </view>
      </view>
    </view>

    <su-fixed bottom>
      <view class="footer-bar bg-white ss-flex ss-col-center ss-row-between ss-p-x-30">
        <view class="ss-flex-1 ss-m-r-20">
          <view class="ss-font-24 text-gray">已选项目</view>
          <view class="ss-font-26 ss-line-1">{{ selectedSummary }}</view>
        </view>
        <button class="confirm-btn ss-reset-button" @tap="confirmSelection">确认选择</button>
      </view>
    </su-fixed>

    <s-select-sku
      v-if="state.currentGoods.id"
      :key="state.currentGoods.id"
      :goodsInfo="state.currentGoods"
      :show="state.showSkuPopup"
      @change="onSkuChange"
      @buy="onConfirmSku"
      @addCart="onConfirmSku"
      @close="state.showSkuPopup = false"
    />
  </s-layout>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import { onLoad, onReachBottom } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import SpuApi from '@/sheep/api/product/spu';
  import sSelectSku from '@/sheep/components/s-select-sku/s-select-sku.vue';
  import { goToOrderConfirm } from './logic';

  function fen2yuan(fen) {
    return ((fen || 0) / 100).toFixed(2);
  }

  function formatSkuName(sku) {
    if (!sku) return '';
    return sku.name || sku.goods_sku_text || '';
  }

  const state = reactive({
    flow: 'create',
    timeSlotId: 0,
    technicianId: 0,
    storeId: 0,
    parentOrderId: 0,
    addonType: 0,
    loading: false,
    loadStatus: 'more',
    pagination: {
      pageNo: 1,
      pageSize: 10,
      list: [],
      total: 0,
    },
    currentGoods: {
      id: 0,
      skus: [],
    },
    selectedSpuId: 0,
    selectedSku: {},
    selectedServiceName: '',
    selectedSkuName: '',
    showSkuPopup: false,
  });

  const selectedSummary = computed(() => {
    if (!state.selectedSpuId || !state.selectedSku?.id) {
      return '未选择服务项目';
    }
    const skuName = state.selectedSkuName ? ` / ${state.selectedSkuName}` : '';
    return `${state.selectedServiceName}${skuName}`;
  });

  async function getList(reset = false) {
    if (state.loading) return;
    if (reset) {
      state.pagination.pageNo = 1;
      state.pagination.list = [];
      state.pagination.total = 0;
      state.loadStatus = 'more';
    }
    state.loading = true;
    const { code, data } = await SpuApi.getSpuPage({
      pageNo: state.pagination.pageNo,
      pageSize: state.pagination.pageSize,
    });
    if (code === 0) {
      const nextList = data?.list || [];
      state.pagination.list = reset ? nextList : state.pagination.list.concat(nextList);
      state.pagination.total = data?.total || 0;
      state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
    }
    state.loading = false;
  }

  async function openSkuSelector(item) {
    const { code, data } = await SpuApi.getSpuDetail(item.id);
    if (code !== 0 || !data) {
      sheep.$helper.toast('服务详情加载失败');
      return;
    }
    state.currentGoods = {
      skus: [],
      ...data,
    };
    state.showSkuPopup = true;
  }

  function onSkuChange(sku) {
    state.selectedSku = sku || {};
  }

  function onConfirmSku(sku) {
    const nextSku = sku || state.selectedSku;
    if (!nextSku?.id) {
      sheep.$helper.toast('请选择规格');
      return;
    }
    state.selectedSpuId = state.currentGoods.id;
    state.selectedSku = nextSku;
    state.selectedServiceName = state.currentGoods.name || '';
    state.selectedSkuName = formatSkuName(nextSku);
    state.showSkuPopup = false;
  }

  function confirmSelection() {
    if (!state.selectedSpuId || !state.selectedSku?.id) {
      sheep.$helper.toast('请先选择服务项目');
      return;
    }
    if (state.flow === 'addon') {
      sheep.$router.go('/pages/booking/addon', {
        parentOrderId: state.parentOrderId,
        addonType: state.addonType,
        spuId: state.selectedSpuId,
        skuId: state.selectedSku.id,
      });
      return;
    }
    goToOrderConfirm(sheep.$router, {
      timeSlotId: state.timeSlotId,
      technicianId: state.technicianId,
      storeId: state.storeId,
      spuId: state.selectedSpuId,
      skuId: state.selectedSku.id,
    });
  }

  onLoad((options) => {
    state.flow = options.flow || 'create';
    state.timeSlotId = Number(options.timeSlotId || 0);
    state.technicianId = Number(options.technicianId || 0);
    state.storeId = Number(options.storeId || 0);
    state.parentOrderId = Number(options.parentOrderId || 0);
    state.addonType = Number(options.addonType || 0);
    getList(true);
  });

  onReachBottom(() => {
    if (state.loadStatus === 'noMore') {
      return;
    }
    state.pagination.pageNo += 1;
    getList();
  });
</script>

<style lang="scss" scoped>
  .text-gray {
    color: #999;
  }

  .text-orange {
    color: #ff6600;
  }

  .info-row {
    font-size: 24rpx;
  }

  .service-card {
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f5f5f5;

    &:last-child {
      margin-bottom: 0;
      border-bottom: none;
      padding-bottom: 0;
    }
  }

  .service-pic {
    width: 140rpx;
    height: 140rpx;
    border-radius: 12rpx;
    background: #f5f5f5;
  }

  .select-btn {
    min-width: 160rpx;
    height: 64rpx;
    padding: 0 24rpx;
    border-radius: 999rpx;
    background: #fff7e6;
    color: #ad6800;
    font-size: 24rpx;
  }

  .footer-bar {
    height: 112rpx;
  }

  .confirm-btn {
    width: 220rpx;
    height: 72rpx;
    border-radius: 36rpx;
    background: var(--ui-BG-Main, #ff6600);
    color: #fff;
    font-size: 28rpx;
  }
</style>
