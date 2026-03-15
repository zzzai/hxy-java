<!-- 首页，支持店铺装修 -->
<template>
  <view v-if="template">
    <s-layout
      title="首页"
      navbar="custom"
      tabbar="/pages/index/index"
      :bgStyle="templatePage"
      :navbarStyle="templateNavbar"
      onShareAppMessage
    >
      <s-block
        v-for="(item, index) in templateComponents"
        :key="index"
        :styles="item.property.style"
      >
        <s-block-item :type="item.id" :data="item.property" :styles="item.property.style" />
      </s-block>
      <view v-if="templateComponents.length === 0" class="empty-placeholder">
        <view class="empty-title">首页模板未配置</view>
        <view class="empty-desc">请在管理后台完成商城装修后刷新小程序</view>
      </view>
    </s-layout>
  </view>
</template>

<script setup>
  import { computed } from 'vue';
  import { onLoad, onShow, onPageScroll, onPullDownRefresh } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import $share from '@/sheep/platform/share';
  // 隐藏原生tabBar
  uni.hideTabBar({
    fail: () => {},
  });

  const template = computed(() => sheep.$store('app').template?.home || {});
  const templateComponents = computed(() =>
    Array.isArray(template.value?.components) ? template.value.components : [],
  );
  const templatePage = computed(() => template.value?.page || {});
  const templateNavbar = computed(() => template.value?.navigationBar || {});
  // 在此处拦截改变一下首页轮播图 此处先写死后期复活 放到启动函数里
  // (async function() {
  // console.log('原代码首页定制化数据',template)
  // let {
  // 	data
  // } = await index2Api.decorate();
  // console.log('首页导航配置化过高无法兼容',JSON.parse(data[1].value))
  // 改变首页底部数据 但是没有通过数组id获取商品数据接口
  // let {
  // 	data: datas
  // } = await index2Api.spids();
  // template.value.data[9].data.goodsIds = datas.list.map(item => item.id);
  // template.value.data[0].data.list = JSON.parse(data[0].value).map(item => {
  // 	return {
  // 		src: item.picUrl,
  // 		url: item.url,
  // 		title: item.name,
  // 		type: "image"
  // 	}
  // })
  // }())

  onLoad((options) => {
    // #ifdef MP
    // 小程序识别二维码
    if (options.scene) {
      const sceneParams = decodeURIComponent(options.scene).split('=');
      console.log('sceneParams=>', sceneParams);
      options[sceneParams[0]] = sceneParams[1];
    }
    // #endif

    // 预览模板
    if (options.templateId) {
      sheep.$store('app').init(options.templateId);
    }

    // 解析分享信息
    if (options.spm) {
      $share.decryptSpm(options.spm);
    }

    // 进入指定页面(完整页面路径)
    if (options.page) {
      sheep.$router.go(decodeURIComponent(options.page));
    }
  });

  onShow(async () => {
    // #ifdef APP-PLUS
    // ios首次授权网络，需要重新加载一次应用初始化
    // 可能需要考虑上uni.onNetworkStatusChange，uni.offNetworkStatusChange组合拳以及主动主动唤起权限申请
    // 一开始放app.vue，感觉负载太重，搬到这里来了。
    // 如果你的首页不是这个页面，需要把代码搬过去。
    if (sheep.$platform.os === 'ios') {
      if (await sheep.$platform.checkNetwork()) {
        await sheep.$store('app').init();
      }
    }
    // #endif
  });

  // 下拉刷新
  onPullDownRefresh(() => {
    sheep.$store('app').init();
    setTimeout(function () {
      uni.stopPullDownRefresh();
    }, 800);
  });

  onPageScroll(() => {});
</script>

<style scoped lang="scss">
  .empty-placeholder {
    margin: 80rpx 24rpx 0;
    padding: 28rpx 24rpx;
    border-radius: 16rpx;
    background: #ffffff;
    text-align: center;
    box-shadow: 0 8rpx 28rpx rgba(0, 0, 0, 0.06);
  }

  .empty-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #303133;
  }

  .empty-desc {
    margin-top: 12rpx;
    font-size: 24rpx;
    color: #606266;
  }
</style>
