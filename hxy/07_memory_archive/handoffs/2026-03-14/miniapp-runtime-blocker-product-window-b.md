# Window B Handoff - MiniApp Runtime Blocker Product Closure（2026-03-14）

## 1. 本批交付
- 分支：`feat/ui-four-account-reconcile-ops`
- 交付类型：仅产品文档与 handoff；未改业务代码、未改 overlay 页面、未动 `.codex`、未动历史 handoff、未处理无关 untracked。
- 变更文件：
  1. `docs/products/miniapp/2026-03-14-miniapp-runtime-blocker-product-closure-v1.md`
  2. `docs/products/miniapp/2026-03-09-miniapp-booking-schedule-prd-v1.md`
  3. `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`
  4. `docs/products/miniapp/2026-03-09-miniapp-feature-priority-alignment-v1.md`
  5. `docs/products/miniapp/2026-03-14-miniapp-finance-ops-technician-commission-detail-config-prd-v1.md`
  6. `hxy/07_memory_archive/handoffs/2026-03-14/miniapp-runtime-blocker-product-window-b.md`

## 2. 核心收口结论
- 本批 blocker 的统一产品标签已固定为：
  - `Doc Closed = Yes`
  - `Engineering Blocked = No`
  - `Can Develop = Yes`
  - `Cannot Release = Yes`
- Booking 最终边界已固定：
  - 当前只承认查询侧真实能力：
    - `/pages/booking/technician-detail -> GET /booking/technician/get`
    - `/pages/booking/order-list -> GET /booking/order/list`
    - `/pages/booking/order-detail -> GET /booking/order/get`
  - `create / cancel / addon` 继续不能写成已闭环：
    - `GET /booking/technician/list-by-store` 不再是真值
    - `GET /booking/time-slot/list` 不再是真值
    - `PUT /booking/order/cancel` 不再是真值
    - `POST /booking/addon/create` 不再是真值
- Member 缺页边界已固定：
  - `/pages/user/level`
  - `/pages/profile/assets`
  - `/pages/user/tag`
  - 三者都只能写成缺页能力，不能写成已上线页面。
- Reserved runtime 边界已固定：
  - gift-card / referral / technician-feed 继续是 `P2 / RB3-P2`
  - 规划、治理、gray 文档齐了，不等于 runtime 已上线
- BO-004 最终边界已固定：
  - 当前只认真实 8 条 `/booking/commission/*`
  - 不得把 `commission-settlement/index.vue`、`commission-settlement/outbox/index.vue` 反推成 BO-004 页面
  - `Boolean true` 不等于业务完成；写接口必须“写后回读确认”

## 3. 给窗口 A / C / D 的联调注意点

### 3.1 给窗口 A（前端 / 集成）
- Booking 不再允许把旧 path / method 写回联调口径：
  - 只认 `GET /booking/technician/list`
  - 只认 `GET /booking/slot/list-by-technician`
  - 只认 `POST /booking/order/cancel`
  - 只认 `POST /app-api/booking/addon/create`
- Member 不得自造缺失页面、入口、状态页：
  - `/pages/user/level`
  - `/pages/profile/assets`
  - `/pages/user/tag`
- BO-004 不得补猜测性页面 path 或菜单 path：
  - 当前没有独立后台页面文件
  - 当前没有独立前端 API 文件
- 关键字段只认真实主键与检索键：
  - Booking：`id / orderNo / technicianId / storeId / status`
  - Member 缺页能力：真实入口键只有已发布页面 route；缺页项没有 route 主键可用于发布统计
  - BO-004：`commissionId(id) / technicianId / orderId / storeId / sourceBizNo / settlementId`

### 3.2 给窗口 C（契约 / 后端）
- Booking 继续只按 canonical `method + path` 写 contract，不再保留旧实现为产品真值。
- Member 缺页能力与 Reserved runtime 未实现能力，如果没有真实对外暴露证据，不要在 contract 或 PRD 中承诺稳定错误码分支。
- Reserved 三项继续只写“可开发，不可放量”，不能把治理冻结写成 runtime 上线。
- BO-004 继续按真实 8 条接口写实：
  - 合法空态：
    - `list-by-technician => []`
    - `list-by-order => []`
    - `pending-amount => 0`
    - `config/list => []`
  - 写接口：
    - `settle`
    - `batch-settle`
    - `config/save`
    - `config/delete`
  - 四条写接口都不能只看 `true`，必须按“写后回读确认”定义结果
- 不要补写不存在的稳定 admin 错误码：
  - BO-004 当前 no-op 也可能返回 `true`
  - Member tag 当前无 app 端读取接口
  - Reserved 三项当前无 runtime outward code 样本

### 3.3 给窗口 D（数据 / 验收）
- Booking 验收重点是“查询侧真实 + 写链未放量”，不是“整域已闭环”。
- Member 验收重点是“缺页能力未被误写成上线页”，不是补 fake page sample。
- Reserved 验收重点是“开关关闭态误返回为 0”，不是把 gray 文档当上线凭证。
- BO-004 验收重点是：
  - `[] / 0` 是合法空态
  - 写接口必须读后确认
  - 不能把 BO-003 页面样本拿来冲抵 BO-004 页面闭环
- 降级 / 失败语义：
  - Booking 查询页：fail-open 允许空态或返回列表，不允许白屏
  - Member 缺页能力：当前不是 fail-open / fail-close 问题，而是“无 runtime 页面，不允许对外”
  - Reserved 三项：关闭态命中属于 `Cannot Release`，不是 warning
  - BO-004：当前无 `degraded` 字段；写接口 true 但读后未变，要按假成功风险处理

## 4. 风险与建议
- 风险 1：A/C 若继续引用旧 booking path，会把产品真值再次污染回 alias / drift 状态。
- 风险 2：Member 缺页能力最容易被写成“页面未联通但已上线”，本批已明确禁止。
- 风险 3：Reserved 三项文档包齐全，最容易被误报成“可灰度”；本批已固定只能 `Can Develop / Cannot Release`。
- 风险 4：BO-004 仍最容易被误写成“后台页面已上线”或“写接口 true 即成功”；本批已固定禁止。
- 建议：
  1. A 后续如果补 Booking/Member/BO-004 页面，只能按本批 fixed truth 开工。
  2. C 后续如果补 contract，只能写真实 outward code，不能补猜测性错误码分支。
  3. D 把“旧 path 清零”“缺页不入发布”“BO-004 读后确认”列为专项验收项。
