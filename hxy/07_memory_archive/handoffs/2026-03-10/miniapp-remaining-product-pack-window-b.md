# Window B Handoff - MiniApp Remaining Product Pack（2026-03-10）

## 1. 本批交付
- 分支：`feat/ui-four-account-reconcile-ops`
- 交付类型：仅新增业务规划文档与 handoff；未改 overlay、未改业务代码、未动历史 handoff。
- 新增文件：
  1. `docs/products/miniapp/2026-03-10-miniapp-content-customer-service-prd-v1.md`
  2. `docs/products/miniapp/2026-03-10-miniapp-brokerage-distribution-prd-v1.md`
  3. `docs/products/miniapp/2026-03-10-miniapp-product-catalog-interaction-prd-v1.md`
  4. `docs/products/miniapp/2026-03-10-miniapp-marketing-expansion-prd-v1.md`
  5. `hxy/07_memory_archive/handoffs/2026-03-10/miniapp-remaining-product-pack-window-b.md`

## 2. 核心收口结论

### 2.1 Content / Customer Service
- `FAQ` 当前真实体验不是 FAQ 数据列表，而是 `/pages/public/faq` 作为壳页后立即跳 `/pages/public/richtext?title=常见问题`。
- 当前真实用户入口已固定：
  - 客服：商品详情底栏、售后申请 / 详情、工具菜单 -> `/pages/chat/index`
  - 内容：`/pages/public/richtext`、`/pages/public/webview`
  - DIY：`app.init -> /promotion/diy-template/used|get`、`/pages/index/page?id=...`
- 后端已存在但前端未消费：
  - `PUT /promotion/kefu-message/update-read-status`
  - `GET /promotion/article/list`
  - `GET /promotion/article/page`
  - `GET /promotion/article-category/list`
  - `PUT /promotion/article/add-browse-count`
- 发送消息产品口径必须是 `fail-close`，禁止“发送成功”伪成功。

### 2.2 Brokerage / Distribution
- 菜单真值已固定：没有 `/pages/commission/apply`，只有团队、佣金明细、分销订单、推广商品、邀请海报、推广排行、佣金排行。
- 资金口径已固定：
  - 当前佣金 = `withdrawPrice`
  - 可提现佣金 = `brokeragePrice`
  - 冻结佣金 = `frozenPrice`
- 提现申请成功只代表 `POST /trade/brokerage-withdraw/create` 建单成功，不代表到账成功。
- 当前无用户侧申诉页、撤回页、取消提现页，只能标为缺页能力。
- 团队字段存在真值风险：
  - 后端 app VO 为 `brokerageOrderCount`
  - 当前前端模板读取 `item.orderCount`
  - A/C 联调必须明确兼容字段，不要静默猜测。

### 2.3 Product Catalog / Interaction
- `search-lite` 已固定为真实上线能力：
  - `/pages/index/search` 保存本地历史
  - 跳 `/pages/goods/list?keyword=...`
  - 实际查询 `GET /product/spu/page`
- `search-canonical` 仍是规划态：
  - `/pages/search/index` 当前无真实 route
  - `GET /product/search/page` 受 `miniapp.search.validation=off` 和 `1008009904` 保护
- 商品详情互动已固定：
  - 收藏真实路径是 `/product/favorite/exits`，不能擅自改成 `/exists`
  - `GET /product/spu/get-detail` 会自动增加浏览量并写浏览记录
  - 评论发布页必须“全部评论成功后才允许离页”

### 2.4 Marketing Expansion
- 当前已承接能力已固定：
  - 秒杀：`/pages/activity/seckill/list`、`/pages/goods/seckill`
  - 拼团：`/pages/activity/groupon/list`、`/pages/goods/groupon`、`/pages/activity/groupon/order`、`/pages/activity/groupon/detail`
  - 满减送：`/pages/activity/index`
  - 商品详情活动聚合：`GET /promotion/activity/list-by-spu-id`
- 砍价当前只能归类为“后端存在但前端未产品化”：
  - `/promotion/bargain-activity/*`
  - `/promotion/bargain-record/*`
  - `/promotion/bargain-help/*`
- 若聚合接口返回 `type=2 bargain`，当前前端必须隐藏或忽略，不能跳转不存在页面。

## 3. 给窗口 A / C / D 的联调注意点

### 3.1 给窗口 A（前端）
- Content / CS：
  - `FAQ` 必须继续按“壳页 -> richtext”理解，不能直接把 FAQ route 当数据页。
  - 聊天发送失败时要保留输入内容，不要先弹成功。
  - DIY 模板失败只能进错误页或稳定模板兜底，不能空白继续展示。
- Brokerage：
  - 提现成功弹窗只能表达“申请已提交”，不能表达“已到账”。
  - 团队页要核实 `brokerageOrderCount` vs `orderCount` 字段兼容。
  - 只有 `status===10 && type===5 && payTransferId>0` 才显示“确认收款”。
- Catalog：
  - 收藏状态查询必须继续用真实 path `/product/favorite/exits`。
  - 评论发布失败时不能部分成功后直接 back。
  - `search-lite` 不要偷偷切成 `/pages/search/index`。
- Marketing：
  - 砍价入口不能深链。
  - 满减送页 `/pages/activity/index` 不是统一活动中心。
  - 聚合接口返回 bargain 类型时只能隐藏 / 忽略。

### 3.2 给窗口 C（契约 / 后端）
- Content / CS：
  - 继续把 `article/list/page/category/list/add-browse-count`、`kefu update-read-status` 视为“后端已存在、前端未消费”。
  - FAQ 不能被 contract 文档误写成独立 app FAQ 数据接口已上线。
- Brokerage：
  - 固定资金字段口径：`withdrawPrice`、`brokeragePrice`、`frozenPrice`。
  - 提现状态要继续以 `0/10/11/20/21` 为准，不要文档里自行换枚举名。
  - 团队字段若保持 `brokerageOrderCount`，需在 contract 中显式写明与前端读取差异。
- Catalog：
  - canonical search 仍应保持 `PLANNED_RESERVED`。
  - `1008009904 MINIAPP_SEARCH_QUERY_INVALID` 只能挂在 canonical search，不得污染 lite 路径。
- Marketing：
  - bargain contract 可以继续写，但必须明确“前端无 route / 无页面”。
  - activity aggregation contract 必须强调前端仅承接 `type=1` 和 `type=3`。

### 3.3 给窗口 D（数据 / 验收）
- Content / CS：
  - 重点验“聊天发送失败不伪成功”“FAQ 真实跳 richtext”“DIY 模板失败不空白”。
- Brokerage：
  - 重点验“申请成功 != 到账成功”“冻结 / 可提现 / 当前佣金口径一致”“确认收款按钮门槛”。
- Catalog：
  - 重点验“`search-lite` 仍走 `/product/spu/page`”“评论必须全部成功才离页”“收藏 / 删除足迹失败不伪成功”。
- Marketing：
  - 重点验“聚合返回 bargain 类型时前端不跳错页”“满减送失败回退普通商品”“秒杀 / 拼团关闭后 CTA 真正失效”。

## 4. 风险与建议
- 风险 1：当前工作树已有 content contract draft 与其他总览文档改动，A 窗口集成时不要把未提交 Draft 当成正式冻结件。
- 风险 2：`search-canonical` 最容易被误写成现网搜索；B 文档已明确拆开，但 C/A 若偷换 route 会再次失真。
- 风险 3：分销团队页字段名差异会直接影响 A/C 联调和 D 验收，优先确认。
- 建议：
  1. A 先按四份 PRD 校正页面说明与联调字段，不要抢先做 reserved 能力。
  2. C 逐域补齐 contract 时，先沿本 handoff 中的 route/API/field 真值对齐。
  3. D 把“禁止伪成功”列为四域共同必验项。
