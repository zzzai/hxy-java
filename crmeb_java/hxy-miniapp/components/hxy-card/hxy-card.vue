<template>
  <view class="hxy-card" :class="cardClass" @tap="handleClick">
    <slot></slot>
  </view>
</template>

<script>
export default {
  name: 'HxyCard',
  props: {
    // 是否有阴影
    shadow: {
      type: Boolean,
      default: true
    },
    // 是否可点击
    clickable: {
      type: Boolean,
      default: false
    },
    // 内边距
    padding: {
      type: String,
      default: '24rpx'
    }
  },
  computed: {
    cardClass() {
      return {
        'card-shadow': this.shadow,
        'card-clickable': this.clickable
      }
    }
  },
  methods: {
    handleClick() {
      if (this.clickable) {
        this.$emit('click')
      }
    }
  }
}
</script>

<style scoped>
.hxy-card {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}

.card-shadow {
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.card-clickable {
  transition: all 0.3s;
}

.card-clickable:active {
  transform: scale(0.98);
  opacity: 0.9;
}
</style>

