# MiniApp Member Missing-Page Activation Checklist v1 (2026-03-11)

## 1. 目标与适用范围
- 目标：把 member 域当前的“缺页能力”固化成可执行激活清单，防止在真实页面、真实入口和真实 API 未闭环前，把 `level / asset hub / tag` 写成已上线能力。
- 适用范围：
  - 当前真实 member 页面、组件入口、`pages.json`
  - 当前缺页能力：`/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag`
  - 相关 API：`/member/level/list`、`/member/experience-record/page`、`/member/asset-ledger/page`
- 对齐基线：
  - `docs/products/miniapp/2026-03-10-miniapp-member-route-truth-and-active-planned-closure-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`
  - `docs/contracts/2026-03-10-miniapp-member-domain-contract-v1.md`
  - `docs/contracts/2026-03-10-miniapp-active-vs-planned-api-matrix-v1.md`
  - `docs/products/miniapp/2026-03-10-miniapp-capability-status-ledger-v1.md`

## 2. 当前真实 `ACTIVE` 入口
- 登录 / 社交登录：`component:s-auth-modal`、`/pages/index/login`
- 个人中心 / 资料 / 安全设置：`/pages/index/user`、`/pages/user/info`、`/pages/public/setting`
- 签到：`/pages/app/sign`
- 地址：`/pages/user/address/list`、`/pages/user/address/edit`
- 钱包 / 积分 / 券 / 积分商城：`/pages/user/wallet/money`、`/pages/user/wallet/score`、`/pages/coupon/list`、`/pages/activity/point/list`

这些入口允许继续按 `ACTIVE` 管理，但不能外推到缺页能力。

## 3. 当前缺页能力清单

| 能力 | 当前 route | 当前状态 | API 边界 | 说明 |
|---|---|---|---|---|
| 等级页 | `/pages/user/level` | 缺页能力 | `/member/level/list`、`/member/experience-record/page` | 当前仓内无真实页面文件与 `pages.json` 入口 |
| 资产总览页 | `/pages/profile/assets` | 缺页能力 | `/member/asset-ledger/page` | 当前仓内无真实页面文件，且资产总账接口仍受 gate 保护 |
| 标签页 | `/pages/user/tag` | 缺页能力 | 当前无 app 端读取接口 | 当前默认隐藏，不允许发未知 app API |

## 4. 激活前置清单

### 4.1 等级页 `/pages/user/level`
- [ ] 新增真实页面文件并进入 `pages.json`
- [ ] 页面真实入口被个人中心或其他已发布入口消费
- [ ] 页面只读取真实接口：`/member/level/list`、`/member/experience-record/page`
- [ ] PRD、contract、field dictionary、errorcopy 与真实 route 一致
- [ ] 验收样本包括：正常查看、无等级记录、经验记录空页、接口失败

### 4.2 资产总览页 `/pages/profile/assets`
- [ ] 新增真实页面文件并进入 `pages.json`
- [ ] `miniapp.asset.ledger` 开关策略写入发布口径
- [ ] 真实后端 controller 与前端读取链路同时存在
- [ ] `wallet / point / coupon` 聚合逻辑与单资产页面不冲突
- [ ] 验收样本包括：聚合成功、单项为空、账本为空、账本 gate 关闭态

### 4.3 标签页 `/pages/user/tag`
- [ ] 新增真实页面文件并进入 `pages.json`
- [ ] app 端存在正式读取接口并进入 contract
- [ ] 标签默认隐藏 / 展示开关进入 capability ledger
- [ ] 标签展示、无标签、标签读取失败均有用户口径
- [ ] 未补读取接口前禁止补假数据或静态标签页

## 5. 明确禁止的表述
- 不得把缺页能力写成“已上线页面，只待联调”。
- 不得把 `/member/asset-ledger/page` 仅因文档完备就升为 `ACTIVE`。
- 不得把 `component:s-auth-modal`、`/pages/index/user`、`/pages/app/sign` 的真实入口误迁移回 alias route。
- 不得在没有 `pages.json` 入口时把页面计入发版范围。

## 6. 文档同步要求
以下项在任一缺页能力激活前必须同步更新：
1. capability ledger
2. domain doc coverage matrix
3. final review
4. freeze review
5. release decision pack
6. member PRD / contract / field dictionary / errorcopy

## 7. 必测样本

| 能力 | 必测样本 |
|---|---|
| 等级页 | 正常等级、无等级记录、经验空页、接口失败 |
| 资产总览页 | 聚合成功、单资产为空、总账为空、gate 关闭、gate 灰度命中 |
| 标签页 | 正常读取、无标签、读取失败、入口关闭 |

## 8. 退出条件
只有同时满足以下条件，member 缺页能力才允许进入下一阶段：
1. 真实页面文件存在并进入 `pages.json`。
2. 页面入口已经从真实已发布路径可达。
3. 真实 API、错误码、恢复动作与页面完全对应。
4. capability ledger 与 release decision 同步改写。
5. A 窗口重新评审后，才允许从“缺页能力”转为 `Ready` 或 `Frozen Candidate`。
