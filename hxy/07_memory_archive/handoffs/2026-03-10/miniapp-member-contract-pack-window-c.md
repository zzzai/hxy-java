# Window C Handoff - MiniApp Member Contract Pack

- Date: 2026-03-10
- Branch: `feat/ui-four-account-reconcile-ops`
- Scope: 会员域契约补齐（Active/Planned API Matrix、Domain Contract、ErrorCode Register、handoff）

## 1. 本批交付

新增文档：
1. `docs/contracts/2026-03-10-miniapp-active-vs-planned-api-matrix-v1.md`
2. `docs/contracts/2026-03-10-miniapp-member-domain-contract-v1.md`

更新文档：
1. `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`

新增 handoff：
1. `hxy/07_memory_archive/handoffs/2026-03-10/miniapp-member-contract-pack-window-c.md`

## 2. 关键结论

- 会员域 `ACTIVE` 能力已经按精确 `method + path + controllerPath` 固定，不再接受通配路由口径。
- `/member/asset-ledger/page` 保持 `PLANNED_RESERVED`：
  - 绑定开关 `miniapp.asset.ledger`
  - 当前分支无落地 controller
  - 不得进入 `ACTIVE` 发布口径
- CRMEB 兼容登录接口 `/api/front/wechat/authorize/program/login` 继续保留 `CrmebCompatResult` 包装，不与 `CommonResult` 合流。

## 3. 会员域错误码补充

- 新增/补齐了会员域与会员登录依赖域错误码条目：
  - `USER_MOBILE_NOT_EXISTS(1004001001)`
  - `USER_MOBILE_USED(1004001002)`
  - `USER_POINT_NOT_ENOUGH(1004001003)`
  - `AUTH_LOGIN_BAD_CREDENTIALS(1004003000)`
  - `AUTH_LOGIN_USER_DISABLED(1004003001)`
  - `AUTH_SOCIAL_USER_NOT_FOUND(1004003005)`
  - `AUTH_MOBILE_USED(1004003007)`
  - `ADDRESS_NOT_EXISTS(1004004000)`
  - `SIGN_IN_CONFIG_NOT_EXISTS(1004009000)`
  - `SIGN_IN_RECORD_TODAY_EXISTS(1004010000)`
  - `LEVEL_NOT_EXISTS(1004011000)`
  - `EXPERIENCE_BIZ_NOT_SUPPORT(1004011201)`
  - `SMS_CODE_NOT_FOUND/EXPIRED/USED/EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY/SEND_TOO_FAST`
  - `SOCIAL_USER_AUTH_FAILURE/SOCIAL_USER_NOT_FOUND/SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR`
- 自动重试边界已明确：
  - 会员域仅 `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901)` 允许后台 `BG_RETRY_JOB`
  - 其余新增会员域错误码均不允许自动重试
  - `AUTH_LOGIN_USER_DISABLED`、`POINT_RECORD_BIZ_NOT_SUPPORT`、`SIGN_IN_CONFIG_NOT_EXISTS`、`LEVEL_NOT_EXISTS`、`EXPERIENCE_BIZ_NOT_SUPPORT` 必须人工接管

## 4. A / B / D 联调提示

- A 窗口
  - 登录链路统一消费显式字段：`userId/accessToken/refreshToken/expiresTime/openid`
  - 兼容层登录单独处理 `CrmebCompatResult.code=200/500`，不要按 `CommonResult.code` 解析
  - 资产账本仍是 `PLANNED_RESERVED`，不得把 `/member/asset-ledger/page` 计入上线清单

- B 窗口
  - 地址接口当前真实语义：
    - `GET /member/address/get` 不存在时返回 `null`
    - `GET /member/address/get-default` 无默认地址时返回 `null`
    - `GET /member/address/list` 无地址时返回 `[]`
  - 签到统计无记录时返回 `totalDay=0/continuousDay=0/todaySignIn=false`
  - 重复签到 `SIGN_IN_RECORD_TODAY_EXISTS(1004010000)` 必须 fail-close，不做按钮级静默重放

- D 窗口
  - 发布门禁必须把 `ACTIVE_SET` 与 `RESERVED_SET` 分开扫描
  - `PLANNED_RESERVED` 接口一旦进入 smoke allowlist 或 OpenAPI 对外基线，固定阻断
  - `miniapp.asset.ledger=off` 条件下出现 `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901)`，按 P1 处理

## 5. 验证命令

1. `git diff --check`
2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
