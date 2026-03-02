# 荷小悦知识库（HXY 长记忆目录）

- 目录：`/root/crmeb-java/hxy`
- 角色：项目知识库 + 长期记忆归档 + 可执行文档入口
- 当前代码底座：`/root/crmeb-java/ruoyi-vue-pro-master`

## 1. 使用原则

1. 新文档、结论、验收证据一律沉淀到 `hxy`。
2. 历史资料与恢复文档进入归档区，不污染主线目录。
3. 以 `文档导航.md` 和 `99_index/` 作为查找入口。

## 2. 新目录结构（2026-02-22）

1. `00_governance/`：项目章程、命名规则、治理边界。
2. `01_product/`：产品设计总纲与业务方案（含 `research_refs/` 参考提炼）。
3. `02_architecture/`：技术架构、部署与演进规划（含 `research_refs/` 参考提炼）。
4. `03_payment/`：支付系统专项规划与联调策略。
5. `04_data/`：数据与合规（含 `sql/` 脚本目录）。
6. `05_engineering/`：研发流程、CI/CD、上线回滚。
7. `06_roadmap/`：里程碑与阶段路线图。
8. `07_memory_archive/`：历史资料归档（含 470 恢复文档整理结果）。
9. `99_index/`：索引、统计、清单、迁移映射。

## 3. 必读文档

1. `99_index/HXY-知识库新结构-2026-02-22.md`
2. `99_index/HXY-二次归并映射表-v1-2026-02-22.md`
3. `99_index/HXY-二次归并映射表-v2-2026-02-22.md`
4. `00_governance/HXY-项目章程-v1-2026-02-22.md`
5. `01_product/HXY-产品设计总纲-v2-2026-02-22.md`
6. `01_product/HXY-瑞幸双商品体系对标-RuoYi落地方案-v1-2026-02-22.md`
7. `02_architecture/HXY-技术架构规划-v2-2026-02-22.md`
8. `03_payment/HXY-支付系统规划-v2-2026-02-22.md`
9. `04_data/HXY-数据与合规规划-v1-2026-02-22.md`
10. `05_engineering/HXY-工程与发布规范-v1-2026-02-22.md`
11. `06_roadmap/HXY-执行路线图-v2-2026-02-22.md`
12. `06_roadmap/HXY-全新工作计划-0到50店-RuoYi版-v1-2026-02-22.md`
13. `00_governance/项目级-命名规范-HXY版-2026-02-21.md`
14. `01_product/HXY-总部门店权责利模型-v1-2026-02-22.md`
15. `02_architecture/HXY-万店连锁-K8s微服务治理蓝图-v1-2026-02-22.md`
16. `03_payment/HXY-支付场景适配评估-v1-2026-02-22.md`
17. `03_payment/HXY-支付模式定版-服务商特约商户-v1-2026-02-22.md`
18. `03_payment/HXY-收银与虚拟账户过渡方案-v1-2026-02-22.md`
19. `02_architecture/HXY-RuoYi底座复用与二开边界-v1-2026-02-22.md`
20. `03_payment/HXY-退款分层策略与工单升级规则-v1-2026-02-22.md`
21. `03_payment/HXY-支付发布回滚Runbook-v1-2026-02-22.md`
22. `01_product/HXY-新中式疗愈社区O2O-完整PRD-v1-2026-03-01.md`
23. `00_governance/HXY-项目事实基线-v1-2026-03-01.md`
24. `00_governance/HXY-架构决策记录-ADR-v1.md`
25. `00_governance/HXY-长记忆治理规范-v1-2026-03-01.md`
26. `06_roadmap/HXY-执行状态看板-v1-2026-03-01.md`
27. `05_engineering/HXY-总部门店双层-AI运营SOP与提示词-v1-2026-03-01.md`

## 6. 长记忆门禁（新增）

1. 脚本：`ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
2. 作用：当核心业务代码或 SQL 发生变化时，强制要求同步更新长记忆文档（事实基线/ADR/执行看板）。
3. CI：`ruoyi-vue-pro-master/.github/workflows/hxy-memory-guard.yml`。
4. 发布入口联动：`ruoyi-vue-pro-master/script/dev/run_payment_stagea_p0_19_20.sh`。

## 4. 归档说明

1. 恢复文档全量目录：`07_memory_archive/recovered_full_2026w08/`（470 份）
2. 恢复文档精筛目录：`07_memory_archive/recovered_useful_2026w08/`（131 份）
3. 外部原始资料归档：`07_memory_archive/external_raw_2026w08/`
4. 清单与统计：
   - `99_index/recovered_full_2026w08_files.txt`
   - `99_index/recovered_useful_2026w08_files.txt`
   - `99_index/recovered_useful_2026w08_summary.txt`
   - `99_index/recovered_useful_2026w08_category_summary.txt`

## 5. hxybase 参考处理

1. 原始参考归档：`07_memory_archive/hxybase_raw_2026w08/`
2. 参考清单：`99_index/hxybase_raw_2026w08_files.txt`
3. 独立判断提炼：`99_index/HXY-hxybase参考提炼-独立判断-v1-2026-02-22.md`
