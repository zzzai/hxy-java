# O2O系统核心知识提取报告

> **来源**: /root/crmeb-java/hxy/external/structured/hxyo2o/  
> **文档**: 荷小悦O2O系统_技术实施手册_V2.0.md + 荷小悦O2O系统_完整优化方案_V2.0.md  
> **重要性**: ⭐⭐⭐⭐⭐ 核心技术方案  
> **日期**: 2026-02-13

---

## 📊 文档概览

### 文档1: 技术实施手册（结构化版）

**内容**：
- ✅ 完整建表SQL脚本
- ✅ 核心业务代码示例（Python）
- ✅ 部署与配置指南
- ✅ Redis分布式锁实现

### 文档2: 完整优化方案（结构化版）

**内容**：
- ✅ 原方案问题分析（8大核心问题）
- ✅ 14项优化建议
- ✅ 技师排班结构设计
- ✅ 套餐定价逻辑
- ✅ 数据隔离安全加固
- ✅ 实施路线图

---

## 🎯 核心发现

### 1. 技师排班系统（核心）

**新增表结构**：

```sql
-- 技师基础信息表
CREATE TABLE `eb_technician` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `store_id` int(11) NOT NULL COMMENT '所属门店ID',
  `name` varchar(50) NOT NULL COMMENT '技师姓名',
  `avatar` varchar(255) DEFAULT '' COMMENT '技师头像',
  `level` tinyint(1) DEFAULT 1 COMMENT '等级：1=初级 2=中级 3=高级 4=首席',
  `service_years` decimal(4,1) DEFAULT 0.0 COMMENT '从业年限',
  `skill_tags` varchar(255) DEFAULT '' COMMENT '技能标签',
  `intro` text COMMENT '技师介绍',
  `rating` decimal(3,2) DEFAULT 5.00 COMMENT '评分',
  `order_count` int(11) DEFAULT 0 COMMENT '累计服务单数',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1=在职 2=离职',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 技师排班表
CREATE TABLE `eb_technician_schedule` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `store_id` int(11) NOT NULL,
  `technician_id` int(11) NOT NULL,
  `service_sku_id` int(11) NOT NULL,
  `work_date` date NOT NULL COMMENT '排班日期',
  `time_slots` json NOT NULL COMMENT '时间槽JSON',
  `total_slots` int(11) DEFAULT 0 COMMENT '总时间槽数',
  `available_slots` int(11) DEFAULT 0 COMMENT '可预约时间槽数',
  `status` tinyint(1) DEFAULT 1 COMMENT '1=正常 2=请假 3=已完成',
  `is_offpeak_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用闲时优惠',
  PRIMARY KEY (`id`),
  KEY `idx_store_date` (`store_id`,`work_date`),
  KEY `idx_technician_date` (`technician_id`,`work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**time_slots JSON结构**：

```json
{
  "slots": [
    {
      "slot_id": "20250212_0900",
      "start_time": "09:00",
      "end_time": "10:00",
      "status": 1,  // 1=可预约 2=已锁定 3=已预约 4=已完成
      "price": 128.00,
      "offpeak_price": 98.00,  // 闲时优惠价
      "is_offpeak": 1,
      "order_id": 0,
      "locked_at": 0,
      "locked_expire": 0
    }
  ],
  "offpeak_rules": {
    "enabled": true,
    "time_ranges": [{"start": "09:00", "end": "11:00"}],
    "discount_type": 1,
    "discount_value": 98.00
  }
}
```

### 2. 库存并发控制（核心）

**新增表**：

```sql
-- 库存变动流水表
CREATE TABLE `eb_stock_flow` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `store_id` int(11) NOT NULL,
  `sku_id` int(11) NOT NULL,
  `order_id` bigint(20) DEFAULT 0,
  `change_type` tinyint(1) NOT NULL COMMENT '1=入库 2=锁定 3=释放 4=扣减 5=退款',
  `change_quantity` int(11) NOT NULL,
  `before_available` int(11) DEFAULT 0,
  `after_available` int(11) DEFAULT 0,
  `before_locked` int(11) DEFAULT 0,
  `after_locked` int(11) DEFAULT 0,
  `operator_id` int(11) DEFAULT 0,
  `remark` varchar(255) DEFAULT '',
  `created_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_store_sku` (`store_id`,`sku_id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**扩展现有表**：

```sql
-- 门店SKU定价表补充库存字段
ALTER TABLE `eb_store_service_sku`
ADD COLUMN `available_stock` int(11) DEFAULT 0 COMMENT '可售库存',
ADD COLUMN `locked_stock` int(11) DEFAULT 0 COMMENT '锁定库存',
ADD COLUMN `sold_stock` int(11) DEFAULT 0 COMMENT '已售库存',
ADD COLUMN `total_stock` int(11) DEFAULT 0 COMMENT '总库存',
ADD COLUMN `low_stock_threshold` int(11) DEFAULT 10 COMMENT '低库存阈值';
```

### 3. 分布式锁实现（Python示例）

```python
import redis
import uuid
from contextlib import contextmanager

redis_client = redis.StrictRedis(host='localhost', port=6379)

@contextmanager
def distributed_lock(lock_key, timeout=10):
    """分布式锁上下文管理器"""
    lock_id = str(uuid.uuid4())
    acquired = redis_client.set(lock_key, lock_id, nx=True, ex=timeout)
    
    if not acquired:
        raise Exception("获取锁失败")
    
    try:
        yield
    finally:
        # 使用Lua脚本确保只删除自己的锁
        lua_script = """
        if redis.call("get", KEYS[1]) == ARGV[1] then
            return redis.call("del", KEYS[1])
        else
            return 0
        end
        """
        redis_client.eval(lua_script, 1, lock_key, lock_id)


def lock_stock(store_id, sku_id, quantity, order_id):
    """下单时锁定库存"""
    lock_key = f"stock_lock:{store_id}:{sku_id}"
    
    with distributed_lock(lock_key):
        # 查询当前库存（数据库行锁）
        store_sku = db.query_one(
            "SELECT * FROM eb_store_service_sku WHERE store_id = %s AND sku_id = %s FOR UPDATE",
            [store_id, sku_id]
        )
        
        if store_sku.available_stock < quantity:
            return False, "库存不足"
        
        # 执行原子化更新
        affected_rows = db.execute(
            """
            UPDATE eb_store_service_sku
            SET available_stock = available_stock - %s,
                locked_stock = locked_stock + %s
            WHERE store_id = %s AND sku_id = %s AND available_stock >= %s
            """,
            [quantity, quantity, store_id, sku_id, quantity]
        )
        
        if affected_rows == 0:
            return False, "库存已被占用"
        
        # 记录库存流水
        db.execute(
            "INSERT INTO eb_stock_flow (...) VALUES (...)",
            [...]
        )
        
        return True, "锁定成功"
```

### 4. 时间槽预约逻辑

```python
def book_time_slot(user_id, schedule_id, slot_id):
    """用户预约时间槽"""
    lock_key = f"schedule_lock:{schedule_id}:{slot_id}"
    
    if not redis_client.set(lock_key, user_id, nx=True, ex=30):
        raise Exception("该时段正在被预约")
    
    try:
        # 查询排班记录（行锁）
        schedule = db.query_one(
            "SELECT * FROM eb_technician_schedule WHERE id = %s FOR UPDATE",
            [schedule_id]
        )
        
        time_slots = json.loads(schedule.time_slots)
        
        # 找到目标时间槽
        for slot in time_slots["slots"]:
            if slot["slot_id"] == slot_id:
                # 检查状态
                if slot["status"] == 2:  # 已锁定
                    if time.time() > slot["locked_expire"]:
                        slot["status"] = 1  # 过期，自动释放
                    else:
                        raise Exception("该时段已被锁定")
                
                if slot["status"] != 1:
                    raise Exception("该时段不可预约")
                
                # 锁定时间槽
                slot["status"] = 2
                slot["locked_at"] = int(time.time())
                slot["locked_expire"] = int(time.time()) + 1800  # 30分钟
                break
        
        # 更新数据库
        db.execute(
            "UPDATE eb_technician_schedule SET time_slots = %s, available_slots = available_slots - 1 WHERE id = %s",
            [json.dumps(time_slots), schedule_id]
        )
        
        return {"schedule_id": schedule_id, "slot_id": slot_id, "locked_expire": slot["locked_expire"]}
    
    finally:
        redis_client.delete(lock_key)
```

### 5. 部分核销功能

**扩展订单子项表**：

```sql
-- 订单子项表补充核销字段
ALTER TABLE `eb_order_item`
ADD COLUMN `total_quantity` int(11) NOT NULL DEFAULT 1 COMMENT '购买总数量',
ADD COLUMN `used_quantity` int(11) DEFAULT 0 COMMENT '已核销数量',
ADD COLUMN `remaining_quantity` int(11) NOT NULL DEFAULT 1 COMMENT '剩余数量',
ADD COLUMN `verification_status` tinyint(1) DEFAULT 0 COMMENT '0=未核销 1=部分核销 2=已完成',
ADD COLUMN `verification_records` json DEFAULT NULL COMMENT '核销记录JSON',
ADD COLUMN `expire_time` int(11) DEFAULT 0 COMMENT '过期时间';
```

---

## 🎯 8大核心问题

| 问题 | 严重程度 | 影响范围 | 解决方案 |
|------|----------|----------|----------|
| 技师排班表结构未定义 | ⭐⭐⭐⭐⭐ | 全系统 | 新增eb_technician + eb_technician_schedule |
| 套餐定价逻辑不完整 | ⭐⭐⭐⭐⭐ | 套餐功能 | 新增套餐定价规则表 |
| 并发控制缺失 | ⭐⭐⭐⭐⭐ | 订单/库存 | Redis分布式锁 + MySQL行锁 |
| 核销功能受限 | ⭐⭐⭐⭐ | 订单核销 | 扩展订单表支持部分核销 |
| 版本控制模糊 | ⭐⭐⭐⭐ | 数据同步 | 新增版本历史表 |
| 数据隔离不足 | ⭐⭐⭐⭐ | 数据安全 | 四层隔离机制 |
| 缓存策略缺失 | ⭐⭐⭐ | 系统性能 | 分层缓存方案 |
| 风控机制缺失 | ⭐⭐⭐ | 业务安全 | 防刷机制 |

---

## 📋 14项优化建议

### P0 必须实施（3项）

1. **技师排班结构设计** - 5天工期
   - 新增技师表、排班表
   - 实现时间槽JSON结构
   - 支持闲时优惠

2. **库存并发控制机制** - 7天工期
   - Redis分布式锁
   - MySQL行锁
   - 库存流水表

3. **套餐定价逻辑完善** - 4天工期
   - 套餐定价规则表
   - 组合优惠计算

### P1 重要优化（4项）

4. **订单部分核销设计** - 3天工期
5. **版本控制机制细化** - 10天工期
6. **价格体系层级设计** - 2天工期
7. **数据隔离安全加固** - 5天工期

### P2 建议实施（7项）

8. 缓存策略设计 - 4天
9. 闲时优惠防套利机制 - 3天
10. 积分兑换风控设计 - 4天
11. 套餐库存提示优化 - 2天
12. 订单异常处理机制 - 3天
13. 数据加密与脱敏 - 3天
14. 操作审计与权限细化 - 3天

---

## 🚀 实施路线图

### 第一阶段（1-2周）- 核心功能

**实施内容**：
- ✅ 技师排班结构
- ✅ 库存并发控制
- ✅ 套餐定价逻辑

**工期**：10天

**里程碑**：核心功能可用，支持基础预约和下单

### 第二阶段（2-3周）- 功能完善

**实施内容**：
- ✅ 订单部分核销
- ✅ 价格体系层级
- ✅ 数据隔离加固

**工期**：10天

**里程碑**：支持次卡、套餐分次消费，安全性提升

### 第三阶段（3-4周）- 性能优化

**实施内容**：
- ✅ 版本控制机制
- ✅ 缓存策略
- ✅ 业务风控

**工期**：14天

**里程碑**：系统稳定性和性能达到生产标准

### 第四阶段（持续）- 持续优化

**实施内容**：
- 监控优化
- 异常处理
- 数据加密脱敏

---

## 🔧 技术准备清单

### 基础设施

- ✅ Redis服务器（分布式锁和缓存）
- ✅ MySQL 5.7+（JSON字段和行级锁）
- ✅ 定时任务调度器（Cron或Celery）

### 开发工具

- ✅ Python 3.8+（后端开发）
- ✅ Node.js 14+（前端开发）
- ✅ Git版本控制

### 监控工具

- ⚠️ 日志系统（ELK或类似）
- ⚠️ 性能监控（New Relic或类似）
- ⚠️ 告警系统（钉钉/企业微信集成）

---

## 💡 关键技术点

### 1. 并发控制双重保障

```
Redis分布式锁（防止并发请求）
    ↓
MySQL行锁（FOR UPDATE）
    ↓
原子化更新（WHERE条件二次校验）
    ↓
库存流水记录
```

### 2. 时间槽状态流转

```
1=可预约 → 2=已锁定（30分钟） → 3=已预约 → 4=已完成
              ↓
         超时自动释放回1
```

### 3. 闲时优惠实现

```
1. 门店配置闲时规则（时间段+折扣）
2. 生成排班时自动标记闲时槽
3. 用户预约时展示闲时价格
4. 防刷机制：限制每日闲时预约次数
```

### 4. 数据隔离四层机制

```
代码层：ORM自动注入WHERE store_id
    ↓
数据库层：RLS（Row-Level Security）
    ↓
接口层：API白名单验证
    ↓
日志层：跨门店查询告警
```

---

## 📊 与CRMEB改造方案对比

| 维度 | CRMEB改造方案 | O2O优化方案 | 差异 |
|------|--------------|------------|------|
| **技师管理** | 简单技师表 | 技师表+排班表+时间槽JSON | O2O更完善 |
| **库存控制** | 基础库存扣减 | 分布式锁+行锁+流水表 | O2O更严谨 |
| **并发处理** | 未提及 | Redis锁+MySQL锁双重保障 | O2O专门设计 |
| **闲时优惠** | 未提及 | 完整闲时规则+防刷机制 | O2O独有 |
| **部分核销** | 未提及 | 支持次卡/套餐分次消费 | O2O独有 |
| **数据隔离** | 字段+索引 | 四层隔离机制 | O2O更安全 |

---

## 🎯 建议

### 立即行动

**优先级P0（必须）**：
1. 创建技师表和排班表
2. 实现库存并发控制
3. 实现时间槽预约逻辑

**SQL脚本已准备好**：
- ✅ eb_technician 建表SQL
- ✅ eb_technician_schedule 建表SQL
- ✅ eb_stock_flow 建表SQL
- ✅ 扩展现有表的ALTER语句

### 技术选型

**后端语言**：
- 文档示例用Python
- 当前项目用Java
- 需要将Python逻辑转换为Java实现

**关键点**：
- Redis分布式锁：使用Redisson（Java）
- JSON字段：使用MySQL JSON函数或Jackson解析
- 定时任务：使用Spring @Scheduled

---

## 📝 下一步

### 选项A：融合O2O方案到CRMEB改造

**优势**：
- 获得更完善的技师排班系统
- 获得更严谨的并发控制机制
- 获得闲时优惠、部分核销等高级功能

**工作量**：
- 需要将Python代码转换为Java
- 需要整合两套方案的数据表设计
- 预计增加10-15天开发周期

### 选项B：先执行CRMEB改造，后续迭代O2O功能

**优势**：
- MVP快速上线（4周）
- 降低初期复杂度
- 验证商业模式后再优化

**风险**：
- 可能遇到并发超卖问题
- 闲时优惠功能缺失
- 后期重构成本高

---

**文档创建时间**: 2026-02-13  
**重要性**: ⭐⭐⭐⭐⭐ 核心技术方案  
**建议**: 融合O2O方案的核心技术点（并发控制、时间槽设计）

