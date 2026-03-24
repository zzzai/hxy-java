# MiniApp Member Route Truth And Active / Planned Closure v1（2026-03-10）

## 1. 目标
- 目标：把 member 域历史 alias route、缺页结论与当前真实 uniapp route 重新对齐，明确哪些能力已经形成真实 runtime，哪些仍只能 `Can Develop / Cannot Release`。
- 边界：仅以当前分支实际存在的 `yudao-mall-uniapp/pages.json`、页面文件、member 前端 API、app controller 与 03-24 Member 缺页闭环结果为准。

## 2. 判定规则
1. `ACTIVE`
   - 有真实 uniapp route 或真实组件入口；
   - 有后端 app API；
   - 已属于既有稳定用户能力，可继续按当前发布主链维护。
2. `Can Develop / Cannot Release`
   - 页面、路由、前端绑定、controller 已形成真实 runtime；
   - 但发布证据、灰度/回滚材料、样本包或 A 窗口 release 审核尚未闭环；
   - 不得计入可放量能力或 `ACTIVE` 发布分母。
3. `PLANNED_RESERVED`
   - 仅有规划文档或保留态契约，尚未形成真实页面/真实 controller 闭环。
4. 历史 alias route 不再作为执行真值，只能作为迁移说明保留。

## 3. Member Route Truth 总表

| 能力 | 历史 alias route | 真实 route / 入口 | 当前状态 | 依据 | 说明 |
|---|---|---|---|---|---|
| 登录能力 | `/pages/public/login` | `component:s-auth-modal`; `/pages/index/login` | ACTIVE | `pages/index/login.vue`、member auth API | 日常登录入口以全局 auth modal 为主；`/pages/index/login` 主要承担 H5/社交登录回调，不应再冻结为 `/pages/public/login` |
| 个人中心 | `/pages/user/index` | `/pages/index/user` | ACTIVE | `pages/index/user.vue`、`pages.json` tabBar | 03-10 member 文档应统一改写为真实 tab 路由 |
| 资料编辑 | `-` | `/pages/user/info` | ACTIVE | `pages/user/info.vue` | 无 alias 漂移 |
| 安全设置 | `-` | `/pages/public/setting` | ACTIVE | `pages/public/setting.vue` | 无 alias 漂移 |
| 签到 | `/pages/user/sign-in` | `/pages/app/sign` | ACTIVE | `pages/app/sign.vue` | 03-10 member 文档应统一按 `/pages/app/sign` 描述 |
| 地址列表 | `/pages/address/list` | `/pages/user/address/list` | ACTIVE | `pages/user/address/list.vue` | 历史 alias 已废弃 |
| 地址编辑 | `/pages/address/edit`（隐含） | `/pages/user/address/edit` | ACTIVE | `pages/user/address/edit.vue` | 应与地址列表一起统一 |
| 钱包余额 | `-` | `/pages/user/wallet/money` | ACTIVE | `pages/user/wallet/money.vue` | 已有真实页面 |
| 积分钱包 | `/pages/point/wallet`（历史描述） | `/pages/user/wallet/score` | ACTIVE | `pages/user/wallet/score.vue` | 以真实钱包积分页为准 |
| 优惠券 | `/pages/coupon/center` | `/pages/coupon/list` | ACTIVE | `pages/coupon/list.vue` | alias route 不再作为执行真值 |
| 积分商城 | `/pages/point/mall` | `/pages/activity/point/list` | ACTIVE | `pages/activity/point/list.vue` | 03-10 member 文档中如引用积分商城，应统一改为真实 route |
| 等级页 | `/pages/user/level` | `/pages/user/level` | `Can Develop / Cannot Release` | `pages/user/level.vue`、`pages.json`、`pages/user/info.vue`、`/member/level/*` API | 真实页面、真实入口、真实 API 已闭环，但发布证据与样本包未闭环 |
| 资产总览页 | `/pages/profile/assets` | `/pages/profile/assets` | `Can Develop / Cannot Release` | `pages/profile/assets.vue`、`pages.json`、`pages/user/info.vue`、`AppMemberAssetLedgerController`、`/member/asset-ledger/page` | 页面与 controller 已落地；当前仍不可外推为可放量资产总账能力 |
| 标签页 | `/pages/user/tag` | `/pages/user/tag` | `Can Develop / Cannot Release` | `pages/user/tag.vue`、`pages.json`、`pages/user/info.vue`、`AppMemberTagController`、`/member/tag/my` | 页面、入口、app 读取接口已落地；仍缺 release 级样本与门禁材料 |

## 4. Active / Can Develop / Cannot Release / Planned 四分法

### 4.1 ACTIVE 能力
1. `member.auth-social`
   - 入口：`component:s-auth-modal` + `/pages/index/login`
   - API：`/member/auth/*`、`/member/social-user/*`
2. `member.profile-security`
   - 入口：`/pages/index/user`、`/pages/user/info`、`/pages/public/setting`
3. `member.sign-in`
   - 入口：`/pages/app/sign`
4. `member.address`
   - 入口：`/pages/user/address/list`、`/pages/user/address/edit`
5. `member.wallet-ledger`
   - 入口：`/pages/user/wallet/money`
6. `member.point-ledger`
   - 入口：`/pages/user/wallet/score`
7. 与 member 资产联动的真实路由：
   - `coupon` -> `/pages/coupon/list`
   - `point mall` -> `/pages/activity/point/list`

### 4.2 `Can Develop / Cannot Release` 能力
1. `member.level-progress`
   - 入口：`/pages/user/level`
   - API：`GET /member/level/list`、`GET /member/experience-record/page`
   - 当前边界：页面已可开发、可回归、可继续迭代，但未形成发布级样本包与 release decision。
2. `member.asset-overview`
   - 入口：`/pages/profile/assets`
   - API：`GET /member/asset-ledger/page`
   - 当前边界：真实页面与 controller 已存在；`degraded/degradeReason` 当前仅有默认字段输出，尚无真实灰度、回滚、降级证据闭环。
3. `member.user-tag`
   - 入口：`/pages/user/tag`
   - API：`GET /member/tag/my`
   - 当前边界：能展示真实标签列表，但未进入 release 样本、客服演练和放量签发范围。

### 4.3 PLANNED_RESERVED 能力
- 当前 member 域已无“controller 缺失型”的新增 `PLANNED_RESERVED` app API。
- 历史 `GET /member/asset-ledger/page` 自 2026-03-24 起已升为 `ACTIVE API/controller truth`，但对应页面 capability 仍只允许写成 `Can Develop / Cannot Release`。

## 5. 当前文档与真实 route 的冲突点

| 文档 | 旧写法 | 当前真值 | 处理结论 |
|---|---|---|---|
| `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 为缺页能力 | 三页均已形成真实 route + 页面 + 入口 | 必须回填为 `Can Develop / Cannot Release` |
| `docs/contracts/2026-03-10-miniapp-member-domain-contract-v1.md` | `/member/asset-ledger/page` 为 `PLANNED_RESERVED`，标签 app API 为 `N/A` | 资产总账 controller 与 `/member/tag/my` 已真实存在 | 必须回填 contract truth |
| `docs/plans/2026-03-11-miniapp-member-missing-page-activation-checklist-v1.md` | 仍按“缺页能力”写前置条件 | 工程缺页闭环已完成 | 必须改写为“已完成开发 / 剩余 release blocker” |

## 6. 对 Frozen / 开发的直接影响
1. Member 域“缺页 blocker”已从工程层解除，不再允许继续写“页面未落地”。
2. Member 域新的真实边界是：`level / assets / tag` 已完成开发，但仍不能进入发布放量、主成功率分母或 Frozen Candidate。
3. `/member/asset-ledger/page` 已不再是 `PLANNED_RESERVED API`；但“API 已有 controller”不等于“资产总账可放量”。
4. 当前若继续把这三项写成 `缺页能力`，会造成真实工程进度被低估；若把它们写成 `ACTIVE 可发布`，则会造成误放量。

## 7. 当前结论
1. Member 域 route truth 已从“缺页能力”升级到“真实 runtime 已落地”。
2. Member 域当前正确口径是：
   - 既有钱包 / 积分 / 券 / 地址 / 签到 / 登录保持 `ACTIVE`
   - `等级 / 资产总览 / 标签` 固定为 `Can Develop / Cannot Release`
3. 后续所有项目级总账、coverage matrix、release gate、客服 SOP 都必须以本文件作为 member route truth 的单一输入。
