# MiniApp 内容文章列表 / 分类 / 浏览回写 / 已读回写 PRD v1（2026-03-12）

## 0. 文档定位
- 目标：把 BF-027 从现有 `content/customer-service` PRD 中拆成独立真值文档，只描述文章列表、文章分类、浏览量回写、客服会话已读回写。
- 分支：`feat/ui-four-account-reconcile-ops`
- 范围约束：
  - 只使用当前代码库真实存在的 uniapp route、前端 API 文件、app controller、真实 method/path。
  - 不把原型 alias、规划 route 名、历史 FAQ 数据页写成现网已上线能力。
  - 不复写 BF-026 的聊天 / 文章详情 / FAQ 壳页 / WebView 真值；这些能力继续以 `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` 为准。
- 真值输入：
  - `yudao-mall-uniapp/pages.json`
  - `yudao-mall-uniapp/sheep/api/promotion/article.js`
  - `yudao-mall-uniapp/sheep/api/promotion/kefu.js`
  - `yudao-mall-uniapp/pages/public/richtext.vue`
  - `yudao-mall-uniapp/pages/public/faq.vue`
  - `yudao-mall-uniapp/pages/chat/index.vue`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-promotion/src/main/java/**/AppArticleController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-promotion/src/main/java/**/AppArticleCategoryController.java`
  - `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-promotion/src/main/java/**/AppKeFuMessageController.java`
  - `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
  - `docs/products/miniapp/2026-03-12-miniapp-business-function-truth-ledger-v1.md`

## 1. BF-026 / BF-027 拆分边界

| 业务功能 | 当前真值归属 | 真实 route / 页面 | 真实 API | 本文是否负责 |
|---|---|---|---|---|
| BF-026 聊天 | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | `/pages/chat/index` | `GET /promotion/kefu-message/list`; `POST /promotion/kefu-message/send` | 否 |
| BF-026 文章详情 | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | `/pages/public/richtext` | `GET /promotion/article/get` | 否 |
| BF-026 FAQ 壳页 | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | `/pages/public/faq` | 当前无独立 FAQ app API | 否 |
| BF-026 WebView | `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md` | `/pages/public/webview` | 无内容域 app API | 否 |
| BF-027 文章列表 | 本文 | `N/A（当前无真实用户页）` | `GET /promotion/article/list`; `GET /promotion/article/page` | 是 |
| BF-027 文章分类 | 本文 | `N/A（当前无真实用户页）` | `GET /promotion/article-category/list` | 是 |
| BF-027 浏览量回写 | 本文 | `N/A（当前无真实用户动作）` | `PUT /promotion/article/add-browse-count` | 是 |
| BF-027 客服会话已读回写 | 本文 | `N/A（当前无真实用户动作）` | `PUT /promotion/kefu-message/update-read-status` | 是 |

## 2. 当前能力边界与缺页说明

| 子能力 | 页面真值 | 当前入口真值 | 能力状态 | API 状态 | 当前结论 |
|---|---|---|---|---|---|
| 文章列表 | `pages.json` 未登记真实 route | 无设置页、首页、帮助中心、工具菜单的真实用户入口 | `PLANNED_RESERVED` | `ACTIVE_BE_ONLY` | 后端可查列表，但前端没有真实列表页 |
| 文章分类列表 | `pages.json` 未登记真实 route | 无真实分类页、分类 tab、分类入口 | `PLANNED_RESERVED` | `ACTIVE_BE_ONLY` | 后端可查分类，但前端没有真实分类承接页 |
| 浏览量回写 | `/pages/public/richtext` 当前未调用回写接口 | 当前没有“用户成功浏览正文后回写浏览量”的真实动作 | `PLANNED_RESERVED` | `ACTIVE_BE_ONLY` | 详情页可读，但浏览量不回写 |
| 客服会话已读回写 | `/pages/chat/index` 当前只发 `send/list` | 当前没有“用户读完客服消息后标记已读”的真实动作 | `PLANNED_RESERVED` | `ACTIVE_BE_ONLY` | 会话页可读写消息，但不回写已读 |

### 2.1 页面真值
- 当前用户侧真实 content 页面只包括：
  - `/pages/public/richtext`
  - `/pages/public/faq`
  - `/pages/public/webview`
  - `/pages/chat/index`
- 以上页面都不属于 BF-027 的“列表 / 分类 / 回写”独立承接页：
  - `/pages/public/richtext` 是 BF-026 文章详情页，不是文章列表页。
  - `/pages/public/faq` 是 BF-026 FAQ 壳页，不是 FAQ 列表或文章分类页。
  - `/pages/chat/index` 是 BF-026 客服会话页，不是会话列表或未读管理页。

### 2.2 缺页清单
- 缺文章中心页。
- 缺文章分类页或分类 tab 承接页。
- 缺文章列表到文章详情的正式产品入口。
- 缺客服会话列表页、未读角标页、历史会话页。
- 缺任何把浏览回写、已读回写暴露为真实用户动作的前端实现。

## 3. 状态判定：`PLANNED_RESERVED` 与 `ACTIVE_BE_ONLY`

### 3.1 判定口径
- BF-027 整体状态固定为 `PLANNED_RESERVED`：
  - 缺真实 route。
  - 缺真实用户入口。
  - 缺当前可执行的前端验收基线。
- BF-027 下面的 5 条接口状态固定为 `ACTIVE_BE_ONLY`：
  - controller 已存在、method/path 已冻结；
  - 当前前端 API 文件与页面代码未绑定；
  - 不能因为“接口能调通”就把能力误报成现网已上线。

### 3.2 API 真值矩阵

| 场景 | 真实 method + path | request | response | 当前状态 | failureMode | 当前说明 |
|---|---|---|---|---|---|---|
| 获取文章列表 | `GET /promotion/article/list` | `recommendHot?` `recommendBanner?` | `list[]:{id,title,author,categoryId,picUrl,introduction,content,createTime,browseCount,spuId}` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | 当前前端未绑定；空列表是合法空态 |
| 获取文章分页 | `GET /promotion/article/page` | `categoryId?` `pageNo` `pageSize` | `PageResult<{id,title,author,categoryId,picUrl,introduction,content,createTime,browseCount,spuId}>` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | 当前前端未绑定；`categoryId` 是唯一业务筛选键 |
| 获取文章分类列表 | `GET /promotion/article-category/list` | 无 | `list[]:{id,name,picUrl}` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | 当前前端未绑定；只返回启用分类 |
| 增加文章浏览量 | `PUT /promotion/article/add-browse-count` | `id(Long)` | `true` | `ACTIVE_BE_ONLY` | `FAIL_OPEN` | 当前前端未绑定；统计失败不阻断正文阅读 |
| 更新客服会话已读 | `PUT /promotion/kefu-message/update-read-status` | `conversationId(Long)` | `true` | `ACTIVE_BE_ONLY` | `FAIL_CLOSE` | 当前前端未绑定；会话不存在或越权时不能假装已读成功 |

## 4. 规划中的页面 / 入口边界

| 规划项 | 允许的未来边界 | 当前禁止事项 |
|---|---|---|
| 文章列表页 | 必须是新增且真实登记到 `pages.json` 的用户页，真实消费 `GET /promotion/article/list` 或 `GET /promotion/article/page` | 不能把 `/pages/public/faq`、`/pages/public/webview`、隐藏 alias page 当成文章列表页 |
| 文章分类承接 | 可以是文章列表页内的分类 tab，也可以是独立分类页，但真实数据必须来自 `GET /promotion/article-category/list` | 不能用静态 mock 分类、不能把 FAQ 标题分组当成分类真值 |
| 文章详情承接 | 继续复用 BF-026 `/pages/public/richtext` + `GET /promotion/article/get` | 本文不新增 detail route，也不重写详情 API |
| 浏览量回写触发器 | 只能挂在“真实用户已进入文章详情且正文加载成功”的链路上 | 不能在列表曝光、预取、FAQ 壳页自动跳转、空详情、WebView 打开时回写 |
| 会话已读回写触发器 | 只能挂在“真实用户已进入客服会话且存在未读客服消息”的链路上 | 不能在后台轮询、WebSocket push、离页批量清空、无 `conversationId` 场景下回写 |

### 4.1 当前真实入口边界
- 当前设置页、授权弹窗、FAQ 壳页都只进入 BF-026 文章详情，不进入 BF-027 文章列表。
- 当前商品详情、售后申请、售后详情、工具菜单都只进入 BF-026 聊天页，不进入 BF-027 会话管理能力。
- 当前没有任何用户入口可以合法触发 `GET /promotion/article/list`、`GET /promotion/article/page`、`GET /promotion/article-category/list`、`PUT /promotion/article/add-browse-count`、`PUT /promotion/kefu-message/update-read-status`。

## 5. 列表、分类、详情联动关系

### 5.1 联动主链
1. 分类列表负责提供 `categoryId`、`name`、`picUrl`，不负责渲染正文。
2. 文章列表 / 分页负责返回文章卡片集合，核心识别键是 `id`，辅助展示字段是 `title`、`picUrl`、`introduction`、`browseCount`、`createTime`。
3. 用户点击文章卡片后，只允许把文章主键交给 BF-026 文章详情页：
   - 进入 `/pages/public/richtext?id={id}`
   - 再由 BF-026 使用 `GET /promotion/article/get`
4. FAQ 壳页继续只做 `/pages/public/richtext?title=常见问题` 跳转，不参与文章分类与文章列表联动。

### 5.2 联动边界
- BF-027 负责“选择哪篇文章”与“是否需要回写统计/已读”。
- BF-026 负责“打开文章正文”和“打开客服会话”。
- 两者之间只允许通过真实主键联动：
  - 文章链路：`article.id`
  - 会话链路：`conversationId`
- 不允许的漂移：
  - 不能用 `title` 充当文章列表主键。
  - 不能让 FAQ 壳页承接文章分类。
  - 不能让聊天发送成功与“会话已读回写成功”混为一个能力。

## 6. 浏览回写与已读回写触发条件

| 回写能力 | 当前 runtime 真值 | 规划触发条件 | 成功后允许行为 | 失败口径 |
|---|---|---|---|---|
| `PUT /promotion/article/add-browse-count` | 当前无前端调用 | 用户从真实文章列表/分类页进入 BF-026 详情页，且 `GET /promotion/article/get` 成功返回非空文章后触发 | 仅记录统计成功；正文继续可读 | `FAIL_OPEN`：正文继续展示，不回滚正文，不本地伪增 `browseCount` |
| `PUT /promotion/kefu-message/update-read-status` | 当前无前端调用 | 用户进入真实客服会话页，存在未读客服消息，且具备合法 `conversationId` 后触发 | 服务端确认成功后再清理未读态 | `FAIL_CLOSE`：本地不得先清空未读，不得展示“已读同步成功”伪成功文案 |

### 6.1 浏览量回写规则
- 允许触发：
  - 真实文章详情页打开成功；
  - 文章主键 `id` 明确；
  - 本次打开是用户主动进入，而不是预取。
- 禁止触发：
  - 文章列表卡片曝光；
  - 分类 tab 切换；
  - FAQ 壳页自动跳转；
  - `GET /promotion/article/get` 返回 `null`；
  - H5 WebView 外链打开。

### 6.2 已读回写规则
- 允许触发：
  - 当前会话存在 `conversationId`；
  - 当前用户确实属于该会话；
  - 服务端有未读客服消息需要落已读。
- 禁止触发：
  - 仅靠消息轮询或本地滚动就直接改已读；
  - 未收到服务端成功返回就先清本地未读角标；
  - 对不存在或不属于当前用户的会话做批量清零。

## 7. fail-open / fail-close 基本口径

| 接口 | 口径 | 用户侧基本规则 |
|---|---|---|
| `GET /promotion/article/list` | `FAIL_OPEN` | 拉取失败显示空态或回到现有帮助入口；不得伪造列表成功态 |
| `GET /promotion/article/page` | `FAIL_OPEN` | 分页失败允许刷新一次；已有列表内容可保留 |
| `GET /promotion/article-category/list` | `FAIL_OPEN` | 分类失败可隐藏分类区并退回默认列表；不得写死分类假数据 |
| `PUT /promotion/article/add-browse-count` | `FAIL_OPEN` | 统计失败不阻断文章正文；禁止本地先改计数再回滚 |
| `PUT /promotion/kefu-message/update-read-status` | `FAIL_CLOSE` | 失败时未读态保持不变；禁止显示“已读成功”或本地偷改 `readStatus` |

## 8. 错误码、恢复动作与页面边界

| 错误码 / 场景 | 触发接口 | 用户恢复动作 | 产品约束 |
|---|---|---|---|
| `1013016000 ARTICLE_NOT_EXISTS` | `PUT /promotion/article/add-browse-count` 或后续详情联动主键失效 | 回退列表或刷新详情后重试 | 不得在正文未打开时仍回写浏览量成功 |
| `1013019000 KEFU_CONVERSATION_NOT_EXISTS` | `PUT /promotion/kefu-message/update-read-status` | 刷新会话或重新进入客服 | 不得把不存在会话标成已读 |
| 空列表 / 空分页（无业务错误码） | `GET /promotion/article/list`; `GET /promotion/article/page`; `GET /promotion/article-category/list` | 展示合法空态 | 空态不是失败，更不是“功能已上线”证明 |

## 9. 与 FAQ 壳页 / 文章详情 / 聊天页的边界

| 能力 | 当前归属 | BF-027 关系 | 禁止混淆 |
|---|---|---|---|
| FAQ 壳页 | BF-026 | 仅作为帮助内容现有入口，不承接列表/分类 | 不能把 FAQ 壳页当文章中心 |
| 文章详情页 | BF-026 | BF-027 只负责向详情页传递 `article.id`，以及未来浏览量回写触发 | 不能在 BF-027 中重写 detail route / detail API |
| 聊天页 | BF-026 | BF-027 只负责未来已读回写触发，不负责消息发送/拉取 | 不能把已读同步当成聊天主链已上线证明 |
| WebView | BF-026 | 与 BF-027 无直接联动 | 不能把外链落地页当作文章列表或文章分类页 |

## 10. 开发 / 发布门禁

| 判断项 | 结论 | 说明 |
|---|---|---|
| 现阶段是否允许把 BF-027 记为已上线能力 | `否` | 仍无真实 route、无真实入口、无真实用户动作 |
| 现阶段是否允许 C 侧把 5 条接口记为真实 canonical API | `是` | method/path 已是真实后端口径，但只能标 `ACTIVE_BE_ONLY` |
| 现阶段是否允许 A 侧直接做产品化联调 | `否` | 需先补真实页面、入口、验收清单，才能脱离 `PLANNED_RESERVED` |
| 是否允许继续维护 BF-026 现有聊天 / 详情 / FAQ / WebView | `是` | BF-026 与 BF-027 已拆开，现有主链维护不受阻断 |

## 11. 升级为可联调能力的前置条件
- 新增并登记真实文章列表 / 分类承接页。
- 为 BF-027 明确真实用户入口，不复用 FAQ 壳页或 WebView 伪承接。
- 前端 API 文件显式绑定本文 5 条真实接口。
- D 窗口补齐空态、错误态、统计失败、已读失败的验收证据。
- A/C/D 同步确认：
  - 列表/分类查询是 `FAIL_OPEN`
  - 浏览量回写是 `FAIL_OPEN`
  - 已读回写是 `FAIL_CLOSE`

## 12. 验收清单
- [ ] 文档明确写出“文章列表 / 分类列表：当前无真实用户页”。
- [ ] 文档明确写出“浏览量回写：当前无真实用户动作”。
- [ ] 文档明确写出“客服会话已读回写：当前无真实用户动作”。
- [ ] 文档只使用 5 条真实接口，不出现原型 alias。
- [ ] 文档明确 BF-026 / BF-027 拆分边界。
- [ ] 文档明确列表、分类、详情只允许以 `article.id` 和 `conversationId` 联动。
