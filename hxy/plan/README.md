# plan 文档总入口

> 本目录已完成第三轮重构，并在第五轮将 reports 细分为 current/history。  
> 目标：减少混杂、降低查找成本、避免历史文档误用。

---

## 目录结构

```text
plan/
├── modules/   # 模块补全与技术实施方案
├── product/   # 产品规划、接口设计、UI与任务清单
├── reports/   # 阶段性实施报告（current/history）
└── tools/     # 调试与测试脚本
```

---

## 1. modules（模块方案）

- 位置：`plan/modules/`
- 内容：`补全-1` 到 `补全-19`、SPU/SKU补全汇总、模块示例、社区小店补全文档。
- 入口：
  - `plan/modules/SPU_SKU方案补全版-目录.md`
  - `plan/modules/SPU_SKU方案补全版-完整汇总.md`
  - `plan/modules/README.md`

## 2. product（产品规划）

- 位置：`plan/product/`
- 内容：小程序PRD、接口/数据库设计、任务清单、MVP规划、UI设计与预览指南。
- 入口：
  - `plan/product/荷小悦小程序产品规划PRD_V1.0.md`
  - `plan/product/荷小悦小程序_开发任务清单.md`
  - `plan/product/README.md`

## 3. reports（阶段报告）

- 位置：`plan/reports/`
- 内容：已拆分为 `current/`（当前入口）与 `history/`（历史沉淀）。
- 入口：
  - `plan/reports/current/项目迁移包-状态快照.md`
  - `plan/reports/current/项目完整部署报告.md`
  - `plan/reports/README.md`

## 4. tools（工具脚本）

- 位置：`plan/tools/`
- 内容：测试 token 生成、会员接口测试脚本。
- 入口：`plan/tools/README.md`

---

## 使用建议

1. 需要功能补全时先看 `modules/`。  
2. 需要产品定义与排期时看 `product/`。  
3. 需要追溯历史实施细节时看 `reports/`。  
4. 执行脚本前先看 `tools/README.md`。
