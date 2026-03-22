# Booking Review Manager Routing Coverage Design（2026-03-22）

## 1. 背景
- 当前 `booking review` 已有：
  - 门店店长路由只读核查页
  - App / 企微双通道路由真值
  - notify outbox 阻断诊断
- 当前缺口也很明确：
  - 页面只能看分页明细和单店核查
  - 不能直接回答“1000 家门店里，多少家双通道已就绪、多少家缺 App、多少家缺企微、多少家双缺失”
  - 运营处理 `BLOCKED_NO_OWNER` 时，仍要翻分页列表手工统计

## 2. 目标
- 为 `/mall/booking/review/manager-routing` 增加覆盖率摘要与缺失绑定运营视图。
- 让运营一眼看懂：
  - 总门店数
  - 双通道就绪覆盖率
  - App 覆盖率
  - 企微覆盖率
  - 缺任一绑定数
  - 缺 App 数
  - 缺企微数
  - 双缺失数
- 让运营可以一键筛出缺失绑定门店，而不是手工逐页翻查。

## 3. 约束
1. 只做 admin-only 只读运营视图，不做在线改绑。
2. 不改 notify outbox 状态机。
3. 不改差评提交流程。
4. 不把覆盖率摘要写成“真实送达率”。
5. 当前发布结论继续固定为 `Doc Closed / Can Develop / Cannot Release / No-Go`。

## 4. 方案比较

### 方案 A：前端只基于当前页 10 条数据做摘要
- 优点：实现最小
- 缺点：
  - 只能反映当前页，不是真实覆盖率
  - 对 `1000` 门店场景没有运营价值
- 结论：不采用

### 方案 B：后端返回全量摘要 + 分页筛选
- 优点：
  - 覆盖率口径真实
  - 缺失绑定可以直接筛选
  - 运营动作路径最短
- 缺点：
  - 需要补 summary 接口和一层过滤逻辑
- 结论：采用

### 方案 C：直接新建独立“绑定治理看板”
- 优点：概念更完整
- 缺点：
  - 页面扩张过快
  - 当前 manager routing 页已经是天然入口，另起页收益不高
- 结论：暂不采用

## 5. 核心设计

### 5.1 新增 summary 接口
- 路径：`GET /booking/review/manager-routing/summary`
- 输入：
  - `storeId`
  - `storeName`
  - `contactMobile`
  - `routingStatus`
  - `appRoutingStatus`
  - `wecomRoutingStatus`
- 输出：
  - `totalStoreCount`
  - `dualReadyCount`
  - `appReadyCount`
  - `wecomReadyCount`
  - `missingAnyCount`
  - `missingAppCount`
  - `missingWecomCount`
  - `missingBothCount`

### 5.2 路由页增强
- 在筛选区下方新增覆盖率摘要卡片。
- 每张卡片展示：
  - 指标名
  - 分子 / 分母
  - 百分比
- 增加快捷筛选按钮：
  - 只看缺任一绑定
  - 只看缺 App
  - 只看缺企微
  - 只看双缺失
  - 只看双通道就绪

### 5.3 过滤口径
- 过滤不是按联系人快照，而是按真实路由状态：
  - `routingStatus`
  - `appRoutingStatus`
  - `wecomRoutingStatus`
- 双缺失判定：
  - `appRoutingStatus=APP_MISSING`
  - `wecomRoutingStatus=WECOM_MISSING`
- 缺任一绑定判定：
  - App 缺失或企微缺失任一成立

### 5.4 实现策略
- 当前用户已确认场景是 `1000` 门店量级。
- 第一版采用：
  - 先按 `storeName/contactMobile` 拉取匹配门店全集
  - 再按真实 routing 规则构造快照
  - 在服务层做汇总与筛选
- 为避免 summary 场景下逐店查单条路由导致过多查询，补一个批量加载 latest routing 的 mapper 能力。

## 6. 测试策略
1. Java service test：
   - 能统计覆盖率摘要
   - 能按 `appRoutingStatus / wecomRoutingStatus / routingStatus` 过滤
2. Java controller test：
   - 暴露 `summary` 接口
3. Node test：
   - 前端 API 暴露 summary type / method
   - manager routing 页存在覆盖率摘要和快捷筛选

## 7. No-Go
1. 不得把“覆盖率”写成“送达率”。
2. 不得把 `contactMobile` 当企微账号。
3. 不得因为有 summary 卡片，就写成“1000 门店绑定已完成”。
4. 不得把本专题外推成 release-ready。
