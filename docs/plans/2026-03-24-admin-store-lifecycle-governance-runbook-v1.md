# Admin Store Lifecycle Governance Runbook v1 (2026-03-24)

## 1. 目标与范围
- 为生命周期批次日志、变更单和复核日志提供后台治理 runbook。
- 范围只覆盖后台治理动作，不替代发布审批与门店上线判定。

## 2. 操作入口

| 能力 | 页面入口 | 核心操作 |
|---|---|---|
| `ADM-011` 批次日志 / 批次复核执行 | `mall/store/lifecycleBatchLog/index` | 查看批次日志、按批次执行复核 |
| `ADM-012` 生命周期变更单 | `mall/store/lifecycleChangeOrder/index` | 创建、提交、审批、驳回、取消、查看详情 |
| `ADM-013` 生命周期复核日志 | `mall/store/lifecycleRecheckLog/index` | 查看复核日志、比对 blocked / warning 明细 |

## 3. 审计键最小集
- `batchNo`
- `recheckNo`
- `logId`
- `orderNo`
- `storeId`
- `targetLifecycleStatus`
- `operator`
- `source`

## 4. 标准操作顺序
1. 先从批次日志看 `blockedCount / warningCount / detailView`，定位风险批次。
2. 需要复核时执行按批次复核，并记录 `recheckNo`。
3. 生命周期变更单先创建再提交，审批前必须回看守卫结果。
4. 审批 / 驳回 / 取消后立即回读详情和复核日志，确认状态与原因链路一致。

## 5. 失败处理
- `blockedCount > 0` 时，不继续推进同批次生命周期切换。
- 变更单提交后若状态未变化，转人工核对状态机和守卫快照。
- 复核日志出现 `detailParseError=true` 时，先保留样本再继续复核。

## 6. 回滚 / 暂停规则
- 某批次存在大面积 blocked 时，暂停继续执行批次复核。
- 生命周期变更单审批异常时，冻结该门店后续生命周期动作。

## 7. 当前结论
- Store lifecycle 域已具备独立后台 runbook。
- 观察、复核、审批动作都不能直接等同于放量结论。
