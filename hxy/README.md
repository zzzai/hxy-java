# 荷小悦O2O多门店系统

> 基于CRMEB-Java的足疗按摩预约服务系统

## 📖 项目概述

荷小悦是一个专注于足疗按摩行业的O2O多门店预约服务系统，采用"服务商品化"架构，将足疗服务作为特殊商品，复用CRMEB成熟的电商体系，实现最小化开发成本。

### 核心特性

- 🗓️ **智能排班** - 5分钟完成一周排班，闲时动态调价
- 💳 **卡项体系** - 次卡/期卡/储值卡，锁定80%稳定营收
- 👤 **派单模式** - 点钟/排钟灵活选择，平衡体验与公平
- ⏰ **加钟场景** - 服务中动态加钟，提升客单价
- 💰 **技师分润** - 多维度提成规则，激励技师积极性
- 📊 **客情档案** - 服务偏好记录，复购率提升40%

---

## 🎯 快速导航

### 📚 产品设计文档（必读）

| 文档 | 说明 | 优先级 |
|------|------|--------|
| [文档导航.md](文档导航.md) | 完整文档索引 | ⭐⭐⭐ |
| [知识库补充-全量文档地图.md](知识库补充-全量文档地图.md) | 全量知识地图与统一口径 | ⭐⭐⭐ |
| [paiban.md](baseline/paiban.md) | 技师排班产品设计（V2.0） | ⭐⭐⭐ |
| [v21.md](baseline/v21.md) | 卡项/派单/分润/客情（V2.1） | ⭐⭐⭐ |
| [服务商品化架构方案.md](baseline/服务商品化架构方案.md) | 技术架构方案 | ⭐⭐⭐ |
| [架构修正方案-可上线版.md](baseline/架构修正方案-可上线版.md) | 可上线架构修正方案 | ⭐⭐⭐ |
| [技术架构蓝图-V1.md](baseline/技术架构蓝图-V1.md) | 模块化单体+AI侧车架构蓝图 | ⭐⭐⭐ |
| [2026-2027产品+技术+AI路线图（季度版）.md](baseline/2026-2027产品+技术+AI路线图（季度版）.md) | 两年季度路线图 | ⭐⭐⭐ |
| [战略规划V2.0.md](baseline/战略规划V2.0.md) | 品牌与连锁战略 | ⭐⭐ |

### 🚀 实施方案（开发必读）

| 文档 | 说明 | 工期 |
|------|------|------|
| [方案B实施计划.md](implementation/方案B实施计划.md) | 4周总体规划 | - |
| [第一周-排班系统实施.md](implementation/第一周-排班系统实施.md) | 智能排班+时间槽 | 7天 |
| [第二周-卡项体系实施.md](implementation/第二周-卡项体系实施.md) | 次卡/期卡/储值卡 | 7天 |
| [第三周-派单加钟实施.md](implementation/第三周-派单加钟实施.md) | 点钟/排钟+加钟 | 7天 |
| [第四周-分润客情实施-Part1.md](implementation/第四周-分润客情实施-Part1.md) | 技师提成系统 | 3天 |
| [第四周-分润客情实施-Part2.md](implementation/第四周-分润客情实施-Part2.md) | 客情档案系统 | 4天 |
| [V1产品范围冻结.md](plan/product/V1产品范围冻结.md) | 本轮交付范围与验收口径 | - |
| [Sprint开发计划-8周.md](plan/product/Sprint开发计划-8周.md) | 8周迭代计划与上线拦截规则 | 8周 |

### 📖 开发指南

- [UniApp开发指南.md](guides/UniApp开发指南.md) - 前端开发规范
- [API接口文档.md](guides/API接口文档.md) - 接口规范
- [部署指南.md](guides/部署指南.md) - 生产环境部署
- [执行步骤指南.md](guides/执行步骤指南.md) - 手动执行步骤
- [external/structured/README.md](external/structured/README.md) - 外部资料结构化入口

---

## 🗂️ 目录分类（2026-02-14）

| 目录 | 说明 |
|------|------|
| [baseline/README.md](baseline/README.md) | 产品与技术基线文档 |
| [implementation/README.md](implementation/README.md) | 分周实施方案 |
| [guides/README.md](guides/README.md) | API/前端/部署执行指南 |
| [database/README.md](database/README.md) | SQL基线与补丁 |
| [reports/README.md](reports/README.md) | 状态与评审报告 |
| [external/README.md](external/README.md) | 外部资料（原始+结构化） |
| [plan/README.md](plan/README.md) | 模块补全与专项方案 |
| [plan_java/DDD开发总结.md](plan_java/DDD开发总结.md) | DDD与演进方法论 |
| [archive/README.md](archive/README.md) | 历史归档说明 |
| [tools/README.md](tools/README.md) | 转换与运维脚本 |

---

## 🗄️ 数据库设计

### V1.0 基础表（9张）
```bash
mysql -u root -p crmeb_java < database/database_migration_v1.0.sql
```

**新增表**：
- `eb_technician` - 技师基础信息
- `eb_technician_schedule` - 技师排班（时间槽JSON）
- `eb_stock_flow` - 库存变动流水
- `eb_member_card` - 会员卡
- `eb_offpeak_rule` - 闲时优惠规则
- `eb_booking_order` - 预约订单

**扩展表**：
- `eb_store_order` - 订单表（+预约字段）
- `eb_store_product_attr_value` - SKU表（+库存字段）
- `eb_store_order_info` - 订单详情（+核销字段）

### V2.0 扩展表（7张）
```bash
mysql -u root -p crmeb_java < database/database_migration_v2.0.sql
```

### 生产补丁（推荐）
```bash
mysql -u root -p crmeb_java < database/database_architecture_fix_v2.sql
```

**新增表**：
- `eb_card_template` - 卡项模板
- `eb_user_card` - 用户持卡记录
- `eb_card_usage` - 卡项核销记录
- `eb_commission_record` - 技师提成记录
- `eb_customer_profile` - 客情档案

**扩展表**：
- `eb_technician_schedule` - 排班表（+智能排班字段）
- `eb_store_order` - 订单表（+派单/加钟字段）

---

## 🏗️ 技术架构

### 核心技术栈

- **后端**: Spring Boot + MyBatis-Plus + Redis
- **数据库**: MySQL 5.7+
- **缓存**: Redis 6.0+
- **前端**: UniApp（多端统一）
- **基础**: CRMEB-Java v1.4

### 核心设计

#### 1. 服务商品化
```
足疗服务 = 特殊商品（product_type=2）
├─ 复用商品管理
├─ 复用订单流程
├─ 复用支付系统
└─ 扩展预约/排班/核销
```

#### 2. 时间槽JSON设计
```json
[
  {
    "id": "slot_0900",
    "startTime": "09:00",
    "endTime": "10:00",
    "status": "available",
    "price": 128.00,
    "isOffpeak": false
  }
]
```

#### 3. 并发控制
```
Redis分布式锁 + MySQL行锁 + 原子更新
```

---

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- MySQL 5.7+
- Redis 6.0+
- Maven 3.6+
- Node.js 14+（UniApp开发）

### 安装步骤

#### 1. 克隆项目
```bash
cd /root/crmeb-java
```

#### 2. 执行数据库脚本
```bash
# 基础表
mysql -u root -p crmeb_java < hxy/database/database_migration_v1.0.sql

# 扩展表
mysql -u root -p crmeb_java < hxy/database/database_migration_v2.0.sql
```

#### 3. 配置数据库和Redis
```bash
vim crmeb_java/crmeb/crmeb-admin/src/main/resources/application-dev.yml
```

修改：
```yaml
datasource:
  url: jdbc:mysql://127.0.0.1:3306/crmeb_java
  username: root
  password: your_password

redis:
  host: 127.0.0.1
  port: 6379
  password: your_password
```

#### 4. 启动后端
```bash
cd crmeb_java/crmeb
mvn clean package -DskipTests
cd crmeb-admin/target
java -jar crmeb-admin-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

#### 5. 访问系统
- 后台管理：http://localhost:8080
- Swagger文档：http://localhost:8080/swagger-ui.html

---

## 📊 产品完整度评估

### 业务场景覆盖：95%
- ✅ 技师排班管理
- ✅ 用户预约流程
- ✅ 卡项购买与核销
- ✅ 点钟/排钟派单
- ✅ 加钟动态变更
- ✅ 技师提成分润
- ✅ 客情档案管理
- ✅ 闲时动态调价

### 技术方案完整度：85%
- ✅ 数据库设计（16张表）
- ✅ API接口设计（20+个）
- ✅ 并发控制方案
- ✅ 分布式锁设计
- ⚠️ 性能优化方案（部分）
- ⚠️ 监控告警方案（待补充）

### 用户体验设计：90%
- ✅ 6个完整用户故事
- ✅ 3大核心界面原型
- ✅ 交互流程设计
- ✅ 异常场景处理

**总体评分：85/100**

适合作为MVP快速上线，后续根据运营数据迭代优化。

---

## 📈 预期效果

### 业务指标
- 技师利用率：60% → 78%
- 闲时预约率：25% → 65%
- 客户复购率：+40%
- 客诉率：-60%
- 客单价：+20%

### 运营效率
- 排班时间：30分钟 → 5分钟
- 预约转化率：+30%
- 技师收入：+15%
- 现金流：提前锁定（卡项）

---

## 📝 开发计划

### 第一周：排班系统
- Day 1-2: 数据库+实体类
- Day 3-4: 后端API（5个）
- Day 5-7: 前端页面（3个）

### 第二周：卡项体系
- Day 1-2: 卡项数据库+实体类
- Day 3-4: 后端API（5个）
- Day 5-7: 前端页面（3个）

### 第三周：派单+加钟
- Day 1-3: 派单模式实现
- Day 4-5: 加钟场景实现
- Day 6-7: 集成测试

### 第四周：分润+客情
- Day 1-3: 技师提成系统
- Day 4-5: 客情档案系统
- Day 6-7: 完整验收

---

## 🤝 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

基于 CRMEB-Java 开源协议

---

## 📞 联系方式

- 项目文档：`/root/crmeb-java/hxy/`
- 技术支持：查看 [文档导航.md](文档导航.md)

---

**最后更新**：2026-02-14  
**版本**：V2.1  
**状态**：产品方案完成，待开发实施
