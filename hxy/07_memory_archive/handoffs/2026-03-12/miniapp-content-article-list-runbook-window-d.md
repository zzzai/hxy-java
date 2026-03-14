# MiniApp Content Article List / Category / Writeback - Window D Handoff (2026-03-12)

## 1. 变更摘要
- 新增 BF-027 客服 / 用户恢复 SOP：
  - `docs/products/miniapp/2026-03-12-miniapp-content-article-list-category-writeback-sop-v1.md`
  - 覆盖文章列表空态与异常态、分类空态与异常态、浏览量回写失败、已读回写失败，以及与聊天发送 / 文章详情 / FAQ 壳页失败的边界。
- 新增 BF-027 运行与验收 runbook：
  - `docs/plans/2026-03-12-miniapp-content-article-list-category-writeback-runbook-v1.md`
  - 覆盖最小监控字段、合法空态判定、`browse-count` / `read-status` 告警分级、主成功率 / 主转化率隔离、回滚 / 降级 / 暂停放量动作。

## 2. 当前真值结论
- `BF-027` 仍是 `PLANNED_RESERVED`，当前无真实用户页入口。
- 本批文档只收口运行与验收口径，不等于 A 窗口可以把 `BF-027` 改成 `ACTIVE`。
- 当前没有服务端 `degraded=true` 字段：
  - 本批 SOP / runbook 没有写任何后端 degraded 逻辑
  - 所有降级都只来自人工 / 运营 / 放量动作
- 查询空态口径已固定：
  - 空列表 / 空页 / 空分类可以视为空态
  - 但不得误报成功，不得记成“内容命中成功”
- 写操作口径已固定：
  - `PUT /promotion/article/add-browse-count`
  - `PUT /promotion/kefu-message/update-read-status`
  - 任一失败都不得伪成功

## 3. 关键代码差异已写入文档
- `GET /promotion/article/list`、`GET /promotion/article/page`、`PUT /promotion/article/add-browse-count` 带 `@PermitAll`
- `GET /promotion/article-category/list` 当前未带 `@PermitAll`
- `PUT /promotion/kefu-message/update-read-status` 需要登录态，并会校验 `conversationId`
- `GET /promotion/article-category/list` 只返回启用分类
- `GET /promotion/article/list`、`GET /promotion/article/page` 当前代码未额外按文章 `status` 过滤

## 4. 对窗口 A / B / C 的联调提醒
- A（集成 / 发布）
  - `BF-027` 继续维持 `PLANNED_RESERVED`；runbook 完整不等于可进主发布口径。
  - 这 5 个接口当前都不得计入 miniapp 主成功率 / 主转化率。
  - 未来若灰度 BF-027，必须单独看空态样本和写回成功率，不能拿 `article/get` 或 FAQ 壳页冲抵成功。
- B（产品 / 运营 / 客服）
  - 前台话术已固定：列表 / 分类空态与异常态必须分开，不得把异常态说成“暂无内容”。
  - 浏览量回写失败只能说“阅读统计暂未同步，不影响查看”，不得承诺“浏览量已增加”。
  - 已读回写失败只能说“已读状态暂未同步”，不得承诺“客服已读”。
- C（契约 / 后端）
  - 当前只允许使用真实错误码：
    - `ARTICLE_NOT_EXISTS(1013016000)` 用于无效 `articleId` 的浏览回写失败
    - `KEFU_CONVERSATION_NOT_EXISTS(1013019000)` 用于已读回写失败
  - 不得补不存在的 BF-027 专属错误码、warning 字段或 degraded 字段。
  - `GET /promotion/article/page` 传不存在的 `categoryId` 当前按空页返回，不得在联调口径里补写 `ARTICLE_CATEGORY_NOT_EXISTS`。

## 5. 固定验证命令
1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
