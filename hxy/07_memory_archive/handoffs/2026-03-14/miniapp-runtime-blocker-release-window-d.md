# MiniApp Runtime Blocker Release Window D Handoff (2026-03-14)

## 1. 变更摘要
- 新增最终 blocker 总文档：
  - `docs/plans/2026-03-14-miniapp-runtime-blocker-release-gate-v1.md`
  - 固定 `Booking / Member / Reserved / BO-004` 的最终发布门禁、验收口径、回滚动作。
- 更新发布验收矩阵：
  - `docs/plans/2026-03-10-miniapp-domain-release-acceptance-matrix-v1.md`
  - 把剩余 blocker 统一收敛到 `Doc Closed / Can Develop / Cannot Release` 三层状态。
- 更新告警 owner 路由：
  - `docs/plans/2026-03-10-miniapp-domain-alert-owner-routing-v1.md`
  - 增加 `booking`、`member`、`reserved-expansion`、`finance-ops-admin` 的最终事故等级与人工接管入口。
- 更新 member gate runbook：
  - `docs/plans/2026-03-10-miniapp-active-planned-gate-runbook-v1.md`
  - 固定缺页能力为何不能进入 `ACTIVE` 分母。
- 更新 booking blocker checklist：
  - `docs/plans/2026-03-11-miniapp-booking-runtime-closure-checklist-v1.md`
  - 固定 Booking 持续 `No-Go` 直到 FE/BE `method + path` 真值彻底收口。
- 更新 `BO-004` 运行 runbook：
  - `docs/plans/2026-03-14-miniapp-finance-ops-technician-commission-admin-runbook-v1.md`
  - 固定 `code=0` 但 no-op、合法空态、写后回读、运维降级的最终口径。

## 2. 当前真值结论
- `Booking`
  - 当前仍是 `Doc Closed + Can Develop + Cannot Release`。
  - 查询侧 `order-list / order-detail / technician-detail` 继续属于 runtime `ACTIVE`。
  - `create / cancel / addon` 继续 `No-Go`，直到旧 path 全部从 FE、样本、allowlist、巡检日志里清零。
- `Member`
  - 当前仍是 `Doc Closed + Can Develop + Cannot Release`。
  - `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 仍是缺页能力，不得算进 `ACTIVE` 验收分母。
  - `/member/asset-ledger/page` 仍受 gate 保护，不能因为文档齐就进入主放量口径。
- `Reserved`
  - 当前仍是 `Doc Closed + Can Develop + Cannot Release`。
  - activation checklist / gray runbook / alert routing 只代表治理闭环，不代表 runtime 可验收。
- `BO-004`
  - 当前仍是 `Doc Closed + Can Develop + Cannot Release`。
  - 主证据只认 `controller 返回 + 写后回读 + 审计键`。
  - `[] / 0` 只算合法空态，不算页面成功或放量成功。
  - 当前无服务端 `degraded=true / degradeReason` 证据，降级只能是运维动作。

## 3. 固定门禁
- 可继续开发：
  - 文档、联调、样本、审计、页面/API 绑定、真实 controller/页面补齐。
- 禁止放量：
  - Booking 旧 path 仍在。
  - Member 缺页能力进入 `ACTIVE` 分母。
  - Reserved 只有治理文档，没有 runtime 三件套。
  - `BO-004` 写接口只验 `true` 不验回读。
- 禁止误发布：
  - 把 `Doc Closed` 写成“可放量”。
  - 把合法空态 `[] / 0` 写成页面成功。
  - 把 `BO-003` 页面样本拿来冲抵 `BO-004`。
  - 把 activation checklist / gray runbook 完整写成 reserved runtime 已验收。
  - 把 `warning / degraded / FAIL_OPEN / legal-empty` 样本写进主成功率、主转化率、主放量判断。

## 4. 对窗口 A / B / C 的联调提醒
- A（集成 / 发布）
  - 只允许把本批 blocker 写成 `Doc Closed + Can Develop + Cannot Release`，禁止写成新增放量范围。
  - Booking 当前只能保留查询侧 `ACTIVE`，不能把 create/cancel/addon 带入发布签发。
  - Member 缺页能力不得进 `ACTIVE` 分母；Reserved 治理闭环不得替代 runtime 验收。
- B（产品 / 口径）
  - `[] / 0` 只能解释为合法空态，不能解释为页面成功、结算成功、已打款或已上线。
  - `Doc Closed` 的正确含义是文档收口，不是能力放行。
  - `pending-amount=0` 只能说明待结算金额为 0，不等于财务已结清。
- C（契约 / 后端）
  - Booking canonical 必须继续固定为：
    - `GET /booking/technician/list`
    - `GET /booking/slot/list-by-technician`
    - `POST /booking/order/cancel`
    - `POST /app-api/booking/addon/create`
  - `BO-004` 的 `errorCode=0` 且读后未变只能记为 pseudo success，不能补写不存在的业务错误码。
  - 当前无服务端 `degraded=true / degradeReason` 字段，不能补写降级返回。

## 5. 固定验证命令
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
