# UniApp前端开发指南

> **项目路径**: `/root/crmeb-java/crmeb_java/hxy-miniapp/`  
> **技术栈**: UniApp + Vue2  
> **更新时间**: 2026-02-13

---

## 📁 项目结构

```
hxy-miniapp/
├── pages/                      # 页面
│   ├── index/                  # 首页（门店列表）
│   ├── booking/                # 预约页面
│   ├── order/                  # 订单页面
│   └── my/                     # 我的页面
│
├── components/                 # 公共组件
│   ├── hxy-button/            # 按钮组件
│   ├── hxy-card/              # 卡片组件
│   └── hxy-empty/             # 空状态组件
│
├── common/                     # 公共资源
│   ├── api/                   # API接口
│   │   ├── request.js         # 请求封装
│   │   └── index.js           # 接口管理
│   ├── utils/                 # 工具函数
│   │   └── index.js           # 工具库
│   └── style/                 # 全局样式
│       └── common.css         # 公共样式
│
├── static/                     # 静态资源
│   └── tabbar/                # TabBar图标
│
├── App.vue                     # 应用入口
├── main.js                     # 主入口
├── pages.json                  # 页面配置
└── package.json                # 依赖配置
```

---

## 🎯 已完成功能

### 1. 项目初始化 ✅

- [x] UniApp项目结构
- [x] package.json配置
- [x] pages.json配置（4个TabBar）
- [x] App.vue入口文件

### 2. API封装 ✅

**request.js**：
- [x] 请求拦截器（添加token）
- [x] 响应拦截器（统一错误处理）
- [x] GET/POST/PUT/DELETE方法封装
- [x] Loading提示
- [x] 401自动跳转登录

**index.js**：
- [x] 门店API（getNearbyStores、getStoreDetail、getStoreServices）
- [x] 预约API（getAvailableTimes、getTechnicians、createBooking、cancelBooking）
- [x] 订单API（getOrderList、getOrderDetail、payOrder、verifyOrder）
- [x] 会员API（getMemberInfo、getMemberBenefits、getGrowthLog）
- [x] 用户API（wxLogin、getUserInfo、updateUserInfo）

### 3. 工具函数 ✅

- [x] 日期格式化（formatDate）
- [x] 价格格式化（formatPrice）
- [x] 手机号脱敏（maskPhone）
- [x] 防抖/节流（debounce/throttle）
- [x] 距离文本（getDistanceText）
- [x] 日期文本（getDateText、isToday、isTomorrow）
- [x] 深拷贝（deepClone）
- [x] 本地存储（setStorage、getStorage、removeStorage）

### 4. 公共组件 ✅

- [x] hxy-button（按钮组件）
  - 支持primary/outline/plain类型
  - 支持small/medium/large尺寸
  - 支持disabled/loading状态
  - 支持block块级按钮

- [x] hxy-card（卡片组件）
  - 支持阴影效果
  - 支持点击态
  - 自定义内边距

- [x] hxy-empty（空状态组件）
  - 自定义图标和文字
  - 可选按钮

### 5. 全局样式 ✅

- [x] CSS变量（颜色、字体）
- [x] 布局类（flex、flex-center、flex-between）
- [x] 文字类（text-primary、text-light、text-bold）
- [x] 间距类（mt-10、mb-20、p-30）
- [x] 卡片样式
- [x] 按钮样式
- [x] 标签样式
- [x] 徽章样式
- [x] 省略号样式

---

## 🎯 待开发功能

### 1. 首页（pages/index/）

**功能**：
- [ ] LBS定位获取用户位置
- [ ] 附近门店列表展示
- [ ] 门店切换功能
- [ ] 服务推荐位
- [ ] 下拉刷新

**组件**：
- [ ] store-card（门店卡片）
- [ ] service-card（服务卡片）

### 2. 预约页（pages/booking/）

**功能**：
- [ ] 服务列表展示
- [ ] 服务分类切换
- [ ] 时间选择器
- [ ] 技师选择器
- [ ] 订单确认

**组件**：
- [ ] service-list（服务列表）
- [ ] time-picker（时间选择器）
- [ ] technician-selector（技师选择器）

### 3. 订单页（pages/order/）

**功能**：
- [ ] 订单列表（待服务/已完成）
- [ ] 订单详情
- [ ] 核销码展示
- [ ] 订单取消
- [ ] 下拉刷新

**组件**：
- [ ] order-card（订单卡片）
- [ ] qrcode（二维码）

### 4. 我的页（pages/my/）

**功能**：
- [ ] 用户信息展示
- [ ] 会员等级展示
- [ ] 资产看板
- [ ] 功能入口

**组件**：
- [ ] user-header（用户头部）
- [ ] member-card（会员卡片）
- [ ] asset-panel（资产面板）

---

## 🎯 开发规范

### 命名规范

**文件命名**：
- 页面：kebab-case（如：store-list.vue）
- 组件：kebab-case（如：hxy-button.vue）
- 工具：camelCase（如：formatDate）

**变量命名**：
- 常量：UPPER_CASE（如：BASE_URL）
- 变量：camelCase（如：userInfo）
- 组件：PascalCase（如：HxyButton）

### 代码规范

**Vue组件结构**：
```vue
<template>
  <!-- 模板 -->
</template>

<script>
export default {
  name: 'ComponentName',
  props: {},
  data() {
    return {}
  },
  computed: {},
  methods: {},
  onLoad() {},
  onShow() {}
}
</script>

<style scoped>
/* 样式 */
</style>
```

**API调用**：
```javascript
import { storeApi } from '@/common/api'

// 使用async/await
async loadData() {
  try {
    const data = await storeApi.getNearbyStores({ lat, lng })
    this.storeList = data
  } catch (error) {
    console.error('加载失败', error)
  }
}
```

---

## 🎯 下一步开发

### Day 3-5: 首页开发

**任务清单**：
1. 创建首页布局
2. 实现LBS定位
3. 门店列表展示
4. 门店切换功能
5. 服务推荐位

**验收标准**：
- ✅ 用户可查看附近门店
- ✅ 可切换门店
- ✅ 可查看推荐服务

---

**文档版本**: V1.0  
**更新时间**: 2026-02-13


