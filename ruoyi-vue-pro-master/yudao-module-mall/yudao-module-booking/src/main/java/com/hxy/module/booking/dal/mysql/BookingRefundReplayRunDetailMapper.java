package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayRunDetailDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BookingRefundReplayRunDetailMapper extends BaseMapperX<BookingRefundReplayRunDetailDO> {

    default List<BookingRefundReplayRunDetailDO> selectByRunId(String runId) {
        return selectList(new LambdaQueryWrapper<BookingRefundReplayRunDetailDO>()
                .eq(BookingRefundReplayRunDetailDO::getRunId, runId)
                .orderByAsc(BookingRefundReplayRunDetailDO::getId));
    }

    default List<BookingRefundReplayRunDetailDO> selectByRunIdAndResultStatus(String runId, String resultStatus) {
        return selectList(new LambdaQueryWrapper<BookingRefundReplayRunDetailDO>()
                .eq(BookingRefundReplayRunDetailDO::getRunId, runId)
                .eq(BookingRefundReplayRunDetailDO::getResultStatus, resultStatus)
                .orderByAsc(BookingRefundReplayRunDetailDO::getId));
    }
}
