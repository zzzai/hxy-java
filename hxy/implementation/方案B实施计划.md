# 方案B实施计划：排班+卡项系统

## 📅 4周开发计划

### 第一周：技师排班系统
- Day 1-2: 数据库表设计与创建
- Day 3-4: 后端API开发（排班CRUD、时间槽管理）
- Day 5-7: 前端界面（店长端排班、用户端选择、技师端查看）

### 第二周：卡项体系
- Day 1-2: 卡项模板与用户持卡表
- Day 3-4: 购卡、核销、余额查询API
- Day 5-7: 前端界面（购卡页、卡包、核销页）

### 第三周：派单模式+加钟
- Day 1-3: 点钟/排钟逻辑实现
- Day 4-5: 加钟动态订单变更
- Day 6-7: 前端交互优化

### 第四周：技师分润+客情档案
- Day 1-3: 提成规则引擎
- Day 4-5: 客情档案采集与应用
- Day 6-7: 集成测试与优化

---

## 🗄️ 数据库设计（增量）

详见：`/root/crmeb-java/hxy/database/database_migration_v2.0.sql`

---

## 📁 文件结构

```text
/root/crmeb-java/hxy/
├── implementation/
│   ├── 方案B实施计划.md (本文件)
│   ├── 第一周-排班系统实施.md
│   ├── 第二周-卡项体系实施.md
│   ├── 第三周-派单加钟实施.md
│   └── 第四周-分润客情实施-Part1/Part2.md
└── database/
    └── database_migration_v2.0.sql
```

---

## 🚀 立即开始

**Step 1**: 执行数据库脚本
```bash
mysql -u root -p crmeb_java < /root/crmeb-java/hxy/database/database_migration_v2.0.sql
```

**Step 2**: 查看第一周详细实施方案
```bash
cat /root/crmeb-java/hxy/implementation/第一周-排班系统实施.md
```

**Step 3**: 开始开发
按周计划逐步实施
