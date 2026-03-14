# MiniApp 内容文章列表 / 分类 / 回写 Runbook v1（2026-03-12）

## 1. 目标与范围
- 目标：为 `BF-027` 建立运行、验收、告警、放量和回滚手册，供未来前端补齐文章列表 / 分类 / 浏览回写 / 已读回写时直接执行。
- 覆盖接口：
  - `GET /promotion/article/list`
  - `GET /promotion/article/page`
  - `GET /promotion/article-category/list`
  - `PUT /promotion/article/add-browse-count`
  - `PUT /promotion/kefu-message/update-read-status`
- 当前真值前提：
  - `BF-027` 当前仍是 `PLANNED_RESERVED`，没有真实用户页面入口
  - 运行手册先服务于联调、验收和未来灰度，当前默认动作仍是“不放量、不入主链路”
  - 当前没有服务端 `degraded=true` 字段；本 runbook 不建立虚构的后端 degraded 逻辑

## 2. 代码真值摘要
- 前端当前真实 API 绑定：
  - `yudao-mall-uniapp/sheep/api/promotion/article.js` 只调用 `GET /promotion/article/get`
  - `yudao-mall-uniapp/sheep/api/promotion/kefu.js` 只调用 `POST /promotion/kefu-message/send`、`GET /promotion/kefu-message/list`
- 鉴权边界：
  - `GET /promotion/article/list`、`GET /promotion/article/page`、`PUT /promotion/article/add-browse-count` 带 `@PermitAll`
  - `GET /promotion/article-category/list` 未带 `@PermitAll`，按现有安全配置不能默认匿名访问
  - `PUT /promotion/kefu-message/update-read-status` 需要登录态，且服务端校验 `conversationId` 属于当前会员
- 查询行为边界：
  - `GET /promotion/article-category/list` 只返回启用分类
  - `GET /promotion/article/list`、`GET /promotion/article/page` 当前代码未额外按文章 `status` 过滤
  - `GET /promotion/article/page` 仅按 `categoryId` 过滤，不会因为分类不存在而抛 `ARTICLE_CATEGORY_NOT_EXISTS`
- 写行为边界：
  - `PUT /promotion/article/add-browse-count` 成功返回 `true`，失败时服务端直接抛错，不存在“半成功”
  - `PUT /promotion/kefu-message/update-read-status` 成功返回 `true`，失败时同样直接抛错，不存在“已读部分成功”

## 3. 监控字段最小集

| 接口 | 最小监控字段 | 说明 |
|---|---|---|
| `GET /promotion/article/list` | `method,path,recommendHot,recommendBanner,httpStatus,commonResult.code,resultCount` | 只用真实 query 键；`resultCount=len(data)` |
| `GET /promotion/article/page` | `method,path,categoryId,pageNo,pageSize,httpStatus,commonResult.code,resultCount` | `resultCount=len(data.list)` |
| `GET /promotion/article-category/list` | `method,path,httpStatus,commonResult.code,resultCount` | 额外单列 `401/403` 次数，防止误按匿名接入 |
| `PUT /promotion/article/add-browse-count` | `method,path,articleId,httpStatus,commonResult.code` | `articleId=id` |
| `PUT /promotion/kefu-message/update-read-status` | `method,path,conversationId,httpStatus,commonResult.code` | `conversationId` 是唯一真实主业务键 |

### 3.1 最小字段规则
- 不追加不存在的 `degraded/degradeReason`
- 不按 `msg` 聚合，只按 `httpStatus + commonResult.code`
- 当前无真实前端页时，不强制要求 `route/pageName`；后续若补前端，可在不改变以上最小集前提下追加

## 4. 合法空态判定

| 场景 | 合法空态判定 | 不合法判定 | 运行解释 |
|---|---|---|---|
| 空列表 | `GET /promotion/article/list` 返回 `code=0` 且 `data=[]` | `HTTP` 失败、`code!=0`、返回结构不是数组 | 这是合法空态，不是异常；但也不是“命中成功” |
| 空页 | `GET /promotion/article/page` 返回 `code=0` 且 `PageResult.list=[]` | `HTTP` 失败、`code!=0`、`data` 缺 `list` | 这是合法空页；不得伪造成列表加载成功且有内容 |
| 空分类 | `GET /promotion/article-category/list` 返回 `code=0` 且 `data=[]` | `HTTP` 失败、`code!=0`、`401/403`、返回结构不是数组 | 当前只返回启用分类，所以空分类在配置为空时合法 |

### 4.1 合法空态的统计口径
- 合法空态可以计入“接口返回成功且结构合法”的技术可用性样本。
- 合法空态不得计入：
  - BF-027 的业务命中成功
  - 文章列表主成功率
  - 文章列表主转化率
  - 任何“内容已展示成功且有数据”的验收结论
- 结论：查询空态可视为空态，不得误报成功。

## 5. 写回失败的告警分级

### 5.1 `PUT /promotion/article/add-browse-count`

| 级别 | 触发条件 | 解释 | 动作 |
|---|---|---|---|
| `P2` | 单篇文章或低频样本写回失败；阅读链路本身不受影响 | 统计补充失败，影响浏览计数但不阻断阅读 | 记录 `articleId/code`，观察 1 个窗口 |
| `P1` | 15 分钟内连续 2 个窗口失败率上升，或未来灰度流量中失败率 `>5%` | 浏览统计开始系统性失真 | 暂停 BF-027 灰度中的浏览量写回，改为只读观察 |
| `P0` | 写回失败仍被前端或 BI 记成“浏览成功 +1” | 命中伪成功红线 | 立即停灰、回滚前端写回开关、重算相关统计 |

### 5.2 `PUT /promotion/kefu-message/update-read-status`

| 级别 | 触发条件 | 解释 | 动作 |
|---|---|---|---|
| `P2` | 单会话偶发 `1013019000 KEFU_CONVERSATION_NOT_EXISTS` | 可能是会话已失效或错误会话号 | 记录 `conversationId/code`，人工复核来源 |
| `P1` | 15 分钟内连续 2 个窗口失败率上升，或未来灰度流量中失败率 `>2%` | 已读同步开始系统性不稳定 | 暂停 BF-027 灰度中的自动已读回写，只保留手动刷新 |
| `P0` | 写回失败但前端仍清空未读态、展示“已读已同步”，或把失败样本记成成功 | 命中写操作伪成功红线 | 立即停灰、回滚已读同步入口、恢复旧未读状态 |

### 5.3 错误码口径
- `browse-count`
  - 可确认的真实业务错误码只有 `ARTICLE_NOT_EXISTS(1013016000)`，来自无效 `articleId`
  - 其余失败统一按非 `0` code / `HTTP` 异常处理，不补不存在的业务错误码
- `read-status`
  - 真实业务错误码为 `KEFU_CONVERSATION_NOT_EXISTS(1013019000)`
  - 不得补写“已读冲突”“已读降级”等不存在的错误码

## 6. 是否进入主成功率 / 主转化率

| 对象 | 当前是否进入主成功率 | 当前是否进入主转化率 | 说明 |
|---|---|---|---|
| `GET /promotion/article/list` | 否 | 否 | 当前无真实用户页，`BF-027` 仍是 `PLANNED_RESERVED` |
| `GET /promotion/article/page` | 否 | 否 | 同上；空页只能记为空态样本 |
| `GET /promotion/article-category/list` | 否 | 否 | 同上；且当前还存在鉴权边界未冻结 |
| `PUT /promotion/article/add-browse-count` | 否 | 否 | 统计写回，不是当前主成功率 / 主转化率分母 |
| `PUT /promotion/kefu-message/update-read-status` | 否 | 否 | 状态同步写回，不是当前主成功率 / 主转化率分母 |

### 6.1 未来补前端时的固定规则
- 在 A/B/C 窗口未把 `BF-027` 正式升为 `ACTIVE` 之前：
  - 这 5 条链路全部不得并入当前 miniapp 主成功率 / 主转化率
- 合法空态即使 `code=0`：
  - 也只能进入 BF-027 的“空态样本”或“结构成功样本”
  - 不能进入主转化、主命中成功、主内容曝光成功
- 写操作失败：
  - 一律不得被 BI、埋点、前台 toast 或客服口径回填成成功

## 7. 回滚 / 降级 / 暂停放量动作

## 7.1 当前默认动作
- 当前没有真实前端入口，因此默认动作是：
  - 不放量
  - 不开放用户入口
  - 不把 BF-027 计入当前发布主链

## 7.2 未来补前端后的动作模板

| 触发条件 | 回滚动作 | 降级动作 | 暂停放量动作 |
|---|---|---|---|
| 列表 / 分页异常态集中爆发 | 回滚文章列表 / 分类入口版本 | 仅保留空态页与刷新动作，不自动跳 FAQ 壳页或文章详情 | 立即停止 BF-027 灰度扩量 |
| 分类列表出现集中 `401/403` | 回滚分类入口或补齐登录门禁后再测 | 暂时隐藏分类入口 | 暂停分类相关放量 |
| 浏览量回写失败率越阈值 | 回滚浏览量写回调用 | 保留阅读，不再调用 `add-browse-count` | 暂停 BF-027 放量直到写回恢复 |
| 已读回写失败率越阈值 | 回滚已读自动同步 | 保留未读态，只允许刷新会话，不再调 `update-read-status` | 暂停 chat-read 相关放量 |
| 任一写链路出现伪成功 | 立即回滚对应前端版本和埋点 | 不允许继续展示任何成功态文案 | 直接停灰并进入发布阻断 |

### 7.3 固定禁止项
- 不得把 BF-027 的降级写成服务端 `degraded=true`
- 不得把 FAQ 壳页当成文章列表 / 分类的回滚承接页
- 不得把 `GET /promotion/article/get` 当成 BF-027 查询成功的兜底
- 不得在写回失败时“先成功展示，后续补偿”

## 8. 发布与验收结论规则

| 结论 | 条件 | 说明 |
|---|---|---|
| `NO_GO_DEFAULT` | 当前无真实前端入口 | BF-027 默认维持 `PLANNED_RESERVED` |
| `PASS_WITH_EMPTY` | 查询返回合法空态且结构合法 | 只代表接口结构可用，不代表业务命中成功 |
| `PASS_WITH_WATCH` | 查询结构成功、写回成功率达标，但仍处灰度 / 联调阶段 | 继续观察，不得直接升 `ACTIVE` |
| `FAIL_ROLLBACK` | 查询异常态集中、鉴权边界错误、写回失败越阈值、任一伪成功 | 立即回滚或停灰 |

## 9. 联调硬边界
- FAQ 只是壳页：
  - `/pages/public/faq` 不能作为列表 / 分类空态兜底
- `GET /promotion/article/get` 属于另一条内容链路：
  - 只能按 BF-026 / 文章详情链路处理
  - 不能拿来冲抵 BF-027 的列表成功率
- 聊天发送失败、文章详情失败、FAQ 壳页失败：
  - 都不并入本 runbook 的告警分级和主分母

## 10. 验收清单
- [ ] 监控字段只使用当前真实 query 键、主业务键和 `CommonResult.code`。
- [ ] 文档明确当前没有服务端 `degraded=true` 字段。
- [ ] 文档明确空列表 / 空页 / 空分类的合法空态判定。
- [ ] 文档明确查询空态可以展示为空态，但不得误报成功。
- [ ] 文档明确 `browse-count` / `read-status` 写回失败一律不得伪成功。
- [ ] 文档明确当前 BF-027 不进入主成功率 / 主转化率。
- [ ] 文档明确 FAQ 壳页和 `GET /promotion/article/get` 都是边界外链路。
