package cn.iocoder.yudao.module.product.dal.mysql.store;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreAuditLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductStoreAuditLogMapper extends BaseMapperX<ProductStoreAuditLogDO> {
}
