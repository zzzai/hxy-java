# Window B Handoff - MiniApp Content Article List / Writeback Pack（2026-03-14）

## 1. 本批交付
- 分支：`feat/ui-four-account-reconcile-ops`
- 交付类型：仅文档与 handoff；未改业务代码、未改 overlay 页面、未动 `.codex`、未动历史 handoff、未处理无关 untracked。
- 变更文件：
  1. `docs/products/miniapp/2026-03-12-miniapp-content-article-list-category-writeback-prd-v1.md`
  2. `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md`
  3. `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`
  4. `hxy/07_memory_archive/handoffs/2026-03-14/miniapp-content-article-list-writeback-pack-window-b.md`

## 2. 核心收口结论
- BF-026 / BF-027 已明确拆开：
  - BF-026 继续只管聊天 / 文章详情 / FAQ 壳页 / WebView。
  - BF-027 独立只管文章列表、文章分类、浏览回写、客服会话已读回写。
- BF-027 当前能力边界已固定：
  - 文章列表 / 分类列表：当前无真实用户页。
  - 浏览量回写：当前无真实用户动作。
  - 客服会话已读回写：当前无真实用户动作。
- 状态口径已固定：
  - BF-027 整体：`PLANNED_RESERVED`
  - 5 条接口：`ACTIVE_BE_ONLY`
- 联动关系已固定：
  - 列表 / 分类未来只能把 `article.id` 交给 BF-026 `/pages/public/richtext`
  - FAQ 壳页不能充当文章列表或文章分类页
  - 聊天页不能把“已读回写”偷换成“聊天主链已上线”
- fail-open / fail-close 已固定：
  - `GET /promotion/article/list`
  - `GET /promotion/article/page`
  - `GET /promotion/article-category/list`
  - `PUT /promotion/article/add-browse-count`
  - 以上 4 条统一 `FAIL_OPEN`
  - `PUT /promotion/kefu-message/update-read-status` 固定 `FAIL_CLOSE`

## 3. 给窗口 A / C / D 的联调注意点

### 3.1 给窗口 A（前端）
- 不要自造 BF-027 页面真值：
  - 当前 `pages.json` 没有文章列表页、分类页、会话列表页
  - 不能把 `/pages/public/faq`、`/pages/public/webview`、隐藏页、原型 alias 当现网入口
- 关键字段只认真实主键：
  - 文章：`id`、`categoryId`、`title`、`picUrl`、`introduction`、`browseCount`、`createTime`
  - 分类：`id`、`name`、`picUrl`
  - 会话：`conversationId`
- 回写行为不能乐观成功：
  - 浏览量回写失败时正文继续可读，但不能本地先改 `browseCount`
  - 已读回写失败时不能先清本地未读、不能弹“已读成功”

### 3.2 给窗口 C（契约 / 后端）
- method/path 只能继续使用这 5 条真实接口：
  - `GET /promotion/article/list`
  - `GET /promotion/article/page`
  - `GET /promotion/article-category/list`
  - `PUT /promotion/article/add-browse-count`
  - `PUT /promotion/kefu-message/update-read-status`
- 错误码只按真实 code 继续写：
  - `1013016000 ARTICLE_NOT_EXISTS`
  - `1013019000 KEFU_CONVERSATION_NOT_EXISTS`
  - 不要给文章列表 / 分类列表补写不存在的 FAQ/分类错误码
- 继续保持状态不变：
  - BF-027 capability：`PLANNED_RESERVED`
  - 5 条接口：`ACTIVE_BE_ONLY`
  - 不能因为 controller 已存在就升成现网已上线

### 3.3 给窗口 D（数据 / 验收）
- 当前验收重点不是“页面功能可用”，而是“边界没被写错”：
  - 无真实用户页不得验成上线
  - 无真实用户动作不得验成浏览/已读已闭环
- 后续若进入实现联调，验收必须单独盯这三点：
  - 列表 / 分类查询空态是合法空态，不是假失败
  - 浏览量回写失败是 `FAIL_OPEN`
  - 已读回写失败是 `FAIL_CLOSE`

## 4. 风险与建议
- 风险 1：旧 `content/customer-service` PRD 之前混带 BF-027；A/C 若继续引用旧口径，会再次把列表/回写误报成现网能力。
- 风险 2：`/pages/chat/index` 当前只绑定 `send/list`；不要把 WebSocket 的已读提示误当成 `update-read-status` 已落地。
- 风险 3：`/pages/public/richtext` 当前只有 `GET /promotion/article/get`，不要把正文可读误判为浏览量已回写。
- 建议：
  1. A 若后续补 BF-027 页面，先补真实 route 和入口，再补 UI。
  2. C 若后续补 contract 扩展，先保持 `ACTIVE_BE_ONLY` 状态不漂移。
  3. D 把“禁止伪成功”作为 BF-027 独立验收项，而不是沿用 BF-026 旧结论。
