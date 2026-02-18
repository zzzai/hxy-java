# 荷小悦小程序产品规划方案

> **文档版本**: V1.0  
> **技术底座**: CRMEB (ThinkPHP 6 + UniApp)  
> **参考标杆**: 瑞幸咖啡小程序运营模式  
> **核心理念**: 新中式 · 草本疗愈 · 社区第三空间

---

## 一、产品战略定位

### 1.1 核心定位

荷小悦小程序不是传统的预约工具，而是：

| 维度 | 定位描述 | 对标参考 |
|------|----------|----------|
| **功能定位** | AI驱动的健康管理助手 | 私人医生 + 星巴克会员体系 |
| **体验定位** | 游戏化的养生陪伴平台 | Keep游戏化 + 瑞幸裂变 |
| **社交定位** | 社区化的健康生活方式 | 小红书社区 + 邻里社交 |

### 1.2 产品Slogan

> **"荷小悦，你的AI养生管家"**

让每个人都能拥有一个24小时在线、懂你身体、陪你变健康的贴心伙伴。

### 1.3 第一性原理

基于品牌战略文档，小程序的核心使命是：

```
让人离开后感觉明显不一样
        ↓
通过数字化手段，让"效果感知"可视化、可追踪、可分享
        ↓
建立"效果→信任→复购→裂变"的完整闭环
```

---

## 二、核心用户画像

### 2.1 目标用户（基于艾瑞研报数据）

| 维度 | 核心客群 | 占比 |
|------|----------|------|
| **性别** | 女性用户 | 70% |
| **年龄** | 25-40岁职场女性 | 核心客群 |
| **职业** | 企业职员/管理者、全职妈妈 | 上班族为主 |
| **收入** | 家庭月收入14,000-30,000元 | 中产阶层 |
| **痛点** | 湿气重、睡眠差、肩颈痛、压力大 | 亚健康问题 |

### 2.2 用户分层运营策略

借鉴瑞幸会员体系，设计荷小悦六级会员等级：

| 等级 | 名称 | 成长值门槛 | 核心权益 | 瑞幸对标 |
|------|------|------------|----------|----------|
| V0 | 荷芽 | 0-99 | 新人首单半价 | 小迷鹿 |
| V1 | 荷叶 | 100-500 | 9.5折+生日礼 | 小蓝鹿 |
| V2 | 荷花 | 501-1000 | 9折+优先预约 | 小银鹿 |
| V3 | 荷露 | 1001-2000 | 8.5折+专属技师 | 小金鹿 |
| V4 | 荷月 | 2001-5000 | 8折+季度礼盒 | 小钻鹿 |
| V5 | 荷尊 | 5001-10000 | 7.5折+私人顾问 | 黑金鹿 |
| V6 | 荷仙 | 10000+ | 7折+全年免费 | 九色鹿 |

**成长值获取方式**（小蓝豆 → 荷气值）：
- 消费1元 = 1荷气值（小程序）/ 2荷气值（APP）
- 每日签到 = 5荷气值
- 完成健康挑战 = 50-200荷气值
- 邀请好友 = 100荷气值/人
- 发布UGC内容 = 20荷气值/条

---

## 三、功能架构设计

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      荷小悦小程序                            │
├─────────────┬─────────────┬─────────────┬─────────────────┤
│   首页      │   预约      │   发现      │    我的         │
│  (会员中心) │  (服务商城) │  (社交游戏) │  (个人中心)     │
├─────────────┼─────────────┼─────────────┼─────────────────┤
│ • 会员卡片  │ • 门店切换  │ • 健康挑战  │ • 订单管理      │
│ • 资产看板  │ • 服务列表  │ • 养生社区  │ • 健康档案      │
│ • AI诊断    │ • 技师选择  │ • 虚拟农场  │ • 分销中心      │
│ • 快捷预约  │ • 时间选择  │ • 排行榜    │ • 设置帮助      │
└─────────────┴─────────────┴─────────────┴─────────────────┘
```

### 3.2 底部TabBar设计

借鉴瑞幸简洁设计，采用4Tab结构：

```javascript
// tabBar配置
{
  "list": [
    {
      "pagePath": "pages/index/index",
      "text": "首页",
      "iconPath": "static/tabbar/home.png",
      "selectedIconPath": "static/tabbar/home-active.png"
    },
    {
      "pagePath": "pages/booking/index",
      "text": "预约",
      "iconPath": "static/tabbar/booking.png",
      "selectedIconPath": "static/tabbar/booking-active.png"
    },
    {
      "pagePath": "pages/discover/index",
      "text": "发现",
      "iconPath": "static/tabbar/discover.png",
      "selectedIconPath": "static/tabbar/discover-active.png"
    },
    {
      "pagePath": "pages/my/index",
      "text": "我的",
      "iconPath": "static/tabbar/my.png",
      "selectedIconPath": "static/tabbar/my-active.png"
    }
  ]
}
```

---

## 四、核心功能模块详解

### 4.1 首页（会员中心化改造）

**设计理念**："我的身份"大于"商品列表"，像星巴克一样建立会员心智

#### 4.1.1 CRMEB改造点

| 改造类型 | 具体内容 |
|----------|----------|
| **移除** | 轮播图广告、秒杀拼团、商品瀑布流、购物车图标 |
| **新增** | 会员状态卡片、资产看板、AI诊断入口、快捷预约 |

#### 4.1.2 首页布局设计

```
┌─────────────────────────────────────┐
│  状态栏                              │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐   │
│  │     会员状态卡片              │   │
│  │  ┌─────┐  荷小悦·荷叶会员    │   │
│  │  │头像 │  当前等级: V2荷花   │   │
│  │  └─────┘  成长值: 680/1000   │   │
│  │  ████████████░░░░░ 68%       │   │
│  │  [查看权益]  [升级攻略]       │   │
│  └─────────────────────────────┘   │
├─────────────────────────────────────┤
│  ┌────────┬────────┬────────┐     │
│  │  ¥288  │  3张   │  2次   │     │
│  │  余额  │ 优惠券 │ 次卡   │     │
│  └────────┴────────┴────────┘     │
├─────────────────────────────────────┤
│  [AI健康诊断]  [立即预约]  [送礼]   │
├─────────────────────────────────────┤
│  最近预约                            │
│  ┌─────────────────────────────┐   │
│  │ 世纪大道店  今天 20:00  待服务 │   │
│  │ 60分钟推拿+艾灸    [查看详情] │   │
│  └─────────────────────────────┘   │
├─────────────────────────────────────┤
│  AI推荐 · 猜你喜欢                   │
│  ┌────────┐ ┌────────┐ ┌────────┐ │
│  │祛湿套餐│ │肩颈调理│ │睡眠改善│ │
│  │ ¥128   │ │ ¥168   │ │ ¥148   │ │
│  └────────┘ └────────┘ └────────┘ │
└─────────────────────────────────────┘
```

#### 4.1.3 会员卡片设计（星巴克式）

```vue
<!-- 会员卡片组件 -->
<template>
  <view class="member-card">
    <view class="card-bg" :class="`level-${userInfo.level}`">
      <view class="user-info">
        <image class="avatar" :src="userInfo.avatar" />
        <view class="info">
          <text class="nickname">{{userInfo.nickname}}</text>
          <view class="level-tag">
            <image class="level-icon" :src="levelIcon" />
            <text>荷小悦·{{levelName}}</text>
          </view>
        </view>
      </view>
      <view class="progress-section">
        <text class="progress-text">成长值 {{userInfo.growth}}/{{nextLevelGrowth}}</text>
        <progress :percent="growthPercent" activeColor="#FFD700" />
        <text class="hint">再消费¥{{needAmount}}升级{{nextLevelName}}</text>
      </view>
      <view class="action-btns">
        <button class="btn-outline" @click="goBenefits">查看权益</button>
        <button class="btn-primary" @click="goUpgrade">升级攻略</button>
      </view>
    </view>
  </view>
</template>
```

---

### 4.2 预约模块（核心交易链路）

**设计理念**：3步完成预约，比点外卖还简单

#### 4.2.1 预约流程

```
选门店 → 选服务 → 选时间/技师 → 确认支付 → 预约成功
  ↓        ↓          ↓            ↓           ↓
LBS定位  分类展示   时段可视化    余额优先    核销码生成
```

#### 4.2.2 门店选择（LBS定位）

```vue
<!-- 门店选择组件 -->
<template>
  <view class="store-selector">
    <view class="current-store" @click="showStoreList">
      <text class="icon-location">📍</text>
      <text class="store-name">{{currentStore.name}}</text>
      <text class="distance">{{currentStore.distance}}m</text>
      <text class="arrow">></text>
    </view>
    
    <!-- 门店列表弹窗 -->
    <uni-popup ref="storePopup" type="bottom">
      <view class="store-list">
        <view class="popup-header">
          <text>选择门店</text>
          <text class="close" @click="closePopup">×</text>
        </view>
        <scroll-view scroll-y class="list">
          <view 
            v-for="store in storeList" 
            :key="store.id"
            class="store-item"
            :class="{active: store.id === currentStore.id}"
            @click="selectStore(store)"
          >
            <view class="store-info">
              <text class="name">{{store.name}}</text>
              <text class="address">{{store.address}}</text>
              <view class="tags">
                <text class="tag">{{store.distance}}m</text>
                <text class="tag" v-if="store.isOpen">营业中</text>
              </view>
            </view>
            <text class="check" v-if="store.id === currentStore.id">✓</text>
          </view>
        </scroll-view>
      </view>
    </uni-popup>
  </view>
</template>
```

#### 4.2.3 服务列表设计

```
┌─────────────────────────────────────┐
│  世纪大道店  营业中 10:00-22:00     │
├─────────────────────────────────────┤
│  ┌────────┐                         │
│  │ 足疗   │  ┌─────────────────┐   │
│  │ 推拿   │  │ 经典足疗 60分钟  │   │
│  │ 艾灸   │  │ ¥88  会员¥79    │   │
│  │ 拔罐   │  │ [立即预约]      │   │
│  │ 组合   │  └─────────────────┘   │
│  └────────┘  ┌─────────────────┐   │
│              │ 养生足疗 90分钟  │   │
│              │ ¥128 会员¥115   │   │
│              │ [立即预约]      │   │
│              └─────────────────┘   │
└─────────────────────────────────────┘
```

#### 4.2.4 SKU面板改造（时间+技师选择）

借鉴瑞幸的简洁设计，改造CRMEB的SKU选择器：

```vue
<!-- 预约SKU面板 -->
<template>
  <view class="booking-sku">
    <view class="service-info">
      <image :src="service.image" class="service-img" />
      <view class="info">
        <text class="name">{{service.name}}</text>
        <text class="desc">{{service.duration}}分钟 · {{service.desc}}</text>
        <view class="price">
          <text class="current">¥{{service.price}}</text>
          <text class="original" v-if="service.originalPrice">¥{{service.originalPrice}}</text>
          <text class="member-price">会员¥{{memberPrice}}</text>
        </view>
      </view>
    </view>
    
    <!-- 时间选择 -->
    <view class="section">
      <view class="section-title">选择时间</view>
      <view class="date-tabs">
        <view 
          v-for="(date, index) in dateList" 
          :key="index"
          class="date-tab"
          :class="{active: selectedDate === index}"
          @click="selectDate(index)"
        >
          <text class="day">{{date.day}}</text>
          <text class="date">{{date.date}}</text>
        </view>
      </view>
      <view class="time-grid">
        <view 
          v-for="time in timeSlots" 
          :key="time.value"
          class="time-item"
          :class="{
            active: selectedTime === time.value,
            disabled: !time.available
          }"
          @click="selectTime(time)"
        >
          {{time.label}}
          <text class="status" v-if="!time.available">已满</text>
        </view>
      </view>
    </view>
    
    <!-- 技师选择 -->
    <view class="section">
      <view class="section-title">
        选择技师
        <text class="sub">(可选，默认到店安排)</text>
      </view>
      <scroll-view scroll-x class="technician-list">
        <view class="tech-item any" :class="{active: !selectedTech}" @click="selectTech(null)">
          <text>到店安排</text>
        </view>
        <view 
          v-for="tech in technicians" 
          :key="tech.id"
          class="tech-item"
          :class="{active: selectedTech === tech.id, busy: tech.isBusy}"
          @click="selectTech(tech)"
        >
          <image :src="tech.avatar" class="tech-avatar" />
          <text class="tech-name">{{tech.name}}</text>
          <text class="tech-tag" v-if="tech.isFavorite">常选</text>
        </view>
      </scroll-view>
    </view>
    
    <!-- 底部按钮 -->
    <view class="footer">
      <view class="total">
        <text class="label">合计:</text>
        <text class="price">¥{{totalPrice}}</text>
        <text class="member-save" v-if="isMember">会员省¥{{saveAmount}}</text>
      </view>
      <button class="btn-submit" @click="submitBooking">确认预约</button>
    </view>
  </view>
</template>
```

#### 4.2.5 数据库变更（CRMEB改造）

```sql
-- 核心订单表变更
ALTER TABLE `eb_store_order`
ADD COLUMN `technician_id` int(11) NOT NULL DEFAULT '0' COMMENT '预约技师ID',
ADD COLUMN `reserve_time` int(11) NOT NULL DEFAULT '0' COMMENT '预约到店时间戳',
ADD COLUMN `reserve_date` date DEFAULT NULL COMMENT '预约日期',
ADD COLUMN `reserve_time_slot` varchar(20) DEFAULT NULL COMMENT '预约时段(如:14:00)',
ADD COLUMN `service_duration` int(11) NOT NULL DEFAULT '60' COMMENT '服务时长(分钟)',
ADD COLUMN `store_id` int(11) NOT NULL DEFAULT '0' COMMENT '门店ID',
ADD COLUMN `check_in_time` int(11) DEFAULT '0' COMMENT '到店核销时间',
ADD COLUMN `service_start_time` int(11) DEFAULT '0' COMMENT '服务开始时间',
ADD COLUMN `service_end_time` int(11) DEFAULT '0' COMMENT '服务结束时间',
ADD COLUMN `shipping_type` tinyint(1) NOT NULL DEFAULT '2' COMMENT '配送方式:2=到店核销';

-- 新增技师表
CREATE TABLE `eb_store_technician` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `store_id` int(11) NOT NULL DEFAULT '0' COMMENT '所属门店ID',
  `name` varchar(50) NOT NULL COMMENT '技师姓名',
  `work_no` varchar(20) DEFAULT NULL COMMENT '工号',
  `avatar` varchar(255) DEFAULT '' COMMENT '头像',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `specialty` varchar(255) DEFAULT NULL COMMENT '专长标签(JSON)',
  `intro` text COMMENT '个人简介',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态:1=在职 0=离职',
  `is_busy` tinyint(1) DEFAULT '0' COMMENT '是否忙碌:1=是 0=否',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `create_time` int(11) DEFAULT '0',
  `update_time` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师表';

-- 技师排班表
CREATE TABLE `eb_store_technician_schedule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `technician_id` int(11) NOT NULL COMMENT '技师ID',
  `store_id` int(11) NOT NULL COMMENT '门店ID',
  `work_date` date NOT NULL COMMENT '工作日期',
  `time_slot` varchar(20) NOT NULL COMMENT '时段',
  `is_available` tinyint(1) DEFAULT '1' COMMENT '是否可预约:1=是 0=否',
  `order_id` int(11) DEFAULT '0' COMMENT '占用订单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `tech_date_slot` (`technician_id`, `work_date`, `time_slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师排班表';

-- 门店表扩展
ALTER TABLE `eb_system_store`
ADD COLUMN `business_hours` varchar(50) DEFAULT '10:00-22:00' COMMENT '营业时间',
ADD COLUMN `service_phone` varchar(20) DEFAULT NULL COMMENT '门店电话',
ADD COLUMN `features` varchar(255) DEFAULT NULL COMMENT '门店特色(JSON)',
ADD COLUMN `images` text COMMENT '门店图片(JSON)',
ADD COLUMN `is_female_friendly` tinyint(1) DEFAULT '1' COMMENT '是否女性友好店';
```

---

### 4.3 发现模块（游戏化+社交裂变）

**设计理念**：把健康管理变成好玩的游戏，让用户"上瘾"

#### 4.3.1 功能架构

```
┌─────────────────────────────────────┐
│  发现                               │
├─────────────────────────────────────┤
│  ┌────────┬────────┬────────┐     │
│  │ 健康挑战│ 养生社区│ 排行榜 │     │
│  └────────┴────────┴────────┘     │
├─────────────────────────────────────┤
│  我的虚拟农场                        │
│  ┌─────────────────────────────┐   │
│  │  🌿 艾草  [浇水]  成熟度80%  │   │
│  │  🌸 玫瑰  [施肥]  成熟度50%  │   │
│  │  收获可兑换: 艾草泡脚包      │   │
│  └─────────────────────────────┘   │
├─────────────────────────────────────┤
│  热门挑战                            │
│  ┌─────────────────────────────┐   │
│  │ 21天祛湿挑战  🔥 1234人参与  │   │
│  │ 已进行15天  奖励: ¥50券      │   │
│  │ [立即参与]                   │   │
│  └─────────────────────────────┘   │
├─────────────────────────────────────┤
│  养生社区                            │
│  ┌─────────────────────────────┐   │
│  │ 👤 小美  祛湿21天，湿气指数  │   │
│  │    从85降到45！附详细攻略    │   │
│  │    ❤️ 128  💬 32  [查看详情] │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

#### 4.3.2 健康挑战赛（瑞幸裂变玩法迁移）

借鉴瑞幸的"喝咖啡挑战"，设计养生挑战：

| 挑战类型 | 规则 | 奖励 | 瑞幸对标 |
|----------|------|------|----------|
| **21天祛湿挑战** | 连续21天打卡（泡脚+早睡） | 押金99元→返还+50元券 | 连续消费挑战 |
| **7天睡眠改善** | 每天11点前睡觉 | 睡眠质量报告+助眠礼包 | 健康打卡 |
| **30天体态矫正** | 每周拍照对比 | 体态评估+矫正方案 | 长期目标 |
| **邀请好友挑战** | 邀请3位好友首次体验 | 各得免费体验券 | 老带新裂变 |

**挑战机制设计**：

```vue
<!-- 挑战赛组件 -->
<template>
  <view class="challenge-card">
    <view class="challenge-header">
      <image class="challenge-icon" :src="challenge.icon" />
      <view class="challenge-info">
        <text class="name">{{challenge.name}}</text>
        <text class="desc">{{challenge.description}}</text>
        <view class="stats">
          <text class="participants">{{challenge.participants}}人参与</text>
          <text class="success-rate">成功率{{challenge.successRate}}%</text>
        </view>
      </view>
    </view>
    
    <view class="challenge-progress" v-if="isJoined">
      <view class="progress-bar">
        <view class="progress-fill" :style="{width: progressPercent + '%'}"></view>
      </view>
      <text class="progress-text">已完成 {{completedDays}}/{{totalDays}} 天</text>
    </view>
    
    <view class="challenge-reward">
      <text class="reward-label">挑战奖励:</text>
      <view class="reward-list">
        <text v-for="(reward, index) in challenge.rewards" :key="index" class="reward-item">
          {{reward}}
        </text>
      </view>
    </view>
    
    <view class="challenge-action">
      <button 
        v-if="!isJoined" 
        class="btn-join"
        @click="joinChallenge"
      >
        缴纳¥{{challenge.deposit}}参与挑战
      </button>
      <button 
        v-else-if="!isCheckedToday"
        class="btn-checkin"
        @click="checkIn"
      >
        今日打卡
      </button>
      <text v-else class="checked">今日已打卡 ✓</text>
    </view>
  </view>
</template>
```

#### 4.3.3 虚拟农场（养成游戏）

借鉴蚂蚁森林，设计"荷花园"养成游戏：

```vue
<!-- 虚拟农场组件 -->
<template>
  <view class="virtual-farm">
    <view class="farm-header">
      <text class="title">我的荷花园</text>
      <view class="resources">
        <text class="resource">☀️ {{sunshine}}</text>
        <text class="resource">💧 {{water}}</text>
      </view>
    </view>
    
    <view class="farm-plot">
      <view 
        v-for="(plant, index) in plants" 
        :key="index"
        class="plant-slot"
        :class="{empty: !plant}"
        @click="handleSlot(index)"
      >
        <image v-if="plant" :src="plant.image" class="plant-img" />
        <text v-if="plant" class="plant-progress">{{plant.progress}}%</text>
        <text v-else class="plant-action">+种植</text>
      </view>
    </view>
    
    <view class="farm-actions">
      <button class="action-btn" @click="waterAll">一键浇水</button>
      <button class="action-btn" @click="fertilize">施肥</button>
      <button class="action-btn" @click="harvestAll">一键收获</button>
    </view>
    
    <view class="exchange-shop">
      <text class="shop-title">收获兑换</text>
      <scroll-view scroll-x class="exchange-list">
        <view 
          v-for="item in exchangeItems" 
          :key="item.id"
          class="exchange-item"
        >
          <image :src="item.image" class="item-img" />
          <text class="item-name">{{item.name}}</text>
          <text class="item-cost">{{item.cost}}艾草</text>
          <button class="btn-exchange" @click="exchange(item)">兑换</button>
        </view>
      </scroll-view>
    </view>
  </view>
</template>
```

**游戏机制**：
- 每日签到、预约服务、完成挑战获得"阳光"和"雨露"
- 种植艾草、玫瑰、薏米等草本植��
- 成熟后兑换真实商品（泡脚包、精油等）
- 好友可互相"偷菜"增加社交互动

#### 4.3.4 社交裂变功能

**1. 送礼功能（借鉴瑞幸礼品卡）**

```vue
<!-- 送礼功能 -->
<template>
  <view class="gift-section">
    <view class="section-title">送给TA健康</view>
    <scroll-view scroll-x class="gift-card-list">
      <view 
        v-for="card in giftCards" 
        :key="card.id"
        class="gift-card"
        :style="{background: card.bgColor}"
        @click="selectGift(card)"
      >
        <image :src="card.cover" class="card-cover" />
        <text class="card-title">{{card.title}}</text>
        <text class="card-price">¥{{card.price}}</text>
      </view>
    </scroll-view>
    
    <!-- 送礼弹窗 -->
    <uni-popup ref="giftPopup" type="bottom">
      <view class="gift-popup">
        <view class="popup-header">
          <text>送给好友</text>
          <text class="close" @click="closePopup">×</text>
        </view>
        <view class="gift-form">
          <view class="selected-card">
            <image :src="selectedCard.cover" />
            <text>{{selectedCard.title}}</text>
          </view>
          <input 
            v-model="giftMessage" 
            placeholder="写下你的祝福..."
            class="message-input"
          />
          <button class="btn-pay-gift" @click="payAndSend">
            支付¥{{selectedCard.price}}并赠送
          </button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>
```

**礼品卡类型设计**：

| 卡类型 | 价格 | 适用场景 | 卡面设计 |
|--------|------|----------|----------|
| 孝心卡 | ¥128 | 送父母 | 温馨荷花主题 |
| 爱妻卡 | ¥168 | 送伴侣 | 浪漫玫瑰主题 |
| 闺蜜卡 | ¥198 | 送闺蜜 | 清新荷叶主题 |
| 感恩卡 | ¥88 | 送朋友 | 简约中式主题 |

**2. 裂变红包**

```vue
<!-- 裂变红包 -->
<template>
  <view class="lucky-draw">
    <view class="draw-machine">
      <image src="/static/draw-machine.gif" class="machine-img" />
      <text class="draw-hint">消费后可抽奖，最高得全年免费</text>
    </view>
    <button class="btn-draw" @click="startDraw" :disabled="!canDraw">
      {{canDraw ? '立即抽奖' : '今日已抽'}}
    </button>
    
    <!-- 奖品展示 -->
    <view class="prize-list">
      <view v-for="prize in prizes" :key="prize.id" class="prize-item">
        <image :src="prize.image" />
        <text>{{prize.name}}</text>
      </view>
    </view>
  </view>
</template>
```

---

### 4.4 我的模块（个人中心）

**设计理念**：减法与提权，突出会员资产

#### 4.4.1 CRMEB改造点

| 改造类型 | 具体内容 |
|----------|----------|
| **隐藏** | 待发货、待收货、退换货（服务类不支持） |
| **提权** | 待服务订单置顶、会员等级突出、资产看板 |
| **新增** | 健康档案、我的勋章、分销中心 |

#### 4.4.2 页面布局

```
┌─────────────────────────────────────┐
│  我的                               │
├─────────────────────────────────────┤
│  ┌─────────────────────────────┐   │
│  │  👤 小美                     │   │
│  │  荷小悦·荷花会员 V2          │   │
│  │  [个人资料] [会员中心]        │   │
│  └─────────────────────────────┘   │
├─────────────────────────────────────┤
│  我的资产                            │
│  ┌────────┬────────┬────────┐     │
│  │  ¥288  │  3张   │  680   │     │
│  │  余额  │ 优惠券 │ 荷气值 │     │
│  └────────┴────────┴────────┘     │
├─────────────────────────────────────┤
│  我的订单                            │
│  ┌────────┬────────┬────────┬────┐│
│  │  2     │  0     │  5     │  3  ││
│  │ 待服务 │ 服务中 │ 已完成 │ 全部││
│  └────────┴────────┴────────┴────┘│
├─────────────────────────────────────┤
│  我的健康档案                        │
│  ┌─────────────────────────────┐   │
│  │ 湿气指数: 65/100 (中度)       │   │
│  │ 睡眠质量: 良好               │   │
│  │ 肩颈状态: 需关注             │   │
│  │ [查看完整报告]               │   │
│  └─────────────────────────────┘   │
├─────────────────────────────────────┤
│  我的勋章                            │
│  ┌────────┬────────┬────────┐     │
│  │ 🏆    │ 🌟    │ 💪    │     │
│  │祛湿达人│睡眠改善│坚持打卡│     │
│  └────────┴────────┴────────┘     │
├─────────────────────────────────────┤
│  更多服务                            │
│  ┌────────┬────────┬────────┐     │
│  │ 分销中心│ 地址管理│ 客服帮助│     │
│  │ 设置   │ 关于我们│ 意见反馈│     │
│  └────────┴────────┴────────┘     │
└─────────────────────────────────────┘
```

---

## 五、AI功能模块

### 5.1 AI健康诊断

**核心功能**：舌诊 + 面诊 + 症状自评

```vue
<!-- AI诊断页面 -->
<template>
  <view class="ai-diagnosis">
    <view class="diagnosis-header">
      <image class="ai-avatar" src="/static/xiaohe-ai.gif" />
      <view class="ai-welcome">
        <text class="greeting">你好，我是小荷</text>
        <text class="hint">拍照或描述症状，我帮你分析健康状况</text>
      </view>
    </view>
    
    <!-- 诊断方式选择 -->
    <view class="diagnosis-methods">
      <view class="method-card" @click="startSheZhen">
        <image src="/static/icon-tongue.png" />
        <text class="method-name">AI舌诊</text>
        <text class="method-desc">拍舌头，辨体质</text>
      </view>
      <view class="method-card" @click="startMianZhen">
        <image src="/static/icon-face.png" />
        <text class="method-name">AI面诊</text>
        <text class="method-desc">看面色，识健康</text>
      </view>
      <view class="method-card" @click="startWenZhen">
        <image src="/static/icon-chat.png" />
        <text class="method-name">症状问诊</text>
        <text class="method-desc">说症状，得建议</text>
      </view>
    </view>
    
    <!-- 历史诊断记录 -->
    <view class="history-section">
      <view class="section-header">
        <text>诊断记录</text>
        <text class="more" @click="goHistory">查看全部 ></text>
      </view>
      <view class="history-list">
        <view v-for="record in diagnosisHistory" :key="record.id" class="history-item">
          <text class="date">{{record.date}}</text>
          <text class="result">{{record.result}}</text>
          <text class="score" :class="record.level">{{record.score}}分</text>
        </view>
      </view>
    </view>
  </view>
</template>
```

### 5.2 AI智能推荐

```javascript
// AI推荐算法逻辑
const AIRecommendation = {
  // 基于用户画像推荐服务
  recommendServices(userProfile) {
    const { healthData, historyOrders, preferences } = userProfile;
    
    // 根据健康数据推荐
    if (healthData.shiqi > 70) {
      return {
        primary: { name: '祛湿艾灸套餐', reason: '您的湿气指数偏高' },
        secondary: { name: '红豆薏米泡脚', reason: '居家祛湿好帮手' }
      };
    }
    
    // 根据节气推荐
    const solarTerm = getCurrentSolarTerm();
    if (solarTerm === '三伏天') {
      return {
        primary: { name: '三伏灸', reason: '三伏天祛湿最佳时机' }
      };
    }
    
    // 根据历史订单推荐
    if (historyOrders.length > 0) {
      const lastService = historyOrders[0];
      return {
        primary: { 
          name: lastService.name, 
          reason: '您常选的服务，预约更便捷' 
        }
      };
    }
  }
};
```

---

## 六、运营策略设计

### 6.1 瑞幸式裂变运营

| 运营手段 | 具体方案 | 预期效果 |
|----------|----------|----------|
| **首单优惠** | 新人首单半价（¥44体验¥88服务） | 降低决策门槛 |
| **邀请裂变** | 邀请好友，双方各得¥20券 | 老带新低成本获客 |
| **拼团优惠** | 2人同行，第二人半价 | 社交裂变+提升客单价 |
| **礼品卡** | 节日主题礼品卡，可赠送 | 情感营销+裂变 |
| **挑战押金** | 21天挑战，完成返押金+奖励 | 提高留存率 |

### 6.2 私域运营体系

```
┌─────────────────────────────────────────────────────────────┐
│                    荷小悦私域运营闭环                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌────────┐ │
│   │ 小程序  │───→│ 企业微信│───→│  社群   │───→│ 朋友圈 │ │
│   │ (入口)  │←───│ (沉淀)  │←───│ (活跃)  │←───│ (触达) │ │
│   └─────────┘    └─────────┘    └─────────┘    └────────┘ │
│        ↑                                              │     │
│        └──────────────────────────────────────────────┘     │
│                      (转化复购)                              │
│                                                             │
│   社群运营SOP:                                               │
│   • 早上9:00  健康小贴士 + 今日预约提醒                      │
│   • 中午12:00 限时秒杀优惠券                                 │
│   • 下午15:00 养生知识科普                                   │
│   • 晚上20:00 用户UGC分享 + 互动抽奖                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.3 游戏化运营日历

| 周期 | 活动 | 玩法 | 奖励 |
|------|------|------|------|
| 每日 | 签到打卡 | 连续签到得荷气值 | 荷气值+抽奖机会 |
| 每周 | 限时秒杀 | 周二/周五 10:00秒杀 | 5折优惠券 |
| 每月 | 月度挑战 | 完成4次到店体验 | 月度勋章+¥50券 |
| 节气 | 主题活动 | 三伏灸、立冬温补等 | 限定礼品+折扣 |
| 节日 | 礼品卡营销 | 母亲节/情人节送礼 | 定制卡面+额外赠送 |

---

## 七、技术实现方案

### 7.1 CRMEB二次开发规划

```
┌─────────────────────────────────────────────────────────────┐
│                    技术架构图                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  前端层 (UniApp)                                            │
│  ┌─────────┬─────────┬─────────┬─────────┐                 │
│  │  首页   │  预约   │  发现   │  我的   │                 │
│  │ (改造)  │ (改造)  │ (新增)  │ (改造)  │                 │
│  └─────────┴─────────┴─────────┴─────────┘                 │
│                                                             │
│  后端层 (ThinkPHP 6)                                        │
│  ┌─────────┬─────────┬─────────┬─────────┐                 │
│  │ 用户模块│ 订单模块│ 营销模块│ 内容模块│                 │
│  │ (扩展)  │ (改造)  │ (扩展)  │ (新增)  │                 │
│  └─────────┴─────────┴─────────┴─────────┘                 │
│                                                             │
│  数据库层 (MySQL)                                           │
│  ┌─────────┬─────────┬─────────┬─────────┐                 │
│  │ 用户表  │ 订单表  │ 技师表  │ 挑战表  │                 │
│  │ (扩展)  │ (改造)  │ (新增)  │ (新增)  │                 │
│  └─────────┴─────────┴─────────┴─────────┘                 │
│                                                             │
│  AI服务层 (Python/Node.js)                                  │
│  ┌─────────┬─────────┬─────────┐                           │
│  │ 舌诊AI  │ 面诊AI  │ 推荐AI  │                           │
│  │ (接入)  │ (接入)  │ (自研)  │                           │
│  └─────────┴─────────┴─────────┘                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 核心API接口设计

```javascript
// 预约相关API
const bookingAPI = {
  // 获取门店列表
  getStores: '/api/store/list',
  
  // 获取门店服务
  getServices: '/api/store/services',
  
  // 获取可预约时间
  getAvailableTimes: '/api/booking/times',
  
  // 获取技师列表
  getTechnicians: '/api/technician/list',
  
  // 创建预约订单
  createBooking: '/api/booking/create',
  
  // 取消预约
  cancelBooking: '/api/booking/cancel'
};

// 会员相关API
const memberAPI = {
  // 获取会员信息
  getMemberInfo: '/api/member/info',
  
  // 获取成长值记录
  getGrowthLog: '/api/member/growth/log',
  
  // 获取会员权益
  getBenefits: '/api/member/benefits'
};

// 游戏化相关API
const gameAPI = {
  // 获取挑战列表
  getChallenges: '/api/challenge/list',
  
  // 参与挑战
  joinChallenge: '/api/challenge/join',
  
  // 挑战打卡
  challengeCheckIn: '/api/challenge/checkin',
  
  // 获取农场数据
  getFarmData: '/api/farm/data',
  
  // 农场操作
  farmAction: '/api/farm/action'
};
```

### 7.3 数据埋点设计

为支持AI自动化营销，预埋数据埋点：

| 事件 | 触发时机 | 记录字段 | AI用途 |
|------|----------|----------|--------|
| view_service | 查看服务详情 | service_id, price, user_level | 用户兴趣分析 |
| book_success | 预约成功 | technician_id, reserve_time | 用户习惯分析 |
| check_in | 到店核销 | store_id, actual_time | 履约率计算 |
| challenge_join | 参与挑战 | challenge_type | 用户活跃度 |
| gift_share | 分享礼品卡 | gift_type | 社交关系链 |
| ai_diagnosis | AI诊断 | diagnosis_type, result | 健康档案构建 |

---

## 八、开发路线图

### 8.1 三阶段开发计划

```
┌─────────────────────────────────────────────────────────────┐
│                    开发路线图                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  第一阶段: MVP (4-6周)                                       │
│  ━━━━━━━━━━━━━━━━━━━━━━━                                    │
│  目标: 跑通核心预约闭环                                       │
│  • 首页会员卡片化改造                                         │
│  • 预约模块改造(时间+技师选择)                                 │
│  • 订单管理改造(待服务/已完成)                                 │
│  • 基础会员等级体系                                           │
│                                                             │
│  第二阶段: 增强版 (+4周)                                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━                                    │
│  目标: 增加游戏化和社交裂变                                    │
│  • 健康挑战系统                                               │
│  • 虚拟农场游戏                                               │
│  • 送礼功能                                                   │
│  • 裂变红包                                                   │
│  • 技师管理系统                                               │
│                                                             │
│  第三阶段: AI智能化 (+6周)                                   │
│  ━━━━━━━━━━━━━━━━━━━━━━━                                    │
│  目标: AI赋能全链路                                           │
│  • AI舌诊/面诊                                                │
│  • 智能推荐系统                                               │
│  • 流失预警与召回                                             │
│  • 数据可视化报表                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 里程碑节点

| 阶段 | 时间 | 里程碑 | 验收标准 |
|------|------|--------|----------|
| MVP | Week 6 | 核心预约上线 | 完成预约→支付→核销闭环 |
| 增强版 | Week 10 | 游戏化功能上线 | 挑战赛+农场日活>30% |
| AI版 | Week 16 | 智能化功能上线 | AI诊断准确率>80% |
| 运营期 | Week 20 | 全面运营推广 | 注册用户>5000 |

---

## 九、核心指标与成功标准

### 9.1 用户指标

| 指标 | 目标值 | 行业参考 |
|------|--------|----------|
| 注册转化率 | >30% | 行业平均20% |
| 次日留存 | >50% | 行业平均30% |
| 7日留存 | >40% | 行业平均20% |
| 30日留存 | >25% | 行业平均15% |
| DAU/MAU | >40% | 行业平均25% |

### 9.2 商业指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 首单转化率 | >40% | 新人福利吸引 |
| 复购率 | >50% | 会员体系驱动 |
| 客单价 | ¥150 | AI推荐提升 |
| 裂变系数 | >1.5 | 每个用户带来1.5个新用户 |
| 会员充值率 | >30% | 资产化运营 |

### 9.3 AI指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| AI诊断准确率 | >85% | 舌诊/面诊 |
| 推荐点击率 | >30% | 智能推荐 |
| 预约转化率 | >60% | AI推荐引导 |

---

## 十、总结

### 10.1 核心差异化

| 维度 | 传统足疗小程序 | 荷小悦小程序 |
|------|----------------|--------------|
| **定位** | 预约工具 | AI养生管家 |
| **会员** | 简单的积分 | 游戏化等级体系 |
| **裂变** | 无 | 瑞幸式社交裂变 |
| **体验** | 冷冰冰 | 有温度的小荷AI |
| **数据** | 无 | 可视化健康档案 |

### 10.2 成功关键

1. **效果可视化**：让用户"看见"健康改善
2. **游戏化运营**：让养生变得好玩上瘾
3. **社交裂变**：利用社区熟人关系低成本获客
4. **AI赋能**：从诊断到推荐全流程智能化
5. **会员资产化**：像星巴克一样建立高粘性

### 10.3 愿景

> **让每个社区居民都有一个AI养生管家，让荷小悦成为10亿中国人的健康陪伴者。**

---

**文档结束**

*本文档基于CRMEB开源项目设计，参考瑞幸咖啡运营模式，专为荷小悦品牌战略定制。*
