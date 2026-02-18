# 荷小悦现有项目 UI 预览指南

> **更新时间**: 2026-02-08  
> **Docker状态**: ✅ 运行中

---

## 📊 当前运行状态

### Docker 容器状态
```
✅ hexiaoyue_nginx  - 运行中 (端口: 8080)
✅ hexiaoyue_php    - 运行中
✅ hexiaoyue_mysql  - 运行中 (端口: 3307)
```

---

## 🌐 预览地址

### 1. 后端管理系统（Admin）

**访问地址**:
```
http://115.190.245.14:8080/admin
```

或本地访问：
```
http://localhost:8080/admin
```

**功能说明**:
- 📊 商品管理（SPU/SKU）
- 👥 用户管理
- 💰 订单管理
- 🎫 优惠券管理
- 📈 数据统计
- ⚙️ 系统设置

---

### 2. 前台页面（PC端）

**访问地址**:
```
http://115.190.245.14:8080
```

或本地访问：
```
http://localhost:8080
```

**功能说明**:
- 🏠 首页展示
- 🛍️ 商品列表
- 🛒 购物车
- 👤 用户中心

---

### 3. 移动端小程序（Uni-app）

**项目路径**:
```
/root/crmeb/CRMEB-5.6.3.1/template/uni-app
```

**预览方式**:

#### 方式1: HBuilderX 预览（推荐）
1. 用 HBuilderX 打开 `template/uni-app` 目录
2. 点击 **运行 → 运行到浏览器 → Chrome**
3. 或点击 **运行 → 运行到小程序模拟器 → 微信开发者工具**

#### 方式2: 命令行启动开发服务器
```bash
cd /root/crmeb/CRMEB-5.6.3.1/template/uni-app
npm install
npm run dev:h5
```

然后访问：
```
http://localhost:8081
```

#### 方式3: 微信开发者工具预览
1. 打开微信开发者工具
2. 导入项目：`/root/crmeb/CRMEB-5.6.3.1/template/uni-app`
3. 选择小程序模式
4. 实时预览和调试

---

## 🎨 UI 组件库

### Admin 后台（Vue 2 + Element UI）

**路径**: `/root/crmeb/CRMEB-5.6.3.1/template/admin`

**技术栈**:
- Vue 2.x
- Element UI
- Vue Router
- Vuex

**预览方式**:
```bash
cd /root/crmeb/CRMEB-5.6.3.1/template/admin
npm install
npm run dev
```

访问地址：
```
http://localhost:9527
```

---

### Uni-app 移动端（Vue 2 + uni-ui）

**路径**: `/root/crmeb/CRMEB-5.6.3.1/template/uni-app`

**技术栈**:
- Vue 2.x
- uni-ui 组件库
- uView UI（可选）
- Vuex

**页面结构**:
```
template/uni-app/
├── pages/
│   ├── index/          # 首页
│   ├── goods/          # 商品相关
│   ├── order/          # 订单相关
│   ├── user/           # 用户中心
│   └── ...
├── components/         # 公共组件
├── static/            # 静态资源
└── uni_modules/       # uni-app 插件
```

---

## 🔧 快速启动指南

### 启动后端服务（已运行）
```bash
cd /root/crmeb/CRMEB-5.6.3.1
docker-compose -f docker-compose.hexiaoyue.yml up -d
```

### 启动前端开发服务器

#### Admin 后台
```bash
cd /root/crmeb/CRMEB-5.6.3.1/template/admin
npm install
npm run dev
```

#### Uni-app 移动端
```bash
cd /root/crmeb/CRMEB-5.6.3.1/template/uni-app
npm install
npm run dev:h5        # H5 预览
npm run dev:mp-weixin # 微信小程序
```

---

## 📱 移动端预览方式对比

| 预览方式 | 优点 | 缺点 | 推荐度 |
|---------|------|------|--------|
| **HBuilderX** | 集成开发环境，功能完整 | 需要安装软件 | ⭐⭐⭐⭐⭐ |
| **H5 浏览器** | 快速预览，无需安装 | 部分API不支持 | ⭐⭐⭐⭐ |
| **微信开发者工具** | 真实小程序环境 | 需要配置AppID | ⭐⭐⭐⭐⭐ |
| **真机调试** | 最真实的体验 | 需要扫码 | ⭐⭐⭐⭐⭐ |

---

## 🎯 推荐预览流程

### 第一步：查看后端管理系统
```
http://115.190.245.14:8080/admin
```
- 了解现有的商品管理、订单管理等功能
- 查看数据结构和业务逻辑

### 第二步：启动 Uni-app H5 预览
```bash
cd /root/crmeb/CRMEB-5.6.3.1/template/uni-app
npm run dev:h5
```
- 在浏览器中查看移动端UI
- 测试页面跳转和交互

### 第三步：微信开发者工具预览（可选）
- 打开微信开发者工具
- 导入 `template/uni-app` 项目
- 查看真实小程序效果

---

## 🔍 查看现有 UI 组件

### 查看 Uni-app 页面列表
```bash
ls -la /root/crmeb/CRMEB-5.6.3.1/template/uni-app/pages/
```

### 查看公共组件
```bash
ls -la /root/crmeb/CRMEB-5.6.3.1/template/uni-app/components/
```

### 查看主题配置
```bash
cat /root/crmeb/CRMEB-5.6.3.1/template/uni-app/App.vue
cat /root/crmeb/CRMEB-5.6.3.1/template/uni-app/uni.scss
```

---

## 📸 截图工具

### 浏览器截图
- Chrome DevTools → 切换到移动设备模式（F12）
- 选择设备：iPhone 12 Pro / 小米手机等
- 截图：右键 → Capture screenshot

### 微信开发者工具截图
- 工具栏 → 截图按钮
- 或使用快捷键截图

---

## 🚀 下一步：对比设计

### 对比维度

1. **首页布局**
   - 现有：查看 `template/uni-app/pages/index/index.vue`
   - 新设计：参考 `/root/crmeb-java/hxy/plan/product/UI预览-快速访问.md`

2. **会员卡片**
   - 现有：查看用户中心页面
   - 新设计：Lannlife + 瑞幸风格融合

3. **服务列表**
   - 现有：查看商品列表页面
   - 新设计：大图 + 价格锚点 + HOT标签

4. **色彩系统**
   - 现有：查看 `uni.scss` 中的颜色变量
   - 新设计：紫色主题 + 红色价格

---

## 💡 快速对比命令

### 启动现有项目预览
```bash
# 终端1: 后端已运行
# 访问 http://115.190.245.14:8080

# 终端2: 启动 uni-app H5
cd /root/crmeb/CRMEB-5.6.3.1/template/uni-app && npm run dev:h5
```

### 启动新设计预览
```bash
# 终端3: 新设计预览（已启动）
# 访问 http://115.190.245.14:8888/前端设计预览.html
```

### 并排对比
- 左侧浏览器：现有项目 UI
- 右侧浏览器：新设计方案
- 对比差异，提取改进点

---

## 📋 预览检查清单

- [ ] 后端管理系统可访问
- [ ] 前台页面可访问
- [ ] Uni-app H5 可预览
- [ ] 微信开发者工具可打开
- [ ] 查看首页布局
- [ ] 查看商品列表
- [ ] 查看用户中心
- [ ] 查看订单页面
- [ ] 对比新旧设计
- [ ] 提取改进建议

---

## 🎨 设计对比要点

### 现有 UI → 新设计改进

| 模块 | 现有特点 | 新设计改进 |
|------|---------|-----------|
| **首页** | 传统电商布局 | Lannlife高级感 + 大图背景 |
| **会员卡** | 简单等级显示 | 瑞幸风格进度条 + VIP徽章 |
| **价格展示** | 单一价格 | 价格锚点（大字会员价 + 划线原价） |
| **服务列表** | 列表式 | 大图卡片 + HOT标签 + 时长标签 |
| **色彩** | 常规配色 | 紫色主题 + 红色促销 |
| **新人引流** | 无明显入口 | 9.9/19.9元爆款突出展示 |

---

**现在就可以开始预览和对比了！** 🎉

需要我帮您启动 uni-app 的开发服务器吗？

