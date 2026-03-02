package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStorePageReqVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.List;

@Mapper
public interface ProductStoreMapper extends BaseMapperX<ProductStoreDO> {

    default ProductStoreDO selectByCode(String code) {
        return selectOne(ProductStoreDO::getCode, code);
    }

    default PageResult<ProductStoreDO> selectPage(ProductStorePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductStoreDO>()
                .likeIfPresent(ProductStoreDO::getCode, reqVO.getCode())
                .likeIfPresent(ProductStoreDO::getName, reqVO.getName())
                .likeIfPresent(ProductStoreDO::getShortName, reqVO.getShortName())
                .eqIfPresent(ProductStoreDO::getCategoryId, reqVO.getCategoryId())
                .eqIfPresent(ProductStoreDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ProductStoreDO::getLifecycleStatus, reqVO.getLifecycleStatus())
                .likeIfPresent(ProductStoreDO::getContactMobile, reqVO.getContactMobile())
                .betweenIfPresent(ProductStoreDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProductStoreDO::getSort)
                .orderByDesc(ProductStoreDO::getId));
    }

    default List<ProductStoreDO> selectSimpleList(String keyword, Integer status) {
        LambdaQueryWrapperX<ProductStoreDO> wrapper = new LambdaQueryWrapperX<ProductStoreDO>()
                .eqIfPresent(ProductStoreDO::getStatus, status)
                .orderByDesc(ProductStoreDO::getSort)
                .orderByDesc(ProductStoreDO::getId)
                .last("LIMIT 200");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ProductStoreDO::getCode, keyword)
                    .or().like(ProductStoreDO::getName, keyword)
                    .or().like(ProductStoreDO::getShortName, keyword));
        }
        return selectList(wrapper);
    }
}
