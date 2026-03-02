# HXY 类目模板与 SKU 自动生成接口草案（并轨版）

## 1. 目标与边界

- 目标：在不破坏现有 `product_spu/product_sku` 与门店价库存主链路的前提下，落地
  - 类目属性模板版本化
  - SKU 规格自动生成（预览/提交）
  - 模板校验门禁
- 非目标：
  - 本次不替换现有下单/支付逻辑
  - 本次不引入 ES，仅定义扩展接口和数据契约

## 2. 与现有链路关系

- `product_spu.product_type` 继续作为服务/实物分流主字段。
- 现有 `product_sku.properties` 继续保留，作为运行态兼容字段。
- 新增表（见 `sql/mysql/hxy/2026-03-01-hxy-category-attribute-template-v1.sql`）提供模板与组合治理能力。
- 后续由后台生成流程将模板结果同步回 `product_sku.properties`，保证旧链路可用。

## 3. API 草案（Admin）

### 3.1 校验类目模板

- `POST /admin-api/product/category-template/validate`

请求体：

```json
{
  "categoryId": 101,
  "templateVersionId": 12,
  "items": [
    {
      "attributeId": 1,
      "attrRole": 2,
      "required": true,
      "affectsPrice": true,
      "affectsStock": true
    }
  ]
}
```

响应体：

```json
{
  "pass": false,
  "errors": [
    {
      "code": "SKU_SPEC_DATA_TYPE_INVALID",
      "message": "SKU_SPEC 仅允许 ENUM/MULTI_ENUM"
    }
  ],
  "warnings": [
    {
      "code": "LARGE_COMBINATION_RISK",
      "message": "规格组合预计 360 个，建议拆分模板"
    }
  ]
}
```

校验规则：

- 仅叶子类目允许发布模板。
- 单模板内 `attributeId` 不能重复。
- `attrRole=SKU_SPEC` 时，属性 `dataType` 必须是单选/多选。
- `affectsStock=true` 仅允许零售类目；服务类目强制 `affectsStock=false`。
- `required=true` 且有默认值时，默认值必须满足 `validation_json`。

### 3.2 SKU 生成预览

- `POST /admin-api/product/spu/sku-generator/preview`

请求体：

```json
{
  "spuId": 30001,
  "categoryId": 101,
  "templateVersionId": 12,
  "baseSku": {
    "price": 9800,
    "marketPrice": 12800,
    "costPrice": 5200,
    "stock": 999
  },
  "specSelections": [
    {
      "attributeId": 1,
      "optionIds": [11, 12, 13]
    },
    {
      "attributeId": 2,
      "optionIds": [21, 22]
    }
  ]
}
```

响应体：

```json
{
  "taskNo": "SKU_PREVIEW_20260301_00001",
  "combinationCount": 6,
  "truncated": false,
  "items": [
    {
      "specHash": "6f6b5f6d2d...",
      "specSummary": "60分钟/高级技师",
      "existsSkuId": null,
      "suggestedSku": {
        "price": 9800,
        "marketPrice": 12800,
        "stock": 999
      }
    }
  ]
}
```

预览规则：

- 组合上限默认 200，超过返回 `truncated=true` 和告警。
- `specHash` 生成规则：`spuId + 排序后的(attributeId:optionIds)` 做 `SHA-256`。
- 若 `specHash` 已存在（已有关联 SKU），标记 `existsSkuId`。

### 3.3 SKU 生成提交

- `POST /admin-api/product/spu/sku-generator/commit`

请求体：

```json
{
  "taskNo": "SKU_PREVIEW_20260301_00001",
  "idempotencyKey": "SPU30001-V12-COMBOV1"
}
```

响应体：

```json
{
  "taskNo": "SKU_COMMIT_20260301_00009",
  "status": 1,
  "accepted": true
}
```

提交规则：

- 同一 `tenantId + spuId + idempotencyKey` 幂等。
- 如果已提交过，返回同一任务结果，不重复写 SKU。
- 提交仅创建/更新 `product_sku` + `hxy_sku_attr_value`，不直接改门店库存。

### 3.4 任务查询与重试

- `GET /admin-api/product/spu/sku-generator/task/{taskNo}`
- `POST /admin-api/product/spu/sku-generator/task/{taskNo}/retry`

重试条件：

- 任务状态为 `失败/部分成功`。
- 重试仅处理失败明细项，不重复处理成功项。

## 4. 错误码建议

- `1-008-015-000` 模板不存在
- `1-008-015-001` 模板校验失败
- `1-008-015-002` 规格组合数量超限
- `1-008-015-003` SKU 生成任务不存在
- `1-008-015-004` SKU 生成任务状态非法
- `1-008-015-005` SKU 生成重复提交（幂等命中）

## 5. 实施顺序建议（紧贴当前 P0）

1. 先落表：执行 `2026-03-01-hxy-category-attribute-template-v1.sql`。
2. 实现模板校验 API（只校验，不写 SKU）。
3. 实现预览 API（只写任务与明细，不写 SKU）。
4. 实现提交 API（按明细落 `product_sku`，并同步 `properties`）。
5. 加 Job：失败明细重试与告警聚合。

## 6. 验收口径

- 同一输入请求重复提交，SKU 结果不重复新增。
- 预览组合与提交生成数量一致（剔除已存在 SKU 后）。
- 服务类目模板禁止库存影响项，零售类目允许。
- 既有下单/支付回归通过（主链路不受影响）。
