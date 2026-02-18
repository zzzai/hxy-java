# CRMEB可复用模块分析与改造方案

> **更新时间**: 2026-02-13  
> **重要性**: ⭐⭐⭐⭐⭐ 战略级文档

---

## 📊 CRMEB项目结构分析

### 目录结构

```
/root/crmeb-java/crmeb_java/
├── admin/              # PC管理后台 (Vue + ElementUI)
├── app/                # 移动端 (UniApp: H5 + 小程序)
├── crmeb/              # 后端服务 (Java SpringBoot)
│   ├── crmeb-admin/    # 管理端接口
│   ├── crmeb-common/   # 公共模块
│   ├── crmeb-front/    # 前台接口
│   ├── crmeb-service/  # 业务服务层
│   └── sql/            # 数据库脚本
└── 接口文档/           # API文档
```

---

## ✅ 可复用模块清单（80%）

### 1. 用户系统 ✅ 100%复用

**前端页面**：
- `/app/pages/user/index.vue` - 个人中心
- `/app/pages/users/login/index.vue` - 登录
- `/app/pages/users/user_info/index.vue` - 个人资料

**后端服务**：
- `UserService.java` - 用户服务
- `UserTokenService.java` - Token管理
- `UserAddressService.java` - 地址管理

**数据库表**：
- `eb_user` - 用户表
- `eb_user_address` - 地址表
- `eb_user_token` - Token表

**改造点**：无需改造，直接复用

---

### 2. 会员系统 ✅ 95%复用

**前端页面**：
- `/app/pages/infos/user_vip/index.vue` - 会员中心
- `/app/pages/users/user_integral/index.vue` - 积分详情
- `/app/pages/users/user_money/index.vue` - 余额账户

**后端服务**：
- `UserLevelService.java` - 用户等级
- `SystemUserLevelService.java` - 等级配置
- `UserIntegralRecordService.java` - 积分记录
- `UserBillService.java` - 账单明细

**数据库表**：
- `eb_user_level` - 用户等级表
- `eb_system_user_level` - 等级配置表
- `eb_user_integral_record` - 积分记录
- `eb_user_bill` - 账单表

**改造点**：
```sql
-- 扩展等级表，增加荷小悦特色字段
ALTER TABLE `eb_system_user_level`
ADD COLUMN `growth_value` int(11) DEFAULT '0' COMMENT '成长值门槛',
ADD COLUMN `level_icon` varchar(255) DEFAULT '' COMMENT '等级图标',
ADD COLUMN `level_benefits` text COMMENT '等级权益(JSON)';
```

---

### 3. 支付系统 ✅ 100%复用

**前端页面**：
- `/app/pages/order/order_payment/index.vue` - 支付页面
- `/app/pages/users/user_payment/index.vue` - 余额充值

**后端服务**：
- `WeChatPayService.java` - 微信支付
- `OrderPayService.java` - 订单支付
- `RechargePayService.java` - 充值支付

**配置文件**：
- `application.yml` - 微信支付配置

**改造点**：无需改造，直接复用

---

### 4. 优惠券系统 ✅ 100%复用

**前端页面**：
- `/app/pages/users/user_coupon/index.vue` - 我的优惠券
- `/app/pages/users/user_get_coupon/index.vue` - 领取优惠券

**后端服务**：
- `StoreCouponService.java` - 优惠券服务
- `StoreCouponUserService.java` - 用户优惠券

**数据库表**：
- `eb_store_coupon` - 优惠券表
- `eb_store_coupon_user` - 用户优惠券表

**改造点**：无需改造，直接复用

---

### 5. 订单系统 🔄 70%复用 + 30%改造

**前端页面**：
- `/app/pages/users/order_list/index.vue` - 订单列表
- `/app/pages/order/order_details/index.vue` - 订单详情
- `/app/pages/order/order_confirm/index.vue` - 确认订单

**后端服务**：
- `StoreOrderService.java` - 订单服务
- `StoreOrderInfoService.java` - 订单详情
- `StoreOrderStatusService.java` - 订单状态

**数据库表**：
- `eb_store_order` - 订单表（需改造）
- `eb_store_order_info` - 订单详情表
- `eb_store_order_status` - 订单状态表

**改造方案**：

```sql
-- 扩展订单表，支持预约业务
ALTER TABLE `eb_store_order`
ADD COLUMN `order_type` tinyint(1) DEFAULT '1' COMMENT '订单类型:1=商品 2=服务预约',
ADD COLUMN `technician_id` int(11) DEFAULT '0' COMMENT '技师ID',
ADD COLUMN `technician_name` varchar(50) DEFAULT '' COMMENT '技师姓名',
ADD COLUMN `reserve_date` date DEFAULT NULL COMMENT '预约日期',
ADD COLUMN `reserve_time_slot` varchar(20) DEFAULT '' COMMENT '预约时段(14:00)',
ADD COLUMN `reserve_timestamp` int(11) DEFAULT '0' COMMENT '预约时间戳',
ADD COLUMN `service_duration` int(11) DEFAULT '60' COMMENT '服务时长(分钟)',
ADD COLUMN `store_id` int(11) DEFAULT '0' COMMENT '门店ID',
ADD COLUMN `store_name` varchar(100) DEFAULT '' COMMENT '门店名称',
ADD COLUMN `check_in_code` varchar(32) DEFAULT '' COMMENT '核销码',
ADD COLUMN `check_in_time` int(11) DEFAULT '0' COMMENT '核销时间',
ADD COLUMN `service_start_time` int(11) DEFAULT '0' COMMENT '服务开始时间',
ADD COLUMN `service_end_time` int(11) DEFAULT '0' COMMENT '服务结束时间',
MODIFY COLUMN `shipping_type` tinyint(1) DEFAULT '2' COMMENT '配送方式:1=快递 2=到店核销';

-- 订单状态流转改造
-- 原：待付款→待发货→待收货→已完成
-- 新：待付款→待服务→服务中→已完成
```

**Java代码改造**：

```java
// StoreOrderService.java 新增方法
public interface StoreOrderService {
    // 原有方法保留...
    
    // 新增：创建预约订单
    StoreOrder createBookingOrder(BookingOrderRequest request);
    
    // 新增：核销预约订单
    Boolean verifyBookingOrder(String checkInCode);
    
    // 新增：开始服务
    Boolean startService(Integer orderId);
    
    // 新增：结束服务
    Boolean endService(Integer orderId);
}
```

---

### 6. 门店系统 ✅ 80%复用 + 20%扩展

**前端页面**：
- `/app/pages/goods/goods_details_store/index.vue` - 门店列表

**后端服务**：
- `SystemStoreService.java` - 门店服务
- `SystemStoreStaffService.java` - 门店员工

**数据库表**：
- `eb_system_store` - 门店表（需扩展）
- `eb_system_store_staff` - 门店员工表

**改造方案**：

```sql
-- 扩展门店表
ALTER TABLE `eb_system_store`
ADD COLUMN `business_hours` varchar(50) DEFAULT '10:00-22:00' COMMENT '营业时间',
ADD COLUMN `service_phone` varchar(20) DEFAULT '' COMMENT '服务电话',
ADD COLUMN `features` text COMMENT '门店特色(JSON)',
ADD COLUMN `images` text COMMENT '门店图片(JSON)',
ADD COLUMN `is_female_friendly` tinyint(1) DEFAULT '1' COMMENT '是否女性友好店',
ADD COLUMN `avg_rating` decimal(2,1) DEFAULT '5.0' COMMENT '平均评分';
```

---

### 7. 后台管理系统 ✅ 90%复用

**前端页面**：
- `/admin/src/` - 整个管理后台

**后端服务**：
- `SystemAdminService.java` - 管理员
- `SystemRoleService.java` - 角色权限
- `SystemMenuService.java` - 菜单管理

**改造点**：新增菜单项

```sql
-- 新增技师管理菜单
INSERT INTO `eb_category` VALUES 
(NULL, 42, '/0/42/', '技师管理', 5, '/store/technician', NULL, 1, 1, NOW(), NOW()),
(NULL, 42, '/0/42/', '排班管理', 5, '/store/schedule', NULL, 1, 1, NOW(), NOW());
```

---

## 🔄 需改造模块（15%）

### 1. 商品模块 → 服务项目模块

**原CRMEB结构**：
- 商品表：`eb_store_product`
- 商品SKU：`eb_store_product_attr_value`
- 商品分类：`eb_category`

**改造策略**：复用商品表，通过type字段区分

```sql
-- 扩展商品表
ALTER TABLE `eb_store_product`
ADD COLUMN `product_type` tinyint(1) DEFAULT '1' COMMENT '商品类型:1=实物商品 2=服务项目',
ADD COLUMN `service_duration` int(11) DEFAULT '60' COMMENT '服务时长(分钟)',
ADD COLUMN `service_category` varchar(50) DEFAULT '' COMMENT '服务分类(足疗/推拿/艾灸)',
ADD COLUMN `suitable_crowd` varchar(255) DEFAULT '' COMMENT '适用人群',
ADD COLUMN `service_effect` text COMMENT '服务功效';

-- 服务项目数据示例
INSERT INTO `eb_store_product` 
(product_type, store_name, service_duration, service_category, price, ot_price) 
VALUES 
(2, '经典足疗60分钟', 60, '足疗', 88.00, 128.00),
(2, '养生足疗90分钟', 90, '足疗', 128.00, 168.00),
(2, '肩颈推拿45分钟', 45, '推拿', 98.00, 138.00);
```

**前端改造**：

```vue
<!-- 商品详情页改造为服务详情页 -->
<!-- /app/pages/goods/goods_details/index.vue -->

<template>
  <view class="service-details" v-if="productInfo.product_type === 2">
    <!-- 服务信息 -->
    <view class="service-info">
      <text class="service-name">{{productInfo.store_name}}</text>
      <text class="duration">{{productInfo.service_duration}}分钟</text>
      <text class="category">{{productInfo.service_category}}</text>
    </view>
    
    <!-- 预约按钮（替换加入购物车） -->
    <view class="action-bar">
      <button @click="goBooking">立即预约</button>
    </view>
  </view>
</template>
```

---

### 2. 购物车 → 删除（服务不需要购物车）

**改造策略**：隐藏购物车TabBar，直接跳转预约流程

```json
// /app/pages.json
{
  "tabBar": {
    "list": [
      {"pagePath": "pages/index/index", "text": "首页"},
      // {"pagePath": "pages/order_addcart/order_addcart", "text": "购物车"}, // 删除
      {"pagePath": "pages/booking/index", "text": "预约"}, // 新增
      {"pagePath": "pages/user/index", "text": "我的"}
    ]
  }
}
```

---

## ➕ 新增模块（5%）

### 1. 技师管理模块

**数据库表**：

```sql
-- 技师表
CREATE TABLE `eb_store_technician` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `store_id` int(11) NOT NULL DEFAULT '0' COMMENT '所属门店ID',
  `name` varchar(50) NOT NULL COMMENT '技师姓名',
  `work_no` varchar(20) DEFAULT NULL COMMENT '工号',
  `avatar` varchar(255) DEFAULT '' COMMENT '头像',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `gender` tinyint(1) DEFAULT '1' COMMENT '性别:1=男 2=女',
  `specialty` varchar(255) DEFAULT NULL COMMENT '专长标签(JSON)',
  `intro` text COMMENT '个人简介',
  `service_count` int(11) DEFAULT '0' COMMENT '服务次数',
  `avg_rating` decimal(2,1) DEFAULT '5.0' COMMENT '平均评分',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态:1=在职 0=离职',
  `is_busy` tinyint(1) DEFAULT '0' COMMENT '是否忙碌:1=是 0=否',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `create_time` int(11) DEFAULT '0',
  `update_time` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师表';
```

**后端服务**：

```java
// TechnicianService.java
public interface TechnicianService {
    // 获取门店技师列表
    List<Technician> getStoreList(Integer storeId);
    
    // 获取技师详情
    Technician getDetail(Integer id);
    
    // 获取技师可预约时间
    List<TimeSlot> getAvailableTime(Integer technicianId, String date);
    
    // 更新技师状态
    Boolean updateStatus(Integer id, Integer status);
}
```

---

### 2. 排班管理模块

**数据库表**：

```sql
-- 技师排班表
CREATE TABLE `eb_store_technician_schedule` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `technician_id` int(11) NOT NULL COMMENT '技师ID',
  `store_id` int(11) NOT NULL COMMENT '门店ID',
  `work_date` date NOT NULL COMMENT '工作日期',
  `time_slot` varchar(20) NOT NULL COMMENT '时段(14:00)',
  `is_available` tinyint(1) DEFAULT '1' COMMENT '是否可预约:1=是 0=否',
  `order_id` int(11) DEFAULT '0' COMMENT '占用订单ID',
  `create_time` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `tech_date_slot` (`technician_id`, `work_date`, `time_slot`),
  KEY `store_date` (`store_id`, `work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师排班表';
```

---

## 📱 前端改造方案

### 1. 首页改造

**原CRMEB首页**：
- 轮播图
- 商品分类
- 秒杀/拼团/砍价
- 商品列表

**荷小悦首页**：
- 会员卡片（新增）
- 资产看板（新增）
- 门店切换（改造）
- 服务列表（改造商品列表）
- 快捷预约（新增）

**改造文件**：
- `/app/pages/index/index.vue` - 首页主文件
- `/app/components/` - 新增会员卡片组件

---

### 2. 新增预约页面

**页面路径**：`/app/pages/booking/index.vue`

**功能模块**：
1. 门店选择（LBS定位）
2. 服务选择（分类展示）
3. 时间选择（日历+时段）
4. 技师选择（可选）
5. 确认预约

---

## 🎯 改造优先级

### Phase 1: MVP核心功能（Week 1-2）

**目标**：跑通预约→支付→核销闭环

1. ✅ 扩展订单表（支持预约字段）
2. ✅ 创建技师表、排班表
3. ✅ 改造订单服务（新增预约方法）
4. ✅ 前端新增预约页面
5. ✅ 改造首页（会员卡片化）

### Phase 2: 完善功能（Week 3-4）

**目标**：完善用户体验

1. ✅ 技师管理后台
2. ✅ 排班管理后台
3. ✅ 订单状态流转优化
4. ✅ 会员等级扩展
5. ✅ 优惠券适配服务

### Phase 3: 游戏化运营（Week 5-6）

**目标**：增加用户粘性

1. ✅ 健康挑战系统
2. ✅ 虚拟农场
3. ✅ 送礼功能
4. ✅ 裂变红包

---

## 📊 复用率统计

| 模块 | 复用率 | 改造工作量 | 说明 |
|------|--------|-----------|------|
| 用户系统 | 100% | 0天 | 完全复用 |
| 会员系统 | 95% | 0.5天 | 扩展等级字段 |
| 支付系统 | 100% | 0天 | 完全复用 |
| 优惠券系统 | 100% | 0天 | 完全复用 |
| 订单系统 | 70% | 3天 | 扩展预约字段+改造流程 |
| 门店系统 | 80% | 1天 | 扩展门店字段 |
| 后台管理 | 90% | 1天 | 新增菜单 |
| 商品模块 | 60% | 2天 | 改造为服务项目 |
| 购物车 | 0% | 0.5天 | 删除TabBar |
| 技师管理 | 0% | 3天 | 全新开发 |
| 排班管理 | 0% | 2天 | 全新开发 |
| 预约页面 | 0% | 3天 | 全新开发 |
| **总计** | **80%** | **16天** | **2周完成MVP** |

---

## 🎯 关键认知

### 1. CRMEB的价值

**不是从零开发，而是站在巨人肩膀上**：
- ✅ 用户/会员/支付/优惠券 → 100%复用
- ✅ 订单/门店/后台 → 70-90%复用
- ✅ 前端框架/组件库 → 100%复用

### 2. 改造策略

**扩展而非重建**：
- ✅ 订单表：ADD COLUMN（不是CREATE TABLE）
- ✅ 商品表：type=2表示服务（不是新建服务表）
- ✅ 前端：改造现有页面（不是从零写UniApp）

### 3. 时间节省

**从零开发20周 → 基于CRMEB改造8周**：
- Week 1-2: MVP核心功能
- Week 3-4: 完善功能
- Week 5-6: 游戏化运营
- Week 7-8: 测试上线

---

## 📝 下一步行动

### 立即执行

1. **启动数据库改造**
   ```sql
   -- 执行订单表扩展
   ALTER TABLE `eb_store_order` ADD COLUMN ...
   
   -- 创建技师表
   CREATE TABLE `eb_store_technician` ...
   
   -- 创建排班表
   CREATE TABLE `eb_store_technician_schedule` ...
   ```

2. **改造订单服务**
   - 修改 `StoreOrderService.java`
   - 新增预约订单方法
   - 改造订单状态流转

3. **开发预约页面**
   - 创建 `/app/pages/booking/index.vue`
   - 实现门店选择、时间选择、技师选择

4. **改造首页**
   - 修改 `/app/pages/index/index.vue`
   - 新增会员卡片组件

---

**文档版本**: V1.0  
**更新时间**: 2026-02-13  
**作者**: AI开发助手  
**重要性**: 战略级改造方案


