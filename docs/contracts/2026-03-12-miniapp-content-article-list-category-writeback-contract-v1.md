# MiniApp Content Article List / Category / Writeback Contract v1 (2026-03-12)

## 1. 目标与真值来源
- 目标：把 BF-027 的文章列表 / 分类 / 浏览回写 / 客服已读回写能力从 `content/customer-service` 混合 contract 中拆出，单独固定当前真实 app controller 契约真值。
- 拆分边界：
  - 本文只覆盖以下 5 个真实接口：
    - `GET /promotion/article/list`
    - `GET /promotion/article/page`
    - `GET /promotion/article-category/list`
    - `PUT /promotion/article/add-browse-count`
    - `PUT /promotion/kefu-message/update-read-status`
  - `GET /promotion/article/get` 仍归 BF-026，继续由 `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md` 维护。
  - 禁止把后端已存在但当前无真实 FE 绑定的能力写入 `ACTIVE` allowlist。
- 真值输入：
  - 前端 API / 页面证据：
    - `yudao-mall-uniapp/sheep/api/promotion/article.js`
    - `yudao-mall-uniapp/sheep/api/promotion/kefu.js`
    - `yudao-mall-uniapp/pages.json`
    - `yudao-mall-uniapp/pages/public/richtext.vue`
    - `yudao-mall-uniapp/sheep/components/s-richtext-block/s-richtext-block.vue`
    - `yudao-mall-uniapp/pages/chat/components/messageList.vue`
  - 后端 controller：
    - `AppArticleController`
    - `AppArticleCategoryController`
    - `AppKeFuMessageController`
  - 文档基线：
    - `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md`
    - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md`
    - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`

## 2. 状态定义
- `ACTIVE_BE_ONLY`：controller 已真实存在，但当前没有真实 FE API / 页面绑定。
- `PLANNED_RESERVED`：能力包级别仍未形成真实用户页或冻结发布范围。
- 当前结论：
  - BF-027 能力包状态维持 `PLANNED_RESERVED`。
  - 本文 5 个接口因“后端存在、当前无真实 FE 绑定”统一标记为 `ACTIVE_BE_ONLY`。

## 3. 当前 FE 绑定判定

| 证据文件 | 当前真值 |
|---|---|
| `yudao-mall-uniapp/sheep/api/promotion/article.js` | 仅存在 `ArticleApi.getArticle -> GET /promotion/article/get`；没有 `article/list`、`article/page`、`article-category/list`、`add-browse-count` |
| `yudao-mall-uniapp/sheep/api/promotion/kefu.js` | 仅存在 `send`、`list`；没有 `update-read-status` |
| `yudao-mall-uniapp/pages.json` | 当前无文章列表页 / 分类页 / 文章中心页 route |
| `yudao-mall-uniapp/pages/public/richtext.vue`、`s-richtext-block` | 当前只消费 `GET /promotion/article/get`；不做 browse-count 回写 |
| `yudao-mall-uniapp/pages/chat/components/messageList.vue` | 当前只消费 `GET /promotion/kefu-message/list`；不做 read-status 回写 |

## 4. Canonical Contract Matrix

| 场景 | 页面/前端调用方 | controllerPath | method + path | request params/body/query | response 字段 | canonical errorCode | 状态 | failureMode | retryClass | 自动重试 | degrade 语义 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 获取文章列表 | 当前无真实 FE 绑定 | `AppArticleController#getArticleList` | `GET /promotion/article/list` | query:`recommendHot?`,`recommendBanner?` | `list[]:{id,title,author,categoryId,picUrl,introduction,content,createTime,browseCount,spuId}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 否 | 空列表是合法空态；`null` 不是声明过的成功体；无 `degraded` 字段 |
| 获取文章分页 | 当前无真实 FE 绑定 | `AppArticleController#getArticlePage` | `GET /promotion/article/page` | query:`categoryId?`,`pageNo(Integer>=1)`,`pageSize(Integer 1~200)` | `PageResult<{total,list[]:{id,title,author,categoryId,picUrl,introduction,content,createTime,browseCount,spuId}}>` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 否 | 空页是合法空态；`null` 不是声明过的成功体；无 `degraded` 字段 |
| 获取文章分类列表 | 当前无真实 FE 绑定 | `AppArticleCategoryController#getArticleCategoryList` | `GET /promotion/article-category/list` | 无 | `list[]:{id,name,picUrl}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 否 | 仅返回启用分类；空列表是合法空态；`null` 不是声明过的成功体；无 `degraded` 字段 |
| 增加文章浏览量 | 当前无真实 FE 绑定 | `AppArticleController#addBrowseCount` | `PUT /promotion/article/add-browse-count` | query:`id(Long)` | `true` | `ARTICLE_NOT_EXISTS(1013016000)` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `NO_AUTO_RETRY` | 否 | `true` 是唯一成功体；`null` 不是合法成功态；无 `degraded` 字段；统计失败不应阻断正文或文章列表主流程 |
| 标记客服会话已读 | 当前无真实 FE 绑定 | `AppKeFuMessageController#updateKefuMessageReadStatus` | `PUT /promotion/kefu-message/update-read-status` | query:`conversationId(Long)` | `true` | `KEFU_CONVERSATION_NOT_EXISTS(1013019000)` | `ACTIVE_BE_ONLY` | `FAIL_CLOSE` | `REFRESH_ONCE` | 否 | `true` 是唯一成功体；`null` 不是合法成功态；无 `degraded` 字段；会话不存在或归属不符时不允许 fail-open |

## 5. 空态 / Null / 降级边界
- `GET /promotion/article/list`
  - 合法空态：`[]`
  - 非法成功态：`null`
- `GET /promotion/article/page`
  - 合法空态：`PageResult.total=0` 且 `list=[]`
  - 非法成功态：`null`
- `GET /promotion/article-category/list`
  - 合法空态：`[]`
  - 非法成功态：`null`
- `PUT /promotion/article/add-browse-count`
  - 合法成功态：`true`
  - 不存在“空成功”或 `null` 成功体
  - 可 fail-open：是。原因是统计写回不应阻断文章正文或列表主流程。
  - 允许自动重试：否。`retryClass=NO_AUTO_RETRY`。
- `PUT /promotion/kefu-message/update-read-status`
  - 合法成功态：`true`
  - 不存在“空成功”或 `null` 成功体
  - 可 fail-open：否。原因是会话不存在 / 越权属于明确业务错误。
  - 允许自动重试：否。`retryClass=REFRESH_ONCE` 只代表用户刷新后重新进入，不代表自动重试。

## 6. BF-026 / BF-027 拆分衔接
- BF-026 继续负责：
  - `POST /promotion/kefu-message/send`
  - `GET /promotion/kefu-message/list`
  - `GET /promotion/article/get`
  - FAQ 壳页 / WebView / DIY 启动链路
- BF-027 本文只负责：
  - 文章列表
  - 文章分页
  - 文章分类列表
  - 文章浏览量回写
  - 客服会话已读回写
- 联调时不得再把 `GET /promotion/article/get` 混入 BF-027，也不得把本文 5 个接口回写成 BF-026 的 `ACTIVE` 页面链路。

## 7. 发布口径
- BF-027 当前没有真实用户页，也没有真实 FE API 绑定：
  - 不得把本文 5 个接口写成 `ACTIVE`
  - 不得加入当前发布 allowlist
- 本文 5 个接口虽然是 `ACTIVE_BE_ONLY`，但 BF-027 能力包整体仍是 `PLANNED_RESERVED`。
- 所有接口都没有服务端 `degraded` 字段；A/D 窗口不得伪造 `degraded/degradeReason` 合约。

## 8. 错误码锚点
- `KEFU_CONVERSATION_NOT_EXISTS(1013019000)`
  - 已在 canonical register 中存在
  - 用于 `PUT /promotion/kefu-message/update-read-status`
- `ARTICLE_NOT_EXISTS(1013016000)`
  - 由 `AppArticleController#addBrowseCount -> ArticleServiceImpl#addArticleBrowseCount -> validateArticleExists` 触发
  - 本文已把它固定为 BF-027 浏览回写的唯一显式业务错误码锚点
