package com.zbkj.service.service.privacy;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.privacy.UserConsentRecord;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.privacy.ConsentGrantRequest;

import java.util.List;

/**
 * 用户授权记录服务
 */
public interface UserConsentRecordService extends IService<UserConsentRecord> {

    /**
     * 授权
     */
    UserConsentRecord grant(Integer userId, ConsentGrantRequest request);

    /**
     * 撤回授权
     */
    Boolean withdraw(Integer userId, Long consentId);

    /**
     * 用户授权列表
     */
    List<UserConsentRecord> getByUser(Integer userId, PageParamRequest pageParamRequest);

    /**
     * 管理后台授权列表
     */
    List<UserConsentRecord> getAdminList(String scenarioCode, Integer status, PageParamRequest pageParamRequest);

    /**
     * 是否存在有效授权
     */
    Boolean hasActiveConsent(Integer userId, String scenarioCode);
}

