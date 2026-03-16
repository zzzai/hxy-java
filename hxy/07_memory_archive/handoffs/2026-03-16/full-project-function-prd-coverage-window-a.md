# Window A Handoff - Full Project Function / PRD Coverage (2026-03-16)

## Scope
- 目标：把全项目前后端业务功能清单、后台 `mall` 能力、`SPU / SKU` 专项与对应 PRD 完整度做一次主窗口终审收口。
- 基线：`main@0950cba6de`
- 工作树：`window-a-full-project-function-prd-coverage-20260316`

## Delivered
- 更新逐功能根台账：
  - `docs/products/2026-03-15-hxy-full-project-business-function-ledger-v1.md`
- 新增终审综述：
  - `docs/products/2026-03-16-hxy-full-project-function-prd-completion-review-v1.md`
- 新增可发布清单：
  - `docs/products/2026-03-16-hxy-full-project-function-doc-completion-publishable-list-v1.md`

## Final Conclusions
- 全项目业务能力共 `51` 项：
  - 小程序 `31`
  - 管理后台 `20`
- 后台真实 `mall` 页面文件共 `22` 个 `index.vue`
- 后台真实独立 API 文件共 `18` 个
- 当前 `PRD 缺口 = 0`
- `SPU / SKU` 已明确拆成：
  - `ADM-001`
  - `ADM-002`
  - `ADM-007`
  - `ADM-008`
  - `ADM-009`
  - `ADM-010`
  - 前台承接 `BF-015`

## Fixed Truth
- booking 5 项能力已按 03-16 统一回写为：
  - PRD 完整度：`完整`
  - 工程结论：查询链 query-only `ACTIVE`；写链 `Can Develop / Cannot Release`
- 管理后台不再允许写成“没有列出”
- `SPU / SKU` 不再允许写成模糊的“商品后台”

## Remaining Gaps
- 多数后台能力仍未核到独立 contract/runbook
- `BO-004` 仍是“仅接口闭环 + 页面真值待核”
- booking / member missing pages / reserved 仍是工程真值 blocker，不是 PRD blocker
