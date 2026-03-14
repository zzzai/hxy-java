# MiniApp 财务运营技师提成明细 / 计提管理 Runbook v1（2026-03-14）

## 1. 目标与范围
- 目标：为 `BO-004 技师提成明细 / 计提管理` 建立最终运行门禁，固定“什么算合法空态、什么算真成功、什么必须直接判失败”。
- 当前固定状态：
  - `Doc Closed`：是。`BO-004` PRD / contract / SOP / runbook 已齐。
  - `Can Develop`：是。可以继续开发独立后台页面、独立前端 API 文件、稳定错误码。
  - `Cannot Release`：是。当前仍只有 controller-only 真值，且写接口存在 no-op / 伪成功风险。

## 2. 运行边界
- 当前无服务端 `degraded=true / degradeReason` 证据：
  - 所有降级只允许写成运维动作：`query-only`、`single-review-only`、`default-rate-only`
  - 不得杜撰服务端降级逻辑
- 页面未闭环时，按“仅接口闭环”治理：
  - 不做页面成功率统计
  - 不拿 `BO-003` `/booking/commission-settlement/*` 页面样本冲抵 `BO-004` `/booking/commission/*`
  - 主成功证据只认：`controller 返回 + 写后回读 + 审计键`
- `warning / legal-empty / pseudo-success` 一律不得污染主成功率、主转化率、主放量判断。

## 3. 主证据与监控字段

### 3.1 主证据公式
- `controller 返回`
- `写后回读`
- `审计键`

三者缺任一项，都不能算主成功样本。

### 3.2 监控字段最小集

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

## 4. 审计键

| 键 | 来源 | 何时必填 | 为空时规则 |
|---|---|---|---|
| `technicianId` | 查询参数或佣金记录 | 按技师查询、待结算金额、批量结算必填 | 无值填 `"0"` |
| `storeId` | 配置参数或佣金记录回读 | 配置查询 / 保存必填；其他场景尽量回读 | 无值填 `"0"` |
| `orderId` | 查询参数或佣金记录 | 按订单查佣金必填；直结回读建议补齐 | 无值填 `"0"` |
| `commissionId` | 直结参数或查询回读 | 单条结算必填 | 无值填 `"0"` |
| `settlementId` | 佣金记录回读 | 仅回读到真实结算单绑定时填写 | 直结未生成新结算单时填 `"0"` |
| `runId` | 外部操作批次 / 巡检批次 | 运行审计建议必填 | controller 未提供时填 `"0"` |
| `sourceBizNo` | 佣金记录回读 | 查询与结算回读建议透传 | 配置操作或无回读记录时填 `"0"` |
| `errorCode` | 接口返回 | 返回非 0 时必填真实值 | 若接口 `code=0` 但读后失败，仍填 `"0"`，不得自造服务端错误码 |

## 5. 合法空态与非成功态

| 场景 | 合法空态 | 当前含义 | 禁止外推 |
|---|---|---|---|
| `list-by-technician` | `code=0 && data=[]` | 当前技师没有佣金记录 | 不能写成页面成功、结算成功 |
| `list-by-order` | `code=0 && data=[]` | 当前订单没有佣金记录 | 不能写成页面成功、结算成功 |
| `config/list` | `code=0 && data=[]` | 当前门店没有覆盖配置，系统回退默认比例 | 不能写成配置保存成功 |
| `pending-amount` | `code=0 && data=0` | 当前没有待结算金额 | 不能写成已打款、已结清、页面成功 |

### 5.1 特别说明
- `pending-amount => 0` 只表示当前 `PENDING` 合计为 0。
- `[] / 0` 只算合法空态，不算主成功样本。
- 所有合法空态、warning、side pool 样本都不得进入主成功率和主放量判断。

## 6. 写操作成功判定

| 操作 | 最低成功证据 | 直接判失败 |
|---|---|---|
| `settle` | 写后回读目标 `commissionId` 状态变为 `SETTLED`，且 `settlementTime` 有值 | `code=0 && true` 但状态未变、目标不存在或仍不可定位 |
| `batch-settle` | 写后回读 `postPendingCount/postPendingAmount` 按预期下降 | `code=0 && true` 但待结算数和金额都不变 |
| `config/save` | 写后 `config/list(storeId)` 中目标 `storeId+commissionType` 恰好 1 条，字段与预期一致 | `code=0 && true` 但没落库、字段没变、或出现重复配置 |
| `config/delete` | 写后 `config/list(storeId)` 不再包含目标 `id` | `code=0 && true` 但目标记录仍存在 |

### 6.1 `code=0` 但仍失败的固定记录方式
- `errorCode=0`
- `readbackResult=unchanged/not-found/duplicate`
- `pseudoSuccessFlag=true`

不允许为了方便统计而补造业务错误码。

## 7. 告警分级

| 等级 | 触发条件 | 动作 |
|---|---|---|
| `P0` | 任一写接口 `code=0` 但写后未变；`BO-003` 页面样本被拿来冲抵 `BO-004` | 立即冻结写操作，切 `query-only` 或 `single-review-only` |
| `P1` | 待结算金额与明细回读不一致；配置出现重复 `storeId+commissionType`；删除残留 | 暂停批量结算和配置变更，人工按门店回读核对 |
| `P2` | `[] / 0` 合法空态争议、局部查询失败、样本不足 | 保持查询能力，转人工复核，不升级为页面成功 |

## 8. 回滚 / 降级 / 暂停操作策略

### 8.1 回滚动作

| 触发条件 | 回滚动作 |
|---|---|
| 单条结算伪成功 | 立即停用 `/booking/commission/settle`，改为查询 + 人工登记 |
| 批量结算伪成功 | 立即停用 `/booking/commission/batch-settle`，同批次切回单条复核 |
| 配置保存错误 / 重复配置 | 按写前快照恢复门店配置，只保留查询 |
| 配置删除错误 | 用写前快照恢复被删配置，只保留查询 |

### 8.2 降级动作
- 当前允许的“降级”只有运维动作：
  - `query-only`
  - `single-review-only`
  - `default-rate-only`
- 当前没有服务端 degraded 逻辑，不得自造 `degraded=true / degradeReason` 返回。

### 8.3 暂停操作策略
- 出现以下任一情况，直接暂停 `BO-004` 写操作：
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

### 9.1 页面样本隔离
- `/booking/commission-settlement/*` 的页面成功、告警、回滚样本只属于 `BO-003`
- `/booking/commission/*` 的 controller 成功、告警、回滚样本只属于 `BO-004`
- 两者不得混算，不得互相冲抵

## 10. 验收结论规则

| 结论 | 条件 | 说明 |
|---|---|---|
| `NO_PAGE_CLOSED_DEFAULT` | 当前无独立后台页面文件 | `BO-004` 维持“仅接口闭环 + 页面真值待核” |
| `PASS_WITH_EMPTY` | 查询接口返回合法空态 | 只代表接口结构合法，不代表页面闭环或放量成功 |
| `PASS_WITH_READBACK` | 写接口返回成功且写后回读真实变更 | 只代表接口闭环有效，不代表页面已上线 |
| `FAIL_PSEUDO_SUCCESS` | 任一写接口返回成功但读后未变 | 直接视为运行阻断 |

## 11. 验收清单
- [ ] 文档明确主证据只认 `controller 返回 + 写后回读 + 审计键`
- [ ] 文档明确 `technicianId/storeId/orderId/commissionId/settlementId/runId/sourceBizNo/errorCode` 的填充规则
- [ ] 文档明确 `list-by-technician/list-by-order/config-list => []`、`pending-amount => 0` 是合法空态，不是页面成功
- [ ] 文档明确 `settle / batch-settle / config save/delete` 不能只看 `true`
- [ ] 文档明确当前无服务端 `degraded=true / degradeReason` 证据
- [ ] 文档明确页面未闭环时，只能执行“仅接口闭环”的运行治理
- [ ] 文档明确 warning / legal-empty / pseudo-success 不得污染主成功率和主放量判断
