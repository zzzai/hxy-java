# Admin Supply Chain Stock Approval Runbook v1 (2026-03-24)

## 1. 目标与范围
- 为库存调整单与跨店调拨单提供统一后台审批 runbook。
- 范围只覆盖后台申请、审批、驳回、取消和详情追踪。

## 2. 操作入口

| 能力 | 页面入口 | 核心操作 |
|---|---|---|
| `ADM-009` 库存调整单 | `mall/store/stockAdjustOrder/index` | 创建、提交、审批、驳回、取消、查看详情 |
| `ADM-010` 跨店调拨单 | `mall/store/transferOrder/index` | 创建、提交、审批、驳回、取消、查看详情 |

## 3. 审计键最小集
- `orderId`
- `orderNo`
- `storeId`
- `fromStoreId`
- `toStoreId`
- `bizType`
- `applyOperator`
- `approveOperator`
- `lastActionCode`

## 4. 标准操作顺序
1. 先创建单据，确保 `reason`、`remark`、明细项完整。
2. 提交前核对涉及门店和 SKU 明细，防止误提单。
3. 审批或驳回后立即回读详情，确认 `status`、`approveOperator`、`approveTime`、`lastActionCode`。
4. 若单据不再继续，统一走取消，不直接通过备注代替状态流转。

## 5. 失败处理
- 创建成功但详情回读为空时，按“受理异常待核”处理。
- 审批后状态未变更时，暂停后续同类审批，先核单据状态机。
- 调拨单两侧门店信息不完整时，不继续审批。

## 6. 回滚 / 暂停规则
- 单据状态流转异常时，暂停该批次继续审批。
- 取消动作失败或状态不一致时，保留单据并转人工接管。

## 7. 当前结论
- 供应链审批域已具备独立后台 runbook。
- 审批成功不自动等于库存、履约、前台展示已经全部一致。
