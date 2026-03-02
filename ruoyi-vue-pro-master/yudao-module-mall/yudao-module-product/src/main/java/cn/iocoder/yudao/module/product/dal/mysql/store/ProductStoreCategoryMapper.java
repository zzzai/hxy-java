package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreCategoryListReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductStoreCategoryMapper extends BaseMapperX<ProductStoreCategoryDO> {

    default ProductStoreCategoryDO selectByCode(String code) {
        return selectOne(ProductStoreCategoryDO::getCode, code);
    }

    default List<ProductStoreCategoryDO> selectList(ProductStoreCategoryListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ProductStoreCategoryDO>()
                .likeIfPresent(ProductStoreCategoryDO::getCode, reqVO.getCode())
                .likeIfPresent(ProductStoreCategoryDO::getName, reqVO.getName())
                .eqIfPresent(ProductStoreCategoryDO::getParentId, reqVO.getParentId())
                .eqIfPresent(ProductStoreCategoryDO::getLevel, reqVO.getLevel())
                .eqIfPresent(ProductStoreCategoryDO::getStatus, reqVO.getStatus())
                .orderByDesc(ProductStoreCategoryDO::getSort)
                .orderByDesc(ProductStoreCategoryDO::getId));
    }
}
