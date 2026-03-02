# store-stock-idempotency-and-memory-guard

## 输入背景
- 业务目标：修复库存并发与幂等一致性风险，升级长记忆门禁为强制模式。
- 约束条件：不依赖聊天历史；核心领域改动必须同步三份记忆文件；CI 与发布脚本可直接复用同一门禁脚本。

## 变更清单
1. 代码：
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/store/ProductStoreMappingServiceImpl.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/dal/mysql/store/ProductStoreSkuStockFlowMapper.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/service/template/ProductTemplateGenerateServiceImpl.java`
2. SQL/配置：
- `ruoyi-vue-pro-master/sql/mysql/hxy/2026-03-01-hxy-store-sku-stock-flow.sql`
- `ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
3. 文档：
- `hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md`
- `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`
- `hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md`
- `hxy/99_index/memory-index.yaml`
- `hxy/07_memory_archive/handoffs/README.md`

## 风险与回滚
1. 风险：
- 门禁强制化后，历史分支提交可能因未更新记忆文件被阻断。
- 库存流水状态新增 `PROCESSING` 后，旧查询逻辑若硬编码状态集合可能漏统计。
2. 监控点：
- 库存流水 `status in (0,2,3)` 数量趋势。
- 门禁脚本退出码：`0/2/1` 分布。
3. 回滚命令/步骤：
- 门禁紧急降级：临时在 CI 中将 `check_hxy_memory_guard.sh` 改为 warning（仅用于紧急恢复，恢复后补回）。
- 库存执行回滚：回退 `ProductStoreMappingServiceImpl` 与 `ProductStoreSkuStockFlowMapper` 到上一个 tag，并清理 `PROCESSING` 残留记录到 `FAILED`。

## 下一窗口接力点
1. 先执行：
- 将 `check_hxy_memory_guard.sh` 的“路径映射”扩展到其他核心仓库路径（若存在新 frontend/backend 根目录）。
2. 验证命令：
- `bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-product -Dtest=ProductStoreMappingServiceImplTest,ProductTemplateGenerateServiceImplTest test`
3. 完成标准：
- 门禁脚本在核心域变更下可准确拦截未更新记忆文档的提交。
- 库存幂等与重试链路测试持续通过。
