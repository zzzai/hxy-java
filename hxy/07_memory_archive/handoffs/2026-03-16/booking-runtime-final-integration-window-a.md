# Window A Handoff - Booking Runtime Final Integration (2026-03-16)

## Scope
- Window: A（CTO / 集成窗口）
- Branch: `window-a-booking-runtime-final-integration-20260316-v2`
- Batch target:
  - booking 03-16 最终集成 review
  - booking 产品/contract/gate 真值统一回写主索引
  - `BO-004` 03-15 专项 truth review / evidence ledger 正式吸收入主索引

## Delivered
- 新增 A 侧最终集成单一真值：
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md`
- booking 产品真值已统一到 03-16 contract 口径：
  - `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
  - `docs/products/miniapp/2026-03-15-miniapp-booking-runtime-acceptance-and-recovery-prd-v1.md`
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-page-field-dictionary-v1.md`
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-user-structure-and-recovery-prd-v1.md`
- 主索引 / 主台账已回写：
  - `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`
  - `docs/products/miniapp/2026-03-09-miniapp-release-decision-pack-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-doc-completion-final-review-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
  - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
  - `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-final-integration-v1.md`
- 03-15 旧 A review 已转为历史批次证据，03-16 起单一真值切到：
  - `docs/products/miniapp/2026-03-16-miniapp-booking-runtime-final-integration-review-v1.md`

## Absorbed Formal Window Commits
- B:
  - `1da286e09a docs(booking): close runtime acceptance and recovery prd`
  - `fc586ada58 docs(booking): add runtime field and recovery prds`
- C:
  - `39a5e7d4ac docs: close booking runtime contract evidence`
  - `c0fa2827dc docs(booking): tighten runtime contract truth matrix`
- D:
  - `cb7e197ee0 docs(booking): audit runtime release gate semantics`
- E:
  - `17149c1dea docs(admin): backfill contract runbook truth mapping`

## Final Conclusions
- booking 当前唯一允许结论：`Doc Closed / Can Develop / Cannot Release`
- query-only `ACTIVE` 只认：
  - `/pages/booking/technician-list`
  - `/pages/booking/technician-detail`
  - `/pages/booking/order-list`
  - `/pages/booking/order-detail`
- write-chain blocker 只认：
  - `POST /booking/order/create`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
- `BO-004` 当前唯一允许结论仍是：`仅接口闭环 + 页面真值待核`

## Fixed Truth
- 字段：
  - 技师页 backend 稳定字段只到 `id,name,avatar,introduction,tags,rating,serviceCount`
  - `title/specialties/status` 仍是页面 fallback，不得写成 backend 已绑定
  - `order-list` 当前按 `data.list/data.total` 读取，但 backend 返回 `data[]`
  - `payOrderId` 当前没有已提交响应绑定证据
  - `order-confirm` 的 `duration/spuId/skuId` 仍未闭环
  - `addon` 页当前只提交 `parentOrderId,addonType`
- 错误码：
  - create 只认 `1030003001`
  - cancel 只认 `1030004000/1030004005/1030004006`
  - addon 只认 `1030003001/1030004000/1030004001/1030004006`
  - `1030001000/1030001001/1030002001/1030003002` 当前不得写成 booking runtime page 稳定分支
- 降级行为：
  - 当前没有已提交服务端 `degraded=true / degradeReason` 证据
  - `[] / null / 0` 只算合法空态或空结果，不算成功样本
  - `code=0` 但读后未变必须按 pseudo success / no-op risk 处理

## No-Go
- 不能把 gate `PASS`、tests `PASS`、空态样本、query-only `ACTIVE` 写成 release-ready
- 不能把未绑定字段或页面 fallback 写成后端稳定协议
- 不能把 `commission-settlement/*.vue` 或 `commissionSettlement.ts` 反推成 `BO-004` 页面 / API binding 证据
- 不能吸收未正式提交的窗口口头结论
