# MiniApp Growth & Unit Economics Pack - Window D Handoff (2026-03-09)

## 1. 变更摘要
- 新增增长 KPI 与实验计划文档：
  - `docs/products/miniapp/2026-03-09-miniapp-growth-kpi-and-experiment-plan-v1.md`
  - 包含指标树（北极星/一级/二级）、实验假设、样本口径、判定阈值、止损规则。
- 新增单店经营模型文档：
  - `docs/products/miniapp/2026-03-09-miniapp-commercial-model-and-unit-economics-v1.md`
  - 包含收入结构、券/积分/礼品卡成本、CAC、毛利口径、ROI 公式与敏感性分析。
- 两份文档均补齐“埋点字段 + 台账字段 + 主键”的映射关系，可直接用于经营看板建设与复盘。

## 2. 对齐关系
- 与 `miniapp-event-taxonomy-v2` 对齐：统一 `resultCode/errorCode/degraded/degradeReason` 与主键追踪口径。
- 与 `feature-inventory-and-release-matrix` 对齐：指标与实验优先支持 RB1-P0/RB2-P1 功能域。

## 3. 验证命令与结果
1. `git diff --check` -> PASS
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh` -> PASS
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh` -> PASS

## 4. 联调注意点（A/B/C）
- 字段：经营指标统一读取事件字段 `resultCode/errorCode/degraded/degradeReason`，并必须与 `orderId/afterSaleId/payRefundId` 关联。
- 错误码：看板侧按字符串存储并展示，避免数值类型漂移导致聚合错误。
- 降级行为：`degraded=true` 的流量与成功流量必须分池统计，不能混入支付成功率与 ROI 计算。
