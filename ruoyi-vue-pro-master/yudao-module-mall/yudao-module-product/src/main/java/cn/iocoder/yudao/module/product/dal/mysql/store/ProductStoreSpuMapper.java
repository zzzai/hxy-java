package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSpuPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSpuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductStoreSpuMapper extends BaseMapperX<ProductStoreSpuDO> {

    default ProductStoreSpuDO selectByStoreIdAndSpuId(Long storeId, Long spuId) {
        return selectOne(new LambdaQueryWrapperX<ProductStoreSpuDO>()
                .eq(ProductStoreSpuDO::getStoreId, storeId)
                .eq(ProductStoreSpuDO::getSpuId, spuId));
    }

    @Select("SELECT * FROM hxy_store_product_spu WHERE store_id = #{storeId} AND spu_id = #{spuId} LIMIT 1")
    ProductStoreSpuDO selectByStoreIdAndSpuIdIncludeDeleted(@Param("storeId") Long storeId,
                                                             @Param("spuId") Long spuId);

    @Update("UPDATE hxy_store_product_spu " +
            "SET product_type = #{productType}, sale_status = #{saleStatus}, sort = #{sort}, remark = #{remark}, " +
            "deleted = b'0', update_time = NOW() " +
            "WHERE id = #{id}")
    int recoverById(ProductStoreSpuDO recoverObj);

    default PageResult<ProductStoreSpuDO> selectPage(ProductStoreSpuPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductStoreSpuDO>()
                .eqIfPresent(ProductStoreSpuDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(ProductStoreSpuDO::getSpuId, reqVO.getSpuId())
                .eqIfPresent(ProductStoreSpuDO::getProductType, reqVO.getProductType())
                .eqIfPresent(ProductStoreSpuDO::getSaleStatus, reqVO.getSaleStatus())
                .betweenIfPresent(ProductStoreSpuDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductStoreSpuDO::getId));
    }

    @Select("SELECT DISTINCT store_id FROM hxy_store_product_spu WHERE deleted = b'0' ORDER BY store_id ASC LIMIT #{limit}")
    List<Long> selectDistinctStoreIds(@Param("limit") Integer limit);

    default Long selectCountByStoreId(Long storeId) {
        return selectCount(ProductStoreSpuDO::getStoreId, storeId);
    }
}
