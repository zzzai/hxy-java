package cn.iocoder.yudao.module.trade.convert.order;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.order.vo.serviceorder.TradeServiceOrderRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.order.TradeServiceOrderDO;
import cn.iocoder.yudao.module.trade.enums.order.TradeServiceOrderStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface TradeServiceOrderConvert {

    TradeServiceOrderConvert INSTANCE = Mappers.getMapper(TradeServiceOrderConvert.class);

    TradeServiceOrderRespVO convert(TradeServiceOrderDO bean);

    default TradeServiceOrderRespVO convertWithMeta(TradeServiceOrderDO bean) {
        TradeServiceOrderRespVO vo = convert(bean);
        fillComputedFields(vo);
        return vo;
    }

    default List<TradeServiceOrderRespVO> convertListWithMeta(List<TradeServiceOrderDO> list) {
        if (list == null) {
            return null;
        }
        List<TradeServiceOrderRespVO> vos = new ArrayList<>(list.size());
        for (TradeServiceOrderDO bean : list) {
            vos.add(convertWithMeta(bean));
        }
        return vos;
    }

    default PageResult<TradeServiceOrderRespVO> convertPageWithMeta(PageResult<TradeServiceOrderDO> pageResult) {
        if (pageResult == null) {
            return PageResult.empty();
        }
        return new PageResult<>(convertListWithMeta(pageResult.getList()), pageResult.getTotal());
    }

    default void fillComputedFields(TradeServiceOrderRespVO vo) {
        if (vo == null) {
            return;
        }
        vo.setStatusName(TradeServiceOrderStatusEnum.getNameByStatus(vo.getStatus()));
    }

}
