package cn.iocoder.yudao.module.product.dal.mysql.store;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreSkuPageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreSkuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ProductStoreSkuMapper extends BaseMapperX<ProductStoreSkuDO> {

    default ProductStoreSkuDO selectByStoreIdAndSkuId(Long storeId, Long skuId) {
        return selectOne(new LambdaQueryWrapperX<ProductStoreSkuDO>()
                .eq(ProductStoreSkuDO::getStoreId, storeId)
                .eq(ProductStoreSkuDO::getSkuId, skuId));
    }

    @Select("SELECT * FROM hxy_store_product_sku WHERE store_id = #{storeId} AND sku_id = #{skuId} LIMIT 1")
    ProductStoreSkuDO selectByStoreIdAndSkuIdIncludeDeleted(@Param("storeId") Long storeId,
                                                             @Param("skuId") Long skuId);

    @Update("UPDATE hxy_store_product_sku " +
            "SET spu_id = #{spuId}, sale_status = #{saleStatus}, sale_price = #{salePrice}, market_price = #{marketPrice}, " +
            "stock = #{stock}, sort = #{sort}, remark = #{remark}, deleted = b'0', update_time = NOW() " +
            "WHERE id = #{id}")
    int recoverById(ProductStoreSkuDO recoverObj);

    default PageResult<ProductStoreSkuDO> selectPage(ProductStoreSkuPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductStoreSkuDO>()
                .eqIfPresent(ProductStoreSkuDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(ProductStoreSkuDO::getSpuId, reqVO.getSpuId())
                .eqIfPresent(ProductStoreSkuDO::getSkuId, reqVO.getSkuId())
                .eqIfPresent(ProductStoreSkuDO::getSaleStatus, reqVO.getSaleStatus())
                .betweenIfPresent(ProductStoreSkuDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductStoreSkuDO::getId));
    }

    @Select("SELECT DISTINCT store_id FROM hxy_store_product_sku WHERE deleted = b'0' ORDER BY store_id ASC LIMIT #{limit}")
    List<Long> selectDistinctStoreIds(@Param("limit") Integer limit);

    default Long selectCountByStoreId(Long storeId) {
        return selectCount(ProductStoreSkuDO::getStoreId, storeId);
    }

    default Long selectPositiveStockCountByStoreId(Long storeId) {
        return selectCount(new LambdaQueryWrapperX<ProductStoreSkuDO>()
                .eq(ProductStoreSkuDO::getStoreId, storeId)
                .gt(ProductStoreSkuDO::getStock, 0));
    }

    default List<ProductStoreSkuDO> selectListByStoreIdAndSkuIds(Long storeId, Collection<Long> skuIds) {
        return selectList(new LambdaQueryWrapperX<ProductStoreSkuDO>()
                .eq(ProductStoreSkuDO::getStoreId, storeId)
                .inIfPresent(ProductStoreSkuDO::getSkuId, skuIds));
    }

    default int updateStockIncrByStoreIdAndSkuId(Long storeId, Long skuId, Integer incrCount) {
        LambdaUpdateWrapper<ProductStoreSkuDO> updateWrapper = new LambdaUpdateWrapper<ProductStoreSkuDO>()
                .setSql("stock = stock + " + incrCount)
                .eq(ProductStoreSkuDO::getStoreId, storeId)
                .eq(ProductStoreSkuDO::getSkuId, skuId);
        return update(null, updateWrapper);
    }

    default int updateStockDecrByStoreIdAndSkuId(Long storeId, Long skuId, Integer decrCount) {
        LambdaUpdateWrapper<ProductStoreSkuDO> updateWrapper = new LambdaUpdateWrapper<ProductStoreSkuDO>()
                .setSql("stock = stock - " + decrCount)
                .eq(ProductStoreSkuDO::getStoreId, storeId)
                .eq(ProductStoreSkuDO::getSkuId, skuId)
                .ge(ProductStoreSkuDO::getStock, decrCount);
        return update(null, updateWrapper);
    }
}
