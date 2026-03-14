# MiniApp 财务运营技师提成明细 / 计提管理 Runbook v1（2026-03-14）

## 1. 目标与范围
- 目标：为 `BO-004 技师提成明细 / 计提管理` 建立监控、告警、回滚、暂停操作和接口闭环治理手册。
- 覆盖接口：
  - `GET /booking/commission/list-by-technician`
  - `GET /booking/commission/list-by-order`
  - `GET /booking/commission/pending-amount`
  - `POST /booking/commission/settle`
  - `POST /booking/commission/batch-settle`
  - `GET /booking/commission/config/list`
  - `POST /booking/commission/config/save`
  - `DELETE /booking/commission/config/delete`
- 当前真值前提：
  - 已核到真实 `TechnicianCommissionController`
  - 当前未核到独立后台页面文件与独立前端 API 文件
  - 所以本 runbook 只治理“接口真实存在”的后台运营能力，不得宣称页面功能已上线

## 2. 运行边界
- 当前无服务端 `degraded=true / degradeReason` 证据：
  - 所有降级只允许写成“人工冻结 / 只读模式 / 暂停操作”
  - 不得杜撰服务端降级逻辑
- 页面未闭环时，按“仅接口闭环”治理：
  - 不做页面成功率统计
  - 不拿 BO-003 `/booking/commission-settlement/*` 页面样本冲抵 BO-004 `/booking/commission/*`
  - 所有成功判定仅以 controller 返回 + 写后回读 + 审计证据为准
- 写操作当前存在 no-op 风险：
  - `settle` 对不存在记录或非 `PENDING` 记录直接返回，不抛显式业务码
  - `batch-settle` 仅循环调用 `settle`，也不返回影响行数
  - `config/save`、`config/delete` 不校验更新 / 删除影响行数
  - 因此 `code=0 && data=true` 不能直接视为成功

## 3. 监控字段最小集

| 接口 | 监控字段最小集 | 说明 |
|---|---|---|
| `GET /booking/commission/list-by-technician` | `method,path,technicianId,httpStatus,commonResult.code,resultCount` | `resultCount=len(data)` |
| `GET /booking/commission/list-by-order` | `method,path,orderId,httpStatus,commonResult.code,resultCount` | `resultCount=len(data)` |
| `GET /booking/commission/pending-amount` | `method,path,technicianId,httpStatus,commonResult.code,pendingAmount` | `pendingAmount=data` |
| `POST /booking/commission/settle` | `method,path,commissionId,httpStatus,commonResult.code,preStatus,postStatus,postSettlementTime` | 写后必须补回读字段 |
| `POST /booking/commission/batch-settle` | `method,path,technicianId,httpStatus,commonResult.code,prePendingCount,postPendingCount,prePendingAmount,postPendingAmount` | 不返回 count，必须靠前后对比 |
| `GET /booking/commission/config/list` | `method,path,storeId,httpStatus,commonResult.code,resultCount` | `resultCount=len(data)` |
| `POST /booking/commission/config/save` | `method,path,id,storeId,commissionType,rate,fixedAmount,httpStatus,commonResult.code,postConfigCount` | `postConfigCount` 指同 `storeId+commissionType` 的记录数 |
| `DELETE /booking/commission/config/delete` | `method,path,id,storeId,httpStatus,commonResult.code,postExists` | `postExists=true/false` 来自回读 |

### 3.1 字段规则
- 不按 `msg` 聚合，只按 `commonResult.code`
- 当前 BO-004 接口层没有显式 `runId` 入参，运行批次必须从外部手工批次 / 巡检批次补齐
- 当前也没有稳定 `pageId/viewId` 可用，不能强塞页面维度

## 4. 关键主键与审计键

| 键 | 来源 | 何时必填 | 为空时规则 |
|---|---|---|---|
| `technicianId` | 查询参数或佣金记录 | 按技师查询、待结算金额、批量结算必填 | 无值填 `"0"` |
| `storeId` | 配置参数或佣金记录回读 | 配置查询 / 保存必填；其他场景尽量回读 | 无值填 `"0"` |
| `orderId` | 查询参数或佣金记录 | 按订单查佣金必填；直结回读建议补齐 | 无值填 `"0"` |
| `commissionId` | 直结参数或查询回读 | 单条结算必填 | 无值填 `"0"` |
| `settlementId` | 佣金记录回读 | 仅回读到真实结算单绑定时填写 | BO-004 直结未生成新结算单时填 `"0"` |
| `runId` | 外部操作批次 / 巡检批次 | 运行审计建议必填 | controller 未提供时填 `"0"` |
| `sourceBizNo` | 佣金记录回读 | 查询与结算回读建议透传 | 配置操作或无回读记录时填 `"0"` |
| `errorCode` | 接口返回 | 返回非 0 时必填真实值 | 若接口 `code=0` 但读后失败，仍填 `"0"`，不得自造服务端错误码 |

### 4.1 `errorCode=0` 但仍失败的判定
- 当前 BO-004 写接口可能出现：
  - 接口返回 `code=0`
  - 但读后状态未变化
- 这种情况统一记为：
  - `errorCode=0`
  - `readbackResult=unchanged/not-found/duplicate`
  - `pseudoSuccessFlag=true`
- 不允许为了方便统计而补造业务错误码

## 5. 合法空态判定

| 场景 | 合法空态 | 说明 | 非法判定 |
|---|---|---|---|
| 按技师查佣金 | `code=0 && data=[]` | 当前技师没有佣金记录时合法 | 请求失败、权限失败、结构异常 |
| 按订单查佣金 | `code=0 && data=[]` | 当前订单没有佣金记录时合法 | 请求失败、权限失败、结构异常 |
| 待结算金额 | `code=0 && data=0` | 无待结算记录或待结算记录合计为 0 都合法 | 请求失败、结构异常、与明细明显冲突 |
| 配置列表 | `code=0 && data=[]` | 当前门店没有覆盖配置时合法，系统会回退枚举默认佣金比例 | 请求失败、权限失败、结构异常 |

### 5.1 特别说明
- `pending-amount=0` 不等于“已打款完成”：
  - 它只表示当前 `PENDING` 记录合计为 0
  - 可能是没有待结算记录
  - 也可能是待结算记录的 `commissionAmount` 合计为 0
- 空配置不等于系统异常：
  - `TechnicianCommissionServiceImpl.resolveCommissionRate()` 会在无门店配置时回退 `CommissionTypeEnum` 默认比例

## 6. 写操作成功判定

| 操作 | 最低成功证据 | 不得接受的“假成功” |
|---|---|---|
| `settle` | 写后回读目标 `commissionId` 状态变为 `SETTLED`，且 `settlementTime` 有值 | 仅接口返回 `true`，但记录仍是 `PENDING` 或找不到目标记录 |
| `batch-settle` | 写后回读 `postPendingCount/postPendingAmount` 按预期下降 | 仅接口返回 `true`，但待结算数和金额都不变 |
| `config/save` | 写后 `config/list(storeId)` 中目标 `storeId+commissionType` 恰好 1 条，字段与预期一致 | 接口返回 `true`，但没落库、字段没变、或出现重复配置 |
| `config/delete` | 写后 `config/list(storeId)` 不再包含目标 `id` | 接口返回 `true`，但目标记录仍存在 |

## 7. 告警分级

### 7.1 `settle`

| 等级 | 触发条件 | 动作 |
|---|---|---|
| `P0` | `code=0` 且写后目标 `commissionId` 状态未变 / 仍不可定位 | 立即冻结单条结算操作，转人工复核全部相同批次 |
| `P1` | 集中出现权限失败或状态解释冲突 | 暂停结算入口，复核权限与操作批次 |
| `P2` | 单笔误操作但已被写前查询拦截 | 记录并回访操作员 |

### 7.2 `batch-settle`

| 等级 | 触发条件 | 动作 |
|---|---|---|
| `P0` | `code=0` 且 `postPendingCount/postPendingAmount` 与写前完全一致 | 立即暂停批量结算，冻结该技师及同批次操作 |
| `P1` | 写后仍有残余 `PENDING` 且与预期不一致 | 改为单条复核，不再继续批量 |
| `P2` | 写前本就无待结算记录 | 仅记录空操作，不升级 |

### 7.3 `config/save`

| 等级 | 触发条件 | 动作 |
|---|---|---|
| `P0` | `code=0` 但写后无目标配置或保存到了错误门店 / 错误类型 | 立即冻结配置变更，回滚到写前快照 |
| `P1` | 同一 `storeId+commissionType` 写后出现多条记录 | 立即停止该门店配置放量，人工清理重复配置 |
| `P2` | 单门店局部字段异常但未实际生效 | 记录并重新保存，持续观察 |

### 7.4 `config/delete`

| 等级 | 触发条件 | 动作 |
|---|---|---|
| `P0` | `code=0` 但目标 `id` 仍存在，或删除影响了错误门店 | 立即停止删除动作，按写前快照恢复 |
| `P1` | 删除后同类重复配置仍残留，导致默认回退口径不明确 | 人工清理残留配置并复核门店影响面 |
| `P2` | 删除空记录 / 不存在记录，但未产生实际影响 | 记录空操作，不得报成功 |

## 8. 回滚 / 降级 / 暂停操作策略

### 8.1 回滚动作

| 触发条件 | 回滚动作 |
|---|---|
| 单条结算伪成功 | 立即停用 `/booking/commission/settle` 的人工操作，改为查询 + 人工登记 |
| 批量结算伪成功 | 立即停用 `/booking/commission/batch-settle`，同批次切回单条复核 |
| 配置保存错误 / 重复配置 | 按写前快照恢复门店配置，只保留查询 |
| 配置删除错误 | 用写前快照恢复被删配置，只保留查询 |

### 8.2 降级动作
- 当前没有服务端 degraded 逻辑，允许的“降级”只有运维动作：
  - `query-only`
    - 仅保留 `list-by-technician` / `list-by-order` / `pending-amount` / `config/list`
    - 停掉所有写操作
  - `single-review-only`
    - 停掉 `batch-settle`
    - 只允许单条人工复核后操作
  - `default-rate-only`
    - 停掉 `config/save` / `config/delete`
    - 佣金比例解释只按默认枚举比例

### 8.3 暂停操作策略
- 出现以下任一情况，直接暂停 BO-004 写操作：
  - 任一写接口出现伪成功
  - `BO-003` 页面样本被错误计入 `BO-004`
  - 配置重复导致同 `storeId+commissionType` 不唯一
  - 权限点 `booking:commission:query|settle|config` 出现集中异常

## 9. 页面未闭环时的“仅接口闭环”治理
- 只承认以下事实：
  - controller 存在
  - 查询 / 写接口存在
  - 可通过接口回读验证状态
- 不承认以下事实：
  - 独立后台页面已上线
  - 独立前端 API 文件已上线
  - 页面可观测性已完整
- 因此运行治理必须补三层证据：
  - 接口调用证据
  - 写后回读证据
  - 审计键证据 `technicianId/storeId/orderId/commissionId/settlementId/runId/sourceBizNo/errorCode`

### 9.1 页面样本隔离
- `/booking/commission-settlement/*` 的页面成功、告警、回滚样本只属于 `BO-003`
- `/booking/commission/*` 的 controller 成功、告警、回滚样本只属于 `BO-004`
- 两者不得混算，不得互相冲抵

## 10. 验收结论规则

| 结论 | 条件 | 说明 |
|---|---|---|
| `NO_PAGE_CLOSED_DEFAULT` | 当前无独立后台页面文件 | BO-004 维持“仅接口闭环 + 页面真值待核” |
| `PASS_WITH_EMPTY` | 查询接口返回合法空态 | 只代表接口结构合法，不代表页面闭环 |
| `PASS_WITH_READBACK` | 写接口返回成功且写后回读真实变更 | 只代表接口闭环有效，不代表页面已上线 |
| `FAIL_PSEUDO_SUCCESS` | 任一写接口返回成功但读后未变 | 直接视为运行阻断 |

## 11. 验收清单
- [ ] 文档明确监控字段最小集。
- [ ] 文档明确 `technicianId/storeId/orderId/commissionId/settlementId/runId/sourceBizNo/errorCode` 的填充规则。
- [ ] 文档明确空列表、空金额、空配置的合法空态。
- [ ] 文档明确 `settle / batch-settle / config save/delete` 的告警等级。
- [ ] 文档明确当前无服务端 `degraded=true / degradeReason` 证据。
- [ ] 文档明确页面未闭环时，只能执行“仅接口闭环”的运行治理。
- [ ] 文档明确写操作失败一律不得伪成功。
