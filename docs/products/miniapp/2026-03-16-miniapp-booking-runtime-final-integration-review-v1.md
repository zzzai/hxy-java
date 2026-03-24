# MiniApp Booking Runtime Final Integration Review v1（2026-03-16）

## 1. 评审边界与吸收规则
- 目标：把 2026-03-16 这一轮 booking runtime 的产品、contract、gate 与后台专项真值输出做最终集成，形成当前分支可引用的单一真值。
- 本文只基于以下真实证据判断：
  - `yudao-mall-uniapp/sheep/api/trade/booking.js`
  - `yudao-mall-uniapp/pages/booking/*.vue`
  - `yudao-mall-uniapp/pages/booking/logic.js`
  - `yudao-mall-uniapp/tests/booking-api-alignment.test.mjs`
  - `yudao-mall-uniapp/tests/booking-page-smoke.test.mjs`
  - `ruoyi-vue-pro-master/script/dev/check_booking_miniapp_runtime_gate.sh`
  - `ruoyi-vue-pro-master/script/dev/run_ops_stageb_p1_local_ci.sh`
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-release-evidence-review-v1.md`
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`
  - `docs/contracts/2026-03-15-miniapp-booking-runtime-release-evidence-contract-v1.md`
  - `docs/plans/2026-03-15-miniapp-booking-runtime-release-runbook-v1.md`
- 本文只吸收当前分支真实存在、已正式提交的窗口产出：
  - B：`1da286e09a`、`fc586ada58`
  - C：`39a5e7d4ac`、`c0fa2827dc`
  - D：`cb7e197ee0`
  - E：`17149c1dea`
- 交叉域约束：
  - booking 不得因为 query-only `ACTIVE`、gate `PASS`、tests `PASS` 被改写成可放量。
  - `BO-004` 不得因为 controller、service test 或 gate 存在就被改写成后台页面闭环。
  - 当前没有已提交服务端 `degraded=true / degradeReason` 证据。

## 2. 最终结论

| 判断项 | 当前结论 | 说明 |
|---|---|---|
| booking 文档状态 | `Doc Closed` | 03-15/03-16 的产品、contract、gate、集成 review 已正式落盘 |
| booking 工程状态 | `Still Blocked` | query-only `ACTIVE` 与 write-chain blocker 已分开，但字段/绑定/发布证据仍未闭环 |
| 当前是否可开发 | `Yes` | 允许继续做 booking 真值修复、字段对齐、样本补齐、发布证据补齐 |
| 当前是否可放量 | `No` | create / cancel / addon 仍不得写成 `Ready`、`Go`、`Frozen Candidate` 或 release-ready |
| Release Decision | `No-Go` | gate `PASS` 只表示边界守住，不表示写链路已可放量 |
| 最终单一真值引用 | `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md` | 03-16 起 booking 跨窗口最终判断统一以本文为准 |

## 3. 本轮已吸收并收口的真值项

| 主题 | 03-16 固定口径 | 仍未闭环的工程项 |
|---|---|---|
| canonical method/path | `GET /booking/technician/list`、`GET /booking/technician/get`、`GET /booking/slot/list-by-technician`、`GET /booking/order/list`、`GET /booking/order/get`、`POST /booking/order/create`、`POST /booking/order/cancel`、`POST /app-api/booking/addon/create` | 不得再回退到 `list-by-store / time-slot/list / PUT cancel / POST /booking/addon/create` |
| runtime page stable errorCode | create 只认 `1030003001`；cancel 只认 `1030004000/1030004005/1030004006`；addon 只认 `1030003001/1030004000/1030004001/1030004006` | `1030001000/1030001001/1030002001/1030003002` 当前不得写成 booking runtime page 稳定分支 |
| technician 页面字段 | backend 稳定字段只到 `id,name,avatar,introduction,tags,rating,serviceCount` | `title/specialties/status` 仍是页面 fallback，无 backend 绑定 |
| order-confirm 字段 | 页面稳定可见字段已到 `slotDate,startTime,endTime,duration,isOffpeak,offpeakPrice,avatar,name`；时段读取已切到 `GET /booking/slot/get` | `spuId/skuId` 真实商品来源仍未闭环，create 仍不能改写成可放量 |
| order-list / order-detail 字段 | backend 稳定字段已含 `payOrderId,timeSlotId,spuId,skuId`；`GET /booking/order/list` 已真实消费 `pageNo/pageSize/status` 并返回 `PageResult` | `GET /booking/order/get` miss 仍是 `success(null)`；写链发布证据仍缺 |
| addon 提交边界 | 页面只提交 `parentOrderId,addonType`；`remark` 只在本地 | controller 虽支持 `spuId/skuId`，但当前页面不提交；`code=0` 但读后未变必须按 pseudo success / no-op risk 管理 |
| degrade 语义 | 当前只有合法空态 `[] / null / 0` 与 runbook 人工动作 | 没有服务端 `degraded=true / degradeReason` 证据，不得补写成服务端降级返回 |

## 4. booking 当前唯一允许的范围拆分

### 4.1 Query-only `ACTIVE`
- `/pages/booking/technician-list`
- `/pages/booking/technician-detail`
- `/pages/booking/order-list`
- `/pages/booking/order-detail`

说明：
- 这里只承认查询链路。
- 同页存在的“去支付 / 取消 / 加钟”按钮，不构成写链已闭环或已可放量。

### 4.2 Write-chain blocker
- create：`POST /booking/order/create`
- cancel：`POST /booking/order/cancel`
- addon：`POST /app-api/booking/addon/create`

说明：
- 当前结论统一固定为：`Can Develop / Cannot Release`
- 任何将其改写为 `Ready / Go / 可放量` 的说法，都是越界

## 5. 为什么当前仍是 `No-Go`
1. `check_booking_miniapp_runtime_gate.sh` 与 shared local CI 的 `booking_miniapp_runtime_gate_rc=0` 只证明边界被守住，日志仍固定 `can_release=NO`。
2. 当前缺少 create / cancel / addon 的发布级 success + failure 样本包、allowlist 命中记录、巡检日志、回放证据，无法证明写链路已经具备 release proof。
3. query-only 页面虽然已可维护，但 `title/specialties/status` fallback、create 的 `spuId/skuId` 商品来源、addon 的 `upgrade / add-item` 提交来源仍未闭环，不能把“页面可打开”写成“能力已放心开发并可直接放量”。
4. addon 当前只提交 `parentOrderId,addonType`，这使 `upgrade / add-item` 路径仍有 pseudo success / no-op risk；`code=0` 不能直接等于“业务写入已真实生效”。
5. 当前没有已提交服务端 `degraded=true / degradeReason` 证据，因此任何“服务端降级成功兜底”的 release 叙述都不成立。

## 6. 对 E 窗口专项真值的吸收边界
1. 已吸收：
   - `docs/products/miniapp/2026-03-15-miniapp-finance-ops-technician-commission-admin-page-api-binding-truth-review-v1.md`
   - `docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`
2. 吸收后的固定结论不变：
   - `BO-004` 当前仍是 `仅接口闭环 + 页面真值待核`
   - `BO-004` 独立后台页面文件：`未核出`
   - `BO-004` 独立后台 API 文件：`未核出`
   - 运行样本只到 service/test；发布证据 `未核出`
3. 因此 03-16 这轮 A 集成只是在主索引中把 E 的专项证据正式吸收，不会把 `BO-004` 改写成后台页面闭环或 release-ready。

## 7. 03-16 起的单一真值引用
- booking 最终集成：`docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md`
- booking 03-24 写链收口增量：`docs/products/miniapp/2026-03-24-miniapp-booking-write-chain-closure-review-v1.md`
- booking 页面字段：`docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`
- booking 用户结构态与恢复动作：`docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md`
- booking canonical method/path 与 errorCode：`docs/contracts/2026-03-15-miniapp-booking-runtime-canonical-api-and-errorcode-matrix-v1.md`
- booking release gate：`docs/plans/2026-03-16-miniapp-booking-runtime-release-gate-audit-v1.md`
- booking gate 验收 SOP：`docs/products/miniapp/2026-03-16-miniapp-booking-runtime-gate-acceptance-sop-v1.md`
- BO-004 page/API binding truth：`docs/products/miniapp/2026-03-15-miniapp-finance-ops-technician-commission-admin-page-api-binding-truth-review-v1.md`
- BO-004 evidence ledger：`docs/plans/2026-03-15-miniapp-finance-ops-technician-commission-admin-evidence-ledger-v1.md`
