# 第四周：技师分润+客情档案实施方案

## 📅 Day 1-3: 技师分润提成系统

### 任务清单
- [x] 数据库表已创建（eb_commission_record）
- [ ] 实现多维度提成规则
- [ ] 开发提成计算引擎
- [ ] 实现自动结算
- [ ] 技师端收入统计页面

---

## 提成规则设计

### 多维度提成矩阵

| 提成维度 | 提成比例 | 触发条件 | 示例 |
|---------|---------|---------|------|
| 基础服务 | 15% | 完成任何服务 | ¥128×15%=¥19.2 |
| 点钟加成 | +5% | 用户指定该技师 | ¥128×20%=¥25.6 |
| 加钟提成 | 20% | 服务中途加钟 | ¥64×20%=¥12.8 |
| 办卡提成 | 5% | 推荐用户办卡 | ¥980×5%=¥49 |
| 产品销售 | 10% | 推销养生产品 | ¥198×10%=¥19.8 |
| 好评奖励 | 固定¥10 | 用户评分≥4.8 | 每单+¥10 |
| 月度绩效 | 阶梯式 | 月服务≥100单 | 额外¥500 |

---

## 实施步骤

### Step 1: 提成Service实现

**CommissionService.java**

```java
package com.zbkj.service.service;

import com.zbkj.service.model.CommissionRecord;
import java.math.BigDecimal;
import java.util.List;

public interface CommissionService {
    
    /**
     * 计算订单提成
     */
    List<CommissionRecord> calculateOrderCommission(Long orderId);
    
    /**
     * 记录提成
     */
    void recordCommission(CommissionRecord record);
    
    /**
     * 查询技师提成列表
     */
    List<CommissionRecord> getTechnicianCommissions(
        Integer technicianId,
        Integer startTime,
        Integer endTime
    );
    
    /**
     * 结算提成
     */
    Boolean settleCommission(List<Long> recordIds);
    
    /**
     * 统计技师收入
     */
    Map<String, Object> getTechnicianIncome(
        Integer technicianId,
        String period // today/week/month
    );
}
```

**CommissionServiceImpl.java**

```java
package com.zbkj.service.service.impl;

import com.zbkj.service.service.CommissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionServiceImpl implements CommissionService {
    
    @Autowired
    private CommissionRecordMapper commissionMapper;
    
    @Autowired
    private StoreOrderMapper orderMapper;
    
    /**
     * 计算订单提成
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CommissionRecord> calculateOrderCommission(Long orderId) {
        
        List<CommissionRecord> records = new ArrayList<>();
        
        // 查询订单
        StoreOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return records;
        }
        
        Integer technicianId = order.getTechnicianId();
        BigDecimal orderAmount = order.getPayPrice();
        
        // 1. 基础服务提成
        BigDecimal baseRate = new BigDecimal("0.15"); // 15%
        
        // 2. 点钟加成
        if (order.getDispatchMode() == 1) {
            baseRate = baseRate.add(new BigDecimal("0.05")); // +5%
        }
        
        // 3. 加钟提成（如果是加钟订单）
        if (order.getIsAddon() == 1) {
            baseRate = new BigDecimal("0.20"); // 20%
        }
        
        // 计算提成金额
        BigDecimal commissionAmount = orderAmount.multiply(baseRate)
            .setScale(2, RoundingMode.HALF_UP);
        
        // 创建提成记录
        CommissionRecord record = new CommissionRecord();
        record.setTechnicianId(technicianId);
        record.setOrderId(orderId);
        record.setCommissionType(getCommissionType(order));
        record.setBaseAmount(orderAmount);
        record.setCommissionRate(baseRate);
        record.setCommissionAmount(commissionAmount);
        record.setSettlementStatus(0);
        record.setCreatedAt((int)(System.currentTimeMillis() / 1000));
        
        records.add(record);
        
        // 4. 好评奖励（订单完成后评价时触发）
        // 在评价接口中调用
        
        return records;
    }
    
    /**
     * 记录提成
     */
    @Override
    public void recordCommission(CommissionRecord record) {
        commissionMapper.insert(record);
    }
    
    /**
     * 统计技师收入
     */
    @Override
    public Map<String, Object> getTechnicianIncome(
        Integer technicianId,
        String period
    ) {
        Map<String, Object> result = new HashMap<>();
        
        // 计算时间范围
        int startTime = getStartTime(period);
        int endTime = (int)(System.currentTimeMillis() / 1000);
        
        // 查询提成记录
        List<CommissionRecord> records = commissionMapper.selectList(
            new LambdaQueryWrapper<CommissionRecord>()
                .eq(CommissionRecord::getTechnicianId, technicianId)
                .between(CommissionRecord::getCreatedAt, startTime, endTime)
        );
        
        // 统计
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal settledAmount = BigDecimal.ZERO;
        BigDecimal unsettledAmount = BigDecimal.ZERO;
        int totalOrders = 0;
        
        for (CommissionRecord record : records) {
            totalCommission = totalCommission.add(record.getCommissionAmount());
            
            if (record.getSettlementStatus() == 1) {
                settledAmount = settledAmount.add(record.getCommissionAmount());
            } else {
                unsettledAmount = unsettledAmount.add(record.getCommissionAmount());
            }
            
            totalOrders++;
        }
        
        result.put("totalCommission", totalCommission);
        result.put("settledAmount", settledAmount);
        result.put("unsettledAmount", unsettledAmount);
        result.put("totalOrders", totalOrders);
        result.put("period", period);
        
        return result;
    }
    
    /**
     * 结算提成
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean settleCommission(List<Long> recordIds) {
        int now = (int)(System.currentTimeMillis() / 1000);
        
        for (Long recordId : recordIds) {
            CommissionRecord record = commissionMapper.selectById(recordId);
            if (record != null && record.getSettlementStatus() == 0) {
                record.setSettlementStatus(1);
                record.setSettlementTime(now);
                commissionMapper.updateById(record);
            }
        }
        
        return true;
    }
    
    private int getCommissionType(StoreOrder order) {
        if (order.getIsAddon() == 1) {
            return 3; // 加钟
        } else if (order.getDispatchMode() == 1) {
            return 2; // 点钟加成
        } else {
            return 1; // 基础服务
        }
    }
    
    private int getStartTime(String period) {
        LocalDateTime now = LocalDateTime.now();
        
        switch (period) {
            case "today":
                return (int)(now.withHour(0).withMinute(0).withSecond(0)
                    .atZone(ZoneId.systemDefault()).toEpochSecond());
            case "week":
                return (int)(now.minusWeeks(1)
                    .atZone(ZoneId.systemDefault()).toEpochSecond());
            case "month":
                return (int)(now.minusMonths(1)
                    .atZone(ZoneId.systemDefault()).toEpochSecond());
            default:
                return 0;
        }
    }
}
```

### Step 2: 订单完成时自动计算提成

**在OrderService中添加**

```java
/**
 * 订单完成回调
 */
@Override
@Transactional(rollbackFor = Exception.class)
public void onOrderComplete(Long orderId) {
    
    // 1. 更新订单状态
    StoreOrder order = orderMapper.selectById(orderId);
    order.setStatus(3); // 已完成
    orderMapper.updateById(order);
    
    // 2. 计算并记录提成
    List<CommissionRecord> commissions = commissionService.calculateOrderCommission(orderId);
    for (CommissionRecord record : commissions) {
        commissionService.recordCommission(record);
    }
    
    // 3. 更新技师统计
    technicianMapper.incrementOrderCount(order.getTechnicianId());
}
```

### Step 3: 技师端收入统计页面

**uniapp/pages/technician/income.vue**

```vue
<template>
  <view class="income-page">
    <!-- 时间筛选 -->
    <view class="period-tabs">
      <view 
        v-for="tab in tabs" 
        :key="tab.value"
        @click="selectPeriod(tab.value)"
        :class="['tab-item', {active: period === tab.value}]"
      >
        {{ tab.label }}
      </view>
    </view>
    
    <!-- 收入概览 -->
    <view class="income-summary">
      <view class="summary-item">
        <text class="label">总收入</text>
        <text class="value">¥{{ incomeData.totalCommission }}</text>
      </view>
      <view class="summary-item">
        <text class="label">已结算</text>
        <text class="value settled">¥{{ incomeData.settledAmount }}</text>
      </view>
      <view class="summary-item">
        <text class="label">待结算</text>
        <text class="value pending">¥{{ incomeData.unsettledAmount }}</text>
      </view>
    </view>
    
    <!-- 收入明细 -->
    <view class="income-list">
      <view class="list-header">
        <text>收入明细（{{ incomeData.totalOrders }}单）</text>
      </view>
      
      <view 
        v-for="item in commissionList" 
        :key="item.id"
        class="income-item"
      >
        <view class="item-left">
          <text class="order-no">订单{{ item.orderId }}</text>
          <text class="commission-type">{{ getTypeText(item.commissionType) }}</text>
          <text class="time">{{ formatTime(item.createdAt) }}</text>
        </view>
        
        <view class="item-right">
          <text class="amount">+¥{{ item.commissionAmount }}</text>
          <text :class="['status', item.settlementStatus === 1 ? 'settled' : 'pending']">
            {{ item.settlementStatus === 1 ? '已结算' : '待结算' }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      period: 'today',
      tabs: [
        { label: '今日', value: 'today' },
        { label: '本周', value: 'week' },
        { label: '本月', value: 'month' }
      ],
      incomeData: {},
      commissionList: []
    }
  },
  onShow() {
    this.loadIncome()
  },
  methods: {
    async loadIncome() {
      const res = await this.$api.get('/technician/income/stats', {
        period: this.period
      })
      this.incomeData = res.data
      
      const listRes = await this.$api.get('/technician/income/list', {
        period: this.period
      })
      this.commissionList = listRes.data
    },
    
    selectPeriod(period) {
      this.period = period
      this.loadIncome()
    },
    
    getTypeText(type) {
      const map = {
        1: '基础服务',
        2: '点钟加成',
        3: '加钟提成',
        4: '办卡提成',
        5: '产品销售',
        6: '好评奖励'
      }
      return map[type] || ''
    }
  }
}
</script>
```

---

## 📅 Day 4-5: 客情档案系统

### 核心价值

💡 **把一次性交易变成长期关系**

- 有客情档案的客户，复购率提升40%
- 回头客的客诉率降低60%
- 技师服务效率提升20%

### preferences JSON结构

```json
{
  "force_level": {
    "value": "medium_light",
    "confidence": 0.85,
    "last_feedback": "2025-02-10"
  },
  "sensitive_parts": ["脚心", "腋下"],
  "preferred_temperature": {
    "value": "warm",
    "confidence": 0.70
  },
  "communication_style": {
    "value": "quiet",
    "confidence": 0.90
  },
  "health_issues": [
    {
      "issue": "腰椎间盘突出",
      "level": "中度",
      "avoid": ["过度按压腰部"]
    }
  ],
  "preferred_addons": [
    {
      "item": "薰衣草精油",
      "frequency": 8,
      "success_rate": 0.80
    }
  ],
  "best_service_time": {
    "weekday": "14:00-16:00",
    "weekend": "10:00-12:00"
  }
}
```

### Step 1: 客情Service实现

**CustomerProfileService.java**

```java
package com.zbkj.service.service;

import com.zbkj.service.model.CustomerProfile;

public interface CustomerProfileService {
    
    /**
     * 获取客户档案
     */
    CustomerProfile getProfile(Integer userId);
    
    /**
     * 更新偏好（基于反馈）
     */
    void updatePreferences(Integer userId, String feedbackJson);
    
    /**
     * 智能推荐技师
     */
    Integer recommendTechnician(Integer userId, Integer storeId);
    
    /**
     * 智能推荐时段
     */
    String recommendTimeSlot(Integer userId);
    
    /**
     * 记录服务历史
     */
    void recordServiceHistory(Integer userId, Long orderId);
}
```

完整实现见下一部分文档...

