package cn.iocoder.yudao.module.trade.dal.mysql.aftersale;

import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleRefundRuleConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 售后退款风控规则配置 Mapper
 *
 * @author HXY
 */
@Mapper
public interface AfterSaleRefundRuleConfigMapper extends BaseMapperX<AfterSaleRefundRuleConfigDO> {

    default AfterSaleRefundRuleConfigDO selectLatest() {
        List<AfterSaleRefundRuleConfigDO> list = selectList(new LambdaQueryWrapperX<AfterSaleRefundRuleConfigDO>()
                .orderByDesc(AfterSaleRefundRuleConfigDO::getId)
                .last("LIMIT 1"));
        return CollectionUtils.getFirst(list);
    }

}
