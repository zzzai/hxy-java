package com.zbkj.service.service.privacy;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.privacy.DataAccessTicket;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.privacy.DataAccessTicketCreateRequest;

import java.util.List;

/**
 * 数据访问工单服务
 */
public interface DataAccessTicketService extends IService<DataAccessTicket> {

    /**
     * 创建工单
     */
    DataAccessTicket createTicket(Integer applicantId, DataAccessTicketCreateRequest request);

    /**
     * 审批通过
     */
    Boolean approve(Long ticketId, Integer approverId);

    /**
     * 驳回
     */
    Boolean reject(Long ticketId, Integer approverId, String rejectReason);

    /**
     * 关闭
     */
    Boolean closeTicket(Long ticketId, Integer operatorId);

    /**
     * 列表
     */
    List<DataAccessTicket> getList(Integer status, PageParamRequest pageParamRequest);
}

