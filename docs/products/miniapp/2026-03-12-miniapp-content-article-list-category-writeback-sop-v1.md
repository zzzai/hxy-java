# MiniApp 内容文章列表 / 分类 / 回写 SOP v1（2026-03-12）

## 1. 目标与适用范围
- 目标：为 `BF-027` 补齐客服接待、用户恢复、升级闭环和验收口径，确保未来补前端承接 `article list/category/writeback` 时有可执行 SOP。
- 适用接口：
  - `GET /promotion/article/list`
  - `GET /promotion/article/page`
  - `GET /promotion/article-category/list`
  - `PUT /promotion/article/add-browse-count`
  - `PUT /promotion/kefu-message/update-read-status`
- 代码真值来源：
  - `ruoyi-vue-pro-master/.../AppArticleController`
  - `ruoyi-vue-pro-master/.../AppArticleCategoryController`
  - `ruoyi-vue-pro-master/.../AppKeFuMessageController`
  - `yudao-mall-uniapp/sheep/api/promotion/article.js`
  - `yudao-mall-uniapp/sheep/api/promotion/kefu.js`
- 当前 capability 真值：
  - `BF-027` 在业务台账中仍是 `PLANNED_RESERVED`
  - 上述 5 个接口已在后端存在，但当前前端未绑定文章列表 / 分类 / 浏览回写 / 已读回写
  - `yudao-mall-uniapp` 当前只真实调用 `GET /promotion/article/get`、`POST /promotion/kefu-message/send`、`GET /promotion/kefu-message/list`
- 本文只负责 `list/category/writeback` 运行面，不负责：
  - `GET /promotion/article/get` 文章详情链路
  - `POST /promotion/kefu-message/send` 聊天发送链路
  - `/pages/public/faq` FAQ 壳页跳转链路

## 2. 当前代码真值与处理原则
- 不得杜撰服务端 `degraded=true` 或 `degradeReason`：
  - 当前这 5 条链路真实返回模型只有 `CommonResult.code/msg/data`
  - 运行降级只能来自人工 / 运营 / 放量动作，不能写成后端自动降级字段
- 查询空态可以展示为空态，但不得误报成功：
  - `code=0` 且返回 `[]` / 空页，只能解释为“当前无内容”或“当前无可用分类”
  - 不得解释为“内容加载成功且已有数据”
  - 不得把查询空态回填成文章详情成功、FAQ 成功或内容转化成功
- 写操作失败一律不得伪成功：
  - `PUT /promotion/article/add-browse-count` 失败时，不得展示“浏览量已同步”
  - `PUT /promotion/kefu-message/update-read-status` 失败时，不得展示“已读已同步”或清空未读态
- 当前接口的真实差异必须保留：
  - `GET /promotion/article/list`、`GET /promotion/article/page`、`PUT /promotion/article/add-browse-count` 标注了 `@PermitAll`
  - `GET /promotion/article-category/list` 当前 controller 未标注 `@PermitAll`，按现有安全配置不能默认假设匿名可读
  - `PUT /promotion/kefu-message/update-read-status` 需要登录态，且会校验 `conversationId` 属于当前会员
  - `GET /promotion/article-category/list` 只返回启用分类
  - `GET /promotion/article/list`、`GET /promotion/article/page` 当前代码未额外按文章 `status` 过滤，SOP 不得自行补写“只返回启用文章”的假口径

## 3. 客服接待分层

| 层级 | 角色 | 职责 | 首响时限 |
|---|---|---|---|
| `L1` | 客服一线 | 识别列表 / 分类 / 浏览回写 / 已读回写场景，执行标准话术和恢复动作 | 5 分钟 |
| `L2` | 客服组长 / 内容运营支撑 | 区分合法空态与异常态，判断是否为误接、鉴权错误、写回失败 | 15 分钟 |
| `L3` | Content on-call / Product on-call / 发布负责人 | 执行暂停放量、撤入口、关闭写回、回滚联调版本 | P0 5 分钟，P1 15 分钟 |

## 4. 标准处理流程

### 4.1 接待步骤
1. 先确认场景：
   - 文章列表：`GET /promotion/article/list`
   - 文章分页：`GET /promotion/article/page`
   - 分类列表：`GET /promotion/article-category/list`
   - 浏览回写：`PUT /promotion/article/add-browse-count`
   - 已读回写：`PUT /promotion/kefu-message/update-read-status`
2. 采集最小信息：
   - `method/path`
   - `articleId`
   - `categoryId`
   - `conversationId`
   - `pageNo/pageSize`
   - `recommendHot/recommendBanner`
   - `errorCode`
   - 无值字段统一填 `"0"`
3. 先判断是查询场景还是写场景：
   - 查询：允许空态、允许刷新、允许回退，但不得把异常态包装成自然空态
   - 写场景：一律 `FAIL_CLOSE`，必须保留旧状态，不得先改 UI 再补错误提示
4. 按下表执行前台话术、客服口径和升级动作。

### 4.2 转人工条件
满足任一条件必须升级到 `L2/L3`：
- 同一用户连续 2 次刷新后仍是异常态，而不是合法空态
- 未来前端把 `GET /promotion/article-category/list` 以匿名方式调用，出现集中 `401/403`
- 浏览回写或已读回写失败后，前端仍展示成功态或已变更状态
- 同类问题 15 分钟内出现 3 单及以上
- 发现前端把 FAQ 壳页、文章详情成功、聊天发送成功误算成 BF-027 成功

## 5. 合法空态与异常态区分

| 场景 | 合法空态 | 异常态 | 禁止行为 |
|---|---|---|---|
| 文章列表 `GET /promotion/article/list` | `code=0` 且 `data=[]` | `HTTP` 失败、`code!=0`、返回结构不是数组 | 不得把异常态解释成“当前暂无文章” |
| 文章分页 `GET /promotion/article/page` | `code=0` 且 `PageResult` 结构完整、`list=[]` | `HTTP` 失败、`code!=0`、返回结构缺 `list` | 不得把异常页解释成“已成功加载但无结果” |
| 分类列表 `GET /promotion/article-category/list` | `code=0` 且 `data=[]`；当前所有启用分类为空时可成立 | `HTTP` 失败、`code!=0`、鉴权失败、返回结构不是数组 | 不得把 `401/403` 或请求失败说成“暂无分类” |

### 5.1 特别说明
- `GET /promotion/article/page?categoryId=...` 当前代码只按 `categoryId` 过滤，不校验分类是否存在。
  - 因此未来若传入不存在的 `categoryId` 而返回空页，按“合法空页”处理，不得伪造 `ARTICLE_CATEGORY_NOT_EXISTS`
- `GET /promotion/article-category/list` 当前只返回启用分类。
  - 因此“空分类”可以是合法空态，但不能据此宣称分类加载成功且已有业务命中

## 6. 场景话术与恢复动作

### 6.1 文章列表 / 分页查询

| 场景 | 前台话术 | 客服口径 | 恢复动作 | 禁止行为 |
|---|---|---|---|---|
| 文章列表合法空态 | 当前还没有可展示的内容，稍后再来看看。 | 当前查询返回为空，属于合法空态；不代表列表异常，也不代表已有内容成功命中。 | 保留空态页，允许刷新一次。 | 不得补跳文章详情或 FAQ 壳页充当列表成功。 |
| 文章分页合法空页 | 当前页暂无内容，请返回上一页或稍后刷新。 | 当前分页结果为空页，先按空页记录，不按异常升级。 | 保留分页参数，允许返回上一页。 | 不得把空页解释成“分页已成功加载全部内容”。 |
| 文章列表 / 分页异常态 | 当前内容加载失败，请刷新后重试。 | 当前不是空态，是查询失败；需记录 `method/path/errorCode`。 | 刷新 1 次；仍失败则转 `L2`。 | 不得把错误态说成“暂无内容”。 |

### 6.2 分类查询

| 场景 | 前台话术 | 客服口径 | 恢复动作 | 禁止行为 |
|---|---|---|---|---|
| 分类合法空态 | 当前暂无可用分类，请稍后刷新。 | 当前启用分类列表为空，可先按合法空态记录。 | 刷新 1 次；无恢复则转内容运营复核分类配置。 | 不得伪造静态分类或拿 FAQ 壳页替代分类页。 |
| 分类鉴权或请求异常 | 当前分类加载失败，请稍后重试。 | 当前是分类请求异常，不是空分类；若未来前端匿名调用导致 `401/403`，按联调错误处理。 | 校验登录态与调用方式；仍失败则转 `L3`。 | 不得把鉴权错误说成“暂无分类”。 |

### 6.3 浏览量回写失败

| 场景 | 前台话术 | 客服口径 | 恢复动作 | 禁止行为 |
|---|---|---|---|---|
| `PUT /promotion/article/add-browse-count` 失败 | 当前阅读统计暂未同步，不影响继续查看内容。 | 本次失败只代表浏览统计未回写，不代表文章详情失败；客服不得承诺“浏览量已增加”。 | 保留当前阅读内容；不自动重试；记录 `articleId/errorCode`。 | 不得展示“浏览量 +1 已同步”或本地先加计数。 |
| `articleId` 无效导致写回失败 | 当前阅读统计暂不可更新，请稍后重试。 | 当前应先核对 `articleId` 是否来自真实文章详情，不得把此异常回写到 FAQ 壳页或列表页。 | 转 `L2` 复核文章来源。 | 不得伪造成功，也不得把失败解释成“文章已阅读完成”。 |

### 6.4 已读回写失败

| 场景 | 前台话术 | 客服口径 | 恢复动作 | 禁止行为 |
|---|---|---|---|---|
| `PUT /promotion/kefu-message/update-read-status` 失败 | 当前已读状态暂未同步，请稍后刷新会话后重试。 | 本次失败不代表客服已读；请以当前未读状态和后续刷新结果为准。 | 保留未读态；刷新会话 1 次；仍失败转 `L2`。 | 不得清空未读标记或提示“已读已同步”。 |
| `1013019000 KEFU_CONVERSATION_NOT_EXISTS` | 当前会话状态已变化，请重新进入会话后再试。 | 会话不存在或当前会员无权写回，必须按错误码处理。 | 退出当前会话并重新进入；仍失败转 `L3`。 | 不得把错误码解释成“已读成功但页面未刷新”。 |

## 7. 与其他内容链路的边界

| 链路 | 当前真值 | 本文口径 |
|---|---|---|
| `GET /promotion/article/get` | 属于文章详情 / 富文本链路 | 不纳入本文成功率、空态、写回口径；另按文章详情 SOP 处理 |
| `POST /promotion/kefu-message/send` | 属于聊天发送链路 | 发送失败按聊天 fail-close 处理，不纳入 BF-027 |
| `GET /promotion/kefu-message/list` | 属于聊天列表链路 | 仅 `update-read-status` 属于本文；消息拉取空列表/异常另按聊天 SOP |
| `/pages/public/faq` | FAQ 只是壳页，真实运行时跳 `/pages/public/richtext?title=常见问题` | 不得把 FAQ 壳页成功误记为文章列表 / 分类成功 |

## 8. 验收清单
- [ ] 文章列表、文章分页、分类列表都明确区分合法空态与异常态。
- [ ] `browse-count` 与 `read-status` 写回失败都有前台话术和客服口径。
- [ ] 文档明确当前没有服务端 `degraded=true` 字段。
- [ ] 文档明确查询空态可以展示为空态，但不得误报成功。
- [ ] 文档明确写操作失败一律不得伪成功。
- [ ] 文档明确 FAQ 只是壳页，`GET /promotion/article/get` 是另一条内容链路。
