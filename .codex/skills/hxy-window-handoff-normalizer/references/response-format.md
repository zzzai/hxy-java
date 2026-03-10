# Fixed Response Format

Every window returns exactly these sections:

1. `commit 列表（hash + message）`
2. `变更文件清单`
3. `验证命令与结果`
4. `handoff 文件路径`
5. `联调注意点（字段/错误码/降级行为）`

## Rules
- No missing verification details.
- No "见提交" or "同上" shortcuts.
- Use exact file paths.
- If cherry-pickability matters, state conflict points explicitly.
