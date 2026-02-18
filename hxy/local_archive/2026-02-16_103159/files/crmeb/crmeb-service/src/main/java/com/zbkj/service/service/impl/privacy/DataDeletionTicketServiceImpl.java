package com.zbkj.service.service.impl.privacy;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.model.privacy.DataDeletionTicket;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.privacy.DataDeletionRequest;
import com.zbkj.service.dao.privacy.DataDeletionTicketDao;
import com.zbkj.service.service.privacy.DataDeletionTicketService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 数据删除工单服务实现
 */
@Service
public class DataDeletionTicketServiceImpl extends ServiceImpl<DataDeletionTicketDao, DataDeletionTicket>
        implements DataDeletionTicketService {

    @Resource
    private DataDeletionTicketDao dao;

    @Override
    public DataDeletionTicket requestDeletion(Integer userId, DataDeletionRequest request) {
        int now = nowSeconds();
        DataDeletionTicket ticket = new DataDeletionTicket();
        ticket.setTicketNo(generateTicketNo())
                .setTenantId(0)
                .setUserId(userId)
                .setScopeCode(request.getScopeCode())
                .setScopeJson(request.getScopeJson())
                .setStatus(1)
                .setCoolingUntil(now + 7 * 24 * 3600)
                .setRequestedAt(now)
                .setLegalHold(0)
                .setCreatedAt(now)
                .setUpdatedAt(now);
        dao.insert(ticket);
        return ticket;
    }

    @Override
    public Boolean cancelDeletion(Integer userId, Long ticketId) {
        DataDeletionTicket ticket = dao.selectById(ticketId);
        if (ticket == null || !ticket.getUserId().equals(userId)) {
            return false;
        }
        if (!(ticket.getStatus() == 0 || ticket.getStatus() == 1)) {
            return false;
        }
        int now = nowSeconds();
        ticket.setStatus(6)
                .setResultSummary("用户主动取消删除申请")
                .setUpdatedAt(now);
        return dao.updateById(ticket) > 0;
    }

    @Override
    public List<DataDeletionTicket> getByUser(Integer userId, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        return dao.selectList(new LambdaQueryWrapper<DataDeletionTicket>()
                .eq(DataDeletionTicket::getUserId, userId)
                .orderByDesc(DataDeletionTicket::getId));
    }

    @Override
    public List<DataDeletionTicket> getList(Integer status, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<DataDeletionTicket> lqw = new LambdaQueryWrapper<>();
        if (status != null) {
            lqw.eq(DataDeletionTicket::getStatus, status);
        }
        lqw.orderByDesc(DataDeletionTicket::getId);
        return dao.selectList(lqw);
    }

    private String generateTicketNo() {
        return "DDT" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
    }

    private int nowSeconds() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}

