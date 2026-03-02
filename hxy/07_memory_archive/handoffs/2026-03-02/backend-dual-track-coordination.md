# 后端双窗口协作任务单（2026-03-02）

## 输入背景
- 当前主线分支：`feat/hxy-hard-cut-vue3-admin`
- 当前已完成：P0（模板主数据绑定、预约占位实接、支付 CI 收口）
- 当前目标：仅后端并行推进 P1，不做 UI 设计/实现
- 并行窗口：
  - 窗口 A（当前目录）：`/root/crmeb-java`，分支 `feat/hxy-hard-cut-vue3-admin`
  - 窗口 B（新目录）：`/root/crmeb-java/.worktrees/be-settlement-sla`，分支 `feat/p1-settlement-sla`

## 变更清单
### 窗口 A（套餐履约/退款闭环）
- 目标：补强“套餐子项履约状态 -> 可退金额 -> 售后单审计”的闭环一致性
- 代码边界（仅改这些目录）：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/**/aftersale/**`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/**/order/**`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/**`
  - `ruoyi-vue-pro-master/sql/mysql/hxy/*after-sale*`
- 交付物：
  1. 套餐子项可退上限改为“持久化快照优先 + JSON兜底”
  2. 售后创建接口落审计字段（命中规则来源、子项明细）
  3. 单测覆盖：正常退款、超额退款、子项已履约不可退

### 窗口 B（提成/结算审批/SLA）
- 目标：打通“提成计提/冲正 -> 结算单审批 -> SLA任务”主链
- 代码边界（仅改这些目录）：
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/**/commission/**`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/**/settlement/**`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/**/job/**`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/**`
  - `ruoyi-vue-pro-master/sql/mysql/hxy/*settlement*`
- 交付物：
  1. 结算审批状态机（草稿/待审/通过/驳回/作废）
  2. 提成冲正幂等键（`biz_type+biz_no+staff_id`）
  3. SLA Job（超时预警 + 自动升级）

## 风险与回滚
- 风险 1：两个窗口同时改 `trade` 同一类导致冲突
  - 控制：严格按“代码边界”执行，禁止跨边界改动
- 风险 2：SQL 迁移脚本冲突
  - 控制：文件名前缀分离（A: `after-sale`，B: `settlement`）
- 风险 3：门禁被历史文件阻断
  - 控制：本批 gate 统一使用差异范围运行，不扫全仓历史
- 回滚策略：
  - 每窗口单独提交可回滚 commit；不 squash 到不可拆分大提交

## 下一窗口接力点
1. 窗口 A 先建分支并开工：
   - `git -C /root/crmeb-java checkout -b feat/p1-package-refund-hardening`
2. 窗口 B 已就绪，直接执行：
   - `cd /root/crmeb-java/.worktrees/be-settlement-sla`
   - `git status --short --branch`
3. 两窗口统一每日两次对齐：
   - 12:00：互看差异并 rebase 到 `origin/feat/hxy-hard-cut-vue3-admin`
   - 20:00：跑门禁 + 关键单测 + 交接文档
4. 每次提交前最小验证：
   - `bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
   - `bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
   - 各自模块定向 `mvn -pl ... -am -Dtest=... test`
