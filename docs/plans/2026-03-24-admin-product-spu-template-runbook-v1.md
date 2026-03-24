# Admin Product SPU / Template Runbook v1 (2026-03-24)

## 1. 目标与范围
- 为 `ADM-001`、`ADM-002` 提供后台商品主数据操作顺序、审计键和失败处理基线。
- 只覆盖后台运营 / 商品配置动作，不覆盖门店铺货、库存审批与前台展示放量。

## 2. 操作入口

| 能力 | 页面入口 | 核心操作 |
|---|---|---|
| `ADM-001` SPU 管理 | `mall/product/spu/index`; `mall/product/spu/form/index` | 新建、编辑、上下架、查看详情、导出 |
| `ADM-002` 模板校验 / SKU 自动生成 | `mall/product/template/index` | 校验模板输入、预览 SKU 组合、提交生成 |

## 3. 审计键最小集
- `spuId`
- `categoryId`
- `templateVersionId`
- `taskNo`
- `idempotencyKey`
- `operator`
- `status`

## 4. 标准操作顺序
1. 先用 `page` / `get-detail` 确认当前 SPU 状态，再做新增或编辑。
2. 状态切换前记录旧状态，保存或变更后回读详情确认。
3. 使用模板时先 `validate`，确认 `errors` / `warnings`。
4. 做 SKU 生成时必须先 `preview`，确认 `combinationCount`、`truncated` 和建议结果。
5. 只有预览结论清晰时才执行 `commit`，并记录 `taskNo + idempotencyKey`。

## 5. 失败处理
- `validate.pass=false` 时，不继续 `commit`。
- `truncated=true` 时，必须人工确认组合裁剪风险，不能直接视为正常量产。
- `accepted=true` 但页面未回读到预期结果时，按“受理成功、后效待核”处理。

## 6. 回滚 / 暂停规则
- SPU 保存后若详情回读不一致，暂停继续批量编辑。
- 模板生成如连续出现裁剪或幂等冲突，暂停继续提交，保留预览样本排查。

## 7. 当前结论
- `ADM-001`、`ADM-002` 已具备独立后台 runbook。
- 这不等于门店商品、库存、交易链路已同步完成。
