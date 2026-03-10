# Window C Handoff - MiniApp Remaining Contract Pack（2026-03-10）

## 1. 本批交付
- 分支：`feat/ui-four-account-reconcile-ops`
- 交付类型：仅补 contract、canonical error register 与 handoff；未改 overlay、未改业务代码、未动历史 handoff、未处理无关 untracked。
- 新增文件：
  1. `docs/contracts/2026-03-10-miniapp-booking-user-api-alignment-v1.md`
  2. `docs/contracts/2026-03-10-miniapp-content-customer-service-contract-v1.md`
  3. `docs/contracts/2026-03-10-miniapp-brokerage-domain-contract-v1.md`
  4. `docs/contracts/2026-03-10-miniapp-product-catalog-contract-v1.md`
  5. `docs/contracts/2026-03-10-miniapp-marketing-expansion-contract-v1.md`
  6. `hxy/07_memory_archive/handoffs/2026-03-10/miniapp-remaining-contract-pack-window-c.md`
- 更新文件：
  1. `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`

## 2. 核心收口结论

### 2.1 Booking 真值正式收口
- 本次 booking contract 已把页面调用方、controllerPath、method/path、request、response、状态、failureMode、retryClass、degrade、发布口径全部落成单表。
- 唯一允许作为 canonical 的 booking 核心路径已固定：
  - `GET /booking/technician/list`
  - `GET /booking/slot/list`
  - `GET /booking/slot/list-by-technician`
  - `POST /booking/order/create`
  - `GET /booking/order/list-by-status`
  - `GET /booking/order/get-by-order-no`
  - `POST /booking/order/cancel`
  - `POST /app-api/booking/addon/create`
- 明确禁止进入 `ACTIVE` 的旧路径已固定为 `BLOCKED`：
  - `GET /booking/technician/list-by-store`
  - `GET /booking/time-slot/list`
  - `PUT /booking/order/cancel`
  - `POST /booking/addon/create`
- booking 链路结论已固定：
  - `technician list`：`PLANNED_RESERVED`
  - `slot list/list-by-technician`：store 级 `ACTIVE_BE_ONLY`，technician 级 `PLANNED_RESERVED`
  - `order create`：接口 path 对齐，但上游链路未对齐，保持 `PLANNED_RESERVED`
  - `order cancel`：旧 `PUT` 固定阻断，保持 `PLANNED_RESERVED`
  - `addon create`：缺 `/app-api` 前缀，固定阻断，保持 `PLANNED_RESERVED`
  - `order list-by-status`、`get-by-order-no`：后端真值存在但 FE 未绑，固定 `ACTIVE_BE_ONLY`

### 2.2 其余 contract 域补齐结果
- Content / Customer Service
  - DIY 启动链路 `GET /promotion/diy-template/used|get`、`GET /promotion/diy-page/get` 固定 `ACTIVE`。
  - 客服/文章当前真实存在，但仍按 capability 冻结口径保持 `PLANNED_RESERVED`。
  - `update-read-status`、`article/list`、`article/page`、`article-category/list`、`add-browse-count` 明确为 `ACTIVE_BE_ONLY`。
- Brokerage
  - 分销中心、排行、团队、提现等真实 path 已固化，但整域仍不能升 `ACTIVE`，统一 `PLANNED_RESERVED`。
  - 佣金排行分页响应字段真值已收口为 `brokeragePrice`。
  - 团队排行分页响应字段真值已收口为 `brokerageUserCount`。
- Product Catalog
  - `search-lite` 真值固定为 `GET /product/spu/page`。
  - `GET /product/search/page`、`GET /product/catalog/page` 因当前分支无真实 app controller，固定 `BLOCKED`。
  - 评价/收藏/浏览历史套件保留 `PLANNED_RESERVED`；未绑定收藏数量接口显式记为 `ACTIVE_BE_ONLY`。
- Marketing Expansion
  - 活动聚合、拼团、秒杀、满减送页面真实调用全部固化为 `PLANNED_RESERVED`。
  - 砍价 controller 全部真实存在，但当前无 FE API/页面绑定，统一记为 `ACTIVE_BE_ONLY`。

### 2.3 Canonical error / degrade 约束补齐
- 已把本批 contract 新引用的 booking、content、brokerage、product、bargain 错误码补入 canonical register。
- 所有新增/收口的 contract 都明确：
  - 只按 `errorCode` 分支
  - 显式标 `failureMode`
  - 显式标 `retryClass`
  - 不把提示文案当分支依据
- degrade 语义已固定：
  - 查询型 fail-open：只允许 `[]` / `null` / 空页作为合法空态
  - 写操作 fail-close：必须直接按错误码阻断
  - 当前这五个域均没有新增服务端 `degraded/degradeReason` 字段契约

## 3. 给窗口 A / B / D 的联调注意点

### 3.1 给窗口 A（集成 / 前端）
- Booking
  - 必须把 `GET /booking/technician/list-by-store`、`GET /booking/time-slot/list`、`PUT /booking/order/cancel`、`POST /booking/addon/create` 继续视为禁用旧路径。
  - `cancelOrder` 实参必须向 `id + reason` 收口，不能继续提交 `cancelReason` body。
  - 如需状态筛选或订单号查单，只能接 `list-by-status`、`get-by-order-no` 新真值接口。
- Content / Customer Service
  - `article/get -> null`、`kefu-message/list -> []`、DIY 模板/页面 -> `null` 都是合法协议，不能靠提示文案猜状态。
- Brokerage
  - 排行页字段只能读 `brokeragePrice`、`brokerageUserCount`，不要继续写 `price`、`userCount` 别名。
  - 提现成功只能表述为“申请提交成功”，不是“到账成功”。
- Product / Marketing
  - `search-lite` 只能继续走 `/product/spu/page`。
  - 拼团/秒杀/满减送详情返回 `null`、砍价查询返回 `[]` 都只代表 fail-open 空态，不代表自动降级后可继续下单。

### 3.2 给窗口 B（产品 / 文档）
- Booking 发布口径必须继续保留阻断结论，不能把 create/cancel/addon 提前改写成 `ACTIVE`。
- Content / Brokerage / Marketing 当前“页面可访问”不等于 capability 已上线；PRD、SOP、freeze 文档必须继续沿用 `PLANNED_RESERVED` / `ACTIVE_BE_ONLY`。
- Product 文档必须继续区分：
  - `search-lite = /product/spu/page`
  - `search-canonical = /product/search/page（BLOCKED）`

### 3.3 给窗口 D（runbook / 验收 / 数据）
- Booking 重点验旧路径未被误放行，尤其是 cancel/addon。
- Brokerage 重点验资金相关失败码：
  - `1011008002`
  - `1011008003`
  - 不允许被“处理中”之类提示文案吞掉。
- Bargain 重点验 `ACTIVE_BE_ONLY` 边界：
  - 后端有 controller
  - 但当前无 FE route / API 绑定
  - 不能把它算成正式发布能力

## 4. 风险与建议
- 风险 1：工作树里还有其他窗口未提交的产品文档与无关修改，本窗口提交只应挑 contract/register/handoff 目标文件，避免混入。
- 风险 2：booking create 单接口虽已对齐，但因为 technician/slot 入口仍漂移，A/B 若忽略阻断结论会再次把 booking 真值做散。
- 风险 3：分销排行字段若继续沿用旧别名，会直接导致 A/C 联调和 D 验收口径不一致。
- 建议：
  1. A 先按本 handoff 完成旧路径禁用和字段名收口，再评估是否进入下一轮联调。
  2. B 更新 capability / freeze 文档时，直接引用本批 contract 的状态字段，不再做自然语言泛化。
  3. D 将“按错误码分支、按空态降级、不按提示文案分支”列为四域共同必验项。
