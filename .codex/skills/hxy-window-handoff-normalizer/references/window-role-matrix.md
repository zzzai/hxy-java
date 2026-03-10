# Window Role Matrix

| Window | Primary responsibility | Must mention |
|---|---|---|
| A | 集成、单一真值、冻结与发布门禁 | capability status, freeze state, go/no-go blockers |
| B | 产品业务、字段字典、用户恢复动作 | field truth, user-facing copy, recovery actions |
| C | contract、API canonical、errorCode、failureMode | canonical method/path, stable errorCode, fail-open/fail-close |
| D | KPI、runbook、alert、灰度、回滚 | gate thresholds, degraded_pool, rollback triggers |

## Shared Requirements
- 字段：必须列出关键检索键与主业务键
- 错误码：只按 code 分支，不按 message
- 降级行为：必须区分 fail-open / fail-close 与用户提示策略
