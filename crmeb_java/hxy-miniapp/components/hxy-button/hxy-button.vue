<template>
  <view class="hxy-button" :class="buttonClass" :hover-class="hoverClass" @tap="handleClick">
    <text v-if="loading" class="loading-icon">⏳</text>
    <text>{{ text }}</text>
  </view>
</template>

<script>
export default {
  name: 'HxyButton',
  props: {
    // 按钮文字
    text: {
      type: String,
      default: '按钮'
    },
    // 按钮类型：primary, outline, plain
    type: {
      type: String,
      default: 'primary'
    },
    // 按钮尺寸：small, medium, large
    size: {
      type: String,
      default: 'medium'
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    },
    // 是否加载中
    loading: {
      type: Boolean,
      default: false
    },
    // 是否块级按钮
    block: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    buttonClass() {
      return [
        `btn-${this.type}`,
        `btn-${this.size}`,
        {
          'btn-disabled': this.disabled || this.loading,
          'btn-block': this.block
        }
      ]
    },
    hoverClass() {
      return this.disabled || this.loading ? '' : 'btn-hover'
    }
  },
  methods: {
    handleClick() {
      if (this.disabled || this.loading) return
      this.$emit('click')
    }
  }
}
</script>

<style scoped>
.hxy-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 20rpx 40rpx;
  border-radius: 8rpx;
  font-size: 28rpx;
  transition: all 0.3s;
}

/* 类型 */
.btn-primary {
  background: #4CAF50;
  color: #FFFFFF;
}

.btn-outline {
  background: transparent;
  border: 2rpx solid #4CAF50;
  color: #4CAF50;
}

.btn-plain {
  background: #F5F5F5;
  color: #333333;
}

/* 尺寸 */
.btn-small {
  padding: 12rpx 24rpx;
  font-size: 24rpx;
}

.btn-medium {
  padding: 20rpx 40rpx;
  font-size: 28rpx;
}

.btn-large {
  padding: 28rpx 56rpx;
  font-size: 32rpx;
}

/* 状态 */
.btn-disabled {
  opacity: 0.6;
}

.btn-hover {
  opacity: 0.8;
}

.btn-block {
  display: flex;
  width: 100%;
}

.loading-icon {
  margin-right: 8rpx;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>

