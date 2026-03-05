package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleChangeOrderPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleBatchLogDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleChangeOrderDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleRecheckLogDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class ProductStoreLifecycleBatchLogMapperTest {

    @Test
    void selectPage_shouldBuildFiltersWhenReqFieldsPresent() {
        TableInfo tableInfo = TableInfoHelper.initTableInfo(
                new MybatisMapperBuilderAssistant(new MybatisConfiguration(), ""),
                ProductStoreLifecycleBatchLogDO.class);
        LambdaUtils.installCache(tableInfo);

        ProductStoreLifecycleBatchLogMapper mapper = mock(ProductStoreLifecycleBatchLogMapper.class, CALLS_REAL_METHODS);
        AtomicReference<Wrapper<ProductStoreLifecycleBatchLogDO>> wrapperRef = new AtomicReference<>();
        PageResult<ProductStoreLifecycleBatchLogDO> expected = new PageResult<>(Collections.emptyList(), 0L);
        doAnswer(invocation -> {
            wrapperRef.set(invocation.getArgument(1));
            return expected;
        }).when(mapper).selectPage(any(PageParam.class), org.mockito.ArgumentMatchers.<Wrapper<ProductStoreLifecycleBatchLogDO>>any());

        LocalDateTime begin = LocalDateTime.of(2026, 3, 4, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 4, 23, 59, 59);
        ProductStoreLifecycleBatchLogPageReqVO reqVO = new ProductStoreLifecycleBatchLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setBatchNo("LIFECYCLE-20260304");
        reqVO.setTargetLifecycleStatus(35);
        reqVO.setOperator("运营同学");
        reqVO.setSource("ADMIN_UI");
        reqVO.setCreateTime(new LocalDateTime[]{begin, end});

        PageResult<ProductStoreLifecycleBatchLogDO> actual = mapper.selectPage(reqVO);

        assertSame(expected, actual);
        Wrapper<ProductStoreLifecycleBatchLogDO> wrapper = wrapperRef.get();
        assertNotNull(wrapper);
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("batch_no"));
        assertTrue(sqlSegment.contains("target_lifecycle_status"));
        assertTrue(sqlSegment.contains("operator"));
        assertTrue(sqlSegment.contains("source"));
        assertTrue(sqlSegment.contains("create_time"));

        AbstractWrapper<?, ?, ?> abstractWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
        Map<String, Object> params = abstractWrapper.getParamNameValuePairs();
        assertTrue(params.size() >= 6);
        assertTrue(params.values().contains(35));
        assertTrue(params.values().contains("ADMIN_UI"));
        assertTrue(params.values().contains(begin));
        assertTrue(params.values().contains(end));
        assertTrue(params.values().stream().anyMatch(v -> String.valueOf(v).contains("LIFECYCLE-20260304")));
        assertTrue(params.values().stream().anyMatch(v -> String.valueOf(v).contains("运营同学")));
    }

    @Test
    void selectLatestByBatchNo_shouldBuildBatchNoFilterAndLimit() {
        TableInfo tableInfo = TableInfoHelper.initTableInfo(
                new MybatisMapperBuilderAssistant(new MybatisConfiguration(), ""),
                ProductStoreLifecycleBatchLogDO.class);
        LambdaUtils.installCache(tableInfo);

        ProductStoreLifecycleBatchLogMapper mapper = mock(ProductStoreLifecycleBatchLogMapper.class, CALLS_REAL_METHODS);
        AtomicReference<Wrapper<ProductStoreLifecycleBatchLogDO>> wrapperRef = new AtomicReference<>();
        ProductStoreLifecycleBatchLogDO expected = ProductStoreLifecycleBatchLogDO.builder().id(1L).build();
        doAnswer(invocation -> {
            wrapperRef.set(invocation.getArgument(0));
            return expected;
        }).when(mapper).selectOne(org.mockito.ArgumentMatchers.<Wrapper<ProductStoreLifecycleBatchLogDO>>any());

        ProductStoreLifecycleBatchLogDO actual = mapper.selectLatestByBatchNo("LIFECYCLE-20260304");

        assertSame(expected, actual);
        Wrapper<ProductStoreLifecycleBatchLogDO> wrapper = wrapperRef.get();
        assertNotNull(wrapper);
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("batch_no"));
        assertTrue(sqlSegment.contains("ORDER BY"));
        assertTrue(sqlSegment.contains("LIMIT 1"));
        AbstractWrapper<?, ?, ?> abstractWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
        assertTrue(abstractWrapper.getParamNameValuePairs().values().contains("LIFECYCLE-20260304"));
    }

    @Test
    void recheckLogSelectPage_shouldBuildFiltersWhenReqFieldsPresent() {
        TableInfo tableInfo = TableInfoHelper.initTableInfo(
                new MybatisMapperBuilderAssistant(new MybatisConfiguration(), ""),
                ProductStoreLifecycleRecheckLogDO.class);
        LambdaUtils.installCache(tableInfo);

        ProductStoreLifecycleRecheckLogMapper mapper = mock(ProductStoreLifecycleRecheckLogMapper.class, CALLS_REAL_METHODS);
        AtomicReference<Wrapper<ProductStoreLifecycleRecheckLogDO>> wrapperRef = new AtomicReference<>();
        PageResult<ProductStoreLifecycleRecheckLogDO> expected = new PageResult<>(Collections.emptyList(), 0L);
        doAnswer(invocation -> {
            wrapperRef.set(invocation.getArgument(1));
            return expected;
        }).when(mapper).selectPage(any(PageParam.class), org.mockito.ArgumentMatchers.<Wrapper<ProductStoreLifecycleRecheckLogDO>>any());

        LocalDateTime begin = LocalDateTime.of(2026, 3, 5, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 5, 23, 59, 59);
        ProductStoreLifecycleRecheckLogPageReqVO reqVO = new ProductStoreLifecycleRecheckLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setRecheckNo("RECHECK-20260305");
        reqVO.setLogId(1001L);
        reqVO.setBatchNo("LIFECYCLE-20260305");
        reqVO.setTargetLifecycleStatus(35);
        reqVO.setOperator("运营同学");
        reqVO.setSource("ADMIN_UI");
        reqVO.setCreateTime(new LocalDateTime[]{begin, end});

        PageResult<ProductStoreLifecycleRecheckLogDO> actual = mapper.selectPage(reqVO);

        assertSame(expected, actual);
        Wrapper<ProductStoreLifecycleRecheckLogDO> wrapper = wrapperRef.get();
        assertNotNull(wrapper);
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("recheck_no"));
        assertTrue(sqlSegment.contains("log_id"));
        assertTrue(sqlSegment.contains("batch_no"));
        assertTrue(sqlSegment.contains("target_lifecycle_status"));
        assertTrue(sqlSegment.contains("operator"));
        assertTrue(sqlSegment.contains("source"));
        assertTrue(sqlSegment.contains("create_time"));

        AbstractWrapper<?, ?, ?> abstractWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
        Map<String, Object> params = abstractWrapper.getParamNameValuePairs();
        assertTrue(params.values().contains(1001L));
        assertTrue(params.values().contains(35));
        assertTrue(params.values().contains("ADMIN_UI"));
        assertTrue(params.values().contains(begin));
        assertTrue(params.values().contains(end));
        assertTrue(params.values().stream().anyMatch(v -> String.valueOf(v).contains("RECHECK-20260305")));
        assertTrue(params.values().stream().anyMatch(v -> String.valueOf(v).contains("LIFECYCLE-20260305")));
    }

    @Test
    void changeOrderSelectPage_shouldBuildFiltersWhenReqFieldsPresent() {
        TableInfo tableInfo = TableInfoHelper.initTableInfo(
                new MybatisMapperBuilderAssistant(new MybatisConfiguration(), ""),
                ProductStoreLifecycleChangeOrderDO.class);
        LambdaUtils.installCache(tableInfo);

        ProductStoreLifecycleChangeOrderMapper mapper =
                mock(ProductStoreLifecycleChangeOrderMapper.class, CALLS_REAL_METHODS);
        AtomicReference<Wrapper<ProductStoreLifecycleChangeOrderDO>> wrapperRef = new AtomicReference<>();
        PageResult<ProductStoreLifecycleChangeOrderDO> expected = new PageResult<>(Collections.emptyList(), 0L);
        doAnswer(invocation -> {
            wrapperRef.set(invocation.getArgument(1));
            return expected;
        }).when(mapper).selectPage(any(PageParam.class),
                org.mockito.ArgumentMatchers.<Wrapper<ProductStoreLifecycleChangeOrderDO>>any());

        LocalDateTime begin = LocalDateTime.of(2026, 3, 5, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 5, 23, 59, 59);
        ProductStoreLifecycleChangeOrderPageReqVO reqVO = new ProductStoreLifecycleChangeOrderPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(20);
        reqVO.setOrderNo("LCO-20260305");
        reqVO.setStoreId(2001L);
        reqVO.setStatus(10);
        reqVO.setFromLifecycleStatus(30);
        reqVO.setToLifecycleStatus(35);
        reqVO.setApplyOperator("运营同学");
        reqVO.setOverdue(true);
        reqVO.setLastActionCode("SUBMIT");
        reqVO.setLastActionOperator("审批同学");
        reqVO.setCreateTime(new LocalDateTime[]{begin, end});

        PageResult<ProductStoreLifecycleChangeOrderDO> actual = mapper.selectPage(reqVO);

        assertSame(expected, actual);
        Wrapper<ProductStoreLifecycleChangeOrderDO> wrapper = wrapperRef.get();
        assertNotNull(wrapper);
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("order_no"));
        assertTrue(sqlSegment.contains("store_id"));
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("from_lifecycle_status"));
        assertTrue(sqlSegment.contains("to_lifecycle_status"));
        assertTrue(sqlSegment.contains("apply_operator"));
        assertTrue(sqlSegment.contains("last_action_code"));
        assertTrue(sqlSegment.contains("last_action_operator"));
        assertTrue(sqlSegment.contains("sla_deadline_time"));
        assertTrue(sqlSegment.contains("create_time"));

        AbstractWrapper<?, ?, ?> abstractWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
        Map<String, Object> params = abstractWrapper.getParamNameValuePairs();
        assertTrue(params.values().contains(2001L));
        assertTrue(params.values().contains(10));
        assertTrue(params.values().contains(30));
        assertTrue(params.values().contains(35));
        assertTrue(params.values().contains(begin));
        assertTrue(params.values().contains(end));
        assertTrue(params.values().contains("SUBMIT"));
        assertTrue(params.values().stream().anyMatch(v -> String.valueOf(v).contains("LCO-20260305")));
        assertTrue(params.values().stream().anyMatch(v -> String.valueOf(v).contains("运营同学")));
        assertTrue(params.values().stream().anyMatch(v -> String.valueOf(v).contains("审批同学")));
    }

    @Test
    void changeOrderSelectSlaExpiredPendingList_shouldBuildDeadlineFilterAndLimit() {
        TableInfo tableInfo = TableInfoHelper.initTableInfo(
                new MybatisMapperBuilderAssistant(new MybatisConfiguration(), ""),
                ProductStoreLifecycleChangeOrderDO.class);
        LambdaUtils.installCache(tableInfo);

        ProductStoreLifecycleChangeOrderMapper mapper =
                mock(ProductStoreLifecycleChangeOrderMapper.class, CALLS_REAL_METHODS);
        AtomicReference<Wrapper<ProductStoreLifecycleChangeOrderDO>> wrapperRef = new AtomicReference<>();
        doAnswer(invocation -> {
            wrapperRef.set(invocation.getArgument(0));
            return Collections.emptyList();
        }).when(mapper).selectList(org.mockito.ArgumentMatchers.<Wrapper<ProductStoreLifecycleChangeOrderDO>>any());

        LocalDateTime now = LocalDateTime.of(2026, 3, 5, 18, 30);
        List<ProductStoreLifecycleChangeOrderDO> list = mapper.selectSlaExpiredPendingList(now, 100);

        assertNotNull(list);
        Wrapper<ProductStoreLifecycleChangeOrderDO> wrapper = wrapperRef.get();
        assertNotNull(wrapper);
        String sqlSegment = wrapper.getSqlSegment();
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("sla_deadline_time"));
        assertTrue(sqlSegment.contains("LIMIT 100"));
        AbstractWrapper<?, ?, ?> abstractWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
        assertTrue(abstractWrapper.getParamNameValuePairs().values().contains(10));
        assertTrue(abstractWrapper.getParamNameValuePairs().values().contains(now));
    }
}
