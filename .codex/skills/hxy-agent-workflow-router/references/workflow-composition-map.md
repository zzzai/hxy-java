# Workflow Composition Map

## 1. 发布前文档收口
1. `hxy-miniapp-capability-auditor`
2. `hxy-miniapp-doc-freeze-closer`
3. `hxy-release-gate-decider`

适用：需要同时判断能力真值、Ready/Frozen、Go/No-Go。

## 2. booking 域真实对齐
1. `hxy-booking-contract-alignment`
2. `hxy-miniapp-capability-auditor`
3. `hxy-miniapp-doc-freeze-closer`
4. `hxy-release-gate-decider`（如涉及放量）

适用：技师、时段、加钟、取消、冲突码、退款重放。

## 3. member 域收口
1. `hxy-member-domain-closer`
2. `hxy-miniapp-capability-auditor`
3. `hxy-miniapp-doc-freeze-closer`
4. `hxy-release-gate-decider`（如涉及 Active 集合或门禁）

适用：登录、签到、地址、钱包、积分、资产页、资产账本。

## 4. 健康数据与标签治理
1. `hxy-health-data-compliance-guard`
2. `hxy-miniapp-doc-freeze-closer`
3. `hxy-release-gate-decider`

适用：面诊、舌诊、体征、标签、抽象推荐、审计字段、营销使用。

## 5. 多窗口并行派发
1. `hxy-agent-workflow-router`
2. `hxy-window-handoff-normalizer`
3. 按窗口目标加载对应领域技能

适用：A/B/C/D 同时推进并要求固定回收结构。
