# 第四周：技师分润+客情档案实施方案 (Part 2)

## 客情档案系统（续）

### CustomerProfileServiceImpl.java

```java
package com.zbkj.service.service.impl;

import com.zbkj.service.service.CustomerProfileService;
import org.springframework.stereotype.Service;

@Service
public class CustomerProfileServiceImpl implements CustomerProfileService {
    
    @Autowired
    private CustomerProfileMapper profileMapper;
    
    @Autowired
    private StoreOrderMapper orderMapper;
    
    /**
     * 获取客户档案
     */
    @Override
    public CustomerProfile getProfile(Integer userId) {
        CustomerProfile profile = profileMapper.selectOne(
            new LambdaQueryWrapper<CustomerProfile>()
                .eq(CustomerProfile::getUserId, userId)
        );
        
        if (profile == null) {
            // 首次访问，创建档案
            profile = new CustomerProfile();
            profile.setUserId(userId);
            profile.setPreferences("{}");
            profile.setServiceHistory("[]");
            profile.setTags("");
            profile.setTotalVisits(0);
            profile.setTotalConsumption(BigDecimal.ZERO);
            profile.setCreatedAt((int)(System.currentTimeMillis() / 1000));
            profileMapper.insert(profile);
        }
        
        return profile;
    }
    
    /**
     * 更新偏好（基于用户反馈）
     */
    @Override
    public void updatePreferences(Integer userId, String feedbackJson) {
        CustomerProfile profile = getProfile(userId);
        
        // 解析反馈
        JSONObject feedback = JSON.parseObject(feedbackJson);
        JSONObject preferences = JSON.parseObject(profile.getPreferences());
        
        // 更新力度偏好
        if (feedback.containsKey("force_level")) {
            JSONObject forceLevel = preferences.getJSONObject("force_level");
            if (forceLevel == null) {
                forceLevel = new JSONObject();
            }
            
            String newValue = feedback.getString("force_level");
            forceLevel.put("value", newValue);
            
            // 更新置信度
            double confidence = forceLevel.getDoubleValue("confidence");
            confidence = Math.min(confidence + 0.1, 1.0);
            forceLevel.put("confidence", confidence);
            forceLevel.put("last_feedback", LocalDate.now().toString());
            
            preferences.put("force_level", forceLevel);
        }
        
        // 更新其他偏好...
        
        profile.setPreferences(preferences.toJSONString());
        profile.setUpdatedAt((int)(System.currentTimeMillis() / 1000));
        profileMapper.updateById(profile);
    }
    
    /**
     * 智能推荐技师
     */
    @Override
    public Integer recommendTechnician(Integer userId, Integer storeId) {
        CustomerProfile profile = getProfile(userId);
        
        // 1. 优先推荐最喜欢的技师
        if (profile.getFavoriteTechnicianId() != null && profile.getFavoriteTechnicianId() > 0) {
            return profile.getFavoriteTechnicianId();
        }
        
        // 2. 查询历史服务过的技师
        List<StoreOrder> orders = orderMapper.selectList(
            new LambdaQueryWrapper<StoreOrder>()
                .eq(StoreOrder::getUid, userId)
                .eq(StoreOrder::getStoreId, storeId)
                .orderByDesc(StoreOrder::getCreateTime)
                .last("LIMIT 5")
        );
        
        if (!orders.isEmpty()) {
            // 统计技师服务次数
            Map<Integer, Integer> techCountMap = new HashMap<>();
            for (StoreOrder order : orders) {
                Integer techId = order.getTechnicianId();
                techCountMap.put(techId, techCountMap.getOrDefault(techId, 0) + 1);
            }
            
            // 返回服务次数最多的技师
            return techCountMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        return null;
    }
    
    /**
     * 智能推荐时段
     */
    @Override
    public String recommendTimeSlot(Integer userId) {
        CustomerProfile profile = getProfile(userId);
        
        JSONObject preferences = JSON.parseObject(profile.getPreferences());
        JSONObject bestTime = preferences.getJSONObject("best_service_time");
        
        if (bestTime != null) {
            // 判断今天是工作日还是周末
            LocalDate today = LocalDate.now();
            boolean isWeekend = today.getDayOfWeek() == DayOfWeek.SATURDAY 
                             || today.getDayOfWeek() == DayOfWeek.SUNDAY;
            
            if (isWeekend) {
                return bestTime.getString("weekend");
            } else {
                return bestTime.getString("weekday");
            }
        }
        
        return null;
    }
    
    /**
     * 记录服务历史
     */
    @Override
    public void recordServiceHistory(Integer userId, Long orderId) {
        CustomerProfile profile = getProfile(userId);
        StoreOrder order = orderMapper.selectById(orderId);
        
        // 更新统计
        profile.setTotalVisits(profile.getTotalVisits() + 1);
        profile.setTotalConsumption(
            profile.getTotalConsumption().add(order.getPayPrice())
        );
        profile.setLastVisitTime((int)(System.currentTimeMillis() / 1000));
        
        // 更新服务历史
        JSONArray history = JSON.parseArray(profile.getServiceHistory());
        if (history == null) {
            history = new JSONArray();
        }
        
        JSONObject record = new JSONObject();
        record.put("orderId", orderId);
        record.put("technicianId", order.getTechnicianId());
        record.put("serviceDate", order.getReserveDate());
        record.put("amount", order.getPayPrice());
        
        history.add(record);
        
        // 只保留最近10次
        if (history.size() > 10) {
            history.remove(0);
        }
        
        profile.setServiceHistory(history.toJSONString());
        profile.setUpdatedAt((int)(System.currentTimeMillis() / 1000));
        
        profileMapper.updateById(profile);
    }
}
```

### Step 2: 技师端显示客情提示

**uniapp/pages/technician/customer-info.vue**

```vue
<template>
  <view class="customer-info">
    <view class="header">
      <text class="customer-name">{{ customer.name }}</text>
      <text class="visit-count">第{{ profile.totalVisits }}次到店</text>
    </view>
    
    <!-- 重要提示 -->
    <view class="important-tips">
      <view class="tip-title">⚠️ 重要提示</view>
      
      <view v-if="preferences.force_level" class="tip-item">
        <text class="label">力度偏好：</text>
        <text class="value">{{ getForceLevelText(preferences.force_level.value) }}</text>
        <text class="confidence">置信度{{ (preferences.force_level.confidence * 100).toFixed(0) }}%</text>
      </view>
      
      <view v-if="preferences.sensitive_parts && preferences.sensitive_parts.length > 0" class="tip-item">
        <text class="label">敏感部位：</text>
        <text class="value warning">{{ preferences.sensitive_parts.join('、') }}</text>
      </view>
      
      <view v-if="preferences.health_issues && preferences.health_issues.length > 0" class="tip-item">
        <text class="label">健康问题：</text>
        <view v-for="issue in preferences.health_issues" :key="issue.issue" class="health-issue">
          <text class="issue-name">{{ issue.issue }}（{{ issue.level }}）</text>
          <text class="avoid">避免：{{ issue.avoid.join('、') }}</text>
        </view>
      </view>
      
      <view v-if="preferences.communication_style" class="tip-item">
        <text class="label">交流偏好：</text>
        <text class="value">{{ getCommStyleText(preferences.communication_style.value) }}</text>
      </view>
    </view>
    
    <!-- 推荐加项 -->
    <view v-if="preferences.preferred_addons && preferences.preferred_addons.length > 0" class="addon-recommend">
      <view class="section-title">💡 推荐加项</view>
      <view 
        v-for="addon in preferences.preferred_addons" 
        :key="addon.item"
        class="addon-item"
      >
        <text class="addon-name">{{ addon.item }}</text>
        <text class="addon-rate">历史接受率{{ (addon.success_rate * 100).toFixed(0) }}%</text>
      </view>
    </view>
    
    <!-- 服务历史 -->
    <view class="service-history">
      <view class="section-title">📋 最近服务</view>
      <view 
        v-for="record in serviceHistory" 
        :key="record.orderId"
        class="history-item"
      >
        <text class="date">{{ record.serviceDate }}</text>
        <text class="tech">技师{{ getTechName(record.technicianId) }}</text>
        <text class="amount">¥{{ record.amount }}</text>
      </view>
    </view>
    
    <!-- 手动备注 -->
    <view class="manual-notes">
      <view class="section-title">📝 特殊备注</view>
      <textarea 
        v-model="specialNotes"
        placeholder="记录客户特殊需求或偏好..."
        @blur="saveNotes"
      />
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      customer: {},
      profile: {},
      preferences: {},
      serviceHistory: [],
      specialNotes: ''
    }
  },
  onLoad(options) {
    this.userId = options.userId
    this.loadCustomerInfo()
  },
  methods: {
    async loadCustomerInfo() {
      const res = await this.$api.get('/technician/customer/profile', {
        userId: this.userId
      })
      
      this.profile = res.data
      this.preferences = JSON.parse(res.data.preferences || '{}')
      this.serviceHistory = JSON.parse(res.data.serviceHistory || '[]')
      this.specialNotes = res.data.specialNotes || ''
    },
    
    getForceLevelText(value) {
      const map = {
        'light': '轻柔',
        'medium_light': '中轻',
        'medium': '适中',
        'medium_heavy': '中重',
        'heavy': '重手法'
      }
      return map[value] || value
    },
    
    getCommStyleText(value) {
      const map = {
        'quiet': '偏好安静，减少闲聊',
        'moderate': '适度交流',
        'chatty': '喜欢聊天'
      }
      return map[value] || value
    },
    
    async saveNotes() {
      await this.$api.post('/technician/customer/update-notes', {
        userId: this.userId,
        specialNotes: this.specialNotes
      })
      
      uni.showToast({
        title: '备注已保存',
        icon: 'success'
      })
    }
  }
}
</script>
```

---

## 📅 Day 6-7: 集成测试与优化

### 测试场景

#### 场景1：提成自动计算
1. 用户预约并完成服务（点钟）
2. 订单金额¥128
3. 系统自动计算提成：¥128 × 20% = ¥25.6
4. 验证：提成记录生成，技师端显示收入

#### 场景2：加钟提成
1. 服务中加钟30分钟，金额¥64
2. 系统计算加钟提成：¥64 × 20% = ¥12.8
3. 验证：生成独立提成记录

#### 场景3：好评奖励
1. 用户评价5星（≥4.8）
2. 系统自动发放好评奖励¥10
3. 验证：提成记录type=6，金额¥10

#### 场景4：客情采集
1. 用户首次到店
2. 服务完成后评价：力度适中
3. 系统记录偏好，置信度0.5
4. 第二次到店，技师端显示提示
5. 验证：preferences JSON正确更新

#### 场景5：智能推荐
1. 用户第3次预约
2. 系统推荐：上次服务的技师小王
3. 推荐时段：14:00-16:00（历史偏好）
4. 验证：推荐准确

---

## 📊 验收标准

### 提成系统
- [ ] 订单完成自动计算提成
- [ ] 点钟/排钟提成比例正确
- [ ] 加钟提成单独计算
- [ ] 好评奖励正确发放
- [ ] 技师端收入统计准确
- [ ] 结算功能正常

### 客情档案
- [ ] 首次到店自动创建档案
- [ ] 服务完成后更新历史
- [ ] 偏好置信度正确计算
- [ ] 技师端正确显示客情提示
- [ ] 智能推荐技师准确
- [ ] 智能推荐时段准确
- [ ] 手动备注可保存

---

## 🎯 完整系统验收

### 端到端测试流程

**完整预约-服务-结算流程**

1. **用户预约**
   - 选择服务项目
   - 选择派单模式（点钟/排钟）
   - 选择日期和时间
   - 使用会员卡支付
   - 验证：订单创建，时间槽锁定

2. **技师准备**
   - 查看今日排班
   - 看到新预约通知
   - 查看客户档案（偏好提示）
   - 验证：客情信息正确显示

3. **服务过程**
   - 开始服务
   - 中途加钟30分钟
   - 用户支付加钟费用
   - 验证：加钟订单创建，时间顺延

4. **服务完成**
   - 技师点击"服务完成"
   - 核销主订单+加钟订单
   - 邀请用户评价
   - 验证：订单状态更新

5. **用户评价**
   - 用户评分5星
   - 反馈力度适中
   - 验证：好评奖励发放，偏好更新

6. **提成结算**
   - 系统自动计算提成
   - 基础提成 + 点钟加成 + 加钟提成 + 好评奖励
   - 技师端查看收入明细
   - 验证：提成金额正确

7. **客情更新**
   - 档案记录本次服务
   - 更新偏好置信度
   - 累计消费金额
   - 验证：档案数据准确

---

## 📈 性能优化建议

### 1. 提成计算优化
- 使用异步任务计算提成，避免阻塞订单完成
- 批量结算时使用事务，提高效率

### 2. 客情查询优化
- 客情档案添加Redis缓存
- 偏好JSON使用索引加速查询

### 3. 统计查询优化
- 技师收入统计使用物化视图
- 定时任务预计算统计数据

---

## 🎉 方案B完成总结

### 4周开发成果

**数据库**
- 新增表：8张
- 扩展表：4张
- 视图：3个

**后端API**
- 排班系统：5个API
- 卡项体系：5个API
- 派单加钟：3个API
- 提成系统：4个API
- 客情档案：3个API
- **总计：20个核心API**

**前端页面**
- 店长端：3个页面
- 用户端：6个页面
- 技师端：5个页面
- **总计：14个页面**

**核心功能**
- ✅ 智能排班
- ✅ 时间槽管理
- ✅ 卡项购买与核销
- ✅ 点钟/排钟派单
- ✅ 加钟动态订单
- ✅ 多维度提成
- ✅ 客情档案
- ✅ 智能推荐

---

## 📝 后续优化方向

1. **数据分析**
   - 技师绩效分析
   - 客户RFM模型
   - 预约热力图

2. **营销自动化**
   - 流失客户召回
   - 生日关怀
   - 个性化优惠券

3. **智能化升级**
   - AI推荐技师
   - 动态定价优化
   - 需求预测

---

**🎊 恭喜！方案B完整实施方案已全部完成！**

现在可以按照4周计划，逐步实施排班+卡项+派单+分润+客情的完整O2O系统！

