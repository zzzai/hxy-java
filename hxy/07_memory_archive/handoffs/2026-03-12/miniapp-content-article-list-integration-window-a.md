# Window A Handoff - MiniApp BF-027 Content Article List Integration (2026-03-12)

## 1. Objective
- Integrate the formally committed 03-12 BF-027 documents from windows B/C/D into the A-window source-of-truth set, master index, and review docs.

## 2. Delivered Changes
1. Updated `docs/products/miniapp/2026-03-08-miniapp-doc-gap-closure-index-v1.md`
2. Updated `docs/products/miniapp/2026-03-10-miniapp-doc-completion-final-review-v1.md`
3. Updated `docs/products/miniapp/2026-03-10-miniapp-domain-doc-coverage-matrix-v1.md`
4. Added `hxy/07_memory_archive/handoffs/2026-03-12/miniapp-content-article-list-integration-window-a.md`

## 3. Current Judgment
- The 03-12 BF-027 split pack is now formally committed and absorbed into `Ready`.
- `Pending formal window output` for BF-027 is cleared.
- BF-027 still remains `PLANNED_RESERVED / ACTIVE_BE_ONLY` by scope; document completeness does not upgrade runtime status.
- Current source-of-truth snapshot becomes:
  - `Frozen = 39`
  - `Ready = 49`
  - `Draft = 0`
  - `Pending formal window output = 0`

## 4. Practical Effect
1. Content scope
   - BF-026 and BF-027 are now split across independent product and contract documents.
   - Article detail, FAQ shell, chat, and WebView remain in BF-026.
   - Article list/category/writeback now has a dedicated PRD, contract, SOP, and runbook.
2. Governance effect
   - The index no longer treats BF-027 as waiting for formal output.
   - The review docs now explicitly keep BF-027 in `Ready` while preserving its non-active runtime boundary.
3. Release effect
   - These five BF-027 endpoints still stay out of active allowlists and primary success-rate denominators.

## 5. Coordination Notes
- Window B
  - Keep BF-027 wording strictly on real fields only: `id`, `categoryId`, `title`, `picUrl`, `introduction`, `createTime`, `browseCount`, `conversationId`, `pageNo`, `pageSize`, `recommendHot`, `recommendBanner`.
  - Do not create user routes or entry points that do not exist in code.
- Window C
  - Keep the five BF-027 endpoints as the only contract truth: `GET /promotion/article/list`, `GET /promotion/article/page`, `GET /promotion/article-category/list`, `PUT /promotion/article/add-browse-count`, `PUT /promotion/kefu-message/update-read-status`.
  - `ARTICLE_NOT_EXISTS(1013016000)` stays bound to browse-count only; `KEFU_CONVERSATION_NOT_EXISTS(1013019000)` stays bound to read-status only.
- Window D
  - Empty list/page/category remains a legal empty sample, not active success.
  - No server-side `degraded=true` or `degradeReason` exists on this chain; degradation wording must stay at manual/runbook level only.
