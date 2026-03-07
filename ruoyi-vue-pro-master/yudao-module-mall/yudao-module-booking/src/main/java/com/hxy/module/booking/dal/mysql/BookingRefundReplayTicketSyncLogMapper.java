package com.hxy.module.booking.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import com.hxy.module.booking.dal.dataobject.BookingRefundReplayTicketSyncLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookingRefundReplayTicketSyncLogMapper extends BaseMapperX<BookingRefundReplayTicketSyncLogDO> {

    @Select({
            "<script>",
            "SELECT l.*",
            "FROM hxy_booking_refund_replay_ticket_sync_log l",
            "INNER JOIN (",
            "  SELECT run_id, notify_log_id, MAX(id) AS max_id",
            "  FROM hxy_booking_refund_replay_ticket_sync_log",
            "  WHERE deleted = 0",
            "    AND run_id = #{runId}",
            "  GROUP BY run_id, notify_log_id",
            ") latest ON l.id = latest.max_id",
            "WHERE l.deleted = 0",
            "ORDER BY l.id DESC",
            "</script>"
    })
    List<BookingRefundReplayTicketSyncLogDO> selectLatestByRunId(@Param("runId") String runId);

    @Select({
            "<script>",
            "SELECT l.*",
            "FROM hxy_booking_refund_replay_ticket_sync_log l",
            "INNER JOIN (",
            "  SELECT run_id, notify_log_id, MAX(id) AS max_id",
            "  FROM hxy_booking_refund_replay_ticket_sync_log",
            "  WHERE deleted = 0",
            "    AND run_id = #{runId}",
            "    AND notify_log_id IN",
            "    <foreach collection='notifyLogIds' item='notifyLogId' open='(' separator=',' close=')'>",
            "      #{notifyLogId}",
            "    </foreach>",
            "  GROUP BY run_id, notify_log_id",
            ") latest ON l.id = latest.max_id",
            "WHERE l.deleted = 0",
            "ORDER BY l.id DESC",
            "</script>"
    })
    List<BookingRefundReplayTicketSyncLogDO> selectLatestByRunIdAndNotifyLogIds(
            @Param("runId") String runId,
            @Param("notifyLogIds") List<Long> notifyLogIds);
}
