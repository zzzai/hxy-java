package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagRelDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductStoreTagRelMapper extends BaseMapperX<ProductStoreTagRelDO> {

    default List<ProductStoreTagRelDO> selectListByStoreId(Long storeId) {
        return selectList(ProductStoreTagRelDO::getStoreId, storeId);
    }

    default void deleteByStoreId(Long storeId) {
        delete(new LambdaQueryWrapperX<ProductStoreTagRelDO>()
                .eq(ProductStoreTagRelDO::getStoreId, storeId));
    }
}
