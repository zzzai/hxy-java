package com.zbkj.service.service.impl.privacy;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.model.privacy.DataAccessTicket;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.privacy.DataAccessTicketCreateRequest;
import com.zbkj.service.dao.privacy.DataAccessTicketDao;
import com.zbkj.service.service.privacy.DataAccessTicketService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 数据访问工单服务实现
 */
@Service
public class DataAccessTicketServiceImpl extends ServiceImpl<DataAccessTicketDao, DataAccessTicket>
        implements DataAccessTicketService {

    @Resource
    private DataAccessTicketDao dao;

    @Override
    public DataAccessTicket createTicket(Integer applicantId, DataAccessTicketCreateRequest request) {
        int now = nowSeconds();
        DataAccessTicket ticket = new DataAccessTicket();
        boolean needApproval = request.getDataLevel() != null && request.getDataLevel() >= 3;
        ticket.setTicketNo(generateTicketNo())
                .setTenantId(0)
                .setUserId(request.getUserId())
                .setApplicantId(applicantId)
                .setApplicantRole(request.getApplicantRole())
                .setDataLevel(request.getDataLevel())
                .setDataFieldsJson(request.getDataFieldsJson())
                .setPurposeCode(request.getPurposeCode())
                .setReason(request.getReason())
                .setApprovalRequired(needApproval ? 1 : 0)
                .setStatus(needApproval ? 0 : 1)
                .setApprovedAt(needApproval ? null : now)
                .setExpireAt(now + (needApproval ? 2 * 24 * 3600 : 24 * 3600))
                .setTraceId(ticketTraceId(applicantId, request.getUserId(), now))
                .setCreatedAt(now)
                .setUpdatedAt(now);
        dao.insert(ticket);
        return ticket;
    }

    @Override
    public Boolean approve(Long ticketId, Integer approverId) {
        DataAccessTicket ticket = dao.selectById(ticketId);
        if (ticket == null || ticket.getStatus() != 0) {
            return false;
        }
        int now = nowSeconds();
        ticket.setStatus(2)
                .setApproverId(approverId)
                .setApprovedAt(now)
                .setUpdatedAt(now);
        return dao.updateById(ticket) > 0;
    }

    @Override
    public Boolean reject(Long ticketId, Integer approverId, String rejectReason) {
        DataAccessTicket ticket = dao.selectById(ticketId);
        if (ticket == null || ticket.getStatus() != 0) {
            return false;
        }
        int now = nowSeconds();
        ticket.setStatus(3)
                .setApproverId(approverId)
                .setRejectedAt(now)
                .setRejectReason(rejectReason)
                .setUpdatedAt(now);
        return dao.updateById(ticket) > 0;
    }

    @Override
    public Boolean closeTicket(Long ticketId, Integer operatorId) {
        DataAccessTicket ticket = dao.selectById(ticketId);
        if (ticket == null) {
            return false;
        }
        if (!(ticket.getStatus() == 1 || ticket.getStatus() == 2 || ticket.getStatus() == 4)) {
            return false;
        }
        int now = nowSeconds();
        ticket.setStatus(5)
                .setApproverId(ticket.getApproverId() == null ? operatorId : ticket.getApproverId())
                .setUpdatedAt(now);
        return dao.updateById(ticket) > 0;
    }

    @Override
    public List<DataAccessTicket> getList(Integer status, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<DataAccessTicket> lqw = new LambdaQueryWrapper<>();
        if (status != null) {
            lqw.eq(DataAccessTicket::getStatus, status);
        }
        lqw.orderByDesc(DataAccessTicket::getId);
        return dao.selectList(lqw);
    }

    private String generateTicketNo() {
        return "DAT" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
    }

    private String ticketTraceId(Integer applicantId, Integer userId, int now) {
        return "trace_" + applicantId + "_" + userId + "_" + now;
    }

    private int nowSeconds() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}

