package cn.iocoder.yudao.module.product.api.store;

import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuRespDTO;
import cn.iocoder.yudao.module.product.api.store.dto.ProductStoreSkuUpdateStockReqDTO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreMappingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStoreSkuApiImplTest {

    @Mock
    private ProductStoreMappingService productStoreMappingService;

    @InjectMocks
    private ProductStoreSkuApiImpl productStoreSkuApi;

    @Test
    void getStoreSkuMap_shouldDelegateToService() {
        ProductStoreSkuRespDTO respDTO = new ProductStoreSkuRespDTO();
        respDTO.setSkuId(22L);
        respDTO.setSalePrice(8800);
        when(productStoreMappingService.getStoreSkuMap(11L, Arrays.asList(22L)))
                .thenReturn(Collections.singletonMap(22L, respDTO));

        Map<Long, ProductStoreSkuRespDTO> result = productStoreSkuApi.getStoreSkuMap(11L, Arrays.asList(22L));

        assertEquals(1, result.size());
        assertEquals(8800, result.get(22L).getSalePrice());
        verify(productStoreMappingService).getStoreSkuMap(11L, Arrays.asList(22L));
    }

    @Test
    void updateStoreSkuStock_shouldDelegateToService() {
        ProductStoreSkuUpdateStockReqDTO reqDTO = new ProductStoreSkuUpdateStockReqDTO();
        reqDTO.setStoreId(11L);
        reqDTO.setBizType("TRADE_ORDER_RESERVE");
        reqDTO.setBizNo("T20260301001");
        ProductStoreSkuUpdateStockReqDTO.Item item = new ProductStoreSkuUpdateStockReqDTO.Item();
        item.setSkuId(22L);
        item.setIncrCount(-1);
        reqDTO.setItems(Collections.singletonList(item));

        productStoreSkuApi.updateStoreSkuStock(reqDTO);

        verify(productStoreMappingService).updateStoreSkuStock(reqDTO);
    }
}
