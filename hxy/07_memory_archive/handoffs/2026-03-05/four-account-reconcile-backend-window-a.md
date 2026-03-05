# 四账对账后端收口（窗口A）

## 目标
- 新增四账详情接口 `/booking/four-account-reconcile/get`。
- `page/get` 统一回填 `sourceBizNo`，并在 trade 摘要联查异常时降级不阻断。
- trade 摘要 DTO 增加可选字段 `resolvedTime`。
- 补齐 booking/trade 定向单测与治理文档。

## 关键改动
1. booking
   - `FourAccountReconcileController`
     - 新增 `GET /get?id=`。
     - `/page` 与 `/get` 统一回填 `sourceBizNo=FOUR_ACCOUNT_RECONCILE:<bizDate>`。
     - `bindRelatedTicketSummary` 增加 try/catch，trade API 异常时 fail-open。
   - `FourAccountReconcileService`
     - 新增 `getReconcile(Long id)`。
   - `FourAccountReconcileServiceImpl`
     - 实现 `getReconcile(Long id)`。
   - `FourAccountReconcileRespVO`
     - 新增字段 `sourceBizNo`。

2. trade
   - `TradeReviewTicketSummaryRespDTO`
     - 新增可选字段 `resolvedTime`。
   - `TradeReviewTicketApiImpl`
     - 摘要映射补齐 `resolvedTime`。

3. 测试
   - `FourAccountReconcileControllerTest`
     - 新增 `get_shouldReturnData`。
     - 新增 `page_shouldDegradeWhenTradeTicketApiThrows`。
     - 既有分页测试补 `sourceBizNo` 断言。
   - `TradeReviewTicketApiImplTest`
     - 新增 `resolvedTime` 映射断言。
   - `AfterSaleReviewTicketServiceImplTest`
     - 补 `resolvedTime` 返回断言。

4. 治理文档
   - 事实基线新增第 100 条。
   - ADR 新增 `ADR-077`。
   - 执行看板新增第 83 条。

## 验证
- `git diff --check`：PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_naming_guard.sh`：PASS
- `CHECK_UNSTAGED=1 CHECK_UNTRACKED=1 bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`：PASS
- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-trade,yudao-module-mall/yudao-module-booking -am -Dtest=TradeReviewTicketApiImplTest,AfterSaleReviewTicketServiceImplTest,FourAccountReconcileControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`：PASS

## 风险与回滚
- 风险：trade 摘要查询异常时仅告警不抛错，可能导致短时无关联工单展示。
- 回滚：回退 `FourAccountReconcileController` 本批变更即可恢复“强依赖 trade 联查”行为（不推荐）。
