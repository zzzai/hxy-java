# MiniApp 会员全域 PRD v1（2026-03-10）

## 0. 文档定位
- 目标：补齐小程序“会员全域”执行文档，让页面可实现、运营可执行、客服可解释。
- 分支：`feat/ui-four-account-reconcile-ops`
- 约束：
  - 不新增业务代码口径，不反向修改既有错误码语义。
  - 仅使用真实存在或已规划明确的能力；未开放能力必须标记 `PLANNED_RESERVED`。
  - 降级场景禁止伪成功，前端分支条件只允许使用 `errorCode/degraded/degradeReason`。
  - 若缺少真实对外暴露证据，不在 PRD 中承诺稳定错误码分支。
- 对齐基线：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-08-miniapp-unified-state-machine-v1.md`
  - `docs/contracts/2026-03-08-miniapp-errorcode-recovery-matrix-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`

## 0.1 会员域能力总览

| 功能 | 页面路由 | 能力状态 | 核心 API/依赖 | 说明 |
|---|---|---|---|---|
| 登录/注册 | `component:s-auth-modal`; `/pages/index/login` | `ACTIVE` | `/member/auth/*` | 本期不拆独立注册页；首次短信登录/微信手机号登录自动注册 |
| 个人中心 | `/pages/index/user` | `ACTIVE` | `/member/user/get` `/member/user/update` | 展示会员卡、资料入口、资产入口、订单入口 |
| 等级 | `/pages/user/level` | `Doc Closed / Can Develop / Cannot Release` | `/member/level/list` `/member/experience-record/page` `/member/user/get` | 页面、路由、入口已落地，可继续回归和迭代；当前仍不可放量 |
| 签到 | `/pages/app/sign` | `ACTIVE` | `/member/sign-in/config/list` `/member/sign-in/record/*` | 日历签到、连续签到、奖励回显 |
| 积分 | `/pages/user/wallet/score`; `/pages/activity/point/list` | `ACTIVE` | `/member/point/record/page` `/promotion/point-activity/*` | 积分流水与积分商城联动 |
| 标签 | `/pages/user/tag` | `Doc Closed / Can Develop / Cannot Release` | `/member/tag/my` | 页面与 app 读取接口已落地；当前仍不可放量 |
| 地址 | `/pages/user/address/list` | `ACTIVE` | `/member/address/*` | 列表、默认地址、新增、编辑、删除 |
| 会员资产展示 | `/pages/user/wallet/money`; `/pages/coupon/list`; `/pages/profile/assets` | `分资产 ACTIVE + 统一总账 Doc Closed / Can Develop / Cannot Release` | 钱包/券/积分现有接口 + `/member/asset-ledger/page` | 统一总账真实页面/controller 已落地；灰度、回滚、发布样本未闭环 |

## 0.2 微信登录/手机号能力核验日志
- 核验日期：`2026-03-10`
- 触发范围：`login`、`phone`
- 官方页面：
  - `https://developers.weixin.qq.com/miniprogram/dev/api/open-api/login/wx.login.html`
  - `https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html`
  - `https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/getPhoneNumber.html`
- 本文冻结约束：
  1. 微信一键登录必须成对描述 `wx.login` 前端取 `loginCode` 与服务端换取 `openid/session_key`。
  2. 手机号授权必须成对描述前端 `getPhoneNumber` 返回 `phoneCode` 与服务端 `phonenumber.getPhoneNumber` 解码。
  3. “已拿到手机号”不等于“已建立登录态”；文档与页面状态机必须拆开表达。
  4. 微信授权失败只能按错误码/降级语义处理，不按 message 分支兜底。

## 1. 登录/注册（`ACTIVE`）

### 1.1 用户目标
- 老用户快速登录并恢复订单、资产、预约等历史状态。
- 新用户在不跳转独立注册页的情况下完成首登注册。
- 客服能明确解释“密码登录、短信登录、微信手机号登录”的差异与恢复动作。

### 1.2 主流程
1. 用户通过 `component:s-auth-modal` 或 `/pages/index/login` 进入登录流，默认展示“短信登录/微信一键登录/密码登录”三入口。
2. 短信登录：
   - 先调用 `POST /member/auth/send-sms-code`，`scene=1`。
   - 用户输入验证码后调用 `POST /member/auth/sms-login`。
   - 若手机号未注册，服务端自动创建会员并返回 `accessToken/refreshToken/expiresTime`。
3. 微信一键登录：
   - 前端先取 `loginCode`，再在用户点击手机号授权后取得 `phoneCode`。
   - 调用 `POST /member/auth/weixin-mini-app-login`，必传 `phoneCode/loginCode/state`。
   - 若手机号未注册，服务端自动创建会员并绑定微信小程序社交关系。
4. 密码登录：
   - 调用 `POST /member/auth/login`。
   - 登录成功后进入个人中心或用户原始意图页。
5. 登录成功后前端持久化 `accessToken/refreshToken/expiresTime`，并记录来源方式用于埋点与客服排查。

### 1.3 异常流程
- `AUTH_LOGIN_BAD_CREDENTIALS(1004003000)`：密码错误，阻断登录，保留手机号输入值，不清空页面上下文。
- `AUTH_LOGIN_USER_DISABLED(1004003001)`：账号被禁用，阻断登录，只给“联系客服”主动作。
- `SMS_CODE_NOT_FOUND/EXPIRED/USED(1002014000/1002014001/1002014002)`：短信验证码无效，阻断登录，不允许成功动效。
- `SMS_CODE_SEND_TOO_FAST(1002014005)` 或 `SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY(1002014004)`：发送过频或超上限，只允许倒计时/第二恢复动作，不走 message 猜测。
- `SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR(1002018200)`：手机号授权码失效，回退到授权前状态，要求重新发起手机号授权。
- `SOCIAL_USER_AUTH_FAILURE(1002018000)` 或 `AUTH_SOCIAL_USER_NOT_FOUND(1004003005)`：微信登录链路失败，阻断登录，不建立半登录态。

### 1.4 状态机依赖

| 依赖对象 | 真值状态 | 页面规则 |
|---|---|---|
| 登录页本地状态 | `ANONYMOUS -> CODE_SENT -> CODE_VERIFIED -> AUTHENTICATED` | 只有进入 `AUTHENTICATED` 才允许跳转个人中心 |
| 会员账号状态 | `ENABLE / DISABLE` | `DISABLE` 直接阻断所有登录方式 |
| OAuth2 令牌 | `VALID -> REFRESHING -> EXPIRED -> LOGGED_OUT` | 刷新中不重复弹登录；过期后才回退登录页 |
| 微信手机号授权 | `UNAUTHORIZED -> PHONE_CODE_READY -> CONSUMED` | `PHONE_CODE_READY` 不代表登录成功 |

### 1.5 Owner
- 产品：Member Domain Owner
- 后端：Member/Auth Owner
- 前端：MiniApp FE Owner
- 客服/运营：会员运营 + 客服培训 Owner

### 1.6 运营/客服口径
- 运营口径：本期“注册”定义为首次短信登录或首次微信手机号登录自动建档，不新增独立注册漏斗。
- 客服口径：
  - “短信登录失败”先核对验证码是否超时，再引导重试。
  - “微信手机号授权失败”先让用户重新拉起授权，不可直接承诺已登录成功。
  - 同一手机号首次成功登录即视为已注册。

### 1.7 验收口径
- [ ] `component:s-auth-modal` 与 `/pages/index/login` 能承接密码、短信、微信三种登录方式，且状态切换不冲突。
- [ ] 首次短信登录、首次微信手机号登录都能自动注册并返回同结构登录态字段。
- [ ] `phoneCode/loginCode/state` 缺一不可；接口文档与页面校验一致。
- [ ] 任一错误码路径都不出现“登录成功”“欢迎回来”等成功态文案或动效。
- [ ] `AUTH_LOGIN_USER_DISABLED` 只允许“联系客服”，不允许继续尝试业务动作。

## 2. 个人中心（`ACTIVE`）

### 2.1 用户目标
- 统一查看昵称、头像、手机号、积分、经验、等级、钱包、券、签到等会员信息。
- 在同一入口完成资料修改、手机号换绑、密码修改与资产跳转。
- 客服能从个人中心字段快速判断用户是否已登录、等级是否生效、手机号是否换绑成功。

### 2.2 主流程
1. 用户进入 `/pages/index/user`，先调用 `GET /member/user/get` 获取个人信息卡。
2. 页面展示：
   - 基础信息：`nickname/avatar/mobile/sex`
   - 会员信息：`point/experience/level`
   - 资产入口：钱包、积分、优惠券、地址、签到、等级
3. 用户编辑资料时调用 `PUT /member/user/update` 更新 `nickname/avatar/sex`。
4. 用户换绑手机号：
   - 短信换绑：`PUT /member/user/update-mobile`
   - 微信手机号换绑：`PUT /member/user/update-mobile-by-weixin`
5. 用户修改密码或忘记密码时跳转设置页，分别调用 `PUT /member/user/update-password` 与 `PUT /member/user/reset-password`。

### 2.3 异常流程
- `USER_NOT_EXISTS(1004001000)`：当前登录态失效或用户不存在，回退登录页。
- `USER_MOBILE_USED(1004001002)` 或 `AUTH_MOBILE_USED(1004003007)`：目标手机号已绑定他人，阻断换绑。
- `USER_MOBILE_NOT_EXISTS(1004001001)`：忘记密码手机号未注册，阻断重置。
- `SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR(1002018200)`：微信换绑手机号失败，停留在原手机号状态。
- 短信验证码相关错误：只影响当前设置动作，不影响个人中心主页面保活。

### 2.4 状态机依赖

| 依赖对象 | 真值状态 | 页面规则 |
|---|---|---|
| 登录态 | `AUTHENTICATED / EXPIRED / LOGGED_OUT` | 个人中心为鉴权页，失效即回退登录 |
| 会员资料编辑 | `VIEW -> EDITING -> SAVED` | 只有 `SAVED` 才更新顶部卡片展示 |
| 手机绑定 | `BOUND -> UPDATING -> BOUND` | 换绑失败时回滚旧手机号展示 |
| 等级卡片 | `NO_LEVEL / LEVEL_ACTIVE` | `level=null` 时展示普通会员兜底卡片 |

### 2.5 Owner
- 产品：Member Domain Owner
- 后端：Member User Owner
- 前端：MiniApp FE Owner
- 运营/客服：会员运营 + 客服培训 Owner

### 2.6 运营/客服口径
- 运营口径：个人中心顶部卡片以 `GET /member/user/get` 返回为唯一真值，不做本地拼接猜测。
- 客服口径：
  - 若昵称/头像未更新，先引导用户刷新页面一次，再按错误码排查。
  - 若换绑失败，必须确认错误码后再判断是“验证码问题”还是“手机号已占用”。

### 2.7 验收口径
- [ ] 个人中心首屏字段和 `/member/user/get` 完全对齐，不自行组装 level 文案。
- [ ] 资料修改成功后刷新卡片成功；失败时原数据保留。
- [ ] 手机号换绑失败不污染旧手机号展示。
- [ ] 忘记密码路径与登录页/短信场景一致，不额外发明前端校验规则。
- [ ] `USER_NOT_EXISTS` 统一回退登录页，不停留半鉴权状态。

## 3. 等级页（`Doc Closed / Can Develop / Cannot Release`）

### 3.1 当前真值
- 当前仓内已存在 `/pages/user/level` 页面文件，并已进入 `pages.json`。
- 当前真实入口为：`/pages/user/info -> 会员等级`。
- 当前页面只读取三条真实链路：
  - `GET /member/user/get`
  - `GET /member/level/list`
  - `GET /member/experience-record/page`

### 3.2 主流程
1. 用户从个人资料页进入 `/pages/user/level`。
2. 页面先读取 `GET /member/user/get`，展示当前等级、经验值和下一等级差值。
3. 页面读取 `GET /member/level/list`，展示等级权益列表与当前命中等级。
4. 页面读取 `GET /member/experience-record/page`，展示成长记录、空态和分页加载。

### 3.3 当前仍不可放量的原因
1. 等级页已形成真实 runtime，但发布级样本与 allowlist 未闭环。
2. 客服 / 运营对“无等级记录”“经验空页”“接口失败”的统一演练尚未补齐发布证据。
3. A 窗口未重新签发 release decision 前，等级页只能按 `Can Develop / Cannot Release` 管理。

### 3.4 放量进入条件
- 正常等级、`level=null`、经验空页、接口失败样本全部可回放。
- field dictionary、errorcopy、release gate、客服 SOP 与真实页面字段一致。
- A 窗口重新签发 allowlist 前，等级页持续保持 `Cannot Release`。

### 3.5 产品侧最终标签

| 能力 | Doc Closed | Engineering Blocked | Can Develop | Cannot Release |
|---|---|---|---|---|
| `/pages/user/level` | Yes | No | Yes | Yes |

## 4. 签到（`ACTIVE`）

### 4.1 用户目标
- 明确今天是否已签到、连续签到天数、可获得奖励。
- 用最少点击完成每日签到，并能回看历史签到收益。

### 4.2 主流程
1. 用户进入 `/pages/app/sign`。
2. 页面读取：
   - `GET /member/sign-in/config/list`：签到奖励规则
   - `GET /member/sign-in/record/get-summary`：总签到天数、连续天数、今日状态
   - `GET /member/sign-in/record/page`：签到历史
3. 今日未签到时，按钮可点击；点击后调用 `POST /member/sign-in/record/create`。
4. 签到成功后更新：
   - `todaySignIn=true`
   - `continuousDay` 与 `totalDay`
   - 积分/经验回显

### 4.3 异常流程
- `SIGN_IN_RECORD_TODAY_EXISTS(1004010000)`：今日已签到，按钮变灰，不允许再次成功动效。
- `SIGN_IN_CONFIG_NOT_EXISTS(1004009000)`：规则缺失，隐藏奖励梯度，仅保留签到历史。
- `USER_NOT_EXISTS(1004001000)`：登录态失效，回退登录页。

### 4.4 状态机依赖

| 依赖对象 | 真值状态 | 页面规则 |
|---|---|---|
| 当日签到 | `UNSIGNED_TODAY -> SIGNED_TODAY` | 进入 `SIGNED_TODAY` 后按钮禁用 |
| 连续签到 | `BROKEN / CONTINUOUS_N` | 只按服务端统计结果展示连续天数 |
| 奖励规则 | `ENABLE / DISABLE` | 规则缺失时不能伪造“今日奖励” |

### 4.5 Owner
- 产品：Member Domain Owner
- 后端：Member Sign-in Owner
- 前端：MiniApp FE Owner
- 运营/客服：会员运营 Owner

### 4.6 运营/客服口径
- 运营口径：签到奖励以配置表为准，变更奖励只改后端配置，不改前端枚举。
- 客服口径：若用户反馈“已签到未到账”，先核对 `todaySignIn` 与签到记录，再看积分/经验流水是否延迟。

### 4.7 验收口径
- [ ] 未签到/已签到两种状态可稳定切换。
- [ ] 重复签到只展示 `1004010000` 对应阻断文案，不再次播放成功动画。
- [ ] 规则缺失时页面仍可展示签到历史，不白屏。
- [ ] 签到成功后积分/经验回显与后端返回一致。

## 5. 积分（`ACTIVE`）

### 5.1 用户目标
- 查看当前积分变动来源与剩余余额。
- 了解积分商城可兑换内容和积分消耗门槛。

### 5.2 主流程
1. 用户在个人中心或资产卡进入 `/pages/user/wallet/score`。
2. 页面调用 `GET /member/point/record/page`，支持 `addStatus/createTime/pageNo/pageSize` 筛选。
3. 用户进入 `/pages/activity/point/list` 后读取：
   - `GET /promotion/point-activity/page`
   - `GET /promotion/point-activity/get-detail`
4. 积分页与积分商城共用同一积分余额认知，不允许一边成功、一边显示旧值。

### 5.3 异常流程
- `POINT_RECORD_BIZ_NOT_SUPPORT(1004008000)`：积分流水业务类型映射异常，仅影响流水区。
- `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)`：用户尝试按超限规则兑换时阻断，保留活动详情。
- `POINT_ACTIVITY_NOT_EXISTS(1013007000)`：活动详情失效，回退活动列表。
- `USER_NOT_EXISTS(1004001000)`：登录态失效，回退登录页。

### 5.4 状态机依赖

| 依赖对象 | 真值状态 | 页面规则 |
|---|---|---|
| 积分账户 | `ACCRUAL / CONSUME / REFUND_BACK / ADJUST` | 正向与负向流水必须可区分 |
| 积分商城活动 | `ONLINE / OFFLINE / SOLD_OUT / EXPIRED` | 活动已下线或售罄时禁止展示可兑换 CTA |
| 积分规则限制 | `WITHIN_LIMIT / LIMIT_HIT` | `LIMIT_HIT` 只允许调整数量，不允许成功态 |

### 5.5 Owner
- 产品：Member Domain Owner + Promotion Domain Owner
- 后端：Member Point Owner + Promotion Point Owner
- 前端：MiniApp FE Owner
- 运营/客服：会员运营 Owner

### 5.6 运营/客服口径
- 运营口径：积分记录是用户解释主口径，积分商城是消费口径；两边都以服务端积分流水真值为准。
- 客服口径：若用户反馈“积分不足/活动消失”，先查积分流水，再查活动状态，禁止用前端缓存截图解释。

### 5.7 验收口径
- [ ] 积分流水支持正向/负向筛选与分页。
- [ ] 积分商城列表、详情、规则限制口径一致。
- [ ] `1011003004` 只出现调整动作，不出现成功态按钮反馈。
- [ ] 活动失效时能回退列表，且保留用户来路。

## 6. 标签页（`Doc Closed / Can Develop / Cannot Release`）

### 6.1 当前真值
- 当前仓内已存在 `/pages/user/tag` 页面文件，并已进入 `pages.json`。
- 当前真实入口为：`/pages/user/info -> 我的标签`。
- 当前 app 端正式读取接口为：`GET /member/tag/my`。
- 页面只展示真实标签列表和空态，不补假数据，不拼接静态标签。

### 6.2 主流程
1. 用户从个人资料页进入 `/pages/user/tag`。
2. 页面调用 `GET /member/tag/my` 获取当前用户标签。
3. 有标签时展示标签列表；无标签时展示空态说明。
4. 页面文案只解释“后台会员治理标签”，不承诺任何营销自动化或权益发放结果。

### 6.3 当前仍不可放量的原因
1. 标签页虽然有真实页面与 app 接口，但 release 级样本尚未闭环。
2. 客服 / 运营对“无标签”和“读取失败”的解释口径尚未完成正式演练。
3. A 窗口未签发 release decision 前，标签页仍只能按 `Can Develop / Cannot Release` 管理。

### 6.4 放量进入条件
- 入口关闭、无标签、标签读取失败样本全部可回放。
- 客服统一口径完成演练，不再混淆“当前无标签”和“读取失败”。
- A 窗口重新签发 allowlist 前，标签页持续保持 `Cannot Release`。

### 6.5 产品侧最终标签

| 能力 | Doc Closed | Engineering Blocked | Can Develop | Cannot Release |
|---|---|---|---|---|
| `/pages/user/tag` | Yes | No | Yes | Yes |

## 7. 地址（`ACTIVE`）

### 7.1 用户目标
- 快速查看默认地址，完成新增、编辑、删除、默认地址切换。
- 在下单、预约、礼品卡收货等场景复用同一地址真值。

### 7.2 主流程
1. 用户进入 `/pages/user/address/list`。
2. 页面先调用 `GET /member/address/get-default` 渲染默认地址卡，再调用 `GET /member/address/list` 渲染全量列表。
3. 新增地址调用 `POST /member/address/create`。
4. 编辑地址先调用 `GET /member/address/get` 拉详情，再 `PUT /member/address/update` 保存。
5. 删除地址调用 `DELETE /member/address/delete`。

### 7.3 异常流程
- `ADDRESS_NOT_EXISTS(1004004000)`：地址不存在或已被删除，阻断当前编辑/删除动作后刷新列表。
- `get-default` 返回 `null`：不是错误，页面展示“请新增默认地址”空态。
- 参数校验失败：阻断提交，保留用户已输入内容，不吞掉表单。

### 7.4 状态机依赖

| 依赖对象 | 真值状态 | 页面规则 |
|---|---|---|
| 地址生命周期 | `EMPTY -> DRAFT -> SAVED -> UPDATED -> DELETED` | 删除后必须从列表移除，不做假成功 |
| 默认地址 | `NONE / DEFAULT / NON_DEFAULT` | 同一时间仅一个 `DEFAULT` |
| 表单保存 | `EDITING -> SUBMITTING -> SUCCESS/FAIL` | `FAIL` 时保留输入内容 |

### 7.5 Owner
- 产品：Member Domain Owner
- 后端：Member Address Owner
- 前端：MiniApp FE Owner
- 运营/客服：客服培训 Owner

### 7.6 运营/客服口径
- 运营口径：地址能力为会员域公共基础能力，后续下单/送礼都直接复用，不再各业务域二次存储默认地址。
- 客服口径：默认地址为空不是故障；只有收到 `1004004000` 才按地址异常处理。

### 7.7 验收口径
- [ ] 列表、新增、编辑、删除、默认地址展示全可用。
- [ ] `get-default=null` 时不报错、不白屏。
- [ ] 编辑/删除不存在地址时只影响当前动作，不影响列表保活。
- [ ] 表单失败后已填内容仍可继续修改。

## 8. 会员资产展示（`分资产 ACTIVE / 统一总账 Doc Closed / Can Develop / Cannot Release`）

### 8.1 当前真值
- 当前 runtime 承认四类会员资产页面：
  - 钱包页：`/pages/user/wallet/money`
  - 券页：`/pages/coupon/list`
  - 积分页：`/pages/user/wallet/score`
  - 统一资产总账页：`/pages/profile/assets`
- 当前统一资产总账 app 接口已真实存在：
  - `GET /member/asset-ledger/page`
- 当前统一资产总账 controller 已落在 `yudao-server` 集成层，用于聚合钱包流水、积分流水、优惠券资产。
- 当前仍不能把统一资产总账写成“已可放量能力”；`degraded=false` / `degradeReason=null` 当前只是默认字段输出，不代表真实降级链路已验证。

### 8.2 主流程
1. 个人中心展示资产摘要：
   - `GET /pay/wallet/get`：钱包余额/累计支出/累计充值
   - `GET /promotion/coupon/get-unused-count`：未使用券数量
   - `GET /member/user/get`：积分余额
2. 用户进入钱包页 `/pages/user/wallet/money`：
   - `GET /pay/wallet/get`
   - `GET /pay/wallet-transaction/page`
   - `GET /pay/wallet-transaction/get-summary`
3. 用户进入券页 `/pages/coupon/list`：`GET /promotion/coupon/page`
4. 用户进入积分页 `/pages/user/wallet/score`：`GET /member/point/record/page`
5. `GET /member/asset-ledger/page` 当前已经是 runtime API truth，但仍不能反推成“资产总账已可放量”。

### 8.3 当前仍不可放量的原因
1. 统一资产总账虽然已有真实页面与真实 controller，但当前没有真实灰度、回滚、门禁样本。
2. `degraded / degradeReason` 目前还没有真实降级证据，只能按默认字段存在理解。
3. 聚合成功、单项为空、读取失败之外，缺少发布级样本包和 A 窗口最终签发。

### 8.4 放量进入条件
- 聚合成功、单项为空、总账为空、读取失败、真实降级样本全部可回放。
- 分资产页与统一总账页不会互相覆盖、互相伪成功。
- 灰度、回滚、门禁材料完成工程/文档双闭环。
- A 窗口重新签发 allowlist 前，`/pages/profile/assets` 持续保持 `Cannot Release`。

### 8.5 产品侧最终标签

| 能力 | Doc Closed | Engineering Blocked | Can Develop | Cannot Release |
|---|---|---|---|---|
| `/pages/profile/assets` | Yes | No | Yes | Yes |

## 9. 全局验收口径
- [ ] 所有会员域页面只按 `errorCode/degraded/degradeReason` 分支，不按 message。
- [ ] 所有降级场景不出现成功态文案、成功 icon、成功彩带或完成动效。
- [ ] 客服 SOP、运营执行口径、页面文案与本 PRD 保持单一真值。
- [ ] 会员域联调至少覆盖：1 条 happy path + 1 条业务错误 path + 1 条降级 path。
- [ ] `/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag` 已形成真实页面，但在 release evidence 闭环前，一律只写 `Can Develop / Cannot Release`，不得写成已放量页面。
