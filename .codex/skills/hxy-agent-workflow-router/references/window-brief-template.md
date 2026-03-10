# Window Brief Template

## Prompt Skeleton
- Window role: `A|B|C|D`
- Scope: one explicit target pack only
- Must-create files: exact paths
- Required updates: index/freeze/contract/runbook as applicable
- Constraints: no overlay changes, no business-code edits, no unrelated file handling
- Required verification:
  1. `git diff --check`
  2. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`
  3. `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- Fixed return format:
  1. commit 列表（hash + message）
  2. 变更文件清单
  3. 验证命令与结果
  4. handoff 文件路径
  5. 联调注意点（字段/错误码/降级行为）

## Window Focus
- A: 集成、单一真值、索引/冻结/发布门禁
- B: 产品业务、字段字典、用户恢复动作、业务文案
- C: contract、errorCode、failureMode、route/API canonical truth
- D: KPI、runbook、alert、灰度、回滚、degraded_pool
