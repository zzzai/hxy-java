# MiniApp Active vs Planned API Matrix v1 (2026-03-10)

## 1. 目标
- 将会员域 miniapp 接口按 `ACTIVE` / `PLANNED_RESERVED` 分层固化为可执行清单。
- 每条接口必须绑定精确 `method + path + controllerPath`，禁止使用通配 API。
- 为发布门禁提供固定判定口径：`PLANNED_RESERVED` 不得进入 `ACTIVE` 发布口径。
- 本文件只冻结 API/controller truth；页面缺失、前端绑定缺失、治理文档齐套都不能自动等价为 runtime capability 已上线。

## 2. 状态定义
- `ACTIVE`：当前分支已存在明确 controller 映射，允许进入会员域发布清单。
- `PLANNED_RESERVED`：仅保留精确接口契约，不进入当前发布清单；必须绑定 `switchKey`，默认禁用。
- `switchKey = -`：当前接口无运行期开关，按既有已上线能力管理。
- `switchKey`、灰度规则、`RESERVED_DISABLED` 注册表只算治理证据，不算 runtime capability 证据。

## 3. ACTIVE API Matrix（API-level truth only）

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

### 3.4 标签与统一资产总账

| 能力 | method | path | domain | status | switchKey | owner | controllerPath |
|---|---|---|---|---|---|---|---|
| 当前会员标签 | `GET` | `/member/tag/my` | `member/tag` | `ACTIVE` | `-` | `Member Domain Owner` | `ruoyi-vue-pro-master/yudao-module-member/src/main/java/cn/iocoder/yudao/module/member/controller/app/tag/AppMemberTagController.java#getMyTags` |
| 会员资产账本分页 | `GET` | `/member/asset-ledger/page` | `member/asset-ledger` | `ACTIVE` | `-` | `Member + Promotion Domain Owner` | `ruoyi-vue-pro-master/yudao-server/src/main/java/cn/iocoder/yudao/server/controller/app/member/AppMemberAssetLedgerController.java#getAssetLedgerPage` |

## 4. PLANNED_RESERVED API Matrix

- 当前 member 域已无新增 `PLANNED_RESERVED` app API。
- 历史 `GET /member/asset-ledger/page` 自 2026-03-24 起已升为 `ACTIVE API/controller truth`；但对应页面 capability 仍不是 release-ready。

## 5. 03-14 Runtime Blocker Closure Ledger

| 阻断对象 | API / gate 真值 | 当前缺失的 runtime 证据 | 明确禁止误写为 | Doc Closed | Contract Closed | Runtime Not Proven | Release Blocked |
|---|---|---|---|---|---|---|---|
| `member.level-progress page capability` | `GET /member/level/list`、`GET /member/experience-record/page` 已有真实 controller，且 `/pages/user/level` 已落地 | 当前缺发布级样本、field dictionary、release 签发 | “等级页已上线可放量”“等级 capability 已进入 ACTIVE 发布分母” | `Yes` | `Yes` | `No` | `Yes` |
| `member.asset-hub page capability` | `GET /member/asset-ledger/page` 已有真实 controller，`/pages/profile/assets` 已有页面与前端绑定 | 当前缺灰度 / 回滚 / 样本证据；`degraded/degradeReason` 只有默认字段输出，未形成真实降级证据 | “资产总账已上线可放量”“灰度 / 降级 / 回滚已工程闭环” | `Yes` | `Yes` | `No` | `Yes` |
| `member.user-tag page capability` | `GET /member/tag/my` 已有真实 controller，`/pages/user/tag` 已落地 | 当前缺 release 样本与客服 / 运营演练证据 | “标签页已上线可放量”“标签 app API 已完成 release 审核” | `Yes` | `Yes` | `No` | `Yes` |
| `reserved.gift-card` | 治理文档、domain contract、gate spec、`miniapp.gift-card`、`1011009901/1011009902` 已落盘 | 当前无真实 `/pages/gift-card/*` 页面、无真实 app controller、无前端绑定 | “礼品卡 runtime capability 已上线/可灰度” | `Yes` | `Yes` | `Yes` | `Yes` |
| `reserved.referral` | 治理文档、domain contract、gate spec、`miniapp.referral`、`1013009901/1013009902` 已落盘 | 当前无真实 `/pages/referral/*` 页面、无真实 app controller、无前端绑定 | “邀请有礼 runtime capability 已上线/可灰度” | `Yes` | `Yes` | `Yes` | `Yes` |
| `reserved.technician-feed` | 治理文档、domain contract、gate spec、`miniapp.technician-feed.audit`、`1030009901` 已落盘 | 当前无真实 `/pages/technician/feed` 页面、无真实 app controller、无前端绑定、无运行样本 | “技师动态 runtime capability 已上线/可灰度” | `Yes` | `Yes` | `Yes` | `Yes` |

说明：
- 第 5 节是 runtime blocker ledger，不会因为 PRD、contract、switchKey、灰度 runbook 已齐就自动消失。
- 第 3 节的 `ACTIVE` 行如果命中第 5 节阻断项，只表示 `ACTIVE API`，不表示 `ACTIVE page capability`。

## 6. 发布阻断规则
1. `ACTIVE` 发布口径只允许消费第 3 节接口；若同一能力在第 5 节仍是 runtime blocker，则不得把它外推成可放量页面 capability。
2. 第 3 节的 `ACTIVE` 只表示 API/controller truth；不等于 release-ready。
3. 历史 `switchKey`、灰度规则、`RESERVED_DISABLED` 注册表如果仍停留在文档层，只能证明治理层曾定义过目标，不代表当前代码已经接通真实门禁。
4. 本批次固定阻断项：
   - `/pages/user/level` 对应的等级 page capability
   - `/pages/profile/assets` 对应的资产总账 page capability
   - `/pages/user/tag` 对应的标签 page capability
   - `gift-card / referral / technician-feed` 的 reserved runtime capability
5. 上述阻断项的共同原因：
   - 治理与契约文档可以 `Doc Closed / Contract Closed`；
   - 但只要 release 样本、灰度 / 回滚材料、客服演练或 A 窗口签发任一缺失，就继续 `Release Blocked`。

## 7. 自动门禁判定口径
- `ACTIVE_SET`：本文件第 3 节全部 `method + path`。
- `RESERVED_SET`：本文件第 4 节全部 `method + path`。
- `BLOCKER_SET`：本文件第 5 节全部阻断对象。
- 自动门禁应满足：
  - `ACTIVE_SET` 中每条接口都能扫描到对应 `controllerPath`。
  - `RESERVED_SET` 中任一接口都不得被自动归入 `ACTIVE_SET`。
  - `BLOCKER_SET` 中任一对象都不得因为“命中了 controllerPath”或“文档齐套”而被自动归入可放量能力。
  - 扫描结果出现通配根路由但无法还原到精确子路径时，视为阻断而不是放行。
