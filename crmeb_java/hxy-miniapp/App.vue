<script>
export default {
  onLaunch: function() {
    console.log('App Launch')
    // 初始化配置
    this.initConfig()
  },
  onShow: function() {
    console.log('App Show')
  },
  onHide: function() {
    console.log('App Hide')
  },
  methods: {
    initConfig() {
      // 设置API基础URL
      uni.setStorageSync('apiBaseUrl', 'http://localhost:8081/api/front')
    }
  }
}
</script>

<style>
/* 全局样式 */
@import './common/style/common.css';

page {
  background-color: #F8F8F8;
  font-size: 14px;
  color: #333333;
}
</style>

