# MiniApp Release Gate KPI Runbook v1 (2026-03-09)

## 1. 目标
- 将发布门禁从“人工经验判断”升级为“指标驱动 Go/No-Go”。
- 对齐文档：
  - `docs/plans/2026-03-09-miniapp-store-operations-dashboard-spec-v1.md`
  - `docs/plans/2026-03-09-miniapp-degraded-pool-governance-v1.md`
  - `docs/plans/2026-03-09-miniapp-data-quality-slo-and-alerting-v1.md`

## 2. 发布门禁 KPI（Go/No-Go）

| KPI编码 | 指标 | 阈值 | 观察窗口 | 告警级别 | Go/No-Go 规则 |
|---|---|---|---|---|---|
| `RG-KPI-01` | 支付成功率（main_pool） | >= 12% | 最近2小时 | P1 | 低于阈值 No-Go |
| `RG-KPI-02` | 支付降级率（degraded_pool） | <= 3% | 最近2小时 | P1 | 高于阈值 No-Go |
| `RG-KPI-03` | 退款SLA达标率 | >= 90% | 最近24小时 | P1 | 低于阈值 No-Go |
| `RG-KPI-04` | 履约按时率 | >= 92% | 最近24小时 | P1 | 低于阈值 No-Go |
| `RG-KPI-05` | DQ BLOCK 命中数 | = 0 | 最近2小时 | P0 | 大于0直接 No-Go |
| `RG-KPI-06` | 假成功拦截次数（关键页） | = 0 | 最近24小时 | P0 | 大于0直接 No-Go |
| `RG-KPI-07` | 门店ROI（main_pool） | >= 0 | 最近7天 | P0 | 小于0且连续恶化 No-Go |
| `RG-KPI-08` | 关键字段缺失率 | <= 2% | 最近2小时 | P1 | 高于阈值 No-Go |

## 3. 发布决策流程
1. 发布前 30 分钟冻结窗口，锁定统计区间。
2. 读取 `RG-KPI-*` 全量指标。
3. 判定：
   - 任一 P0 不达标 => `No-Go`。
   - P1 2项及以上不达标 => `No-Go`。
   - 其余为 `Go with Watch`（带观察）。
4. 结论入库：记录 `releaseId/result/reason/owner/timestamp`。

## 4. 高频错误码 TopN 监控策略

### 4.1 统计口径
- 聚合维度：`errorCode + route + storeId + releaseId`。
- 时间窗：5分钟滚动 + 1小时汇总。
- TopN：默认 Top 10（按错误事件次数）。

### 4.2 阈值与处置

| 场景 | 阈值 | 告警 | 动作 |
|---|---|---|---|
| `Top1 占比 > 40%` | 最近30分钟 | P1 | 建立专项排查工单 |
| `Top3 累计占比 > 70%` | 最近1小时 | P1 | 暂停灰度扩容 |
| `P0 错误码出现`（如假成功/关键链路不可追） | 任一命中 | P0 | 立即 No-Go + 回滚 |
| `同错误码连续3窗口上升` | 15分钟 | P2 | 提前预警，观察加密 |

### 4.3 升级路径
- P0：值班技术 -> 域负责人 -> 发布负责人 -> 业务Owner。
- P1：域负责人 -> 发布负责人。
- P2：指标Owner。

## 5. 运行操作手册

### 5.1 发布前检查（T-30min）
1. 校验 KPI 全量可出数。
2. 校验 `degraded_pool` 已独立分池。
3. 校验 DQ BLOCK=0。
4. 校验 TopN 错误码无 P0。

### 5.2 发布中观察（T 到 T+120min）
1. 每15分钟刷新 `RG-KPI-*`。
2. 若 `RG-KPI-02` 超阈值，先限流后排查。
3. 若出现 P0 码，直接回滚。

### 5.3 发布后复盘（T+24h）
1. 记录 KPI 趋势和偏差。
2. 输出 TopN 错误码根因。
3. 更新下一版本门禁阈值建议。

## 6. 审计字段规范
- 发布门禁审计记录最小字段：
  - `releaseId/runId/orderId/payRefundId/sourceBizNo/errorCode/result`
- 任一门禁告警必须带五键：`runId/orderId/payRefundId/sourceBizNo/errorCode`。

## 7. 验收标准
1. Go/No-Go 决策可重放，结论可解释。
2. TopN 错误码监控能定位到具体路由和门店。
3. P0 告警可在 15 分钟内触达并执行动作。
4. 发布结果可关联经营看板与实验影响。
