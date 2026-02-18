# CRMEB + O2O 融合方案执行计划

> **决策**: 选择A - 融合O2O核心技术  
> **目标**: 在CRMEB基础上融合O2O的高级功能  
> **工期**: 5周（原4周 + 增加1周）  
> **日期**: 2026-02-13

---

## 🎯 融合策略

### 核心原则

**保留CRMEB的优势**：
- ✅ 用户/会员/支付/优惠券系统（100%复用）
- ✅ 后台管理框架（90%复用）
- ✅ UniApp前端框架（100%复用）

**融合O2O的核心技术**：
- ✅ 技师排班表结构（时间槽JSON设计）
- ✅ 库存并发控制（Redis锁+MySQL锁）
- ✅ 闲时优惠机制
- ✅ 部分核销功能
- ✅ 库存流水表

---

## 📋 融合后的数据表设计

### 1. 技师管理（融合O2O方案）

```sql
-- 技师基础信息表（采用O2O设计）
CREATE TABLE `eb_technician` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '技师ID',
  `store_id` int(11) NOT NULL COMMENT '所属门店ID',
  `name` varchar(50) NOT NULL COMMENT '技师姓名',
  `avatar` varchar(255) DEFAULT '' COMMENT '技师头像URL',
  `level` tinyint(1) DEFAULT 1 COMMENT '技师等级：1=初级 2=中级 3=高级 4=首席',
  `service_years` decimal(4,1) DEFAULT 0.0 COMMENT '从业年限',
  `skill_tags` varchar(255) DEFAULT '' COMMENT '技能标签（逗号分隔）',
  `intro` text COMMENT '技师介绍',
  `rating` decimal(3,2) DEFAULT 5.00 COMMENT '评分（5.00满分）',
  `order_count` int(11) DEFAULT 0 COMMENT '累计服务单数',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态：1=在职 2=离职',
  `created_at` int(11) DEFAULT 0 COMMENT '创建时间',
  `updated_at` int(11) DEFAULT 0 COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师基础信息表';

-- 技师排班表（采用O2O的时间槽JSON设计）
CREATE TABLE `eb_technician_schedule` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '排班ID',
  `store_id` int(11) NOT NULL COMMENT '门店ID',
  `technician_id` int(11) NOT NULL COMMENT '技师ID',
  `service_sku_id` int(11) NOT NULL COMMENT '服务SKU ID',
  `work_date` date NOT NULL COMMENT '排班日期',
  `time_slots` json NOT NULL COMMENT '时间槽JSON（核心字段）',
  `total_slots` int(11) DEFAULT 0 COMMENT '总时间槽数',
  `available_slots` int(11) DEFAULT 0 COMMENT '可预约时间槽数',
  `status` tinyint(1) DEFAULT 1 COMMENT '排班状态：1=正常 2=请假 3=已完成',
  `is_offpeak_enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用闲时优惠',
  `created_at` int(11) DEFAULT 0,
  `updated_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_store_date` (`store_id`,`work_date`),
  KEY `idx_technician_date` (`technician_id`,`work_date`),
  KEY `idx_sku` (`service_sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='技师排班表';
```

### 2. 订单表扩展（融合CRMEB + O2O）

```sql
-- 扩展CRMEB订单表（融合两套方案）
ALTER TABLE `eb_store_order`
-- CRMEB基础字段
ADD COLUMN `order_type` tinyint(1) DEFAULT '1' COMMENT '订单类型:1=商品 2=服务预约',
ADD COLUMN `store_id` int(11) DEFAULT '0' COMMENT '门店ID',
ADD COLUMN `technician_id` int(11) DEFAULT '0' COMMENT '技师ID',
ADD COLUMN `reserve_date` date DEFAULT NULL COMMENT '预约日期',
ADD COLUMN `reserve_time_slot` varchar(20) DEFAULT '' COMMENT '预约时段(14:00)',
ADD COLUMN `service_duration` int(11) DEFAULT '60' COMMENT '服务时长(分钟)',
ADD COLUMN `check_in_code` varchar(32) DEFAULT '' COMMENT '核销码',
ADD COLUMN `check_in_time` int(11) DEFAULT '0' COMMENT '核销时间',
-- O2O扩展字段
ADD COLUMN `schedule_id` int(11) DEFAULT '0' COMMENT '关联排班ID',
ADD COLUMN `slot_id` varchar(50) DEFAULT '' COMMENT '时间槽ID',
ADD COLUMN `locked_expire` int(11) DEFAULT '0' COMMENT '锁定过期时间',
ADD COLUMN `service_start_time` int(11) DEFAULT '0' COMMENT '服务开始时间',
ADD COLUMN `service_end_time` int(11) DEFAULT '0' COMMENT '服务结束时间';

-- 添加索引
ALTER TABLE `eb_store_order`
ADD INDEX `idx_order_type` (`order_type`),
ADD INDEX `idx_store_date` (`store_id`, `reserve_date`),
ADD INDEX `idx_schedule` (`schedule_id`);
```

### 3. 库存管理（采用O2O方案）

```sql
-- 库存变动流水表（O2O设计）
CREATE TABLE `eb_stock_flow` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `store_id` int(11) NOT NULL COMMENT '门店ID',
  `sku_id` int(11) NOT NULL COMMENT 'SKU ID',
  `order_id` bigint(20) DEFAULT 0 COMMENT '关联订单ID',
  `change_type` tinyint(1) NOT NULL COMMENT '变动类型：1=入库 2=锁定 3=释放 4=扣减 5=退款',
  `change_quantity` int(11) NOT NULL COMMENT '变动数量',
  `before_available` int(11) DEFAULT 0 COMMENT '变动前可售库存',
  `after_available` int(11) DEFAULT 0 COMMENT '变动后可售库存',
  `before_locked` int(11) DEFAULT 0 COMMENT '变动前锁定库存',
  `after_locked` int(11) DEFAULT 0 COMMENT '变动后锁定库存',
  `operator_id` int(11) DEFAULT 0 COMMENT '操作人ID',
  `operator_type` tinyint(1) DEFAULT 1 COMMENT '操作人类型：1=系统 2=店长 3=用户',
  `remark` varchar(255) DEFAULT '' COMMENT '备注',
  `created_at` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_store_sku` (`store_id`,`sku_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动流水表';

-- 扩展CRMEB商品表（如果存在eb_store_product_attr_value）
-- 或者扩展门店SKU表
ALTER TABLE `eb_store_product_attr_value`
ADD COLUMN `available_stock` int(11) DEFAULT 0 COMMENT '可售库存',
ADD COLUMN `locked_stock` int(11) DEFAULT 0 COMMENT '锁定库存',
ADD COLUMN `sold_stock` int(11) DEFAULT 0 COMMENT '已售库存',
ADD COLUMN `total_stock` int(11) DEFAULT 0 COMMENT '总库存',
ADD COLUMN `low_stock_threshold` int(11) DEFAULT 10 COMMENT '低库存阈值';
```

### 4. 部分核销（O2O设计）

```sql
-- 扩展CRMEB订单详情表（如果存在eb_store_order_info）
ALTER TABLE `eb_store_order_info`
ADD COLUMN `total_quantity` int(11) NOT NULL DEFAULT 1 COMMENT '购买总数量',
ADD COLUMN `used_quantity` int(11) DEFAULT 0 COMMENT '已核销数量',
ADD COLUMN `remaining_quantity` int(11) NOT NULL DEFAULT 1 COMMENT '剩余数量',
ADD COLUMN `verification_status` tinyint(1) DEFAULT 0 COMMENT '核销状态：0=未核销 1=部分核销 2=已完成',
ADD COLUMN `verification_records` json DEFAULT NULL COMMENT '核销记录JSON',
ADD COLUMN `expire_time` int(11) DEFAULT 0 COMMENT '过期时间';
```

---

## 💻 Java代码实现（转换Python逻辑）

### 1. 分布式锁工具类

```java
package com.zbkj.service.util;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类（基于Redisson）
 */
@Component
public class DistributedLockUtil {
    
    @Autowired
    private RedissonClient redissonClient;
    
    /**
     * 执行带锁的操作
     * @param lockKey 锁的key
     * @param timeout 锁超时时间（秒）
     * @param supplier 业务逻辑
     * @return 业务逻辑返回值
     */
    public <T> T executeWithLock(String lockKey, int timeout, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁，最多等待10秒，锁定timeout秒后自动释放
            boolean acquired = lock.tryLock(10, timeout, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("获取锁失败: " + lockKey);
            }
            
            // 执行业务逻辑
            return supplier.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断", e);
        } finally {
            // 释放锁（只释放自己持有的锁）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### 2. 库存锁定服务

```java
package com.zbkj.service.service.impl;

import com.zbkj.service.dao.StoreProductAttrValueMapper;
import com.zbkj.service.dao.StockFlowMapper;
import com.zbkj.service.model.StoreProductAttrValue;
import com.zbkj.service.model.StockFlow;
import com.zbkj.service.util.DistributedLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存管理服务
 */
@Service
public class StockServiceImpl {
    
    @Autowired
    private DistributedLockUtil lockUtil;
    
    @Autowired
    private StoreProductAttrValueMapper attrValueMapper;
    
    @Autowired
    private StockFlowMapper stockFlowMapper;
    
    /**
     * 锁定库存
     * @param storeId 门店ID
     * @param skuId SKU ID
     * @param quantity 数量
     * @param orderId 订单ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean lockStock(Integer storeId, Integer skuId, Integer quantity, Long orderId) {
        String lockKey = String.format("stock_lock:%d:%d", storeId, skuId);
        
        return lockUtil.executeWithLock(lockKey, 10, () -> {
            // 1. 查询当前库存（加行锁）
            StoreProductAttrValue attrValue = attrValueMapper.selectByIdForUpdate(skuId);
            
            if (attrValue == null) {
                throw new RuntimeException("SKU不存在");
            }
            
            if (attrValue.getAvailableStock() < quantity) {
                throw new RuntimeException("库存不足");
            }
            
            // 2. 记录变动前状态
            Integer beforeAvailable = attrValue.getAvailableStock();
            Integer beforeLocked = attrValue.getLockedStock();
            
            // 3. 执行原子化更新（WHERE条件二次校验）
            int affected = attrValueMapper.lockStock(skuId, quantity);
            
            if (affected == 0) {
                throw new RuntimeException("库存已被占用");
            }
            
            // 4. 记录库存流水
            StockFlow flow = new StockFlow();
            flow.setStoreId(storeId);
            flow.setSkuId(skuId);
            flow.setOrderId(orderId);
            flow.setChangeType(2); // 2=锁定
            flow.setChangeQuantity(quantity);
            flow.setBeforeAvailable(beforeAvailable);
            flow.setAfterAvailable(beforeAvailable - quantity);
            flow.setBeforeLocked(beforeLocked);
            flow.setAfterLocked(beforeLocked + quantity);
            flow.setOperatorType(1); // 1=系统
            flow.setRemark("订单锁定库存");
            flow.setCreatedAt((int)(System.currentTimeMillis() / 1000));
            
            stockFlowMapper.insert(flow);
            
            return true;
        });
    }
    
    /**
     * 释放库存
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseStock(Integer storeId, Integer skuId, Integer quantity, Long orderId) {
        String lockKey = String.format("stock_lock:%d:%d", storeId, skuId);
        
        return lockUtil.executeWithLock(lockKey, 10, () -> {
            // 查询当前库存
            StoreProductAttrValue attrValue = attrValueMapper.selectByIdForUpdate(skuId);
            
            Integer beforeAvailable = attrValue.getAvailableStock();
            Integer beforeLocked = attrValue.getLockedStock();
            
            // 释放锁定库存
            int affected = attrValueMapper.releaseStock(skuId, quantity);
            
            if (affected == 0) {
                throw new RuntimeException("释放库存失败");
            }
            
            // 记录流水
            StockFlow flow = new StockFlow();
            flow.setStoreId(storeId);
            flow.setSkuId(skuId);
            flow.setOrderId(orderId);
            flow.setChangeType(3); // 3=释放
            flow.setChangeQuantity(quantity);
            flow.setBeforeAvailable(beforeAvailable);
            flow.setAfterAvailable(beforeAvailable + quantity);
            flow.setBeforeLocked(beforeLocked);
            flow.setAfterLocked(beforeLocked - quantity);
            flow.setOperatorType(1);
            flow.setRemark("订单取消释放库存");
            flow.setCreatedAt((int)(System.currentTimeMillis() / 1000));
            
            stockFlowMapper.insert(flow);
            
            return true;
        });
    }
}
```

### 3. 时间槽预约服务

```java
package com.zbkj.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zbkj.service.dao.TechnicianScheduleMapper;
import com.zbkj.service.model.TechnicianSchedule;
import com.zbkj.service.util.DistributedLockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 时间槽预约服务
 */
@Service
public class TimeSlotServiceImpl {
    
    @Autowired
    private DistributedLockUtil lockUtil;
    
    @Autowired
    private TechnicianScheduleMapper scheduleMapper;
    
    /**
     * 预约时间槽
     * @param userId 用户ID
     * @param scheduleId 排班ID
     * @param slotId 时间槽ID
     * @return 预约结果
     */
    @Transactional(rollbackFor = Exception.class)
    public JSONObject bookTimeSlot(Integer userId, Integer scheduleId, String slotId) {
        String lockKey = String.format("schedule_lock:%d:%s", scheduleId, slotId);
        
        return lockUtil.executeWithLock(lockKey, 30, () -> {
            // 1. 查询排班记录（加行锁）
            TechnicianSchedule schedule = scheduleMapper.selectByIdForUpdate(scheduleId);
            
            if (schedule == null) {
                throw new RuntimeException("排班记录不存在");
            }
            
            // 2. 解析时间槽JSON
            JSONObject timeSlotsJson = JSON.parseObject(schedule.getTimeSlots());
            JSONArray slots = timeSlotsJson.getJSONArray("slots");
            
            // 3. 找到目标时间槽
            JSONObject targetSlot = null;
            for (int i = 0; i < slots.size(); i++) {
                JSONObject slot = slots.getJSONObject(i);
                if (slotId.equals(slot.getString("slot_id"))) {
                    targetSlot = slot;
                    break;
                }
            }
            
            if (targetSlot == null) {
                throw new RuntimeException("时间槽不存在");
            }
            
            // 4. 检查状态
            Integer status = targetSlot.getInteger("status");
            
            if (status == 2) { // 已锁定
                Long lockedExpire = targetSlot.getLong("locked_expire");
                if (System.currentTimeMillis() / 1000 > lockedExpire) {
                    // 过期，自动释放
                    targetSlot.put("status", 1);
                } else {
                    throw new RuntimeException("该时段已被锁定");
                }
            }
            
            if (status != 1) {
                throw new RuntimeException("该时段不可预约");
            }
            
            // 5. 锁定时间槽
            long currentTime = System.currentTimeMillis() / 1000;
            targetSlot.put("status", 2); // 已锁定
            targetSlot.put("locked_at", currentTime);
            targetSlot.put("locked_expire", currentTime + 1800); // 30分钟
            
            // 6. 更新数据库
            schedule.setTimeSlots(timeSlotsJson.toJSONString());
            schedule.setAvailableSlots(schedule.getAvailableSlots() - 1);
            scheduleMapper.updateById(schedule);
            
            // 7. 返回锁定信息
            JSONObject result = new JSONObject();
            result.put("schedule_id", scheduleId);
            result.put("slot_id", slotId);
            result.put("locked_expire", targetSlot.getLong("locked_expire"));
            result.put("price", targetSlot.getBigDecimal("price"));
            result.put("offpeak_price", targetSlot.getBigDecimal("offpeak_price"));
            
            return result;
        });
    }
    
    /**
     * 确认预约（支付成功后）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmBooking(Integer scheduleId, String slotId, Long orderId) {
        TechnicianSchedule schedule = scheduleMapper.selectByIdForUpdate(scheduleId);
        
        JSONObject timeSlotsJson = JSON.parseObject(schedule.getTimeSlots());
        JSONArray slots = timeSlotsJson.getJSONArray("slots");
        
        for (int i = 0; i < slots.size(); i++) {
            JSONObject slot = slots.getJSONObject(i);
            if (slotId.equals(slot.getString("slot_id"))) {
                // 将状态从"已锁定"改为"已预约"
                slot.put("status", 3);
                slot.put("order_id", orderId);
                break;
            }
        }
        
        schedule.setTimeSlots(timeSlotsJson.toJSONString());
        scheduleMapper.updateById(schedule);
        
        return true;
    }
}
```

### 4. Mapper接口（MyBatis）

```java
package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.service.model.StoreProductAttrValue;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * SKU Mapper
 */
public interface StoreProductAttrValueMapper extends BaseMapper<StoreProductAttrValue> {
    
    /**
     * 查询并加行锁
     */
    StoreProductAttrValue selectByIdForUpdate(@Param("id") Integer id);
    
    /**
     * 锁定库存（原子化更新）
     */
    @Update("UPDATE eb_store_product_attr_value " +
            "SET available_stock = available_stock - #{quantity}, " +
            "    locked_stock = locked_stock + #{quantity} " +
            "WHERE id = #{skuId} AND available_stock >= #{quantity}")
    int lockStock(@Param("skuId") Integer skuId, @Param("quantity") Integer quantity);
    
    /**
     * 释放库存
     */
    @Update("UPDATE eb_store_product_attr_value " +
            "SET available_stock = available_stock + #{quantity}, " +
            "    locked_stock = locked_stock - #{quantity} " +
            "WHERE id = #{skuId} AND locked_stock >= #{quantity}")
    int releaseStock(@Param("skuId") Integer skuId, @Param("quantity") Integer quantity);
}
```

---

## 📅 5周开发计划

### Week 1: 数据库改造 + 基础服务

**Day 1-2**：
- ✅ 执行所有建表SQL
- ✅ 创建Java实体类（Entity）
- ✅ 创建Mapper接口

**Day 3-4**：
- ✅ 实现分布式锁工具类
- ✅ 实现库存锁定服务
- ✅ 单元测试

**Day 5-7**：
- ✅ 实现时间槽预约服务
- ✅ 实现技师排班生成逻辑
- ✅ 集成测试

### Week 2: UniApp前端开发

**Day 1-3**：
- ✅ 首页会员卡片
- ✅ 门店列表（LBS定位）
- ✅ 服务列表

**Day 4-7**：
- ✅ 预约页面（时间选择+技师选择）
- ✅ 订单确认页面
- ✅ 订单列表页面

### Week 3: 订单流程 + 支付

**Day 1-3**：
- ✅ 创建预约订单接口
- ✅ 库存锁定集成
- ✅ 时间槽锁定集成

**Day 4-7**：
- ✅ 微信支付对接
- ✅ 支付成功回调
- ✅ 订单状态流转

### Week 4: 核销 + 闲时优惠

**Day 1-3**：
- ✅ 核销功能（扫码核销）
- ✅ 部分核销支持
- ✅ 核销记录

**Day 4-7**：
- ✅ 闲时优惠规则配置
- ✅ 闲时价格展示
- ✅ 防刷机制

### Week 5: 测试 + 优化 + 上线

**Day 1-3**：
- ✅ 功能测试
- ✅ 并发测试
- ✅ Bug修复

**Day 4-5**：
- ✅ 性能优化
- ✅ 缓存配置
- ✅ 监控配置

**Day 6-7**：
- ✅ 灰度发布
- ✅ 数据验证
- ✅ 正式上线

---

## 🔧 技术准备

### 1. 依赖添加（pom.xml）

```xml
<!-- Redisson（分布式锁） -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.17.0</version>
</dependency>

<!-- FastJSON（JSON处理） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.83</version>
</dependency>
```

### 2. Redis配置（application.yml）

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
    timeout: 3000
    lettuce:
      pool:
        max-active: 8
        max-wait: -1
        max-idle: 8
        min-idle: 0

# Redisson配置
redisson:
  address: redis://localhost:6379
  password: 
  database: 0
```

### 3. 定时任务配置

```java
@Configuration
@EnableScheduling
public class ScheduleConfig {
    
    @Autowired
    private TimeSlotServiceImpl timeSlotService;
    
    /**
     * 每5分钟释放过期的时间槽锁定
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void releaseExpiredLocks() {
        timeSlotService.releaseExpiredLocks();
    }
}
```

---

## 📊 融合对比

| 功能 | 原CRMEB方案 | 融合后方案 | 提升 |
|------|------------|-----------|------|
| 技师管理 | 简单表 | 表+排班+时间槽JSON | ⭐⭐⭐ |
| 并发控制 | 无 | Redis锁+MySQL锁 | ⭐⭐⭐⭐⭐ |
| 闲时优惠 | 无 | 完整规则+防刷 | ⭐⭐⭐⭐ |
| 部分核销 | 无 | 支持次卡分次消费 | ⭐⭐⭐⭐ |
| 库存流水 | 无 | 完整流水表 | ⭐⭐⭐ |
| 开发周期 | 4周 | 5周 | +1周 |
| 系统稳定性 | 中 | 高 | ⭐⭐⭐⭐⭐ |

---

## 🎯 下一步行动

### 立即执行（今天）

1. ✅ 执行数据库建表SQL
2. ✅ 创建Java实体类
3. ✅ 配置Redisson依赖

### 明天开始

4. 实现分布式锁工具类
5. 实现库存锁定服务
6. 编写单元测试

---

**文档创建时间**: 2026-02-13  
**执行状态**: 准备就绪  
**预计完成**: 2026-03-20（5周后）


