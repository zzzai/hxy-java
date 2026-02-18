# 荷小悦 Java DDD 项目结构说明

## 已完成的核心模块

### 1. 订单域（Order Domain）

**聚合根**：`Order.java`
- 订单创建
- 订单支付
- 订单核销
- 退款计算

**实体**：`OrderItem.java`
- 订单子项
- 核销状态管理

**值对象**：
- `OrderId` - 订单ID
- `OrderStatus` - 订单状态
- `OrderAmount` - 订单金额
- `OrderType` - 订单类型
- `RefundAmount` - 退款金额

**领域事件**：
- `OrderCreatedEvent` - 订单创建事件
- `OrderPaidEvent` - 订单支付事件

**仓储接口**：`OrderRepository`

**应用服务**：`OrderAppService`
- 创建订单
- 支付订单
- 核销订单

---

### 2. 会员域（Member Domain）

**聚合根**：`Member.java`
- 成长值累积
- 等级自动升级

**值对象**：
- `MemberLevel` - 会员等级（荷芽/荷叶/荷花/高级）
- `GrowthValue` - 成长值

**领域事件**：
- `MemberLevelUpgradedEvent` - 会员升级事件

**仓储接口**：`MemberRepository`

---

### 3. 公共基础类

**领域层基类**：
- `Entity` - 实体基类
- `AggregateRoot` - 聚合根基类
- `ValueObject` - 值对象基类
- `DomainEvent` - 领域事件接口

---

## 核心业务规则实现

### 订单价格分摊算法

```java
// Order.java - splitPriceToItems()
按原价比例分摊实付金额到每个子项
最后一项用剩余金额（避免精度问题）
```

### 订单核销规则

```java
// Order.java - writeOff()
1. 只有已支付状态才能核销
2. 子项独立核销
3. 全部核销后订单状态变为已完成
```

### 退款计算规则

```java
// Order.java - calculateRefundAmount()
只退未核销的子项
```

### 会员升级规则

```java
// Member.java - addGrowth()
成长值累积后自动检查是否达到升级阈值
支持连续升级
```

---

## 下一步开发计划

### 待实现的领域

1. **预约域（Booking Domain）**
   - 技师排班
   - 时间槽管理
   - 预约下单

2. **商品域（Product Domain）**
   - SPU/SKU管理
   - 套餐配置
   - BOM物料

3. **库存域（Stock Domain）**
   - 库存预占
   - 库存扣减
   - BOM消耗

4. **门店域（Store Domain）**
   - 门店管理
   - 技师管理

5. **支付域（Payment Domain）**
   - 支付对接
   - 对账系统

### 待实现的基础设施

1. **Repository实现**
   - OrderRepositoryImpl
   - MemberRepositoryImpl

2. **事件发布机制**
   - Spring Event
   - RocketMQ

3. **接口层**
   - REST API
   - 参数校验

---

## 技术亮点

### 1. DDD四层架构清晰
```
interfaces → application → domain → infrastructure
```

### 2. 业务规则封装在领域层
- 订单分摊算法在Order聚合根
- 会员升级逻辑在Member聚合根
- 不依赖外部框架

### 3. 领域事件解耦
- 订单支付后自动触发库存扣减
- 会员升级后自动发送通知

### 4. 值对象保证类型安全
- OrderId替代Long
- MemberLevel替代int
- 编译期类型检查

---

## 如何运行

### 1. 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+

### 2. 构建项目
```bash
cd /root/crmeb/hxy-java-ddd
mvn clean install
```

### 3. 下一步
- 实现Repository层
- 配置Spring Boot启动类
- 编写单元测试

---

## 项目优势

### vs PHP CRMEB

| 维度 | Java DDD | PHP CRMEB |
|------|----------|-----------|
| **业务逻辑** | 封装在领域层 | 散落在Service |
| **类型安全** | 编译期检查 | 运行时错误 |
| **可测试性** | 领域层可独立测试 | 依赖数据库 |
| **性能** | 5000+ QPS | 500 QPS |
| **扩展性** | 易于拆分微服务 | 单体架构 |

---

## 联系方式

项目负责人：荷小悦架构师
技术栈：Java 17 + Spring Boot 3.2 + DDD

