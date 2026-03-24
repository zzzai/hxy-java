# Member Missing-Page Closure Design

## 目标
- 补齐 `Member` 域三个真实缺页能力：`/pages/user/level`、`/pages/profile/assets`、`/pages/user/tag`。
- 让页面、真实入口、app 读取链路、文档真值同时落地，解除“只有文档、没有 runtime”的工程 blocker。
- 阶段结论保持 `Can Develop / Cannot Release`，不把本轮工程闭环误写成可放量。

## 现状裁决
- `level`
  - 真实 app controller 已存在：`GET /member/level/list`、`GET /member/experience-record/page`
  - 缺口只在页面、路由、真实入口。
- `assets`
  - `/pages/profile/assets` 缺页。
  - `GET /member/asset-ledger/page` 仅存在文档契约，当前仓库没有真实 controller。
  - `member` 模块不能直接依赖 `promotion` 模块，否则会形成循环依赖；因此资产账本聚合层放到 `yudao-server` 集成层实现。
- `tag`
  - `/pages/user/tag` 缺页。
  - 后台标签治理能力已存在，但 app 读取接口缺失。

## 方案选择

### 方案 A：只补前端页面
- 优点：快。
- 缺点：`assets/tag` 仍然没有真实 app 读取真值，属于伪闭环。
- 结论：不采用。

### 方案 B：页面 + app 读取接口一起补齐
- 优点：能把缺页能力真正变成“可开发的真实 runtime 能力”。
- 缺点：`assets` 需要跨模块聚合，设计要谨慎。
- 结论：采用。

### 方案 C：先大规模重构模块依赖，再补功能
- 优点：架构更整洁。
- 缺点：当前不是主优先级，会拖慢 blocker 解除。
- 结论：不采用，本轮坚持最小必要改动。

## 本轮设计

### 1. 等级页
- 新增页面：`yudao-mall-uniapp/pages/user/level.vue`
- 新增前端 API：`sheep/api/member/level.js`
- 真实入口：
  - `pages.json` 增加 `/pages/user/level`
  - `pages/user/info.vue` 增加“会员等级”入口
- 数据来源：
  - `GET /member/user/get`
  - `GET /member/level/list`
  - `GET /member/experience-record/page`

### 2. 资产总览页
- 新增页面：`yudao-mall-uniapp/pages/profile/assets.vue`
- 新增前端 API：`sheep/api/member/asset.js`
- 新增后端集成层：
  - `yudao-server` 下新增 `GET /member/asset-ledger/page`
  - 聚合钱包、积分、优惠券三类已存在真实数据源，输出统一列表与汇总卡片
- 真实入口：
  - `pages.json` 增加 `/pages/profile/assets`
  - `pages/user/info.vue` 增加“资产总览”入口
- 设计取舍：
  - 本轮先落真实 controller 和页面真值；
  - `miniapp.asset.ledger` 门禁、灰度、样本包仍按后续发布材料管理；
  - 页面可开发可回归，但仍不可改写成 release-ready。

### 3. 标签页
- 新增页面：`yudao-mall-uniapp/pages/user/tag.vue`
- 新增前端 API：`sheep/api/member/tag.js`
- 新增后端 app controller：
  - `GET /member/tag/my`
  - 返回当前登录用户标签列表
- 真实入口：
  - `pages.json` 增加 `/pages/user/tag`
  - `pages/user/info.vue` 增加“我的标签”入口

## 测试策略
- 前端：
  - 新增 Node 测试，先校验路由、API 路径、页面对真实字段的消费。
- 后端：
  - 新增 Mockito 单测覆盖 `level/tag/asset-ledger` controller/service 真值。
- 验证：
  - `git diff --check`
  - `check_hxy_naming_guard.sh`
  - `check_hxy_memory_guard.sh`

## 边界
- 本轮不做技术架构重构。
- 本轮不把 `Member` 缺页能力直接升为可放量。
- 本轮不补造不存在的成功样本、灰度样本或发布结论。
