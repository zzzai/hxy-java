# MiniApp Active vs Planned API Matrix v1 (2026-03-10)

## 1. 目标
- 将会员域 miniapp 接口按 `ACTIVE` / `PLANNED_RESERVED` 分层固化为可执行清单。
- 每条接口必须绑定精确 `method + path + controllerPath`，禁止使用通配 API。
- 为发布门禁提供固定判定口径：`PLANNED_RESERVED` 不得进入 `ACTIVE` 发布口径。

## 2. 状态定义
- `ACTIVE`：当前分支已存在明确 controller 映射，允许进入会员域发布清单。
- `PLANNED_RESERVED`：仅保留精确接口契约，不进入当前发布清单；必须绑定 `switchKey`，默认禁用。
- `switchKey = -`：当前接口无运行期开关，按既有已上线能力管理。

## 3. ACTIVE API Matrix

### 3.1 认证与身份

| 能力 | method | path | domain | status | switchKey | owner | controllerPath |
|---|---|---|---|---|---|---|---|
| 手机号+密码登录 | `POST` | `/member/auth/login` | `member/auth` | `ACTIVE` | `-` | `Member Auth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/auth/AppAuthController.java#login` |
| 登出 | `POST` | `/member/auth/logout` | `member/auth` | `ACTIVE` | `-` | `Member Auth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/auth/AppAuthController.java#logout` |
| 刷新令牌 | `POST` | `/member/auth/refresh-token` | `member/auth` | `ACTIVE` | `-` | `Member Auth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/auth/AppAuthController.java#refreshToken` |
| 手机号+验证码登录 | `POST` | `/member/auth/sms-login` | `member/auth` | `ACTIVE` | `-` | `Member Auth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/auth/AppAuthController.java#smsLogin` |
| 发送短信验证码 | `POST` | `/member/auth/send-sms-code` | `member/auth` | `ACTIVE` | `-` | `Member Auth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/auth/AppAuthController.java#sendSmsCode` |
| 校验短信验证码 | `POST` | `/member/auth/validate-sms-code` | `member/auth` | `ACTIVE` | `-` | `Member Auth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/auth/AppAuthController.java#validateSmsCode` |
| 社交快捷登录 | `POST` | `/member/auth/social-login` | `member/auth` | `ACTIVE` | `-` | `Member Auth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/auth/AppAuthController.java#socialLogin` |
| 微信小程序一键登录 | `POST` | `/member/auth/weixin-mini-app-login` | `member/auth` | `ACTIVE` | `-` | `Member Auth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/auth/AppAuthController.java#weixinMiniAppLogin` |
| CRMEB 小程序登录兼容层 | `POST` | `/api/front/wechat/authorize/program/login` | `member/auth-compat` | `ACTIVE` | `-` | `Member Auth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/compat/crmeb/CrmebFrontWechatCompatController.java#programLogin` |
| 当前会员信息 | `GET` | `/member/user/get` | `member/profile` | `ACTIVE` | `-` | `Member Profile Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/user/AppMemberUserController.java#getUserInfo` |

### 3.2 等级、签到、积分

| 能力 | method | path | domain | status | switchKey | owner | controllerPath |
|---|---|---|---|---|---|---|---|
| 会员等级列表 | `GET` | `/member/level/list` | `member/growth` | `ACTIVE` | `-` | `Member Growth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/level/AppMemberLevelController.java#getLevelList` |
| 经验记录分页 | `GET` | `/member/experience-record/page` | `member/growth` | `ACTIVE` | `-` | `Member Growth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/level/AppMemberExperienceRecordController.java#getExperienceRecordPage` |
| 签到规则列表 | `GET` | `/member/sign-in/config/list` | `member/growth` | `ACTIVE` | `-` | `Member Growth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/signin/AppMemberSignInConfigController.java#getSignInConfigList` |
| 签到统计 | `GET` | `/member/sign-in/record/get-summary` | `member/growth` | `ACTIVE` | `-` | `Member Growth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/signin/AppMemberSignInRecordController.java#getSignInRecordSummary` |
| 执行签到 | `POST` | `/member/sign-in/record/create` | `member/growth` | `ACTIVE` | `-` | `Member Growth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/signin/AppMemberSignInRecordController.java#createSignInRecord` |
| 签到记录分页 | `GET` | `/member/sign-in/record/page` | `member/growth` | `ACTIVE` | `-` | `Member Growth Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/signin/AppMemberSignInRecordController.java#getSignRecordPage` |
| 积分记录分页 | `GET` | `/member/point/record/page` | `member/asset` | `ACTIVE` | `-` | `Member Asset Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/point/AppMemberPointRecordController.java#getPointRecordPage` |

### 3.3 地址

| 能力 | method | path | domain | status | switchKey | owner | controllerPath |
|---|---|---|---|---|---|---|---|
| 创建地址 | `POST` | `/member/address/create` | `member/profile` | `ACTIVE` | `-` | `Member Profile Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/address/AppAddressController.java#createAddress` |
| 更新地址 | `PUT` | `/member/address/update` | `member/profile` | `ACTIVE` | `-` | `Member Profile Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/address/AppAddressController.java#updateAddress` |
| 删除地址 | `DELETE` | `/member/address/delete` | `member/profile` | `ACTIVE` | `-` | `Member Profile Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/address/AppAddressController.java#deleteAddress` |
| 地址详情 | `GET` | `/member/address/get` | `member/profile` | `ACTIVE` | `-` | `Member Profile Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/address/AppAddressController.java#getAddress` |
| 默认地址 | `GET` | `/member/address/get-default` | `member/profile` | `ACTIVE` | `-` | `Member Profile Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/address/AppAddressController.java#getDefaultUserAddress` |
| 地址列表 | `GET` | `/member/address/list` | `member/profile` | `ACTIVE` | `-` | `Member Profile Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/address/AppAddressController.java#getAddressList` |

## 4. PLANNED_RESERVED API Matrix

| 能力 | method | path | domain | status | switchKey | owner | controllerPath |
|---|---|---|---|---|---|---|---|
| 会员资产账本分页 | `GET` | `/member/asset-ledger/page` | `member/asset-ledger` | `PLANNED_RESERVED` | `miniapp.asset.ledger` | `Member + Promotion Domain Owner` | `TARGET: ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/asset/AppMemberAssetLedgerController.java#page`（当前分支缺失 controller，固定阻断） |

## 5. 发布阻断规则
1. `ACTIVE` 发布口径只允许消费第 3 节接口；第 4 节 `PLANNED_RESERVED` 接口不得进入发布清单、smoke allowlist、页面验收清单、OpenAPI 对外基线。
2. `PLANNED_RESERVED` 接口转 `ACTIVE` 前，必须同时满足：
   - 精确 controller 文件已存在，且 `method + path` 与本矩阵逐字匹配。
   - `switchKey` 已配置，默认 `off`，并完成灰度记录。
   - 对应错误码已进入 canonical register，且 `RESERVED_DISABLED` 门禁为绿色。
   - A/B/D 三窗口已完成字段、错误码、降级行为联调。
3. 任一 `PLANNED_RESERVED` 接口在 `switchKey=off` 或 controller 缺失条件下出现在发布产物中，发布固定阻断。
4. 对本批次固定阻断项：`GET /member/asset-ledger/page`。原因：
   - 当前分支无落地 controller；
   - 已绑定 `miniapp.asset.ledger` 预留开关；
   - 只能作为 `PLANNED_RESERVED` 保留契约，不得计入 `ACTIVE` 发布口径。

## 6. 自动门禁判定口径
- `ACTIVE_SET`：本文件第 3 节全部 `method + path`。
- `RESERVED_SET`：本文件第 4 节全部 `method + path`。
- 自动门禁应满足：
  - `ACTIVE_SET` 中每条接口都能扫描到对应 `controllerPath`。
  - `RESERVED_SET` 中任一接口都不得被自动归入 `ACTIVE_SET`。
  - 扫描结果出现通配根路由但无法还原到精确子路径时，视为阻断而不是放行。
