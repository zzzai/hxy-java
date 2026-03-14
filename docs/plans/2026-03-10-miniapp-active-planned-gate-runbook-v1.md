# MiniApp ACTIVE and PLANNED_RESERVED Gate Runbook v1 (2026-03-10)

## 1. 目标
- 将会员域发布门禁拆成 `ACTIVE` 与 `PLANNED_RESERVED` 两套执行流程，避免“已上线能力”和“缺页/保留能力”混跑。
- 这份 runbook 额外固定三层状态：
  - `Doc Closed`：member 文档已收口
  - `Can Develop`：可以继续做真实页面、真实入口和真实样本
  - `Cannot Release`：在真页面、真入口、真 controller、真 gate 未闭环前不得进入主发布口径

## 2. 状态定义

| 状态 | 定义 | 当前会员域覆盖 |
|---|---|---|
| `ACTIVE` | 已正式对外、生效即参与主发布门禁 | 登录、个人中心/资料/设置、签到、地址、钱包、积分、券、积分商城 |
| `PLANNED_RESERVED` | 契约已冻结但默认关闭，只能在开关开启且命中灰度范围时返回 | 会员资产总账 `/member/asset-ledger/page` |
| `Missing Page` | 文档已齐，但当前仓内无真实页面和真实入口 | `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` |

## 3. ACTIVE 分母真值

### 3.1 允许计入 `ACTIVE` 验收分母
- `component:s-auth-modal`、`/pages/index/login`
- `/pages/index/user`、`/pages/user/info`、`/pages/public/setting`
- `/pages/app/sign`
- `/pages/user/address/list`、`/pages/user/address/edit`
- `/pages/user/wallet/money`、`/pages/user/wallet/score`
- `/pages/coupon/list`、`/pages/activity/point/list`

### 3.2 明确不得计入 `ACTIVE` 分母
- `/pages/user/level`
- `/pages/profile/assets`
- `/pages/user/tag`
- `/member/asset-ledger/page` 在 gate、controller、真实页面三件套未闭环前

### 3.3 为什么缺页能力不能算进分母
1. 缺页能力没有真实入口，无法形成真实用户样本。
2. 把缺页能力写进分母，会把 `Doc Closed` 伪装成 runtime `ACTIVE`。
3. `asset-ledger` 当前仍是 gate 保护能力，只能按 `PLANNED_RESERVED` 管理，不能冲主成功率。

## 4. 运行门禁流程

### 4.1 T-30 分钟：冻结输入
1. 冻结本次 `releaseId`、发布批次、灰度名单和开关快照。
2. 拉取会员域最近一个观察窗口数据。
3. 固定结构化检索键：`runId/orderId/payRefundId/sourceBizNo/errorCode`。
4. 先校验 `ACTIVE` 分母清单，确认缺页能力没有被带入报表。

### 4.2 ACTIVE 门禁

| 检查项 | 通过条件 | 失败动作 |
|---|---|---|
| 登录成功率 | `MEMBER_LOGIN_SUCCESS_RATE >= 97%` | 直接 `No-Go`，停止会员相关放量 |
| 地址可用率 | `MEMBER_ADDRESS_AVAILABILITY_RATE >= 99.50%` | 先降级地址写操作，再判定回滚 |
| 积分流水一致率 | `MEMBER_POINT_LEDGER_CONSISTENCY_RATE >= 99.90%` | 冻结签到/积分投放，`No-Go` |
| 分母纯度 | 只统计真实 `ACTIVE` 页面/入口 | 任一缺页能力混入主分母，直接 `No-Go` |
| 错误码 TopN | `1004001000/1004004000/1004008000` 无异常爆发 | 若 Top1 占比 > 40%，停止放量并拉起排障 |

### 4.3 PLANNED_RESERVED / Missing Page 门禁

| 检查项 | 通过条件 | 失败动作 |
|---|---|---|
| 开关状态 | `miniapp.asset.ledger` 与发布单一致 | 开关不一致直接 `No-Go` |
| 灰度范围 | 命中白名单用户/门店/请求头才允许返回保留能力 | 出现越权命中立即清空灰度名单 |
| 误返回检查 | 开关关闭时 `1004009901` 计数 `=0` | 任一命中即按误发布处置 |
| 缺页能力 | `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 仍停留在“缺页能力” | 被写成已上线页面时直接回退口径 |
| 账本一致率 | 开关开启时 `MEMBER_ASSET_LEDGER_CONSISTENCY_RATE >= 99.95%` | 关闭开关并回滚灰度 |

### 4.4 Go / No-Go 规则
1. 任一 P0 指标失败，直接 `No-Go`。
2. 任一缺页能力被记入 `ACTIVE` 分母，直接 `No-Go`。
3. 任一 `PLANNED_RESERVED` 误返回，直接 `No-Go`。
4. 只有文档闭环但 runtime 未闭环时，状态只能写成 `Doc Closed + Can Develop + Cannot Release`。

## 5. 误发布场景处置

### 5.1 发现
- 发现信号：
  - 开关关闭但日志/前端返回 `1004009901 MINIAPP_ASSET_LEDGER_MISMATCH`
  - 报表把缺页能力写成 `ACTIVE`
  - `ACTIVE` 发布后登录成功率、地址可用率、积分一致率在 2 个窗口内快速跌破阈值
- 发现动作：
  1. 固定采集 `releaseId + switchSnapshot + 五键`
  2. 在 5 分钟内确认影响范围：用户范围、门店范围、批次范围
  3. 拉起会员域 incident 频道并指定 Incident Commander

### 5.2 降级

| 场景 | 降级动作 | 目标 |
|---|---|---|
| 登录链路异常 | 停止灰度扩容，保留已登录会话 | 先稳住主入口 |
| 地址链路异常 | 将地址能力降为只读，关闭新增/编辑/删除入口配置 | 保留查询，不放大写故障 |
| 签到/积分异常 | 下线签到活动投放或奖励发放配置，仅保留查询和历史记录展示 | 阻断新增错误账 |
| 资产账本误发布 | 立即执行 `miniapp.asset.ledger=off`，关闭灰度名单 | 立刻止血保留主链路 |
| 缺页能力分母污染 | 立即回退报表和验收口径，清除缺页能力样本 | 恢复主分母纯度 |

### 5.3 回滚
1. `ACTIVE` 场景：
   - 若登录成功率或地址可用率 15 分钟内未恢复，回滚当前 `releaseId`
   - 若积分一致率持续不达标，回滚发布并暂停签到/积分相关运营活动
2. `PLANNED_RESERVED / Missing Page` 场景：
   - 立即关闭 `miniapp.asset.ledger`
   - 恢复上一稳定开关快照和灰度名单
   - 清理误写成 `ACTIVE` 的缺页能力口径

## 6. 审计要求
- 每次门禁决策必须落库：
  - `releaseId`
  - `gateType`（`ACTIVE`/`PLANNED_RESERVED`/`MISSING_PAGE`）
  - `result`（`GO/NO_GO/GO_WITH_WATCH`）
  - `switchSnapshot`
  - `runId/orderId/payRefundId/sourceBizNo/errorCode`
- 适用字段为空时不允许省略，按 `"0"` 字符串填充。

## 7. 验收标准
1. `ACTIVE`、`PLANNED_RESERVED`、`Missing Page` 的检查项、通过条件、失败动作明确。
2. 缺页能力为什么不能进入 `ACTIVE` 分母写清楚，并能直接执行。
3. 任一门禁结论都能反查到开关快照与五键审计信息。
