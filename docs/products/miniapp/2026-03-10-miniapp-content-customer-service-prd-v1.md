# MiniApp 内容与客服域 PRD v1（2026-03-10）

## 0. 文档定位
- 目标：把小程序 content / customer-service 域收口到“真实路由、真实 app API、真实用户入口”口径，供产品、前端、后端、客服、运营直接执行。
- 分支：`feat/ui-four-account-reconcile-ops`
- 本文当前只覆盖：
  - `BF-025`：DIY 模板 / 自定义页
  - `BF-026`：聊天 / 文章详情 / FAQ 壳页 / WebView
  - `BF-027` 已拆到 `docs/products/miniapp/2026-03-12-miniapp-content-article-list-category-writeback-prd-v1.md`
- 约束：
  - 只使用当前运行中的 uniapp 页面、真实 app controller、真实请求路径。
  - 不把原型 alias route、规划页名、历史 FAQ 数据页写成冻结真值。
  - 降级只允许按 `errorCode/degraded/degradeReason` 处理，禁止 message 猜测和伪成功。
- 对齐基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`

## 1. 业务目标与非目标

### 1.1 业务目标
- 让用户在商品页、售后页、工具菜单中稳定进入客服会话，不丢上下文。
- 让文章 / 富文本 / FAQ 帮助内容有可解释的入口与回退路径。
- 让 DIY 首页模板和自定义页有可审计的模板加载、页面加载、失败兜底规则。
- 让客服、内容运营、前后端在“哪些已上线、哪些只是后端存在、哪些根本没有用户页”上没有歧义。

### 1.2 非目标
- 不新增用户侧文章中心、FAQ 搜索、客服会话列表页。
- 不改写 `sheep.$api.data.faq()` 旧壳逻辑，不把它升级成冻结能力。
- 不把 H5 外链 WebView 当成文章中心或 FAQ 正式承接页。
- 不再承担 BF-027 的文章列表 / 分类 / 浏览回写 / 已读回写真值冻结。

## 2. 域能力总览

| 能力 | 真实页面 / 入口 | 状态 | 真实 API | 结论 |
|---|---|---|---|---|
| 客服聊天 | `/pages/chat/index` | `PLANNED_RESERVED` | `GET /promotion/kefu-message/list` `POST /promotion/kefu-message/send` | 商品详情、售后申请/详情、工具菜单均可进入，但 capability 仍未进入当前正式 `ACTIVE` 发布集 |
| 文章 / 富文本 | `/pages/public/richtext` | `PLANNED_RESERVED` | `GET /promotion/article/get` | 真实承接文章正文，按 `id` 或 `title` 拉取，但能力治理仍归 BF-026 |
| FAQ 壳页 | `/pages/public/faq` | `PLANNED_RESERVED`（仅 route 壳） | 当前真实链路不消费 app FAQ API | `onLoad` 立即跳 `/pages/public/richtext?title=常见问题` |
| WebView 外链 | `/pages/public/webview` | `PLANNED_RESERVED` | 无内容域 app API | 仅承接编码后的 `url` 参数，不作为文章中心 |
| DIY 首页模板 | App 启动阶段 | `ACTIVE` | `GET /promotion/diy-template/used` | 不是单独 route，而是应用初始化能力 |
| DIY 模板预览 | App 启动阶段（预览） | `ACTIVE` | `GET /promotion/diy-template/get` | 指定模板预览时使用 |
| DIY 自定义页 | `/pages/index/page?id=` | `ACTIVE` | `GET /promotion/diy-page/get` | 小程序 scene 预览也走同一接口 |

## 3. 用户场景与页面流转

### 3.1 客服聊天
1. 用户从商品详情底栏点击“客服”，进入 `/pages/chat/index?id={spuId}`。
2. 用户从售后申请页、售后详情页点击联系客服，进入 `/pages/chat/index`。
3. 用户从工具菜单点击客服，进入 `/pages/chat/index`。
4. 聊天页先拉取历史消息，再允许发送文本、图片、商品、订单消息。
5. 若 WebSocket 重连中，页面标题切为“会话重连中”，工具栏点击需被阻断。

### 3.2 文章 / FAQ / 富文本
1. 设置页、授权弹窗、帮助入口通过 `/pages/public/richtext?id={id}` 或 `/pages/public/richtext?title={title}` 进入文章页。
2. `/pages/public/faq` 当前不是独立 FAQ 数据页；页面一加载就跳 `/pages/public/richtext?title=常见问题`。
3. 文章加载失败时必须回退上一页或引导转客服，不能停在“已展示成功”假状态。

### 3.3 DIY 首页与自定义页
1. App 初始化时调用 `GET /promotion/diy-template/used`，取得当前启用模板。
2. 若是模板预览场景，改为 `GET /promotion/diy-template/get?id={templateId}`。
3. 模板返回的 `home` / `user` JSON 结构被规整后注入首页与“我的”页。
4. 用户访问 `/pages/index/page?id={diyPageId}` 时，页面调用 `GET /promotion/diy-page/get` 渲染组件树。
5. 小程序 scene 预览会先解析 `scene`，再回到同一 `id` 加载逻辑。

## 4. 页面 route 真值

| 页面 route | 真实参数 | 页面角色 | 当前真值说明 |
|---|---|---|---|
| `/pages/chat/index` | `id?` | 客服会话页 | `id` 仅在商品详情入口透传；售后页和工具菜单无额外参数 |
| `/pages/public/richtext` | `id?` `title?` | 文章 / 富文本正文页 | 页面先展示 `title`，再用 `id/title` 调 `GET /promotion/article/get` |
| `/pages/public/faq` | 无稳定业务参数 | FAQ 壳页 | 真实运行时立即跳 `/pages/public/richtext?title=常见问题`，不是 FAQ 列表冻结页 |
| `/pages/public/webview` | `url`（需 `decodeURIComponent`） | H5 外链承接 | 不消费文章 API，不得当文章中心使用 |
| App 启动阶段（无单独 route） | `templateId?` | DIY 模板装载 | 真值是 `app.init -> adaptTemplate -> /promotion/diy-template/used|get` |
| `/pages/index/page` | `id` 或 `scene` 解出的 `id` | DIY 自定义页 | 页面最终只认 `id`，无 `id` 不可渲染 |

## 5. 页面 -> API -> 字段关系

### 5.1 客服聊天

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/chat/index` | `POST /promotion/kefu-message/send` | `contentType` `content` | `Long id` | 发送文本 / 图片 / 商品 / 订单消息 |
| `/pages/chat/index` | `GET /promotion/kefu-message/list` | `conversationId?` `createTime?` `limit` | `id` `conversationId` `senderId` `senderAvatar` `senderType` `receiverId` `receiverType` `contentType` `content` `readStatus` `createTime` | 历史消息与增量刷新 |

### 5.2 文章正文 / FAQ 壳页 / WebView 边界

| 页面 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| `/pages/public/richtext` | `GET /promotion/article/get` | `id?` `title?` | `id` `title` `author` `categoryId` `picUrl` `introduction` `content` `createTime` `browseCount` `spuId` | 当前页面只消费 `title` 和 `content` |
| `/pages/public/faq` | 当前无独立 FAQ app API | 无 | 无 | 只做壳页跳转，不承接 FAQ 列表数据 |
| `/pages/public/webview` | 当前无内容域 app API | `url` | 无 | 只承接外链，不承接文章列表与分类 |

### 5.3 DIY 模板与自定义页

| 页面 / 阶段 | API | 请求字段 | 响应字段 | 页面使用 |
|---|---|---|---|---|
| App 初始化 | `GET /promotion/diy-template/used` | 无 | `id` `name` `property` `home` `user` | 加载当前启用模板 |
| App 预览初始化 | `GET /promotion/diy-template/get` | `id` | `id` `name` `property` `home` `user` | 加载指定模板 |
| `/pages/index/page` | `GET /promotion/diy-page/get` | `id` | `id` `name` `property` | 页面读取 `property.components` `property.navigationBar` `property.page` |

## 6. `ACTIVE / PLANNED_RESERVED / 缺页能力` 分层

### 6.1 `ACTIVE`
- DIY 模板装载与 `/pages/index/page?id=` 自定义页渲染。

### 6.2 `PLANNED_RESERVED`
- `/pages/chat/index` 聊天发送、历史消息拉取、工具消息发送。
- `/pages/public/richtext` 文章正文承接。
- `/pages/public/faq` FAQ 壳页跳文章正文。
- `/pages/public/webview` 外链承接。
- FAQ 独立数据页能力；当前不能把旧 `sheep.$api.data.faq()` 当成发布真值。
- BF-027 的文章列表 / 分类 / 浏览回写 / 已读回写，已拆到独立 PRD 管理，当前仍不得误升上线。

### 6.3 缺页能力
- FAQ 搜索页、FAQ 条目详情页。
- 客服会话列表页、未读角标页、历史会话页。
- DIY 页面集合导航页与“最近访问页面”页。

## 7. 错误码与用户恢复动作

| 错误码 | 场景 | 用户侧动作 | 产品约束 |
|---|---|---|---|
| `1013019000 KEFU_CONVERSATION_NOT_EXISTS` | 历史消息拉取 / 发送消息时会话失效 | 提示用户返回上一页后重新进入客服；保留未发送文本供再次发送 | 不允许提示“消息已发送” |
| `1013020000 KEFU_MESSAGE_NOT_EXISTS` | 单条消息刷新失败 | 下拉刷新一次，保留已展示消息列表 | 不清空会话、不弹成功态 |
| `1013016000 ARTICLE_NOT_EXISTS` | 富文本按 `id/title` 取文章失败 | 回退上一页；FAQ 场景回退到帮助入口并提供转客服动作 | 不允许白屏后保持“文章已打开”标题 |
| WebView 加载失败（无业务码） | H5 外链打不开 | 返回上一页，允许再次进入 | 不得冒充为文章内容成功打开 |

## 8. 降级语义与禁止伪成功规则

| 场景 | 降级类型 | 允许行为 | 禁止行为 |
|---|---|---|---|
| 聊天发送失败 | `fail-close` | 保留输入框内容，允许用户重试一次或转人工 | 展示“发送成功”、清空输入框、假装消息已入列 |
| 聊天历史拉取失败 | `fail-open` | 保留已加载消息，展示“下拉重试” | 清空历史消息后显示成功态 |
| 文章正文加载失败 | `fail-close` | 回退上一页或跳客服 | 显示空标题、空正文并让用户误以为内容已发布 |
| FAQ 跳转失败 | `fail-close` | 回帮助入口并给客服入口 | 把 FAQ 壳页当作成功承接 |
| DIY 模板加载失败 | `fail-close` | 进入 `TemplateError` 或稳定模板兜底 | 首页正常展示但内容为空、继续宣传活动可用 |
| DIY 单模块异常 | `fail-open` | 隐藏异常模块，保留主链路 | 继续展示已过期活动或无效 CTA |

### 8.1 明确禁止的伪成功
- `POST /promotion/kefu-message/send` 失败时，不得出现“发送成功”或对话气泡假插入。
- `GET /promotion/article/get` 非 0 返回时，不得保留目标文章标题作为已打开凭证。
- DIY 模板 / 页面数据为空时，不得把空白页包装成“内容更新完成”。

## 9. 是否阻断开发、是否阻断发布

| 判断项 | 结论 | 说明 |
|---|---|---|
| 已有 route 的维护开发是否阻断 | `否` | BF-025 的 DIY 能力为 `ACTIVE`；BF-026 的聊天 / 文章详情 / FAQ 壳页 / WebView 虽可运行，但仍按 `PLANNED_RESERVED` 治理 |
| 把 BF-027 后端已存在但前端未消费能力直接纳入开发是否阻断 | `是` | 文章列表 / 分类 / 浏览回写 / 已读回写已拆到独立 PRD，当前仍缺真实用户页与验收口径 |
| 按当前已确认能力发布是否阻断 | `否` | 只允许把 BF-025 记入当前明确 `ACTIVE` 范围；BF-026 继续按保留能力治理 |
| 把 `PLANNED_RESERVED` 或缺页能力误报为上线能力是否阻断发布 | `是` | 会造成聊天、FAQ、文章中心、客服会话管理的假冻结和假验收 |

## 10. 验收清单
- [ ] `/pages/chat/index` 仅以 `send/list` 两条真实 app API 作为冻结口径。
- [ ] `/pages/public/faq` 在文档中被明确标记为“FAQ 壳页 -> richtext 跳转”，不是 FAQ 正式数据页。
- [ ] BF-027 的文章列表 / 分类 / 浏览回写 / 已读回写已拆到独立真值文档。
- [ ] 错误码与恢复动作覆盖聊天、文章、FAQ、DIY 四类场景。
- [ ] 降级规则明确禁止伪成功，尤其是聊天发送成功和文章打开成功的误提示。
