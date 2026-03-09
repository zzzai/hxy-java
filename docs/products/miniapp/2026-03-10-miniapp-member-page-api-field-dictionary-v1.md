# MiniApp 会员页面 API 字段字典 v1（2026-03-10）

## 1. 文档目标
- 建立会员域“页面 -> 路由 -> API -> 请求字段 -> 响应字段 -> 错误码 -> 降级动作”统一执行字典。
- 只记录两类能力：
  - `ACTIVE`：代码中真实存在、可直接联调的能力。
  - `PLANNED_RESERVED`：文档已明确规划、但必须受门禁控制的能力。
- 约束：
  - 禁止依赖 message 分支。
  - `PLANNED_RESERVED` 未开关生效前不得对外返回。
  - 标签模块当前无 app 冻结接口，不得冒充为 `ACTIVE`。

## 2. 页面 API 字段字典

| 页面 | 路由 | 能力状态 | API | 请求字段 | 响应字段 | 错误码 | 降级动作 |
|---|---|---|---|---|---|---|---|
| 登录/注册-密码登录 | `/pages/public/login` | `ACTIVE` | `POST /member/auth/login` | `mobile`, `password`, `socialType?`, `socialCode?`, `socialState?` | `userId`, `accessToken`, `refreshToken`, `expiresTime`, `openid?` | `AUTH_LOGIN_BAD_CREDENTIALS(1004003000)`, `AUTH_LOGIN_USER_DISABLED(1004003001)` | 无降级；阻断成功态，保留手机号输入值 |
| 登录/注册-发送短信验证码 | `/pages/public/login` | `ACTIVE` | `POST /member/auth/send-sms-code` | `mobile`, `scene` | `true` | `SMS_CODE_SEND_TOO_FAST(1002014005)`, `SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY(1002014004)`, `USER_MOBILE_NOT_EXISTS(1004001001)`（忘记密码场景） | 限频时显示倒计时与刷新动作；不进入登录成功态 |
| 登录/注册-短信登录（含自动注册） | `/pages/public/login` | `ACTIVE` | `POST /member/auth/sms-login` | `mobile`, `code`, `socialType?`, `socialCode?`, `socialState?` | `userId`, `accessToken`, `refreshToken`, `expiresTime`, `openid?` | `SMS_CODE_NOT_FOUND(1002014000)`, `SMS_CODE_EXPIRED(1002014001)`, `SMS_CODE_USED(1002014002)`, `AUTH_LOGIN_USER_DISABLED(1004003001)` | 无降级；失败时保留手机号与登录方式，不清空上下文 |
| 登录/注册-微信一键登录（含自动注册） | `/pages/public/login` | `ACTIVE` | `POST /member/auth/weixin-mini-app-login` | `phoneCode`, `loginCode`, `state` | `userId`, `accessToken`, `refreshToken`, `expiresTime`, `openid` | `SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR(1002018200)`, `SOCIAL_USER_AUTH_FAILURE(1002018000)` | 无降级；授权失败后回退授权前状态，不展示成功文案 |
| 个人中心-首页信息 | `/pages/user/index` | `ACTIVE` | `GET /member/user/get` | 无 | `id`, `nickname`, `avatar`, `mobile`, `sex`, `point`, `experience`, `level:{id,name,level,icon}`, `brokerageEnabled` | `USER_NOT_EXISTS(1004001000)` | 无降级；登录态无效回退登录页 |
| 个人中心-基础资料更新 | `/pages/user/profile/edit` | `ACTIVE` | `PUT /member/user/update` | `nickname`, `avatar`, `sex` | `true` | `USER_NOT_EXISTS(1004001000)` | 无降级；失败时保留本地表单值 |
| 个人中心-手机号换绑（短信） | `/pages/user/profile/mobile` | `ACTIVE` | `PUT /member/user/update-mobile` | `mobile`, `code`, `oldCode?` | `true` | `USER_MOBILE_USED(1004001002)`, `SMS_CODE_NOT_FOUND/EXPIRED/USED(1002014000/1002014001/1002014002)` | 无降级；失败时回显原手机号并保留输入值 |
| 个人中心-手机号换绑（微信） | `/pages/user/profile/mobile` | `ACTIVE` | `PUT /member/user/update-mobile-by-weixin` | `code` | `true` | `SOCIAL_CLIENT_WEIXIN_MINI_APP_PHONE_CODE_ERROR(1002018200)`, `USER_MOBILE_USED(1004001002)` | 无降级；失败时保留原手机号展示 |
| 个人中心-修改密码 | `/pages/user/profile/password` | `ACTIVE` | `PUT /member/user/update-password` | `password`, `code` | `true` | `SMS_CODE_NOT_FOUND/EXPIRED/USED(1002014000/1002014001/1002014002)` | 无降级；失败时停留当前页，提示重试 |
| 忘记密码 | `/pages/public/reset-password` | `ACTIVE` | `PUT /member/user/reset-password` | `mobile`, `code`, `password` | `true` | `USER_MOBILE_NOT_EXISTS(1004001001)`, `SMS_CODE_NOT_FOUND/EXPIRED/USED(1002014000/1002014001/1002014002)` | 无降级；失败时保留手机号和密码输入 |
| 等级页-等级列表 | `/pages/user/level` | `ACTIVE` | `GET /member/level/list` | 无 | `list[]:{name,level,experience,discountPercent,icon,backgroundUrl}` | `LEVEL_NOT_EXISTS(1004011000)` | 若当前用户无等级，则用普通会员卡兜底，不白屏 |
| 等级页-经验记录 | `/pages/user/level` | `ACTIVE` | `GET /member/experience-record/page` | `pageNo`, `pageSize` | `list[]:{title,experience,description,createTime}`, `total` | `EXPERIENCE_BIZ_NOT_SUPPORT(1004011201)` | 经验流水异常只降级流水区，不影响等级卡 |
| 签到页-规则列表 | `/pages/user/sign-in` | `ACTIVE` | `GET /member/sign-in/config/list` | 无 | `list[]:{day,point}` | `SIGN_IN_CONFIG_NOT_EXISTS(1004009000)` | 规则缺失时隐藏梯度区，仅保留历史与空态说明 |
| 签到页-签到统计 | `/pages/user/sign-in` | `ACTIVE` | `GET /member/sign-in/record/get-summary` | 无 | `totalDay`, `continuousDay`, `todaySignIn` | `USER_NOT_EXISTS(1004001000)` | 登录失效回退登录页 |
| 签到页-提交签到 | `/pages/user/sign-in` | `ACTIVE` | `POST /member/sign-in/record/create` | 无 | `day`, `point`, `experience`, `createTime` | `SIGN_IN_RECORD_TODAY_EXISTS(1004010000)` | 无降级；重复签到直接禁用按钮并提示刷新 |
| 签到页-签到记录 | `/pages/user/sign-in` | `ACTIVE` | `GET /member/sign-in/record/page` | `pageNo`, `pageSize` | `list[]:{day,point,experience,createTime}`, `total` | `USER_NOT_EXISTS(1004001000)` | 列表失败只降级记录区，不影响签到摘要 |
| 积分页-积分流水 | `/pages/user/wallet/score` | `ACTIVE` | `GET /member/point/record/page` | `pageNo`, `pageSize`, `addStatus?`, `createTime[0]`, `createTime[1]` | `list[]:{id,title,description,point,createTime}`, `total` | `POINT_RECORD_BIZ_NOT_SUPPORT(1004008000)` | 失败时保留积分页主框架与筛选条件 |
| 积分商城-活动列表 | `/pages/point/mall` | `ACTIVE` | `GET /promotion/point-activity/page` | `pageNo`, `pageSize` | `list[]:{id,spuId,status,stock,totalStock,spuName,picUrl,marketPrice,point,price}`, `total` | `POINT_ACTIVITY_NOT_EXISTS(1013007000)`, `PRICE_CALCULATE_POINT_TOTAL_LIMIT_COUNT(1011003004)` | 活动异常回退空态；不阻断积分流水入口 |
| 积分商城-活动详情 | `/pages/point/mall/detail` | `ACTIVE` | `GET /promotion/point-activity/get-detail` | `id` | `id`, `spuId`, `status`, `stock`, `totalStock`, `products[]:{id,skuId,count,point,price,stock}`, `point`, `price` | `POINT_ACTIVITY_NOT_EXISTS(1013007000)` | 详情不存在时回退活动列表并保留来路 |
| 标签模块-展示位 | `/pages/user/tag` | `PLANNED_RESERVED` | `N/A（app 端标签读取接口未冻结，当前默认关闭）` | `-` | `-` | `-` | 默认隐藏入口；门禁未开前不发请求、不显示成功态文案 |
| 地址页-默认地址 | `/pages/address/list` | `ACTIVE` | `GET /member/address/get-default` | 无 | `id`, `name`, `mobile`, `areaId`, `detailAddress`, `defaultStatus`, `areaName` 或 `null` | `ADDRESS_NOT_EXISTS(1004004000)` | 返回 `null` 视为正常空态，引导新增 |
| 地址页-地址列表 | `/pages/address/list` | `ACTIVE` | `GET /member/address/list` | 无 | `list[]:{id,name,mobile,areaId,detailAddress,defaultStatus,areaName}` | `ADDRESS_NOT_EXISTS(1004004000)` | 列表异常时保活页面并允许刷新 |
| 地址页-地址详情 | `/pages/address/edit` | `ACTIVE` | `GET /member/address/get` | `id` | `id`, `name`, `mobile`, `areaId`, `detailAddress`, `defaultStatus`, `areaName` | `ADDRESS_NOT_EXISTS(1004004000)` | 详情缺失时回退列表并刷新 |
| 地址页-新增地址 | `/pages/address/edit` | `ACTIVE` | `POST /member/address/create` | `name`, `mobile`, `areaId`, `detailAddress`, `defaultStatus` | `id` | `ADDRESS_NOT_EXISTS(1004004000)`（仅底层校验时） | 无降级；失败时保留表单内容 |
| 地址页-编辑地址 | `/pages/address/edit` | `ACTIVE` | `PUT /member/address/update` | `id`, `name`, `mobile`, `areaId`, `detailAddress`, `defaultStatus` | `true` | `ADDRESS_NOT_EXISTS(1004004000)` | 无降级；失败时停留编辑页并允许刷新 |
| 地址页-删除地址 | `/pages/address/list` | `ACTIVE` | `DELETE /member/address/delete` | `id` | `true` | `ADDRESS_NOT_EXISTS(1004004000)` | 删除失败不影响列表保活；刷新后重试 |
| 资产摘要-钱包卡片 | `/pages/user/index` | `ACTIVE` | `GET /pay/wallet/get` | 无 | `balance`, `totalExpense`, `totalRecharge` | `USER_NOT_EXISTS(1004001000)` | 钱包卡片失败时隐藏金额区，保留其他功能入口 |
| 钱包页-流水明细 | `/pages/user/wallet/money` | `ACTIVE` | `GET /pay/wallet-transaction/page` | `pageNo`, `pageSize`, `type`, `createTime[0]`, `createTime[1]` | `list[]:{bizType,price,title,createTime}`, `total` | `USER_NOT_EXISTS(1004001000)` | 查询失败时展示可重试空态，不白屏 |
| 钱包页-流水统计 | `/pages/user/wallet/money` | `ACTIVE` | `GET /pay/wallet-transaction/get-summary` | `createTime[0]`, `createTime[1]` | `totalExpense`, `totalIncome` | `USER_NOT_EXISTS(1004001000)` | 汇总失败时只降级统计卡，不影响流水区 |
| 资产摘要-优惠券计数 | `/pages/user/index` | `ACTIVE` | `GET /promotion/coupon/get-unused-count` | 无 | `count(Long)` | `COUPON_NOT_EXISTS(1013005000)` | 计数失败时展示 `--`，保留券入口 |
| 券资产列表 | `/pages/coupon/list` | `ACTIVE` | `GET /promotion/coupon/page` | `pageNo`, `pageSize`, `status?` | `list[]:{id,name,status,usePrice,productScope,productScopeValues,validStartTime,validEndTime,discountType,discountPercent,discountPrice,discountLimitPrice}`, `total` | `COUPON_NOT_EXISTS(1013005000)` | 券列表失败不影响钱包/积分展示；允许刷新 |
| 统一资产总账 | `/pages/profile/assets` | `PLANNED_RESERVED` | `GET /member/asset-ledger/page` | `memberId`, `assetType?`, `pageNo`, `pageSize` | `list[]:{ledgerId,assetType,bizType,amount,balanceAfter,sourceBizNo,runId}`, `degraded` | `MINIAPP_ASSET_LEDGER_MISMATCH(1004009901, RESERVED_DISABLED)`, `POINT_RECORD_BIZ_NOT_SUPPORT(1004008000)`, `COUPON_NOT_EXISTS(1013005000)` | 汇总异常 fail-open：显示局部降级标签，不隐藏已确认账目 |

## 3. 执行备注
- 登录/注册页不拆独立“注册 API”；注册语义并入首次成功登录。
- 标签模块当前只允许做关闭态页面/入口设计，不允许自行补造 app API。
- `/pages/address/list`、`/pages/point/mall` 等页面即使 UI 原型未补齐，后端能力仍按 `ACTIVE` 处理。
