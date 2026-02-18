# 荷小悦 UI 预览 - 快速访问

> **创建时间**: 2026-02-08  
> **状态**: Docker 运行中 ✅

---

## 🚀 立即预览（无需安装）

### 1. 后端管理系统
```
http://115.190.245.14:8080/admin
```
**功能**: 商品管理、订单管理、用户管理、系统设置

---

### 2. 前台页面
```
http://115.190.245.14:8080
```
**功能**: 商品展示、购物车、用户中心

---

### 3. 新设计方案预览
```
http://115.190.245.14:8888/前端设计预览.html
```
**功能**: Lannlife + 瑞幸风格的新设计方案

---

## 📱 移动端预览方式

### 方式1: 使用 HBuilderX（推荐）⭐⭐⭐⭐⭐

**步骤**:
1. 下载 HBuilderX: https://www.dcloud.io/hbuilderx.html
2. 打开项目: `/root/crmeb/CRMEB-5.6.3.1/template/uni-app`
3. 点击: **运行 → 运行到浏览器 → Chrome**
4. 自动打开预览页面

**优点**: 
- ✅ 无需手动安装依赖
- ✅ 集成开发环境
- ✅ 实时热更新
- ✅ 可直接运行到微信开发者工具

---

### 方式2: 微信开发者工具（真实小程序环境）⭐⭐⭐⭐⭐

**步骤**:
1. 下载微信开发者工具: https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html
2. 打开工具 → 导入项目
3. 项目目录: `/root/crmeb/CRMEB-5.6.3.1/template/uni-app`
4. AppID: 使用测试号或自己的 AppID
5. 点击编译，即可预览

**优点**:
- ✅ 真实小程序环境
- ✅ 完整的 API 支持
- ✅ 可扫码真机预览
- ✅ 调试工具完善

---

### 方式3: 命令行启动（需要 Node.js）⭐⭐⭐

**前提**: 需要安装 Node.js 16+

**步骤**:
```bash
# 1. 进入项目目录
cd /root/crmeb/CRMEB-5.6.3.1/template/uni-app

# 2. 安装 HBuilderX CLI（如果没有）
npm install -g @dcloudio/uvm
uvm install

# 3. 或者使用 uni-app 官方 CLI
npm install -g @vue/cli
vue create -p dcloudio/uni-preset-vue my-project

# 注意: 原项目可能需要在 HBuilderX 中运行
```

---

## 🎨 现有 UI 页面结构

### 主要页面

| 页面 | 路径 | 说明 |
|------|------|------|
| **引导页** | `pages/guide/index` | 首次启动引导 |
| **首页** | `pages/index/index` | 商品展示、分类、活动 |
| **购物车** | `pages/order_addcart/order_addcart` | 购物车列表 |
| **个人中心** | `pages/user/index` | 用户信息、订单、设置 |

### 查看页面代码
```bash
# 查看首页
cat /root/crmeb/CRMEB-5.6.3.1/template/uni-app/pages/index/index.vue

# 查看个人中心
cat /root/crmeb/CRMEB-5.6.3.1/template/uni-app/pages/user/index.vue

# 查看购物车
cat /root/crmeb/CRMEB-5.6.3.1/template/uni-app/pages/order_addcart/order_addcart.vue
```

---

## 📊 三种预览对比

| 预览方式 | 访问地址 | 特点 | 推荐场景 |
|---------|---------|------|---------|
| **后端管理** | http://115.190.245.14:8080/admin | 完整后台功能 | 查看数据管理 |
| **前台页面** | http://115.190.245.14:8080 | PC端展示 | 查看前台功能 |
| **新设计预览** | http://115.190.245.14:8888/前端设计预览.html | 静态设计稿 | 对比新旧设计 |
| **Uni-app** | HBuilderX 运行 | 移动端真实效果 | 查看小程序UI |

---

## 🔍 快速对比现有 UI

### 查看现有首页设计
```bash
# 查看首页布局
cat /root/crmeb/CRMEB-5.6.3.1/template/uni-app/pages/index/index.vue | head -100

# 查看样式配置
cat /root/crmeb/CRMEB-5.6.3.1/template/uni-app/uni.scss
```

### 查看现有色彩系统
```bash
# 查看全局样式
cat /root/crmeb/CRMEB-5.6.3.1/template/uni-app/App.vue
```

---

## 💡 推荐预览流程

### Step 1: 查看后端管理（了解功能）
```
👉 http://115.190.245.14:8080/admin
```
- 查看商品管理界面
- 了解订单流程
- 查看会员系统（如果有）

### Step 2: 查看前台页面（了解布局）
```
👉 http://115.190.245.14:8080
```
- 查看首页布局
- 查看商品列表
- 查看用户中心

### Step 3: 查看新设计方案（对比改进）
```
👉 http://115.190.245.14:8888/前端设计预览.html
```
- 对比首页设计
- 对比会员卡片
- 对比价格展示
- 对比色彩系统

### Step 4: 使用 HBuilderX 查看移动端（可选）
- 下载 HBuilderX
- 打开 uni-app 项目
- 运行到浏览器或微信开发者工具

---

## 📸 截图对比建议

### 现有 UI 截图位置
1. **首页**: 打开 http://115.190.245.14:8080
2. **用户中心**: 登录后查看个人中心
3. **商品列表**: 查看商品分类页面

### 新设计截图位置
1. **首页**: http://115.190.245.14:8888/前端设计预览.html
2. **我的页面**: 点击"我的"按钮切换

### 对比维度
- ✅ 首页布局差异
- ✅ 会员卡片设计
- ✅ 价格展示方式
- ✅ 色彩系统对比
- ✅ 服务列表样式
- ✅ 新人引流品展示

---

## 🎯 下一步行动

### 如果您想查看移动端真实效果:
**推荐使用 HBuilderX**（最简单）
1. 下载: https://www.dcloud.io/hbuilderx.html
2. 打开项目: `/root/crmeb/CRMEB-5.6.3.1/template/uni-app`
3. 运行到浏览器

### 如果您只想快速对比设计:
**直接访问三个地址**
1. 现有后台: http://115.190.245.14:8080/admin
2. 现有前台: http://115.190.245.14:8080
3. 新设计: http://115.190.245.14:8888/前端设计预览.html

---

## 📞 需要帮助？

如果您需要:
- ✅ 查看具体页面的代码
- ✅ 对比现有UI和新设计的差异
- ✅ 提取现有UI的优点
- ✅ 制定UI改造方案

随时告诉我！😊

