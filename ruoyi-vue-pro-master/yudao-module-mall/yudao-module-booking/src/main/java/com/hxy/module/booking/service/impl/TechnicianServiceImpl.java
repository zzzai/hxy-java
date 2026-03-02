package com.hxy.module.booking.service.impl;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import com.hxy.module.booking.dal.dataobject.TechnicianDO;
import com.hxy.module.booking.dal.mysql.TechnicianMapper;
import com.hxy.module.booking.service.TechnicianService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.hxy.module.booking.enums.ErrorCodeConstants.TECHNICIAN_NOT_EXISTS;

@Service
@Validated
@RequiredArgsConstructor
public class TechnicianServiceImpl implements TechnicianService {

    private final TechnicianMapper technicianMapper;

    @Override
    public Long createTechnician(TechnicianDO technician) {
        technicianMapper.insert(technician);
        return technician.getId();
    }

    @Override
    public void updateTechnician(TechnicianDO technician) {
        validateTechnicianExists(technician.getId());
        technicianMapper.updateById(technician);
    }

    @Override
    public void deleteTechnician(Long id) {
        validateTechnicianExists(id);
        technicianMapper.deleteById(id);
    }

    @Override
    public TechnicianDO getTechnician(Long id) {
        return technicianMapper.selectById(id);
    }

    @Override
    public TechnicianDO getTechnicianByUserId(Long userId) {
        return technicianMapper.selectByUserId(userId);
    }

    @Override
    public List<TechnicianDO> getTechnicianListByStoreId(Long storeId) {
        return technicianMapper.selectListByStoreId(storeId);
    }

    @Override
    public List<TechnicianDO> getEnabledTechnicianListByStoreId(Long storeId) {
        return technicianMapper.selectListByStoreIdAndStatus(storeId, CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public void updateTechnicianStatus(Long id, Integer status) {
        validateTechnicianExists(id);
        TechnicianDO update = new TechnicianDO();
        update.setId(id);
        update.setStatus(status);
        technicianMapper.updateById(update);
    }

    private void validateTechnicianExists(Long id) {
        if (technicianMapper.selectById(id) == null) {
            throw exception(TECHNICIAN_NOT_EXISTS);
        }
    }

}
