# 依赖升级计划（fastjson / Spring Boot / Druid）

## 目标
- 降低历史 CVE 风险，避免一次性大升级造成业务中断。
- 先做“安全兜底 + 可回滚”，再做“框架级升级”。

## 范围
- `com.alibaba:fastjson`（现状：1.2.83）
- Spring Boot（现状：2.2.6.RELEASE）
- `com.alibaba:druid`（现状：1.1.20）

## 分阶段执行

### Phase 1（本周）：风险收敛，不改业务行为
1. 建立依赖扫描基线：`mvn -DskipTests dependency:tree > runtime/dependency-tree.txt`
2. 引入 SCA 检查到 CI（仅告警，不阻断发布）
3. 列出 fastjson 使用点清单（按模块、调用场景分组）
4. 明确 Druid 监控端口和访问控制（禁止公网暴露）

验收标准：
- 有可追溯的依赖清单与漏洞清单；
- 上线前能看到依赖风险报告。

### Phase 2（1-2周）：JSON 组件迁移准备
1. 新增 `JsonFacade`（统一 JSON 序列化/反序列化入口）
2. 新代码默认使用 Jackson；旧代码允许继续 fastjson
3. 高风险入口优先迁移（支付回调、登录、权限、工单）
4. 关键链路补测试（支付回调、退款回调、订单状态流转）

验收标准：
- 新增代码不再直接依赖 fastjson；
- 支付链路回归测试通过。

### Phase 3（2-4周）：框架升级演练
1. 建立升级分支：`upgrade/springboot-2.7`
2. 升级 Boot 至 2.7.x（先 dev 环境演练）
3. 升级 Druid 到 1.2.x，并回归数据源/监控
4. 处理废弃 API 与配置差异（启动参数、Actuator、安全配置）

验收标准：
- dev/staging 启动、下单、支付回调、退款回调全通过；
- 压测与发布回滚预案齐备。

### Phase 4（稳定后）：移除 fastjson 直接依赖
1. 业务代码 fastjson 引用全部清零
2. 移除 `com.alibaba:fastjson` 依赖
3. 持续用 Jackson + 统一门面

验收标准：
- `rg "com.alibaba.fastjson"` 仅允许在兼容层（若保留）；
- 主模块不再直接依赖 fastjson。

## 风险与回滚
- 风险：JSON 兼容差异、日期格式差异、BigDecimal 精度处理差异。
- 回滚：每个 phase 独立发版；只要回归失败就回退到上个 phase tag。

## 执行顺序（建议）
1. Phase 1
2. Phase 2
3. 支付/退款全链路回归
4. Phase 3
5. Phase 4

