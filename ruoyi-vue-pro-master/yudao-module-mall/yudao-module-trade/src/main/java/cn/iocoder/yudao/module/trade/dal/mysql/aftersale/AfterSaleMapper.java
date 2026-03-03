package cn.iocoder.yudao.module.trade.dal.mysql.aftersale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.AfterSalePageReqVO;
import cn.iocoder.yudao.module.trade.controller.app.aftersale.vo.AppAfterSalePageReqVO;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface AfterSaleMapper extends BaseMapperX<AfterSaleDO> {

    default PageResult<AfterSaleDO> selectPage(AfterSalePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AfterSaleDO>()
                .eqIfPresent(AfterSaleDO::getUserId, reqVO.getUserId())
                .likeIfPresent(AfterSaleDO::getNo, reqVO.getNo())
                .eqIfPresent(AfterSaleDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AfterSaleDO::getType, reqVO.getType())
                .eqIfPresent(AfterSaleDO::getWay, reqVO.getWay())
                .likeIfPresent(AfterSaleDO::getOrderNo, reqVO.getOrderNo())
                .likeIfPresent(AfterSaleDO::getSpuName, reqVO.getSpuName())
                .eqIfPresent(AfterSaleDO::getRefundLimitSource, reqVO.getRefundLimitSource())
                .betweenIfPresent(AfterSaleDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AfterSaleDO::getId));
    }

    default PageResult<AfterSaleDO> selectPage(Long userId, AppAfterSalePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<AfterSaleDO>()
                .eq(AfterSaleDO::getUserId, userId)
                .inIfPresent(AfterSaleDO::getStatus, pageReqVO.getStatuses())
                .orderByDesc(AfterSaleDO::getId));
    }

    default int updateByIdAndStatus(Long id, Integer status, AfterSaleDO update) {
        return update(update, new LambdaUpdateWrapper<AfterSaleDO>()
                .eq(AfterSaleDO::getId, id).eq(AfterSaleDO::getStatus, status));
    }

    default AfterSaleDO selectByIdAndUserId(Long id, Long userId) {
        return selectOne(AfterSaleDO::getId, id,
                AfterSaleDO::getUserId, userId);
    }

    default Long selectCountByUserIdAndStatus(Long userId, Collection<Integer> statuses) {
        return selectCount(new LambdaQueryWrapperX<AfterSaleDO>()
                .eq(AfterSaleDO::getUserId, userId)
                .in(AfterSaleDO::getStatus, statuses));
    }

    default Long selectCountByUserIdAndCreateTimeBetween(Long userId, LocalDateTime begin, LocalDateTime end) {
        return selectCount(new LambdaQueryWrapperX<AfterSaleDO>()
                .eq(AfterSaleDO::getUserId, userId)
                .between(AfterSaleDO::getCreateTime, begin, end));
    }

    default AfterSaleDO selectFirstByOrderNoAndStatuses(String orderNo, Collection<Integer> statuses) {
        return selectOne(new LambdaQueryWrapperX<AfterSaleDO>()
                .eq(AfterSaleDO::getOrderNo, orderNo)
                .inIfPresent(AfterSaleDO::getStatus, statuses)
                .orderByDesc(AfterSaleDO::getId)
                .last("LIMIT 1"));
    }

    default List<AfterSaleDO> selectListByOrderNoAndStatuses(String orderNo, Collection<Integer> statuses) {
        return selectList(new LambdaQueryWrapperX<AfterSaleDO>()
                .eq(AfterSaleDO::getOrderNo, orderNo)
                .inIfPresent(AfterSaleDO::getStatus, statuses)
                .orderByDesc(AfterSaleDO::getId));
    }

    @Select("<script>"
            + "SELECT COUNT(1) FROM trade_after_sale a "
            + "INNER JOIN trade_order o ON o.id = a.order_id "
            + "WHERE o.pick_up_store_id = #{storeId} "
            + "AND a.status IN "
            + "<foreach collection='statuses' item='status' open='(' separator=',' close=')'>"
            + "#{status}"
            + "</foreach>"
            + "</script>")
    Long selectCountByPickUpStoreIdAndStatuses(@Param("storeId") Long storeId,
                                               @Param("statuses") Collection<Integer> statuses);

}
