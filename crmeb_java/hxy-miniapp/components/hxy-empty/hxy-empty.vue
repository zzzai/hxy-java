<template>
  <view class="hxy-empty">
    <view class="empty-icon">{{ icon }}</view>
    <view class="empty-text">{{ text }}</view>
    <view v-if="showButton" class="empty-button">
      <hxy-button :text="buttonText" type="outline" size="small" @click="handleClick"></hxy-button>
    </view>
  </view>
</template>

<script>
export default {
  name: 'HxyEmpty',
  props: {
    // 图标
    icon: {
      type: String,
      default: '📭'
    },
    // 文字
    text: {
      type: String,
      default: '暂无数据'
    },
    // 是否显示按钮
    showButton: {
      type: Boolean,
      default: false
    },
    // 按钮文字
    buttonText: {
      type: String,
      default: '刷新'
    }
  },
  methods: {
    handleClick() {
      this.$emit('click')
    }
  }
}
</script>

<style scoped>
.hxy-empty {
  padding: 100rpx 0;
  text-align: center;
}

.empty-icon {
  font-size: 120rpx;
  margin-bottom: 20rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #999999;
  margin-bottom: 40rpx;
}

.empty-button {
  display: flex;
  justify-content: center;
}
</style>

