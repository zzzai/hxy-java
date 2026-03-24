<template>
  <s-layout title="礼品卡" navbar="inner">
    <view class="page-wrap ss-p-24">
      <view class="hero-card ss-m-b-20">
        <view class="hero-title">礼品卡</view>
        <view class="hero-desc">选择礼品卡模板，创建自用或赠送订单；当前仍属于工程闭环阶段，不代表已可放量。</view>
      </view>

      <s-empty v-if="!state.loading && state.templates.length === 0" text="暂无礼品卡模板" icon="/static/data-empty.png" />

      <view v-for="item in state.templates" :key="item.templateId" class="template-card ss-m-b-20">
        <view class="template-title">{{ item.title }}</view>
        <view class="template-meta">面值 ¥{{ fen2yuan(item.faceValue) }} · 库存 {{ item.stock || 0 }} · 有效期 {{ item.validDays }} 天</view>
        <view class="action-row ss-m-t-16">
          <button class="action-btn ss-reset-button" @tap="createOrder(item, 'SELF')">自用下单</button>
          <button class="action-btn ghost ss-reset-button" @tap="createOrder(item, 'GIFT')">赠送下单</button>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import GiftCardApi from '@/sheep/api/promotion/giftCard';
  import { fen2yuan } from '@/sheep/hooks/useGoods';

  const state = reactive({
    loading: false,
    templates: [],
  });

  async function loadTemplates() {
    state.loading = true;
    const { code, data } = await GiftCardApi.getTemplatePage({ pageNo: 1, pageSize: 20, status: 'ENABLE' });
    state.loading = false;
    if (code !== 0) {
      return;
    }
    state.templates = data?.list || [];
  }

  async function createOrder(item, sendScene) {
    const { code, data } = await GiftCardApi.createOrder({
      templateId: item.templateId,
      quantity: 1,
      sendScene,
      clientToken: `gift-create-${item.templateId}-${Date.now()}`,
    });
    if (code !== 0) {
      return;
    }
    sheep.$router.go('/pages/gift-card/order-detail', { orderId: data?.orderId });
  }

  onLoad(() => {
    loadTemplates();
  });
</script>

<style scoped lang="scss">
  .page-wrap {
    min-height: 100vh;
    background: #f7f7f7;
  }
  .hero-card,
  .template-card {
    padding: 28rpx;
    border-radius: 24rpx;
    background: #ffffff;
    box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.06);
  }
  .hero-title,
  .template-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #243b53;
  }
  .hero-desc,
  .template-meta {
    margin-top: 12rpx;
    font-size: 24rpx;
    color: #6b7c93;
    line-height: 1.7;
  }
  .action-row {
    display: flex;
    gap: 16rpx;
  }
  .action-btn {
    flex: 1;
    height: 72rpx;
    border-radius: 999rpx;
    background: #f59e0b;
    color: #fff;
    font-size: 24rpx;
  }
  .action-btn.ghost {
    background: #fff7e6;
    color: #ad6800;
  }
</style>
