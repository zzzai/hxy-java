package cn.iocoder.yudao.module.product.service.spu;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuUpdateStatusReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SPU_TYPE_MISMATCH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class TypedProductSpuAdminServiceTest {

    @Mock
    private ProductSpuService productSpuService;

    @InjectMocks
    private TypedProductSpuAdminService typedProductSpuAdminService;

    @Test
    void createSpu_shouldForceExpectedType() {
        ProductSpuSaveReqVO reqVO = new ProductSpuSaveReqVO();
        reqVO.setProductType(ProductTypeEnum.PHYSICAL.getType());
        when(productSpuService.createSpu(any(ProductSpuSaveReqVO.class))).thenReturn(100L);

        Long spuId = typedProductSpuAdminService.createSpu(ProductTypeEnum.SERVICE, reqVO);

        assertEquals(100L, spuId);
        ArgumentCaptor<ProductSpuSaveReqVO> captor = ArgumentCaptor.forClass(ProductSpuSaveReqVO.class);
        verify(productSpuService).createSpu(captor.capture());
        assertEquals(ProductTypeEnum.SERVICE.getType(), captor.getValue().getProductType());
    }

    @Test
    void applyPageType_shouldForceExpectedType() {
        ProductSpuPageReqVO reqVO = new ProductSpuPageReqVO();
        reqVO.setProductType(ProductTypeEnum.VIRTUAL.getType());

        typedProductSpuAdminService.applyPageType(ProductTypeEnum.PHYSICAL, reqVO);

        assertEquals(ProductTypeEnum.PHYSICAL.getType(), reqVO.getProductType());
    }

    @Test
    void validateSpuType_shouldThrowWhenMismatch() {
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(1L);
        spu.setProductType(ProductTypeEnum.PHYSICAL.getType());
        when(productSpuService.getSpu(1L)).thenReturn(spu);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> typedProductSpuAdminService.validateSpuType(1L, ProductTypeEnum.SERVICE));
        assertEquals(SPU_TYPE_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    void updateSpuStatus_shouldForceTypeValidation() {
        ProductSpuDO spu = new ProductSpuDO();
        spu.setId(3L);
        spu.setProductType(ProductTypeEnum.SERVICE.getType());
        when(productSpuService.getSpu(3L)).thenReturn(spu);

        ProductSpuUpdateStatusReqVO reqVO = new ProductSpuUpdateStatusReqVO();
        reqVO.setId(3L);
        reqVO.setStatus(1);

        typedProductSpuAdminService.updateSpuStatus(ProductTypeEnum.SERVICE, reqVO);

        verify(productSpuService).updateSpuStatus(reqVO);
    }

    @Test
    void getTabsCount_shouldFilterByType() {
        when(productSpuService.getSpuPage(any(ProductSpuPageReqVO.class)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 7L));

        Map<Integer, Long> counts = typedProductSpuAdminService.getTabsCount(ProductTypeEnum.PHYSICAL);

        assertEquals(5, counts.size());
        assertEquals(7L, counts.get(ProductSpuPageReqVO.FOR_SALE));
        verify(productSpuService, times(5)).getSpuPage(any(ProductSpuPageReqVO.class));
    }
}
