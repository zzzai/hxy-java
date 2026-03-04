package cn.iocoder.yudao.module.product.service.store;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuUpdateStockReqDTO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuManualStockAdjustReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchAdjustReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuBatchSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuStockFlowPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreOptionRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuBatchSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuSaveReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.sku.ProductSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuStockFlowDO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSpuDO;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSkuStockFlowMapper;
import cn.iocoder.yudao.module.product.dal.mysql.store.ProductStoreSpuMapper;
import cn.iocoder.yudao.module.product.enums.store.ProductStoreSkuStockFlowStatusEnum;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import cn.iocoder.yudao.module.product.service.store.dto.ProductStoreSkuStockFlowBatchRetryResult;
import cn.iocoder.yudao.module.product.service.sku.ProductSkuService;
import cn.iocoder.yudao.module.product.service.spu.ProductSpuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SKU_NOT_EXISTS;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SKU_STOCK_NOT_ENOUGH;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_SKU_BATCH_ADJUST_FIELDS_EMPTY;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_SKU_STOCK_FLOW_TARGETS_EMPTY;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_SKU_STOCK_SERVICE_FORBIDDEN;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_SKU_STOCK_MANUAL_INCR_COUNT_INVALID;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_SKU_STOCK_MANUAL_SKU_DUPLICATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStoreMappingServiceImplTest {

    @Mock
    private ProductStoreSpuMapper storeSpuMapper;
    @Mock
    private ProductStoreSkuMapper storeSkuMapper;
    @Mock
    private ProductStoreSkuStockFlowMapper storeSkuStockFlowMapper;
    @Mock
    private ProductSpuService productSpuService;
    @Mock
    private ProductSkuService productSkuService;
    @Mock
    private ProductStoreService productStoreService;

    @InjectMocks
    private ProductStoreMappingServiceImpl productStoreMappingService;

    @Test
    void saveStoreSku_shouldAutoCreateStoreSpuWhenMissing() {
        ProductStoreSkuSaveReqVO reqVO = new ProductStoreSkuSaveReqVO();
        reqVO.setStoreId(11L);
        reqVO.setSkuId(22L);

        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(22L);
        sku.setSpuId(33L);
        sku.setPrice(9800);
        sku.setMarketPrice(10800);
        sku.setStock(9);
        when(productSkuService.getSku(22L)).thenReturn(sku);

        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(33L);
        spu.setProductType(ProductTypeEnum.SERVICE.getType());
        when(productSpuService.getSpu(33L)).thenReturn(spu);

        when(storeSpuMapper.selectByStoreIdAndSpuId(11L, 33L)).thenReturn(null);
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(null);

        doAnswer(invocation -> {
            ProductStoreSpuDO obj = invocation.getArgument(0);
            obj.setId(100L);
            return 1;
        }).when(storeSpuMapper).insert(any(ProductStoreSpuDO.class));
        doAnswer(invocation -> {
            ProductStoreSkuDO obj = invocation.getArgument(0);
            obj.setId(200L);
            return 1;
        }).when(storeSkuMapper).insert(any(ProductStoreSkuDO.class));

        Long id = productStoreMappingService.saveStoreSku(reqVO);

        assertEquals(200L, id);
        verify(storeSpuMapper).insert(any(ProductStoreSpuDO.class));
        verify(storeSkuMapper).insert(any(ProductStoreSkuDO.class));
    }

    @Test
    void saveStoreSku_shouldThrowWhenSkuNotExists() {
        ProductStoreSkuSaveReqVO reqVO = new ProductStoreSkuSaveReqVO();
        reqVO.setStoreId(11L);
        reqVO.setSkuId(22L);
        when(productSkuService.getSku(22L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreMappingService.saveStoreSku(reqVO));
        assertEquals(SKU_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void saveStoreSpu_shouldRecoverDeletedMappingInsteadOfInsert() {
        ProductStoreSpuSaveReqVO reqVO = new ProductStoreSpuSaveReqVO();
        reqVO.setStoreId(11L);
        reqVO.setSpuId(33L);
        reqVO.setSaleStatus(0);
        reqVO.setSort(1);
        reqVO.setRemark("recover-spu");

        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(33L);
        spu.setProductType(ProductTypeEnum.SERVICE.getType());
        when(productSpuService.getSpu(33L)).thenReturn(spu);

        when(storeSpuMapper.selectByStoreIdAndSpuId(11L, 33L)).thenReturn(null);
        ProductStoreSpuDO deleted = ProductStoreSpuDO.builder().id(100L).storeId(11L).spuId(33L).build();
        when(storeSpuMapper.selectByStoreIdAndSpuIdIncludeDeleted(11L, 33L)).thenReturn(deleted);
        when(storeSpuMapper.recoverById(any(ProductStoreSpuDO.class))).thenReturn(1);

        Long id = productStoreMappingService.saveStoreSpu(reqVO);

        assertEquals(100L, id);
        verify(storeSpuMapper).recoverById(any(ProductStoreSpuDO.class));
        verify(storeSpuMapper, never()).insert(any(ProductStoreSpuDO.class));
    }

    @Test
    void saveStoreSku_shouldRecoverDeletedStoreSpuMappingInsteadOfInsert() {
        ProductStoreSkuSaveReqVO reqVO = new ProductStoreSkuSaveReqVO();
        reqVO.setStoreId(11L);
        reqVO.setSkuId(22L);

        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(22L);
        sku.setSpuId(33L);
        sku.setPrice(9800);
        sku.setMarketPrice(10800);
        sku.setStock(9);
        when(productSkuService.getSku(22L)).thenReturn(sku);

        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(33L);
        spu.setProductType(ProductTypeEnum.SERVICE.getType());
        when(productSpuService.getSpu(33L)).thenReturn(spu);

        when(storeSpuMapper.selectByStoreIdAndSpuId(11L, 33L)).thenReturn(null);
        ProductStoreSpuDO deletedStoreSpu = ProductStoreSpuDO.builder().id(101L).storeId(11L).spuId(33L).build();
        when(storeSpuMapper.selectByStoreIdAndSpuIdIncludeDeleted(11L, 33L)).thenReturn(deletedStoreSpu);
        when(storeSpuMapper.recoverById(any(ProductStoreSpuDO.class))).thenReturn(1);

        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(null);
        when(storeSkuMapper.selectByStoreIdAndSkuIdIncludeDeleted(11L, 22L)).thenReturn(null);
        doAnswer(invocation -> {
            ProductStoreSkuDO obj = invocation.getArgument(0);
            obj.setId(200L);
            return 1;
        }).when(storeSkuMapper).insert(any(ProductStoreSkuDO.class));

        Long id = productStoreMappingService.saveStoreSku(reqVO);

        assertEquals(200L, id);
        verify(storeSpuMapper).recoverById(any(ProductStoreSpuDO.class));
        verify(storeSpuMapper, never()).insert(any(ProductStoreSpuDO.class));
    }

    @Test
    void saveStoreSku_shouldRecoverDeletedStoreSkuMappingInsteadOfInsert() {
        ProductStoreSkuSaveReqVO reqVO = new ProductStoreSkuSaveReqVO();
        reqVO.setStoreId(11L);
        reqVO.setSkuId(22L);

        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(22L);
        sku.setSpuId(33L);
        sku.setPrice(9800);
        sku.setMarketPrice(10800);
        sku.setStock(9);
        when(productSkuService.getSku(22L)).thenReturn(sku);

        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(33L);
        spu.setProductType(ProductTypeEnum.SERVICE.getType());
        when(productSpuService.getSpu(33L)).thenReturn(spu);

        ProductStoreSpuDO activeStoreSpu = ProductStoreSpuDO.builder().id(101L).storeId(11L).spuId(33L).build();
        when(storeSpuMapper.selectByStoreIdAndSpuId(11L, 33L)).thenReturn(activeStoreSpu);

        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(null);
        ProductStoreSkuDO deletedStoreSku = ProductStoreSkuDO.builder().id(201L).storeId(11L).spuId(33L).skuId(22L).build();
        when(storeSkuMapper.selectByStoreIdAndSkuIdIncludeDeleted(11L, 22L)).thenReturn(deletedStoreSku);
        when(storeSkuMapper.recoverById(any(ProductStoreSkuDO.class))).thenReturn(1);

        Long id = productStoreMappingService.saveStoreSku(reqVO);

        assertEquals(201L, id);
        verify(storeSkuMapper).recoverById(any(ProductStoreSkuDO.class));
        verify(storeSkuMapper, never()).insert(any(ProductStoreSkuDO.class));
    }

    @Test
    void getStoreOptions_shouldUseStoreMasterData() {
        ProductStoreOptionRespVO a = new ProductStoreOptionRespVO();
        a.setId(11L);
        a.setName("荷小悦-上海徐汇店");
        ProductStoreOptionRespVO b = new ProductStoreOptionRespVO();
        b.setId(12L);
        b.setName("荷小悦-深圳南山店");
        when(productStoreService.getStoreOptions("荷小悦")).thenReturn(Arrays.asList(a, b));

        List<ProductStoreOptionRespVO> options = productStoreMappingService.getStoreOptions("荷小悦");

        assertEquals(2, options.size());
        assertEquals("荷小悦-上海徐汇店", options.get(0).getName());
        verify(productStoreService).getStoreOptions("荷小悦");
    }

    @Test
    void batchSaveStoreSpu_shouldApplyToDistinctStoreIds() {
        ProductStoreSpuBatchSaveReqVO reqVO = new ProductStoreSpuBatchSaveReqVO();
        reqVO.setStoreIds(Arrays.asList(11L, 12L, 11L));
        reqVO.setSpuId(33L);
        reqVO.setSaleStatus(0);

        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(33L);
        spu.setProductType(ProductTypeEnum.SERVICE.getType());
        when(productSpuService.getSpu(33L)).thenReturn(spu);
        when(storeSpuMapper.selectByStoreIdAndSpuId(11L, 33L)).thenReturn(null);
        when(storeSpuMapper.selectByStoreIdAndSpuId(12L, 33L)).thenReturn(null);
        doAnswer(invocation -> {
            ProductStoreSpuDO obj = invocation.getArgument(0);
            obj.setId(Math.abs(obj.getStoreId()));
            return 1;
        }).when(storeSpuMapper).insert(any(ProductStoreSpuDO.class));

        Integer affected = productStoreMappingService.batchSaveStoreSpu(reqVO);

        assertEquals(2, affected);
    }

    @Test
    void batchSaveStoreSku_shouldApplyToDistinctStoreIds() {
        ProductStoreSkuBatchSaveReqVO reqVO = new ProductStoreSkuBatchSaveReqVO();
        reqVO.setStoreIds(Arrays.asList(11L, 12L, 11L));
        reqVO.setSkuId(22L);
        reqVO.setSalePrice(8800);

        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(22L);
        sku.setSpuId(33L);
        sku.setPrice(9800);
        sku.setMarketPrice(10800);
        sku.setStock(9);
        when(productSkuService.getSku(22L)).thenReturn(sku);

        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(33L);
        spu.setProductType(ProductTypeEnum.SERVICE.getType());
        when(productSpuService.getSpu(33L)).thenReturn(spu);
        when(storeSpuMapper.selectByStoreIdAndSpuId(11L, 33L)).thenReturn(null);
        when(storeSpuMapper.selectByStoreIdAndSpuId(12L, 33L)).thenReturn(null);
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(null);
        when(storeSkuMapper.selectByStoreIdAndSkuId(12L, 22L)).thenReturn(null);

        doAnswer(invocation -> {
            ProductStoreSpuDO obj = invocation.getArgument(0);
            obj.setId(Math.abs(obj.getStoreId()));
            return 1;
        }).when(storeSpuMapper).insert(any(ProductStoreSpuDO.class));
        doAnswer(invocation -> {
            ProductStoreSkuDO obj = invocation.getArgument(0);
            obj.setId(Math.abs(obj.getStoreId()) + 100L);
            return 1;
        }).when(storeSkuMapper).insert(any(ProductStoreSkuDO.class));

        Integer affected = productStoreMappingService.batchSaveStoreSku(reqVO);

        assertEquals(2, affected);
    }

    @Test
    void batchAdjustStoreSku_shouldThrowWhenNoAdjustFieldsProvided() {
        ProductStoreSkuBatchAdjustReqVO reqVO = new ProductStoreSkuBatchAdjustReqVO();
        reqVO.setStoreIds(Arrays.asList(11L, 12L));
        reqVO.setSkuId(22L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreMappingService.batchAdjustStoreSku(reqVO));
        assertEquals(STORE_SKU_BATCH_ADJUST_FIELDS_EMPTY.getCode(), ex.getCode());
    }

    @Test
    void batchAdjustStoreSku_shouldUpdateExistingMappingOnlyChangedFields() {
        ProductStoreSkuBatchAdjustReqVO reqVO = new ProductStoreSkuBatchAdjustReqVO();
        reqVO.setStoreIds(Arrays.asList(11L));
        reqVO.setSkuId(22L);
        reqVO.setSalePrice(8600);
        reqVO.setStock(20);
        reqVO.setRemark("总部统一调价");

        ProductSkuDO sku = new ProductSkuDO();
        sku.setId(22L);
        sku.setSpuId(33L);
        sku.setPrice(9800);
        sku.setMarketPrice(10800);
        sku.setStock(9);
        when(productSkuService.getSku(22L)).thenReturn(sku);

        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(33L);
        spu.setProductType(ProductTypeEnum.SERVICE.getType());
        when(productSpuService.getSpu(33L)).thenReturn(spu);
        ProductStoreSpuDO storeSpuDO = ProductStoreSpuDO.builder().id(101L).storeId(11L).spuId(33L).build();
        when(storeSpuMapper.selectByStoreIdAndSpuId(11L, 33L)).thenReturn(storeSpuDO);
        ProductStoreSkuDO existing = ProductStoreSkuDO.builder()
                .id(201L).storeId(11L).spuId(33L).skuId(22L)
                .saleStatus(0).salePrice(9800).marketPrice(10800).stock(9).sort(0).remark("")
                .build();
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(existing);

        Integer affected = productStoreMappingService.batchAdjustStoreSku(reqVO);

        assertEquals(1, affected);
        verify(storeSkuMapper).updateById(any(ProductStoreSkuDO.class));
    }

    @Test
    void getStoreSkuMap_shouldReturnSkuKeyedResult() {
        ProductStoreSkuDO a = ProductStoreSkuDO.builder()
                .id(301L).storeId(11L).spuId(33L).skuId(22L)
                .saleStatus(0).salePrice(8800).marketPrice(9800).stock(10)
                .build();
        ProductStoreSkuDO b = ProductStoreSkuDO.builder()
                .id(302L).storeId(11L).spuId(33L).skuId(23L)
                .saleStatus(1).salePrice(7800).marketPrice(8800).stock(3)
                .build();
        when(storeSkuMapper.selectListByStoreIdAndSkuIds(11L, Arrays.asList(22L, 23L)))
                .thenReturn(Arrays.asList(a, b));

        Map<Long, ProductStoreSkuRespDTO> result = productStoreMappingService.getStoreSkuMap(11L, Arrays.asList(22L, 23L));

        assertEquals(2, result.size());
        assertEquals(8800, result.get(22L).getSalePrice());
        assertEquals(3, result.get(23L).getStock());
    }

    @Test
    void getStoreSkuMap_shouldReturnEmptyWhenNoSkuIds() {
        Map<Long, ProductStoreSkuRespDTO> result = productStoreMappingService.getStoreSkuMap(11L, Collections.emptyList());
        assertEquals(0, result.size());
    }

    @Test
    void getStoreSkuStockFlowPage_shouldDelegateMapperQuery() {
        ProductStoreSkuStockFlowPageReqVO reqVO = new ProductStoreSkuStockFlowPageReqVO();
        reqVO.setStoreId(11L);
        reqVO.setBizType("MANUAL_REPLENISH_IN");
        ProductStoreSkuStockFlowDO flow = ProductStoreSkuStockFlowDO.builder()
                .id(9801L)
                .storeId(11L)
                .skuId(22L)
                .bizType("MANUAL_REPLENISH_IN")
                .bizNo("SUPPLY-001")
                .incrCount(10)
                .status(ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus())
                .build();
        when(storeSkuStockFlowMapper.selectPage(reqVO)).thenReturn(new PageResult<>(Collections.singletonList(flow), 1L));

        PageResult<ProductStoreSkuStockFlowDO> result = productStoreMappingService.getStoreSkuStockFlowPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("SUPPLY-001", result.getList().get(0).getBizNo());
    }

    @Test
    void updateStoreSkuStock_shouldThrowWhenStockNotEnoughBeforeDecr() {
        ProductStoreSkuUpdateStockReqDTO reqDTO = new ProductStoreSkuUpdateStockReqDTO();
        reqDTO.setStoreId(11L);
        reqDTO.setBizType("TRADE_ORDER_RESERVE");
        reqDTO.setBizNo("T20260301001");
        ProductStoreSkuUpdateStockReqDTO.Item item = new ProductStoreSkuUpdateStockReqDTO.Item();
        item.setSkuId(22L);
        item.setIncrCount(-2);
        reqDTO.setItems(Collections.singletonList(item));

        ProductStoreSkuDO current = ProductStoreSkuDO.builder().id(301L).storeId(11L).skuId(22L).stock(1).build();
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(current);
        ProductStoreSkuStockFlowDO pendingFlow = ProductStoreSkuStockFlowDO.builder()
                .id(9001L)
                .incrCount(-2)
                .status(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus())
                .retryCount(0)
                .build();
        when(storeSkuStockFlowMapper.selectByBizKey("TRADE_ORDER_RESERVE", "T20260301001", 11L, 22L))
                .thenReturn(pendingFlow);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9001L),
                eq(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()), eq(3),
                eq(0), any(), eq(""))).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreMappingService.updateStoreSkuStock(reqDTO));
        assertEquals(SKU_STOCK_NOT_ENOUGH.getCode(), ex.getCode());
    }

    @Test
    void updateStoreSkuStock_shouldApplyDecrAndIncr() {
        ProductStoreSkuUpdateStockReqDTO reqDTO = new ProductStoreSkuUpdateStockReqDTO();
        reqDTO.setStoreId(11L);
        reqDTO.setBizType("TRADE_ORDER_RESERVE");
        reqDTO.setBizNo("T20260301002");
        ProductStoreSkuUpdateStockReqDTO.Item decr = new ProductStoreSkuUpdateStockReqDTO.Item();
        decr.setSkuId(22L);
        decr.setIncrCount(-2);
        ProductStoreSkuUpdateStockReqDTO.Item incr = new ProductStoreSkuUpdateStockReqDTO.Item();
        incr.setSkuId(23L);
        incr.setIncrCount(3);
        reqDTO.setItems(Arrays.asList(decr, incr));

        ProductStoreSkuDO sku22 = ProductStoreSkuDO.builder().id(301L).storeId(11L).skuId(22L).stock(10).build();
        ProductStoreSkuDO sku23 = ProductStoreSkuDO.builder().id(302L).storeId(11L).skuId(23L).stock(2).build();
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(sku22);
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 23L)).thenReturn(sku23);
        ProductStoreSkuStockFlowDO flow22 = ProductStoreSkuStockFlowDO.builder()
                .id(9201L).incrCount(-2).status(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()).retryCount(0).build();
        ProductStoreSkuStockFlowDO flow23 = ProductStoreSkuStockFlowDO.builder()
                .id(9202L).incrCount(3).status(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()).retryCount(0).build();
        when(storeSkuStockFlowMapper.selectByBizKey("TRADE_ORDER_RESERVE", "T20260301002", 11L, 22L))
                .thenReturn(null, flow22);
        when(storeSkuStockFlowMapper.selectByBizKey("TRADE_ORDER_RESERVE", "T20260301002", 11L, 23L))
                .thenReturn(null, flow23);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9201L),
                eq(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()), eq(3),
                eq(0), any(), eq(""))).thenReturn(1);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9202L),
                eq(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()), eq(3),
                eq(0), any(), eq(""))).thenReturn(1);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(any(Long.class), eq(3),
                eq(ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus()),
                any(Integer.class), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(1);
        when(storeSkuMapper.updateStockDecrByStoreIdAndSkuId(11L, 22L, 2)).thenReturn(1);
        when(storeSkuMapper.updateStockIncrByStoreIdAndSkuId(11L, 23L, 3)).thenReturn(1);

        productStoreMappingService.updateStoreSkuStock(reqDTO);

        verify(storeSkuMapper).updateStockDecrByStoreIdAndSkuId(11L, 22L, 2);
        verify(storeSkuMapper).updateStockIncrByStoreIdAndSkuId(11L, 23L, 3);
        verify(storeSkuMapper, never()).updateStockDecrByStoreIdAndSkuId(eq(11L), eq(23L), any(Integer.class));
        verify(storeSkuStockFlowMapper, org.mockito.Mockito.times(2)).insert(any(ProductStoreSkuStockFlowDO.class));
        verify(storeSkuStockFlowMapper, org.mockito.Mockito.times(2))
                .updateStatusByIdAndOldStatus(any(Long.class),
                        eq(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()),
                        eq(3),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        eq(""));
        verify(storeSkuStockFlowMapper, org.mockito.Mockito.times(2))
                .updateStatusByIdAndOldStatus(any(Long.class),
                        eq(3),
                        eq(ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus()),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void updateStoreSkuStock_shouldSkipWhenFlowAlreadySuccess() {
        ProductStoreSkuUpdateStockReqDTO reqDTO = new ProductStoreSkuUpdateStockReqDTO();
        reqDTO.setStoreId(11L);
        reqDTO.setBizType("TRADE_ORDER_RESERVE");
        reqDTO.setBizNo("T20260301003");
        ProductStoreSkuUpdateStockReqDTO.Item decr = new ProductStoreSkuUpdateStockReqDTO.Item();
        decr.setSkuId(22L);
        decr.setIncrCount(-1);
        reqDTO.setItems(Collections.singletonList(decr));

        ProductStoreSkuStockFlowDO successFlow = ProductStoreSkuStockFlowDO.builder()
                .id(1001L)
                .incrCount(-1)
                .status(ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus())
                .build();
        when(storeSkuStockFlowMapper.selectByBizKey("TRADE_ORDER_RESERVE", "T20260301003", 11L, 22L))
                .thenReturn(successFlow);

        productStoreMappingService.updateStoreSkuStock(reqDTO);

        verify(storeSkuMapper, never()).updateStockDecrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
        verify(storeSkuMapper, never()).updateStockIncrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
    }

    @Test
    void retryStoreSkuStockFlow_shouldRetryFailedFlow() {
        ProductStoreSkuStockFlowDO failedFlow = ProductStoreSkuStockFlowDO.builder()
                .id(9301L)
                .storeId(11L)
                .skuId(22L)
                .incrCount(-1)
                .status(ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus())
                .retryCount(1)
                .build();
        when(storeSkuStockFlowMapper.selectRetryableList(any(), eq(100)))
                .thenReturn(Collections.singletonList(failedFlow));
        ProductStoreSkuDO sku22 = ProductStoreSkuDO.builder().id(301L).storeId(11L).skuId(22L).stock(10).build();
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(sku22);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9301L),
                eq(ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus()),
                eq(3), eq(1), any(), eq(""),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull())).thenReturn(1);
        when(storeSkuMapper.updateStockDecrByStoreIdAndSkuId(11L, 22L, 1)).thenReturn(1);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9301L), eq(3),
                eq(ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus()),
                eq(1), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(1);

        int successCount = productStoreMappingService.retryStoreSkuStockFlow(null);

        assertEquals(1, successCount);
        verify(storeSkuMapper).updateStockDecrByStoreIdAndSkuId(11L, 22L, 1);
    }

    @Test
    void retryStoreSkuStockFlowByIds_shouldRetryRetryableFlowOnly() {
        ProductStoreSkuStockFlowDO failedFlow = ProductStoreSkuStockFlowDO.builder()
                .id(9701L)
                .storeId(11L)
                .skuId(22L)
                .incrCount(-1)
                .status(ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus())
                .retryCount(1)
                .build();
        ProductStoreSkuStockFlowDO successFlow = ProductStoreSkuStockFlowDO.builder()
                .id(9702L)
                .storeId(11L)
                .skuId(23L)
                .incrCount(-1)
                .status(ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus())
                .retryCount(0)
                .build();
        when(storeSkuStockFlowMapper.selectBatchIds(Arrays.asList(9701L, 9702L)))
                .thenReturn(Arrays.asList(failedFlow, successFlow));
        ProductStoreSkuDO sku22 = ProductStoreSkuDO.builder().id(301L).storeId(11L).skuId(22L).stock(10).build();
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(sku22);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9701L),
                eq(ProductStoreSkuStockFlowStatusEnum.FAILED.getStatus()),
                eq(ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus()),
                eq(1), any(), eq(""), eq("库存运营A"), eq("ADMIN_UI"))).thenReturn(1);
        when(storeSkuMapper.updateStockDecrByStoreIdAndSkuId(11L, 22L, 1)).thenReturn(1);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9701L),
                eq(ProductStoreSkuStockFlowStatusEnum.PROCESSING.getStatus()),
                eq(ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus()),
                eq(1), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                eq("库存运营A"), eq("ADMIN_UI")))
                .thenReturn(1);

        ProductStoreSkuStockFlowBatchRetryResult result = productStoreMappingService.retryStoreSkuStockFlowByIds(
                Arrays.asList(9701L, 9702L, 9701L), "库存运营A", "admin_ui");

        assertEquals(2, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
        verify(storeSkuMapper).updateStockDecrByStoreIdAndSkuId(11L, 22L, 1);
        verify(storeSkuMapper, never()).updateStockDecrByStoreIdAndSkuId(eq(11L), eq(23L), any(Integer.class));
    }

    @Test
    void retryStoreSkuStockFlowByIds_shouldThrowWhenFlowIdsEmpty() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreMappingService.retryStoreSkuStockFlowByIds(Collections.emptyList(), "op", "source"));
        assertEquals(STORE_SKU_STOCK_FLOW_TARGETS_EMPTY.getCode(), ex.getCode());
    }

    @Test
    void updateStoreSkuStock_shouldSkipWhenClaimFlowFailed() {
        ProductStoreSkuUpdateStockReqDTO reqDTO = new ProductStoreSkuUpdateStockReqDTO();
        reqDTO.setStoreId(11L);
        reqDTO.setBizType("TRADE_ORDER_RESERVE");
        reqDTO.setBizNo("T20260301004");
        ProductStoreSkuUpdateStockReqDTO.Item decr = new ProductStoreSkuUpdateStockReqDTO.Item();
        decr.setSkuId(22L);
        decr.setIncrCount(-1);
        reqDTO.setItems(Collections.singletonList(decr));

        ProductStoreSkuStockFlowDO pendingFlow = ProductStoreSkuStockFlowDO.builder()
                .id(9401L)
                .incrCount(-1)
                .status(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus())
                .retryCount(0)
                .build();
        when(storeSkuStockFlowMapper.selectByBizKey("TRADE_ORDER_RESERVE", "T20260301004", 11L, 22L))
                .thenReturn(pendingFlow);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9401L),
                eq(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()), eq(3),
                eq(0), any(), eq(""))).thenReturn(0);

        productStoreMappingService.updateStoreSkuStock(reqDTO);

        verify(storeSkuMapper, never()).updateStockDecrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
        verify(storeSkuMapper, never()).updateStockIncrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
    }

    @Test
    void updateStoreSkuStock_shouldThrowWhenBizNoTooLong() {
        ProductStoreSkuUpdateStockReqDTO reqDTO = new ProductStoreSkuUpdateStockReqDTO();
        reqDTO.setStoreId(11L);
        reqDTO.setBizType("TRADE_ORDER_RESERVE");
        reqDTO.setBizNo("BIZNO_ABCDEFGHIJKLMNOPQRSTUVWXYZ_ABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789");
        ProductStoreSkuUpdateStockReqDTO.Item item = new ProductStoreSkuUpdateStockReqDTO.Item();
        item.setSkuId(22L);
        item.setIncrCount(-1);
        reqDTO.setItems(Collections.singletonList(item));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreMappingService.updateStoreSkuStock(reqDTO));
        assertEquals(1_008_009_005, ex.getCode());
    }

    @Test
    void updateStoreSkuStock_shouldThrowWhenIdempotencyFlowIncrCountConflict() {
        ProductStoreSkuUpdateStockReqDTO reqDTO = new ProductStoreSkuUpdateStockReqDTO();
        reqDTO.setStoreId(11L);
        reqDTO.setBizType("TRADE_ORDER_RESERVE");
        reqDTO.setBizNo("T20260301005");
        ProductStoreSkuUpdateStockReqDTO.Item item = new ProductStoreSkuUpdateStockReqDTO.Item();
        item.setSkuId(22L);
        item.setIncrCount(-2);
        reqDTO.setItems(Collections.singletonList(item));

        ProductStoreSkuStockFlowDO existedFlow = ProductStoreSkuStockFlowDO.builder()
                .id(9501L)
                .status(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus())
                .retryCount(0)
                .incrCount(-1)
                .build();
        when(storeSkuStockFlowMapper.selectByBizKey("TRADE_ORDER_RESERVE", "T20260301005", 11L, 22L))
                .thenReturn(existedFlow);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreMappingService.updateStoreSkuStock(reqDTO));
        assertEquals(1_008_009_006, ex.getCode());
        verify(storeSkuMapper, never()).updateStockDecrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
    }

    @Test
    void manualAdjustStoreSkuStock_shouldMapManualBizTypeAndApplyIncr() {
        ProductStoreSkuManualStockAdjustReqVO reqVO = new ProductStoreSkuManualStockAdjustReqVO();
        reqVO.setStoreId(11L);
        reqVO.setBizType("replenish_in");
        reqVO.setBizNo("SUPPLY-20260303-001");
        ProductStoreSkuManualStockAdjustReqVO.Item item = new ProductStoreSkuManualStockAdjustReqVO.Item();
        item.setSkuId(22L);
        item.setIncrCount(6);
        reqVO.setItems(Collections.singletonList(item));

        ProductStoreSkuDO sku = ProductStoreSkuDO.builder().id(301L).storeId(11L).skuId(22L).stock(10).build();
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(sku);
        ProductStoreSkuStockFlowDO flow = ProductStoreSkuStockFlowDO.builder()
                .id(9601L).incrCount(6).status(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()).retryCount(0).build();
        when(storeSkuStockFlowMapper.selectByBizKey("MANUAL_REPLENISH_IN", "SUPPLY-20260303-001", 11L, 22L))
                .thenReturn(null, flow);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9601L),
                eq(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()), eq(3),
                eq(0), any(), eq(""))).thenReturn(1);
        when(storeSkuMapper.updateStockIncrByStoreIdAndSkuId(11L, 22L, 6)).thenReturn(1);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9601L), eq(3),
                eq(ProductStoreSkuStockFlowStatusEnum.SUCCESS.getStatus()),
                eq(0), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(1);

        Integer affected = productStoreMappingService.manualAdjustStoreSkuStock(reqVO);

        assertEquals(1, affected);
        verify(storeSkuMapper).updateStockIncrByStoreIdAndSkuId(11L, 22L, 6);
    }

    @Test
    void manualAdjustStoreSkuStock_shouldThrowWhenDirectionMismatch() {
        ProductStoreSkuManualStockAdjustReqVO reqVO = new ProductStoreSkuManualStockAdjustReqVO();
        reqVO.setStoreId(11L);
        reqVO.setBizType("TRANSFER_OUT");
        reqVO.setBizNo("TRANSFER-20260303-001");
        ProductStoreSkuManualStockAdjustReqVO.Item item = new ProductStoreSkuManualStockAdjustReqVO.Item();
        item.setSkuId(22L);
        item.setIncrCount(2);
        reqVO.setItems(Collections.singletonList(item));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreMappingService.manualAdjustStoreSkuStock(reqVO));
        assertEquals(STORE_SKU_STOCK_MANUAL_INCR_COUNT_INVALID.getCode(), ex.getCode());
        verify(storeSkuMapper, never()).updateStockIncrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
        verify(storeSkuMapper, never()).updateStockDecrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
    }

    @Test
    void manualAdjustStoreSkuStock_shouldThrowWhenDuplicateSku() {
        ProductStoreSkuManualStockAdjustReqVO reqVO = new ProductStoreSkuManualStockAdjustReqVO();
        reqVO.setStoreId(11L);
        reqVO.setBizType("STOCKTAKE");
        reqVO.setBizNo("STOCKTAKE-20260303-001");
        ProductStoreSkuManualStockAdjustReqVO.Item item1 = new ProductStoreSkuManualStockAdjustReqVO.Item();
        item1.setSkuId(22L);
        item1.setIncrCount(1);
        ProductStoreSkuManualStockAdjustReqVO.Item item2 = new ProductStoreSkuManualStockAdjustReqVO.Item();
        item2.setSkuId(22L);
        item2.setIncrCount(-1);
        reqVO.setItems(Arrays.asList(item1, item2));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreMappingService.manualAdjustStoreSkuStock(reqVO));
        assertEquals(STORE_SKU_STOCK_MANUAL_SKU_DUPLICATED.getCode(), ex.getCode());
        verify(storeSkuMapper, never()).updateStockIncrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
        verify(storeSkuMapper, never()).updateStockDecrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
    }

    @Test
    void manualAdjustStoreSkuStock_shouldThrowWhenSkuBelongsToServiceProduct() {
        ProductStoreSkuManualStockAdjustReqVO reqVO = new ProductStoreSkuManualStockAdjustReqVO();
        reqVO.setStoreId(11L);
        reqVO.setBizType("REPLENISH_IN");
        reqVO.setBizNo("SUPPLY-20260304-001");
        ProductStoreSkuManualStockAdjustReqVO.Item item = new ProductStoreSkuManualStockAdjustReqVO.Item();
        item.setSkuId(22L);
        item.setIncrCount(5);
        reqVO.setItems(Collections.singletonList(item));

        ProductStoreSkuDO sku = ProductStoreSkuDO.builder().id(301L).storeId(11L).spuId(33L).skuId(22L).stock(10).build();
        when(storeSkuMapper.selectByStoreIdAndSkuId(11L, 22L)).thenReturn(sku);
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(33L);
        spu.setProductType(ProductTypeEnum.SERVICE.getType());
        when(productSpuService.getSpu(33L)).thenReturn(spu);
        ProductStoreSkuStockFlowDO flow = ProductStoreSkuStockFlowDO.builder()
                .id(9801L).incrCount(5).status(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()).retryCount(0).build();
        when(storeSkuStockFlowMapper.selectByBizKey("MANUAL_REPLENISH_IN", "SUPPLY-20260304-001", 11L, 22L))
                .thenReturn(null, flow);
        when(storeSkuStockFlowMapper.updateStatusByIdAndOldStatus(eq(9801L),
                eq(ProductStoreSkuStockFlowStatusEnum.PENDING.getStatus()), eq(3),
                eq(0), any(), eq(""))).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> productStoreMappingService.manualAdjustStoreSkuStock(reqVO));

        assertEquals(STORE_SKU_STOCK_SERVICE_FORBIDDEN.getCode(), ex.getCode());
        verify(storeSkuMapper, never()).updateStockIncrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
        verify(storeSkuMapper, never()).updateStockDecrByStoreIdAndSkuId(any(Long.class), any(Long.class), any(Integer.class));
    }
}
