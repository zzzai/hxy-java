package cn.iocoder.yudao.module.trade.convert.aftersale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.ticket.AfterSaleReviewTicketRespVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleReviewTicketDO;
import cn.iocoder.yudao.module.trade.enums.aftersale.AfterSaleReviewTicketStatusEnum;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AfterSaleReviewTicketConvert {

    AfterSaleReviewTicketConvert INSTANCE = Mappers.getMapper(AfterSaleReviewTicketConvert.class);

    AfterSaleReviewTicketRespVO convert(AfterSaleReviewTicketDO bean);

    default AfterSaleReviewTicketRespVO convertWithMeta(AfterSaleReviewTicketDO bean) {
        AfterSaleReviewTicketRespVO vo = convert(bean);
        fillComputedFields(vo, LocalDateTime.now());
        return vo;
    }

    default PageResult<AfterSaleReviewTicketRespVO> convertPageWithMeta(PageResult<AfterSaleReviewTicketDO> pageResult) {
        if (pageResult == null) {
            return PageResult.empty();
        }
        List<AfterSaleReviewTicketRespVO> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        if (pageResult.getList() != null) {
            for (AfterSaleReviewTicketDO ticketDO : pageResult.getList()) {
                AfterSaleReviewTicketRespVO vo = convert(ticketDO);
                fillComputedFields(vo, now);
                list.add(vo);
            }
        }
        return new PageResult<>(list, pageResult.getTotal());
    }

    default void fillComputedFields(AfterSaleReviewTicketRespVO vo, LocalDateTime now) {
        if (vo == null) {
            return;
        }
        boolean overdue = AfterSaleReviewTicketStatusEnum.isPending(vo.getStatus())
                && vo.getSlaDeadlineTime() != null
                && vo.getSlaDeadlineTime().isBefore(now);
        vo.setOverdue(overdue);
    }

}
