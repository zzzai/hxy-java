package com.hxy.module.booking.service;

import com.hxy.module.booking.dal.dataobject.TechnicianDO;

import java.util.List;

/**
 * 技师 Service 接口
 */
public interface TechnicianService {

    /**
     * 创建技师
     */
    Long createTechnician(TechnicianDO technician);

    /**
     * 更新技师
     */
    void updateTechnician(TechnicianDO technician);

    /**
     * 删除技师
     */
    void deleteTechnician(Long id);

    /**
     * 获取技师
     */
    TechnicianDO getTechnician(Long id);

    /**
     * 根据用户ID获取技师
     */
    TechnicianDO getTechnicianByUserId(Long userId);

    /**
     * 获取门店技师列表
     */
    List<TechnicianDO> getTechnicianListByStoreId(Long storeId);

    /**
     * 获取门店启用的技师列表
     */
    List<TechnicianDO> getEnabledTechnicianListByStoreId(Long storeId);

    /**
     * 更新技师状态
     */
    void updateTechnicianStatus(Long id, Integer status);

}
