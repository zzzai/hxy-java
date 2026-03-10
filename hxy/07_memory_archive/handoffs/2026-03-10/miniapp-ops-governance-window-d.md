# MiniApp Ops Governance - Window D Handoff (2026-03-10)

## 1. 变更摘要
- 新增内容与客服 SOP：
  - `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md`
  - 覆盖客服接待、转人工、失败升级、工单字段、回访闭环，以及文章/FAQ/聊天/DIY 页异常话术和恢复动作。
- 新增分销运行手册：
  - `docs/plans/2026-03-10-miniapp-brokerage-domain-runbook-v1.md`
  - 覆盖佣金结算、提现审核/失败、冲正、冻结/解冻、申诉，明确运营/财务/客服/风控责任边界。
- 新增商品目录 KPI 与告警：
  - `docs/plans/2026-03-10-miniapp-product-catalog-kpi-and-alerting-v1.md`
  - 覆盖商品详情、收藏、浏览历史、评论、搜索的空结果率、错误率、降级率、恢复率、转化率。
- 新增营销扩展运营手册：
  - `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-ops-playbook-v1.md`
  - 覆盖秒杀/拼团/砍价/满减送的上下线、灰度、库存阈值、兜底关闭和异常处置。
- 新增预留能力激活与灰度验收：
  - `docs/plans/2026-03-10-miniapp-reserved-expansion-activation-checklist-v1.md`
  - `docs/plans/2026-03-10-miniapp-reserved-expansion-gray-acceptance-runbook-v1.md`
  - 聚焦 gift/referral/feed 从 `PLANNED_RESERVED` 到 `ACTIVE` 的门禁、样本、回滚和不得越级上线。

## 2. 关键收口
- 客服与运营不再使用自由发挥话术，统一按错误码、降级标记和恢复动作执行。
- 分销域从“代码存在但缺运行口径”推进到可执行 runbook。
- 商品目录域明确 `search-lite` 与 `search-canonical` 双轨治理，不再混算。
- `RESERVED_DISABLED` 在 gift/referral/feed 激活中被固定为“命中即回滚”的硬门禁。

## 3. 验证命令
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`

## 4. 对 A / B / C 联调提醒
- A（集成 / 发布）：
  - gift/referral/feed 状态切换必须同时更新 capability ledger、激活清单和灰度验收记录，不能只改状态描述。
  - 分销、内容、目录告警都统一走五键：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
- B（产品 / 运营 / 客服）：
  - 内容异常场景要区分文章/FAQ/聊天/DIY，不可套用交易客服话术。
  - 营销活动关闭或回滚时，客服公告必须同步更新，避免继续承诺活动可参与。
- C（契约 / 后端）：
  - 重点错误码：`1011009901`、`1011009902`、`1013009901`、`1013009902`、`1030009901`、`1008009902`、`1008009904`、`1030007011`。
  - `RESERVED_DISABLED` 开关关闭态返回即误发布；`search-lite` 和 `search-canonical` 不能共用同一 ACTIVE 口径。
