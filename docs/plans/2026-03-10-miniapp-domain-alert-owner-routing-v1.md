# MiniApp Domain Alert Owner Routing v1 (2026-03-10)

## 1. 目标
- 把剩余 runtime blocker 的告警 owner、升级链、SLA、人工接管入口统一成单一真值。
- 当前重点收口：`booking / member / reserved-expansion / finance-ops-admin`。

## 2. 统一规则
- 五键固定为：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
- 不适用字段统一填字符串 `"0"`；禁止留空、`null`、`-`、`UNKNOWN`。
- `warning`、`degraded=true`、显式 `FAIL_OPEN`、合法空态 `[] / 0 / null` 只进入 side pool，不触发主成功率、主转化率、主放量“恢复正常”结论。
- `RESERVED_DISABLED` 在关闭态或越权范围返回，统一认定为“误发布”。
- `BO-004` 的 `code=0` 但 no-op、`Booking` 旧 path 命中、`Member` 分母污染都按发布级事故处理，不降级成普通 warning。

## 3. 默认响应 SLA

| 等级 | 首响 SLA | 缓解 SLA | 关闭 SLA | 典型适用 |
|---|---|---|---|---|
| `P0` | 5 分钟 | 15 分钟 | 4 小时 | 误发布、主口径污染、伪成功、旧 path 放量 |
| `P1` | 15 分钟 | 1 小时 | 24 小时 | gate 误用、灰度越阈值、回读不一致、数据对账异常 |
| `P2` | 30 分钟 | 4 小时 | 72 小时 | 合法空态争议、样本不足、局部波动 |

## 4. 路由矩阵

| 业务域 | 等级 | 典型触发 | Owner | 升级链 | SLA | 人工接管入口 | 五键补位规则 |
|---|---|---|---|---|---|---|---|
| `booking` | `P0` | `GET /booking/technician/list-by-store`、`GET /booking/time-slot/list`、`PUT /booking/order/cancel`、`POST /booking/addon/create` 仍进入发布样本或 allowlist | Booking Domain Owner + 发布负责人 | 发布值班 -> Booking Domain Owner -> C 窗口 -> 发布负责人 | `5m / 15m / 4h` | 立即冻结 Booking 放量，只保留查询侧 `ACTIVE` | `runId=发布/巡检批次`；`orderId/payRefundId/sourceBizNo=0`；`errorCode=0` |
| `booking` | `P1` | create/cancel/addon 样本不完整、只验单点 `create`、按 `message` 分支 | Booking Domain Owner | Booking Domain Owner -> 发布负责人 | `15m / 1h / 24h` | 回退到 canonical 样本集，暂停继续验收 | 同上 |
| `booking` | `P2` | query-only 链路稳定，但 create/cancel/addon 仍待补样本 | Booking on-call | Booking on-call -> Booking Domain Owner | `30m / 4h / 72h` | 仅继续补样本，不扩大放量 | 同上 |
| `member` | `P0` | `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 被算进 `ACTIVE` 主分母，或 `/member/asset-ledger/page` 被记成已上线能力 | Member Domain Owner + 数据治理值班 | 数据治理值班 -> Member Domain Owner -> 发布负责人 | `5m / 15m / 4h` | 回退主分母，重算成功率与通过样本 | `runId=发布/巡检批次`；`orderId/payRefundId/sourceBizNo=0`；`errorCode=0` |
| `member` | `P1` | gate 口径错用、报表把缺页能力写成“已上线页面” | Member Domain Owner | Member Domain Owner -> 发布负责人 | `15m / 1h / 24h` | 锁定报表与文档口径，撤销误标 | 同上 |
| `member` | `P2` | 合法空态、缺页入口争议、样本不足 | Member 运营支撑 | Member 运营支撑 -> Member Domain Owner | `30m / 4h / 72h` | 维持当前 `Can Develop`，不进入发布判断 | 同上 |
| `reserved-expansion` | `P0` | 未完成页面/controller/runtime 样本却越级改 `ACTIVE` 或直接进灰度 | 发布负责人 + 对应域 Owner | 发布值班 -> 对应域 Owner -> SRE -> 发布负责人 | `5m / 15m / 4h` | 立即关闭开关，回退 capability ledger | `runId=发布/巡检批次`；`orderId/payRefundId/sourceBizNo=0`；`errorCode=0` |
| `reserved-expansion` | `P1` | `RESERVED_DISABLED` 误返回、gray runbook 被误当 runtime 成功 | 对应域 on-call + SRE | SRE -> 对应域 on-call -> 发布负责人 | `15m / 1h / 24h` | 回退上一阶段，清空灰度范围 | `runId=灰度/巡检批次`；`orderId/payRefundId/sourceBizNo=有值透传，无值填 0`；`errorCode=保留码真实值` |
| `reserved-expansion` | `P2` | 样本量不足、治理文档与 runtime 证据仍未闭环 | 对应域 Owner | 指标 Owner -> 域负责人 | `30m / 4h / 72h` | 保持 `Can Develop`，不继续放量 | `runId=灰度批次`；无业务主键填 `0` |
| `finance-ops-admin` | `P0` | `BO-004` 任一写接口 `code=0` 但读后未变；`BO-003` 页面样本被拿来冲抵 `/booking/commission/*` | 财务负责人 + Booking Admin on-call + 发布负责人 | 财务运营值班 -> 财务负责人 -> Booking Admin on-call -> 发布负责人 | `5m / 15m / 4h` | 冻结 `settle/batch-settle/config save/delete`，切回 `query-only` 或 `single-review-only` | `technicianId/storeId/orderId/commissionId/settlementId/runId/sourceBizNo/errorCode` 有值透传，无值填 `0`；读后未变时 `errorCode` 仍填真实返回值 |
| `finance-ops-admin` | `P1` | 待结算金额与明细回读不一致；配置保存后重复 `storeId+commissionType`；删除残留 | 财务负责人 / Booking Admin on-call | 财务运营值班 -> 财务负责人 -> Booking Admin on-call | `15m / 1h / 24h` | 暂停批量结算，冻结配置变更，人工按门店回读核对 | 同上；配置类 `sourceBizNo=0` 合法 |
| `finance-ops-admin` | `P2` | `[] / 0` 合法空态争议、局部查询失败、样本不足 | 财务运营支撑 | 财务运营值班 -> 财务负责人 | `30m / 4h / 72h` | 保持查询能力，转人工复核；不得放大成页面成功 | 同上；查询空态 `errorCode=0` 合法 |

## 5. 人工接管入口索引

| 业务域 | 固定入口 |
|---|---|
| `booking` | `docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md` 第 `4/6/7/9` 节 |
| `member` | `docs/plans/2026-03-10-miniapp-active-planned-gate-runbook-v1.md` 第 `3/4/5` 节；`docs/plans/2026-03-11-miniapp-member-missing-page-activation-checklist-v1.md` 第 `4/8` 节 |
| `reserved-expansion` | `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md` 第 `6/7` 节；`docs/plans/2026-03-11-miniapp-reserved-runtime-readiness-register-v1.md` 第 `5/8` 节 |
| `finance-ops-admin` | `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-admin-sop-v1.md` 第 `4/5/7` 节；`docs/plans/2026-03-14-miniapp-finance-ops-technician-commission-admin-runbook-v1.md` 第 `6/7/8/9/10` 节 |

## 6. 验收标准
1. 四个 blocker 域都能映射到唯一主责、升级链和人工接管入口。
2. side pool 样本不得污染主成功率、主转化率、主放量判断。
3. 任一误发布或越级放量都能在 SLA 内回滚并复盘。
