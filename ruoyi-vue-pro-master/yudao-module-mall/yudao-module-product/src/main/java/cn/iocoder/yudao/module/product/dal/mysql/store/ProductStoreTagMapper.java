package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreTagListReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreTagDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductStoreTagMapper extends BaseMapperX<ProductStoreTagDO> {

    default ProductStoreTagDO selectByCode(String code) {
        return selectOne(ProductStoreTagDO::getCode, code);
    }

    default List<ProductStoreTagDO> selectList(ProductStoreTagListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ProductStoreTagDO>()
                .likeIfPresent(ProductStoreTagDO::getCode, reqVO.getCode())
                .likeIfPresent(ProductStoreTagDO::getName, reqVO.getName())
                .eqIfPresent(ProductStoreTagDO::getGroupId, reqVO.getGroupId())
                .likeIfPresent(ProductStoreTagDO::getGroupName, reqVO.getGroupName())
                .eqIfPresent(ProductStoreTagDO::getStatus, reqVO.getStatus())
                .orderByDesc(ProductStoreTagDO::getSort)
                .orderByDesc(ProductStoreTagDO::getId));
    }
}
