# MiniApp Content / Customer Service Contract v1 (2026-03-10)

## 1. 目标与真值来源
- 目标：把 miniapp 内容域与客服域的真实 `controllerPath + method + path + request/response` 固定下来，明确当前哪些链路可计入发布、哪些仍只到 Ready/BE-only。
- 约束：
  - 仅使用当前前端 API 文件、当前页面调用、当前 app controller 真值。
  - 不允许 wildcard API、`TBD_*`、基于返回文案的分支判断。
  - 统一只按 `errorCode / failureMode / retryClass` 做恢复，不按返回文案做 UI 决策。
- 真值输入：
  - 前端 API：
    - `yudao-mall-uniapp/sheep/api/promotion/kefu.js`
    - `yudao-mall-uniapp/sheep/api/promotion/article.js`
    - `yudao-mall-uniapp/sheep/api/promotion/diy.js`
  - 前端页面/组件：
    - `/pages/chat/index`
    - `/pages/chat/components/messageList.vue`
    - `/pages/public/richtext.vue`
    - `/pages/index/page.vue`
    - `yudao-mall-uniapp/sheep/store/app.js`
  - 后端 controller：
    - `AppKeFuMessageController`
    - `AppArticleController`
    - `AppArticleCategoryController`
    - `AppDiyTemplateController`
    - `AppDiyPageController`
  - 文档基线：
    - `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-sop-v1.md`
    - `docs/products/miniapp/2026-03-09-miniapp-content-compliance-styleguide-v1.md`
    - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`

## 2. 状态定义
- `ACTIVE`：当前真实页面已绑定，且允许进入当前发布口径。
- `ACTIVE_BE_ONLY`：controller 已存在，但当前前端未绑定。
- `PLANNED_RESERVED`：当前前端有调用，但整域仍未达到正式发布级冻结口径。
- `BLOCKED`：本域当前无旧 path 阻断项，本版留空。

## 3. 页面与前端调用方

| 页面/组件 | 前端调用方 |
|---|---|
| `/pages/chat/index` | `KeFuApi.sendKefuMessage` |
| `/pages/chat/components/messageList.vue` | `KeFuApi.getKefuMessageList` |
| `/pages/public/richtext.vue` | `ArticleApi.getArticle` |
| `sheep/components/s-richtext-block/*` | `ArticleApi.getArticle` |
| `/pages/index/page.vue` | `DiyApi.getDiyPage` |
| `yudao-mall-uniapp/sheep/store/app.js` | `DiyApi.getUsedDiyTemplate`、`DiyApi.getDiyTemplate` |

## 4. Canonical Contract Matrix

| 场景 | 页面/前端调用方 | controllerPath | method + path | request params/body/query | response 字段 | canonical errorCode | 状态 | failureMode | retryClass | degrade 语义 | 发布口径 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 发送客服消息 | `/pages/chat/index` / `KeFuApi.sendKefuMessage` | `AppKeFuMessageController#sendKefuMessage` | `POST /promotion/kefu-message/send` | body:`contentType(Integer)`,`content(String JSON)`；`senderId/senderType` 由后端回填 | `messageId(Long)` | `-` | `PLANNED_RESERVED` | `FAIL_CLOSE` | `NO_AUTO_RETRY` | 无 `degraded` 字段；发送失败不允许前端伪成功落气泡 | 聊天链路在当前分支真实存在，但 capability 仍未进入 `ACTIVE` 冻结集；发布时只能按 Ready/验证态处理 |
| 拉取客服消息列表 | `/pages/chat/components/messageList.vue` / `KeFuApi.getKefuMessageList` | `AppKeFuMessageController#getKefuMessageList` | `GET /promotion/kefu-message/list` | query:`conversationId?`,`createTime?`,`limit`；当前 FE 自带的 `pageNo/no` 不是服务端协议 | `list[]:{id,conversationId,senderId,senderAvatar,senderType,receiverId,receiverType,contentType,content,readStatus,createTime}` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 空列表为合法空态；无服务端 `degraded` 字段，聊天页壳体保留 | 只能以当前精确 query 协议联调，不得再把页码字段写入 contract 真值 |
| 标记客服会话已读 | 当前无 FE 绑定 | `AppKeFuMessageController#updateKefuMessageReadStatus` | `PUT /promotion/kefu-message/update-read-status` | query:`conversationId(Long)` | `true` | `KEFU_CONVERSATION_NOT_EXISTS(1013019000)` | `ACTIVE_BE_ONLY` | `FAIL_CLOSE` | `REFRESH_ONCE` | 无 `degraded` 字段；会话不存在或越权时直接报错 | 后端真实存在，当前前端未接；如果后续补消息已读同步，只允许使用此 path |
| 获取文章详情 | `/pages/public/richtext.vue`、`s-richtext-block` / `ArticleApi.getArticle` | `AppArticleController#getArticle` | `GET /promotion/article/get` | query:`id?`,`title?`；controller 优先按 `id`，否则按 `title` 取最后一篇 | `{id,title,author,categoryId,picUrl,introduction,content,createTime,browseCount,spuId}` 或 `null` | `-` | `PLANNED_RESERVED` | `FAIL_OPEN` | `REFRESH_ONCE` | 文章不存在返回 `null` 为真实协议；前端保持空壳页，不得自造成功态文案 | 文章链路当前运行存在，但 capability 仍未进入正式 `ACTIVE` 发布集 |
| 增加文章浏览量 | 当前无 FE 绑定 | `AppArticleController#addBrowseCount` | `PUT /promotion/article/add-browse-count` | query:`id(Long)` | `true` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `NO_AUTO_RETRY` | 统计失败不应阻断正文加载；无 `degraded` 字段 | 仅作为统计补充接口，不计入内容主链路发布门禁 |
| 获取文章列表 | 当前无 FE 绑定 | `AppArticleController#getArticleList` | `GET /promotion/article/list` | query:`recommendHot?`,`recommendBanner?` | `list[]:{id,title,author,categoryId,picUrl,introduction,content,createTime,browseCount,spuId}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 空列表为合法空态；不允许按返回文案自定义分支 | 后端真实存在，当前 FAQ/文章列表页未绑定 |
| 获取文章分页 | 当前无 FE 绑定 | `AppArticleController#getArticlePage` | `GET /promotion/article/page` | query:`categoryId?`,`pageNo`,`pageSize` | `PageResult<AppArticleRespVO>` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 空页为合法空态 | 若后续补文章中心，只允许接此真实分页接口 |
| 获取文章分类列表 | 当前无 FE 绑定 | `AppArticleCategoryController#getArticleCategoryList` | `GET /promotion/article-category/list` | 无 | `list[]:{id,name,picUrl}` | `-` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | `REFRESH_ONCE` | 仅返回启用分类；空列表为合法空态 | 后端已存在，前端未绑定；分类不能靠静态配置伪造 |
| 获取当前生效装修模板 | `sheep/store/app.js` / `DiyApi.getUsedDiyTemplate` | `AppDiyTemplateController#getUsedDiyTemplate` | `GET /promotion/diy-template/used` | 无 | `{id,name,property,home,user}` 或 `null` | `-` | `ACTIVE` | `FAIL_OPEN` | `REFRESH_ONCE` | controller 不返回 `degraded`；当前客户端存在一次 tenant fallback，但这是客户端兜底，不是后端契约 | 该接口属于基础首页 DIY 启动链路，可继续计入当前发布口径 |
| 按模板 ID 获取装修模板 | `sheep/store/app.js` / `DiyApi.getDiyTemplate` | `AppDiyTemplateController#getDiyTemplate` | `GET /promotion/diy-template/get` | query:`id(Long)` | `{id,name,property,home,user}` 或 `null` | `-` | `ACTIVE` | `FAIL_OPEN` | `REFRESH_ONCE` | `property/home/user` 为 JSON RawValue；模板缺失返回 `null` | 属于已运行的基础 DIY 模板能力；字段只能增量追加 |
| 获取装修页面 | `/pages/index/page.vue` / `DiyApi.getDiyPage` | `AppDiyPageController#getDiyPage` | `GET /promotion/diy-page/get` | query:`id(Long)` | `{id,name,property}` 或 `null` | `-` | `ACTIVE` | `FAIL_OPEN` | `REFRESH_ONCE` | `property` 为 JSON RawValue；页面缺失返回 `null`，前端保留容器页 | 属于基础首页/落地页 DIY 能力，可计入当前发布口径 |

## 5. 域级 canonical 约束
- 客服消息 `content` 固定按 JSON 字符串透传：
  - 文本：`{"text":"..."}`
  - 图片：`{"picUrl":"..."}`
  - 商品/订单卡片：对象 JSON 串
- `GET /promotion/kefu-message/list` 的真实翻页锚点只有 `createTime + limit`；页码字段只是前端本地状态。
- `GET /promotion/article/get` 的 `id` 与 `title` 是互斥查找语义；不得同时依赖两者返回不同内容。
- DIY 链路不提供服务端 `degraded/degradeReason` 字段：
  - 当前唯一存在的 fallback 是客户端 tenant fallback
  - A/D 窗口不得把该行为写成后端 fail-open 契约

## 6. 发布口径
- 允许计入当前发布口径的只包括基础 DIY 链路：
  - `GET /promotion/diy-template/used`
  - `GET /promotion/diy-template/get`
  - `GET /promotion/diy-page/get`
- 客服/文章链路虽然当前前后端都存在，但在 B 侧正式 PRD/A 侧冻结前，状态仍固定为 `PLANNED_RESERVED`。
- 所有 `ACTIVE_BE_ONLY` 接口仅代表后端真值已存在，不代表允许前端默认调用。

## 7. 跨窗口联调约束
- A 窗口
  - 不能把客服/文章页面的“当前可打开”误记成 `ACTIVE` 能力；只有 DIY 基础链路可直接入当前发布统计。
- B 窗口
  - `article/get` 返回 `null`、`kefu-message/list` 返回 `[]` 都是合法协议；UI 必须有稳定空态，不得按非结构化文本猜测。
- D 窗口
  - 客户端 tenant fallback 归为 FE 兜底，不算服务端降级；监控和 runbook 不得把它纳入后端 `degraded_pool`。
