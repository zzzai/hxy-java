package cn.iocoder.yudao.module.product.service.spu;

import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuSaveReqVO;
import cn.iocoder.yudao.module.product.controller.admin.spu.vo.ProductSpuUpdateStatusReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.spu.ProductSpuDO;
import cn.iocoder.yudao.module.product.enums.spu.ProductTypeEnum;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SPU_NOT_EXISTS;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.SPU_TYPE_MISMATCH;

/**
 * 按商品类型隔离后台管理操作，避免服务项目与实物商品混管。
 */
@Service
public class TypedProductSpuAdminService {

    @Resource
    private ProductSpuService productSpuService;

    public Long createSpu(ProductTypeEnum expectedType, ProductSpuSaveReqVO reqVO) {
        reqVO.setProductType(expectedType.getType());
        return productSpuService.createSpu(reqVO);
    }

    public void updateSpu(ProductTypeEnum expectedType, ProductSpuSaveReqVO reqVO) {
        validateSpuType(reqVO.getId(), expectedType);
        reqVO.setProductType(expectedType.getType());
        productSpuService.updateSpu(reqVO);
    }

    public void deleteSpu(Long id, ProductTypeEnum expectedType) {
        validateSpuType(id, expectedType);
        productSpuService.deleteSpu(id);
    }

    public void updateSpuStatus(ProductTypeEnum expectedType, ProductSpuUpdateStatusReqVO reqVO) {
        validateSpuType(reqVO.getId(), expectedType);
        productSpuService.updateSpuStatus(reqVO);
    }

    public ProductSpuDO getTypedSpu(Long id, ProductTypeEnum expectedType) {
        ProductSpuDO spu = productSpuService.getSpu(id);
        if (spu == null) {
            throw exception(SPU_NOT_EXISTS);
        }
        assertType(spu, expectedType);
        return spu;
    }

    public void applyPageType(ProductTypeEnum expectedType, ProductSpuPageReqVO reqVO) {
        reqVO.setProductType(expectedType.getType());
    }

    public Map<Integer, Long> getTabsCount(ProductTypeEnum expectedType) {
        Map<Integer, Long> counts = new LinkedHashMap<>(5);
        counts.put(ProductSpuPageReqVO.FOR_SALE, getTypedTabCount(expectedType, ProductSpuPageReqVO.FOR_SALE));
        counts.put(ProductSpuPageReqVO.IN_WAREHOUSE, getTypedTabCount(expectedType, ProductSpuPageReqVO.IN_WAREHOUSE));
        counts.put(ProductSpuPageReqVO.SOLD_OUT, getTypedTabCount(expectedType, ProductSpuPageReqVO.SOLD_OUT));
        counts.put(ProductSpuPageReqVO.ALERT_STOCK, getTypedTabCount(expectedType, ProductSpuPageReqVO.ALERT_STOCK));
        counts.put(ProductSpuPageReqVO.RECYCLE_BIN, getTypedTabCount(expectedType, ProductSpuPageReqVO.RECYCLE_BIN));
        return counts;
    }

    public void validateSpuType(Long id, ProductTypeEnum expectedType) {
        ProductSpuDO spu = productSpuService.getSpu(id);
        if (spu == null) {
            throw exception(SPU_NOT_EXISTS);
        }
        assertType(spu, expectedType);
    }

    private Long getTypedTabCount(ProductTypeEnum expectedType, Integer tabType) {
        ProductSpuPageReqVO reqVO = new ProductSpuPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);
        reqVO.setTabType(tabType);
        reqVO.setProductType(expectedType.getType());
        return productSpuService.getSpuPage(reqVO).getTotal();
    }

    private void assertType(ProductSpuDO spu, ProductTypeEnum expectedType) {
        if (expectedType.getType().equals(spu.getProductType())) {
            return;
        }
        throw exception(SPU_TYPE_MISMATCH, resolveTypeName(spu.getProductType()), expectedType.getName());
    }

    private String resolveTypeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        return Arrays.stream(ProductTypeEnum.values())
                .filter(item -> item.getType().equals(type))
                .map(ProductTypeEnum::getName)
                .findFirst()
                .orElse(String.valueOf(type));
    }
}
