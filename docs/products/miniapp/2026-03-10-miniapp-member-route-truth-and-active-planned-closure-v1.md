# MiniApp Member Route Truth And Active / Planned Closure v1（2026-03-10）

## 1. 目标
- 目标：把 03-10 member 文档中的历史 alias route 校正到当前真实 uniapp route，并明确 `ACTIVE / PLANNED_RESERVED / 缺页能力` 三分法。
- 边界：仅以当前分支实际存在的 `yudao-mall-uniapp/pages.json`、页面文件、member 前端 API 与 03-10 member 文档为准。

## 2. 判定规则
1. `ACTIVE`
   - 有真实 uniapp route 或真实组件入口；
   - 有后端 app API；
   - 有产品/契约/错误码/运行文档口径。
2. `PLANNED_RESERVED`
   - 能力有部分文档或后端接口，但缺真实页面入口，或受开关/预留门禁保护。
3. `缺页能力`
   - 文档中声明存在页面，但当前分支没有对应 `pageRoute` 或页面文件。
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
| 等级页 | `/pages/user/level` | `N/A` | 缺页能力 | 当前分支无 `pages/user/level.vue` | 后端 level 相关接口存在，但缺真实页面，不能记 `ACTIVE` |
| 资产总览页 | `/pages/profile/assets` | `N/A` | 缺页能力 | 当前分支无 `pages/profile/assets.vue` | 只能保留为规划能力，不得冻结为 Active 页面 |
| 标签页 | `/pages/user/tag` | `N/A` | 缺页能力 | 当前分支无 `pages/user/tag.vue` | 当前默认隐藏，不得作为真实 route |

## 4. Active / Planned / 缺页能力 三分法

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

### 4.2 PLANNED_RESERVED 能力
1. `member.asset-overview`
   - 文档存在，但 `/pages/profile/assets` 缺页。
2. `member.asset-ledger-api`
   - `GET /member/asset-ledger/page`
   - 当前仍受 `miniapp.asset.ledger` 预留开关保护，且无 controller 落地。
3. 任何依赖 `/pages/profile/assets` 聚合呈现的能力，在真实页面落地前都不能升 `ACTIVE`。

### 4.3 缺页能力
1. `/pages/user/level`
2. `/pages/profile/assets`
3. `/pages/user/tag`

这些能力当前只能作为“文档规划能力”存在，不能用于 Frozen、验收、联调脚本、埋点口径或发布能力统计。

## 5. 当前文档与真实 route 的冲突点

| 文档 | 当前写法 | 真实 route truth | 处理结论 |
|---|---|---|---|
| `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | `/pages/public/login` | `component:s-auth-modal`; `/pages/index/login` | 需校正 |
| `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | `/pages/user/index` | `/pages/index/user` | 需校正 |
| `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md` | `/pages/user/sign-in` | `/pages/app/sign` | 需校正 |
| `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md` | `/pages/address/list` | `/pages/user/address/list` | 应保留为 alias 迁移说明，不再作为执行真值 |
| `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md` | `/pages/point/mall` | `/pages/activity/point/list` | 同上 |
| 03-10 member 相关文档 | `/pages/user/level`、`/pages/profile/assets`（若按已上线页面描述） | 当前无真实页面 | 一律回退为缺页能力 |

## 6. 对 Frozen / 开发的直接影响
1. 03-10 member 文档包当前最多只能保持 `Ready`，不能进入 `Frozen`。
2. 若后续继续以 alias route 写验收、埋点、回归路径，会污染 `capability ledger` 与 `release gate`。
3. `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 在页面落地前不得进入开发完成口径，不得被统计为 `ACTIVE`。
4. `/member/asset-ledger/page` 在 controller 与页面都落地前，只能保留为 `PLANNED_RESERVED`。

## 7. 当前结论
1. Member 域的主要问题已不再是“缺文档”，而是“route truth 未归一”。
2. A 侧单一真值应以本文件替代历史 alias route 描述。
3. 后续 B/C/D 如补充 member 相关文档，应全部以本文件列出的真实 route 和三分法为准。
