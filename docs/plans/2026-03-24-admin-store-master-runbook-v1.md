# Admin Store Master Runbook v1 (2026-03-24)

## 1. 目标与范围
- 目标：为门店主数据、分类、标签、标签组提供后台标准操作顺序与审计规则。
- 范围只覆盖后台主数据治理，不覆盖库存审批、履约、交易和前台发布。

## 2. 操作入口

| 能力 | 页面入口 | 核心操作 |
|---|---|---|
| `ADM-003` 门店主数据 | `mall/store/index` | 新建 / 编辑门店、查看门店详情、批量分类 / 标签 / 生命周期、检查 launch readiness |
| `ADM-004` 门店分类 | `mall/store/category/index` | 分类列表、保存、删除 |
| `ADM-005` 门店标签 | `mall/store/tag/index` | 标签列表、保存、删除 |
| `ADM-006` 门店标签组 | `mall/store/tag-group/index` | 标签组列表、保存、删除 |

## 3. 审计键最小集
- `storeId`
- `categoryId`
- `tagId`
- `groupId`
- `lifecycleStatus`
- `storeIds[]`
- `reason`
- `operator`

## 4. 标准操作顺序
1. 先建分类、标签组，再建标签，最后落门店主数据。
2. 门店保存后回读 `get`，再决定是否做批量分类 / 标签更新。
3. 生命周期动作前必须先看 `check-launch-readiness` 和 `lifecycle-guard`。
4. 批量动作必须记录 `storeIds[] + reason`，必要时先做小批量验证。

## 5. 失败处理
- `ready=false`、`blocked=true` 时，不继续执行上线型或生命周期变更型操作。
- 分类 / 标签 / 标签组删除若存在引用争议，先暂停删除，转人工核对关联门店。
- 批量动作后如果详情回读不一致，停止继续扩大批次。

## 6. 回滚 / 暂停规则
- 批量标签 / 分类误操作时，按同批次门店列表回滚。
- 生命周期执行异常时，回到只读守卫检查，暂停继续批量操作。

## 7. 当前结论
- Store master 域已具备独立后台 runbook。
- 仍不可把主数据治理文档写成门店前台已可放量。
