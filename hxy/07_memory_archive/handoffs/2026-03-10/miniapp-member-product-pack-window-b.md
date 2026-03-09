# Window B Handoff - MiniApp Member Product Pack（2026-03-10）

## 1. 本批交付
- 分支：`feat/ui-four-account-reconcile-ops`
- 交付类型：仅新增文档与 handoff；未改 overlay、未改业务代码。
- 新增文件：
  1. `docs/products/miniapp/2026-03-10-miniapp-member-domain-prd-v1.md`
  2. `docs/products/miniapp/2026-03-10-miniapp-member-page-api-field-dictionary-v1.md`
  3. `docs/products/miniapp/2026-03-10-miniapp-member-user-facing-errorcopy-v1.md`
  4. `hxy/07_memory_archive/handoffs/2026-03-10/miniapp-member-product-pack-window-b.md`

## 2. 核心收口结论
- 会员域这次明确拆成两层：
  - `ACTIVE`：登录/注册、个人中心、等级、签到、积分、地址、分资产展示。
  - `PLANNED_RESERVED`：标签展示、统一资产总账。
- 登录/注册不再发明独立注册页：
  - 首次短信登录或首次微信手机号登录，服务端自动注册建档。
  - 微信一键登录必须成对描述 `loginCode + phoneCode + state`，不能把手机号授权等同于登录成功。
- 标签能力已明确降级边界：
  - 后台标签与数据治理能力已存在。
  - 小程序 app 端读取接口未冻结，当前版本默认隐藏，不可被 A/C 误当成 `ACTIVE` 联调项。
- 资产展示明确采用“摘要卡 + 分资产明细”先落地：
  - 钱包/积分/优惠券走现有接口。
  - `GET /member/asset-ledger/page` 仍为 `PLANNED_RESERVED`，未开关前不得返回。

## 3. 关键信息源
- 代码事实：
  - `yudao-module-member` app 控制器已存在：`auth/user/level/signin/point/address`
  - `yudao-module-pay` app 控制器已存在：`wallet`、`wallet-transaction`
  - `yudao-module-promotion` app 控制器已存在：`coupon/coupon-template/point-activity`
- 契约事实：
  - `docs/contracts/2026-03-08-miniapp-contract-freeze-v1.md`
  - `docs/contracts/2026-03-09-miniapp-release-api-canonical-list-v1.md`
  - `docs/contracts/2026-03-09-miniapp-errorcode-canonical-register-v1.md`
- 治理事实：
  - `hxy/04_data/HXY-全域数据价值化与门店数据治理蓝图-v1-2026-03-08.md`
  - `hxy/04_data/用户数据-高争议标签治理规则-V1.md`

## 4. 对 A/C/D 的联调注意点

### 4.1 给窗口 A（前端）
- 登录页字段必须按当前后端 VO：
  - 微信一键登录必传 `phoneCode/loginCode/state`
  - `state` 在 `AppAuthWeixinMiniAppLoginReqVO` 中是必填，不能继续沿用旧样例里缺失 `state` 的调用方式
- 个人中心字段真值只认 `/member/user/get`：
  - `point/experience/level` 不允许前端自行拼接或猜测升级结果
- 标签模块当前必须默认隐藏：
  - 不发未知 app API
  - 不展示“已生成画像/已开放标签”等成功态文案
- 降级统一规则：
  - 只按 `errorCode/degraded/degradeReason` 分支
  - warning 态禁止成功 icon、成功彩带、庆祝动画

### 4.2 给窗口 C（契约/后端）
- 保持以下能力状态不变：
  - `ACTIVE`：`/member/auth/*`、`/member/user/*`、`/member/level/list`、`/member/experience-record/page`、`/member/sign-in/*`、`/member/point/record/page`、`/member/address/*`、`/pay/wallet/*`、`/promotion/coupon*`、`/promotion/point-activity/*`
  - `PLANNED_RESERVED`：`/member/asset-ledger/page`、app 端标签读取接口
- 错误码冻结锚点：
  - 登录/会员：`1004003000/1004003001/1004003005/1004003007/1004001000/1004001001/1004001002`
  - 签到/等级/积分：`1004009000/1004010000/1004011000/1004011201/1004008000/1011003004/1013007000`
  - 地址/券/资产：`1004004000/1013004000/1013005000/1004009901`
- 若未来补 app 标签接口：
  - 必须先冻结 `G0/G1/G2` 返回边界
  - 必须给出门禁状态与审计字段
  - 未完成前不得从 `N/A` 偷换成 `ACTIVE`

### 4.3 给窗口 D（数据/验收）
- 验收证据要覆盖：
  - 首次短信登录自动注册
  - 首次微信手机号登录自动注册
  - 重复签到 `1004010000`
  - 积分规则上限 `1011003004`
  - 地址不存在 `1004004000`
  - 资产降级 `1004009901` 或 `TICKET_SYNC_DEGRADED`
- 标签相关验收当前只做“关闭态验收”：
  - 入口默认隐藏
  - 不发请求
  - 不出现成功态
- 统一审查点：
  - 不按 message 分支
  - 降级不伪成功
  - `PLANNED_RESERVED` 不提前放开

## 5. 风险与后续建议
- 风险 1：现有 `.http` 示例中微信一键登录缺少 `state`，若 A 直接照抄会导致联调失败。
- 风险 2：标签功能最容易被误写成“已有能力”；本次文档已明确为关闭态规划项，后续若调整必须同步窗口。
- 风险 3：统一资产总账若被提前放开，前端容易把 `degraded` 误渲染成成功到账，需要继续守住 warning 语义。
- 建议：
  1. A 先落登录/个人中心/签到/地址/积分/资产摘要六块 `ACTIVE` 页面。
  2. C 后续若补标签或资产总账聚合接口，先补 canonical list 再开 UI。
  3. D 把“成功态文案误用在降级场景”作为专项验收项，不只验字段存在。
