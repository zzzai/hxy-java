# MiniApp Member Domain Contract v1 (2026-03-10)

## 1. 目标与范围
- 目标：冻结会员 miniapp 契约，覆盖登录、等级、签到、积分、地址、标签、资产账本七个域。
- 范围限制：仅补齐契约文档，不修改 overlay 页面，不修改业务代码。
- 字段原则：请求/响应字段按当前 controller 与 VO 固定；新增字段只能增量追加，不得改名或改语义。

## 2. 状态边界
- `ACTIVE`：当前分支已有 controller 落地的接口。
- `PLANNED_RESERVED`：仅保留精确预留接口，不得进入当前发布口径。
- 本文件中的 `ACTIVE` 仅表示 API/controller truth；若真实 `pageRoute`、页面文件或页面入口缺失，仍不得外推为 `ACTIVE page capability`。
- 本版不纳入范围的 auth 辅助接口：`GET /member/auth/social-auth-redirect`、`POST /member/auth/create-weixin-jsapi-signature`。

## 3. 契约明细

### 3.1 登录与身份

| 接口 | 状态 | 请求字段 | 响应字段 | 错误码 | fail-open / fail-close | 兼容策略 |
|---|---|---|---|---|---|---|
| `POST /member/auth/login` | `ACTIVE` | `mobile`、`password`、`socialType?`、`socialCode?`、`socialState?` | `userId`、`accessToken`、`refreshToken`、`expiresTime`、`openid?` | `AUTH_LOGIN_BAD_CREDENTIALS(1004003000)`、`AUTH_LOGIN_USER_DISABLED(1004003001)`、`SOCIAL_USER_AUTH_FAILURE(1002018000)` | `FAIL_CLOSE`；账号或社交绑定异常直接阻断登录，不返回局部成功 | `social*` 字段为可选增量；不传时保持纯密码登录；`openid` 仅在社交绑定/社交登录场景返回 |
| `POST /member/auth/logout` | `ACTIVE` | Header/Query 透传访问令牌；无 body | `true` | `-`（无会员域专用业务码） | `FAIL_OPEN`；token 缺失、已失效时仍返回 `true`，保持幂等登出 | 兼容历史 token 取值方式：优先 header，允许回退 query 参数 |
| `POST /member/auth/refresh-token` | `ACTIVE` | `refreshToken`（query） | `userId`、`accessToken`、`refreshToken`、`expiresTime`、`openid=null` | `-`（依赖 OAuth2 公共鉴权错误，不新增会员域数值码） | `FAIL_CLOSE`；refreshToken 无效时不复用旧 token | 响应结构与登录保持同形，旧端可共用 token 刷新解析逻辑 |
| `POST /member/auth/sms-login` | `ACTIVE` | `mobile`、`code`、`socialType?`、`socialCode?`、`socialState?` | `userId`、`accessToken`、`refreshToken`、`expiresTime`、`openid?` | `AUTH_LOGIN_USER_DISABLED(1004003001)`、`SMS_CODE_NOT_FOUND(1002014000)`、`SMS_CODE_EXPIRED(1002014001)`、`SMS_CODE_USED(1002014002)`、`SOCIAL_USER_AUTH_FAILURE(1002018000)` | `FAIL_CLOSE`；验证码或绑定失败直接阻断 | 保持“手机号不存在时自动注册”现行为；`social*` 仍为可选附加字段 |
| `POST /member/auth/send-sms-code` | `ACTIVE` | `mobile?`、`scene` | `true` | `AUTH_MOBILE_USED(1004003007)`、`USER_MOBILE_NOT_EXISTS(1004001001)`、`SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY(1002014004)`、`SMS_CODE_SEND_TOO_FAST(1002014005)` | `FAIL_CLOSE`；发送失败不做静默成功 | `scene=MEMBER_UPDATE_PASSWORD` 时允许前端不传 `mobile`，服务端按当前登录用户手机号回填 |
| `POST /member/auth/validate-sms-code` | `ACTIVE` | `mobile`、`scene`、`code` | `true` | `SMS_CODE_NOT_FOUND(1002014000)`、`SMS_CODE_EXPIRED(1002014001)`、`SMS_CODE_USED(1002014002)` | `FAIL_CLOSE`；验证码校验失败不放行后续写操作 | 保持 4-6 位数字验证码约束；旧端只消费布尔结果仍兼容 |
| `POST /member/auth/social-login` | `ACTIVE` | `type`、`code`、`state` | `userId`、`accessToken`、`refreshToken`、`expiresTime`、`openid` | `AUTH_SOCIAL_USER_NOT_FOUND(1004003005)`、`USER_NOT_EXISTS(1004001000)`、`SOCIAL_USER_AUTH_FAILURE(1002018000)`、`SOCIAL_USER_NOT_FOUND(1002018001)` | `FAIL_CLOSE`；授权解析失败直接阻断 | 保持“未绑定则自动建会员并绑定”的现行为；响应中的 `openid` 为稳定增量字段 |
| `POST /member/auth/weixin-mini-app-login` | `ACTIVE` | `phoneCode`、`loginCode`、`state` | `userId`、`accessToken`、`refreshToken`、`expiresTime`、`openid` | `SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR(1002018200)`、`SOCIAL_USER_AUTH_FAILURE(1002018000)`、`USER_NOT_EXISTS(1004001000)` | `FAIL_CLOSE`；phoneCode/loginCode 任一异常直接阻断 | 与微信小程序现有一键登录流程保持一致；返回结构与其他登录接口同形 |
| `POST /api/front/wechat/authorize/program/login` | `ACTIVE` | Query：`code`；Body：`type?`、`spread_spid?`、`avatar?`、`nickName?`、`state?` | CRMEB 兼容包：`code`、`message?`、`data.type`、`data.token`、`data.uid`、`data.openid` | 内部复用 `AUTH_SOCIAL_USER_NOT_FOUND(1004003005)`、`USER_NOT_EXISTS(1004001000)`、`SOCIAL_USER_AUTH_FAILURE(1002018000)`；外部统一包裹为 `code=500` + `message` | `FAIL_CLOSE`；兼容层不会暴露数值错误码，只返回 CRMEB envelope | `state` 缺失时回退 `crmeb-compat-miniapp-login`；兼容层字段名保持 CRMEB 旧协议，不并入 `CommonResult` |
| `GET /member/user/get` | `ACTIVE` | 无 | `id`、`nickname`、`avatar`、`mobile`、`sex`、`point`、`experience`、`level{id,name,level,icon}?`、`brokerageEnabled` | `USER_NOT_EXISTS(1004001000)` | `FAIL_CLOSE`；登录态失效需重新鉴权；`level` 缺失时不额外报错 | `level` 为可选嵌套对象；无等级时返回 `null`，旧端可忽略 |

### 3.2 等级

| 接口 | 状态 | 请求字段 | 响应字段 | 错误码 | fail-open / fail-close | 兼容策略 |
|---|---|---|---|---|---|---|
| `GET /member/level/list` | `ACTIVE` | 无 | `list[]:{name,level,experience,discountPercent,icon,backgroundUrl}` | `-`（空列表为有效结果） | `FAIL_OPEN`；无可用等级时返回空列表而非业务错误 | 旧端只消费 `name/level` 仍兼容；新增展示字段按可选处理 |
| `GET /member/experience-record/page` | `ACTIVE` | `pageNo`、`pageSize` | `list[]:{title,experience,description,createTime}`、`total` | `LEVEL_NOT_EXISTS(1004011000)`、`EXPERIENCE_BIZ_NOT_SUPPORT(1004011201)`（成长配置异常时人工排查） | 查询主路径按 `FAIL_OPEN` 管理：空页有效；配置异常按错误码 `FAIL_CLOSE` | 分页结构保持 `PageResult`；新增字段只能追加到 `list[]` 元素，不得改动既有四字段 |

### 3.3 签到

| 接口 | 状态 | 请求字段 | 响应字段 | 错误码 | fail-open / fail-close | 兼容策略 |
|---|---|---|---|---|---|---|
| `GET /member/sign-in/config/list` | `ACTIVE` | 无 | `list[]:{day,point}` | `SIGN_IN_CONFIG_NOT_EXISTS(1004009000)`（规则缺失时按告警处理） | 常态 `FAIL_OPEN`；空规则列表按有效空态返回；规则配置损坏时 `FAIL_CLOSE` | 旧端只依赖 `day/point`；后续若追加奖励说明字段，必须保持可选 |
| `GET /member/sign-in/record/get-summary` | `ACTIVE` | 无 | `totalDay`、`continuousDay`、`todaySignIn` | `USER_NOT_EXISTS(1004001000)` | `FAIL_OPEN`；无签到记录时返回 `0/0/false`，不抛业务码 | 三个统计字段固定保留；禁止改名为文案字段 |
| `POST /member/sign-in/record/create` | `ACTIVE` | 无 | `day`、`point`、`experience`、`createTime` | `SIGN_IN_RECORD_TODAY_EXISTS(1004010000)`、`USER_NOT_EXISTS(1004001000)` | `FAIL_CLOSE`；重复签到直接阻断，不自动重试 | 当日奖励 `point/experience` 允许为 `0`；旧端不得依赖“签到成功必有奖励” |
| `GET /member/sign-in/record/page` | `ACTIVE` | `pageNo`、`pageSize` | `list[]:{day,point,experience,createTime}`、`total` | `USER_NOT_EXISTS(1004001000)` | `FAIL_OPEN`；空页视为有效结果 | 分页结构与历史 `PageResult` 一致；新增字段只能追加 |

### 3.4 积分

| 接口 | 状态 | 请求字段 | 响应字段 | 错误码 | fail-open / fail-close | 兼容策略 |
|---|---|---|---|---|---|---|
| `GET /member/point/record/page` | `ACTIVE` | `pageNo`、`pageSize`、`addStatus?`、`createTime?[]` | `list[]:{id,title,description,point,createTime}`、`total` | `POINT_RECORD_BIZ_NOT_SUPPORT(1004008000)`、`USER_POINT_NOT_ENOUGH(1004001003)`（资产修复/回放异常时人工排查） | 查询主路径按 `FAIL_OPEN` 管理：空页有效；积分映射异常按 `FAIL_CLOSE` | 老端只渲染 `title/point/createTime` 仍兼容；`description` 缺失时按空串处理 |

### 3.5 地址

| 接口 | 状态 | 请求字段 | 响应字段 | 错误码 | fail-open / fail-close | 兼容策略 |
|---|---|---|---|---|---|---|
| `POST /member/address/create` | `ACTIVE` | `name`、`mobile`、`areaId`、`detailAddress`、`defaultStatus` | `id` | `-`（仅参数校验错误） | `FAIL_CLOSE`；参数不合法直接阻断写入 | 维持当前创建后仅返回 `id`；不内联完整地址对象，避免破坏旧端 |
| `PUT /member/address/update` | `ACTIVE` | `id`、`name`、`mobile`、`areaId`、`detailAddress`、`defaultStatus` | `true` | `ADDRESS_NOT_EXISTS(1004004000)` | `FAIL_CLOSE`；地址不存在或无权操作时直接阻断 | 保持布尔响应；默认地址切换语义不变 |
| `DELETE /member/address/delete` | `ACTIVE` | `id` | `true` | `ADDRESS_NOT_EXISTS(1004004000)` | `FAIL_CLOSE`；删除不存在地址不做静默成功 | 保持 query `id` 传参，不切换为 body |
| `GET /member/address/get` | `ACTIVE` | `id` | `id`、`name`、`mobile`、`areaId`、`detailAddress`、`defaultStatus`、`areaName` | `-`（当前实现不存在时返回 `null`） | `FAIL_OPEN`；地址缺失返回 `null`，由前端回退列表页 | 旧端不得把 `null` 视为协议破坏；若未来改为抛错，需先双轨兼容 |
| `GET /member/address/get-default` | `ACTIVE` | 无 | `id`、`name`、`mobile`、`areaId`、`detailAddress`、`defaultStatus`、`areaName` 或 `null` | `-`（无默认地址返回 `null`） | `FAIL_OPEN`；无默认地址是合法空态 | 保持 `null` 语义，不回填伪默认地址 |
| `GET /member/address/list` | `ACTIVE` | 无 | `list[]:{id,name,mobile,areaId,detailAddress,defaultStatus,areaName}` | `-`（空列表为有效结果） | `FAIL_OPEN`；无地址返回空列表 | 列表元素字段与详情对齐；新增字段仅允许追加 |

### 3.6 标签

| 接口 | 状态 | 请求字段 | 响应字段 | 错误码 | fail-open / fail-close | 兼容策略 |
|---|---|---|---|---|---|---|
| `GET /member/tag/my` | `ACTIVE` | 无 | `list[]:{id,name}` | `-`（空列表为有效结果；登录失效按统一鉴权处理） | `FAIL_OPEN`；无标签返回 `[]`，不伪造静态标签；鉴权失败仍由统一认证链路 `FAIL_CLOSE` | 返回顺序按用户 `tagIds` 顺序对齐；新增字段只能增量追加 |

### 3.7 资产账本

| 接口 | 状态 | 请求字段 | 响应字段 | 错误码 | fail-open / fail-close | 兼容策略 |
|---|---|---|---|---|---|---|
| `GET /member/asset-ledger/page` | `ACTIVE` | `assetType?`、`pageNo`、`pageSize` | `list[]:{ledgerId,assetType,bizType,title,description,amount,balanceAfter,sourceBizNo,runId,createTime}`、`total`、`degraded`、`degradeReason?` | `-`（当前未核出稳定 member 专属业务码；登录失效按统一鉴权处理） | 当前真实语义是“成功则返回完整聚合结果，失败直接请求失败”；`degraded=false` / `degradeReason=null` 仅为默认输出，不代表已验证真实降级链路 | controller 已真实落地在 `yudao-server` 集成层；请求不再传 `memberId`，统一按当前登录用户聚合；响应字段只能增量追加，不得替换现有钱包/积分/券页面 |

## 4. `Can Develop / Cannot Release` page capability 边界

| 能力 | 真实 pageRoute / 页面文件 | 当前 API/字段真值 | 明确禁止误写为 | Doc Closed | Contract Closed | Runtime Not Proven | Release Blocked |
|---|---|---|---|---|---|---|---|
| 等级页 `/pages/user/level` | 已有 `pages/user/level.vue` 与 `pages.json` 入口 | `GET /member/level/list`、`GET /member/experience-record/page`；`list[]:{name,level,experience,discountPercent,icon,backgroundUrl}`、`list[]:{title,experience,description,createTime}`、`total` | “等级页已放量”“等级页已进入 ACTIVE 发布分母” | `Yes` | `Yes` | `No` | `Yes` |
| 统一资产总账 `/pages/profile/assets` | 已有 `pages/profile/assets.vue` 与 `pages.json` 入口 | `GET /member/asset-ledger/page`；`list[]:{ledgerId,assetType,bizType,title,description,amount,balanceAfter,sourceBizNo,runId,createTime}`、`total`、`degraded`、`degradeReason?` | “资产总览页已可放量”“灰度 / 降级 / 回滚已工程闭环” | `Yes` | `Yes` | `No` | `Yes` |
| 标签页 `/pages/user/tag` | 已有 `pages/user/tag.vue` 与 `pages.json` 入口 | `GET /member/tag/my`；`list[]:{id,name}` | “标签页已放量”“标签能力已完成 release 审核” | `Yes` | `Yes` | `No` | `Yes` |

说明：
- `GET /member/level/list` 与 `GET /member/experience-record/page` 已与真实等级页闭环，但当前只能写成 `Can Develop / Cannot Release` 页面能力。
- `GET /member/asset-ledger/page` 的 `degraded/degradeReason` 已有字段输出，但当前仅是默认值字段，不是发布级降级证据。
- `GET /member/tag/my` 已形成真实 app 读取契约，但空列表与已放量是两回事。

## 5. 跨接口兼容约束
- 错误驱动必须按数值码，不按 message 文本分支。
- `PageResult` 结构固定为 `list[] + total`；不得在本版引入新的分页包装。
- `CommonResult` 与 CRMEB 兼容包并存：
  - 标准会员接口使用 `CommonResult`.
  - `/api/front/wechat/authorize/program/login` 固定使用 `CrmebCompatResult`。
- 任何仍处于 `PLANNED_RESERVED` 的接口不得被前端当作已上线能力默认调用。
- 任何 `Can Develop / Cannot Release` 页面能力不得因 route、页面、controller 已存在，就被统计为可放量 capability。

## 6. 发布联调约束
- A 窗口：统一把登录态、`openid`、`degraded`、`errorCode` 当成显式字段，不得靠 message 猜测状态。
- B 窗口：对地址 `null/[]`、签到 `0/0/false`、标签空列表、资产总账空列表都必须有稳定 UI 分支。
- D 窗口：门禁必须区分 `ACTIVE API/controller truth` 与 `Can Develop / Cannot Release page capability`；不能再把 `/member/asset-ledger/page` 误判为 `PLANNED_RESERVED`，也不能把它误判为 release-ready。
- A/B/D 都不得把 `/member/level/list`、`/member/experience-record/page`、`/member/tag/my`、`/member/asset-ledger/page` 直接外推成“已可放量页面能力”；这些页面当前统一按本文件第 4 节管理。
