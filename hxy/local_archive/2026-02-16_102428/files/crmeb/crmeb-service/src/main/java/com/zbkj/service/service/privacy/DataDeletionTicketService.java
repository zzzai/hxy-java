package com.zbkj.service.service.privacy;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.privacy.DataDeletionTicket;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.privacy.DataDeletionRequest;

import java.util.List;

/**
 * 数据删除工单服务
 */
public interface DataDeletionTicketService extends IService<DataDeletionTicket> {

    /**
     * 提交删除申请
     */
    DataDeletionTicket requestDeletion(Integer userId, DataDeletionRequest request);

    /**
     * 撤销删除申请
     */
    Boolean cancelDeletion(Integer userId, Long ticketId);

    /**
     * 用户列表
     */
    List<DataDeletionTicket> getByUser(Integer userId, PageParamRequest pageParamRequest);

    /**
     * 管理列表
     */
    List<DataDeletionTicket> getList(Integer status, PageParamRequest pageParamRequest);
}

