# trade-price-template-validation-and-service-express-guard

## 输入背景
- 业务目标：将“模板版本有效性”和“服务商品配送分流”前置到价格计算阶段，避免无效订单进入支付/履约链路。
- 约束条件：保持现有交易主链不推翻；模板校验通过 product API 访问，不允许 trade 直连 product DAL。

## 变更清单
1. 代码：
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/main/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceImpl.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade-api/src/main/java/cn/iocoder/yudao/module/trade/enums/ErrorCodeConstants.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/api/template/ProductTemplateVersionApi.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/api/template/ProductTemplateVersionApiImpl.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-product/src/main/java/cn/iocoder/yudao/module/product/api/template/dto/ProductTemplateVersionRespDTO.java`
2. 测试：
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceStoreSkuOverrideTest.java`
- `ruoyi-vue-pro-master/yudao-module-mall/yudao-module-trade/src/test/java/cn/iocoder/yudao/module/trade/service/price/TradePriceServiceTemplateVersionValidationTest.java`
3. 治理文档：
- `hxy/00_governance/HXY-项目事实基线-v1-2026-03-01.md`
- `hxy/00_governance/HXY-架构决策记录-ADR-v1.md`
- `hxy/06_roadmap/HXY-执行状态看板-v1-2026-03-01.md`

## 风险与回滚
1. 风险：
- 当前模板校验在“请求携带 `templateVersionId`”条件下生效，未覆盖“应必填但未传”的场景。
- 历史调用若误传草稿版 `templateVersionId` 将被阻断，需同步前端或调用方口径。
2. 监控点：
- 错误码 `1_011_003_007`、`1_011_003_008~011` 的新增比例。
- 下单前价格计算失败率按门店/渠道分布。
3. 回滚命令/步骤：
- 紧急回退 `TradePriceServiceImpl` 到前一提交，保留模板字段透传但移除强阻断。
- 若需部分降级，可仅放宽“未发布/类目不匹配”拦截为告警日志（临时方案）。

## 下一窗口接力点
1. 先执行：
- 将模板校验从“有 `templateVersionId` 才校验”升级为“服务/实物指定类目强制模板必填”。
2. 验证命令：
- `mvn -f ruoyi-vue-pro-master/pom.xml -pl yudao-module-mall/yudao-module-trade -am -Dtest=TradePriceServiceStoreSkuOverrideTest,TradePriceServiceTemplateVersionValidationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `bash ruoyi-vue-pro-master/script/dev/check_hxy_memory_guard.sh`
3. 完成标准：
- 价格计算阶段对无效模板和错误配送类型做到前置阻断。
- 记忆门禁在 `product/trade` 域变更时可稳定通过。
