package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreTagGroupListReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductStoreTagGroupMapper extends BaseMapperX<ProductStoreTagGroupDO> {

    default ProductStoreTagGroupDO selectByCode(String code) {
        return selectOne(ProductStoreTagGroupDO::getCode, code);
    }

    default List<ProductStoreTagGroupDO> selectList(ProductStoreTagGroupListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ProductStoreTagGroupDO>()
                .likeIfPresent(ProductStoreTagGroupDO::getCode, reqVO.getCode())
                .likeIfPresent(ProductStoreTagGroupDO::getName, reqVO.getName())
                .eqIfPresent(ProductStoreTagGroupDO::getStatus, reqVO.getStatus())
                .orderByDesc(ProductStoreTagGroupDO::getSort)
                .orderByDesc(ProductStoreTagGroupDO::getId));
    }

    default List<ProductStoreTagGroupDO> selectRequiredGroups(Integer status) {
        return selectList(new LambdaQueryWrapperX<ProductStoreTagGroupDO>()
                .eq(ProductStoreTagGroupDO::getRequired, 1)
                .eqIfPresent(ProductStoreTagGroupDO::getStatus, status)
                .orderByDesc(ProductStoreTagGroupDO::getSort)
                .orderByDesc(ProductStoreTagGroupDO::getId));
    }

    default List<ProductStoreTagGroupDO> selectByIds(List<Long> ids) {
        return selectBatchIds(ids);
    }
}
