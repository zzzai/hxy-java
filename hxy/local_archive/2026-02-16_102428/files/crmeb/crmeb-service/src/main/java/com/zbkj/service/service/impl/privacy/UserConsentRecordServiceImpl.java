package com.zbkj.service.service.impl.privacy;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.model.privacy.UserConsentRecord;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.privacy.ConsentGrantRequest;
import com.zbkj.service.dao.privacy.UserConsentRecordDao;
import com.zbkj.service.service.privacy.UserConsentRecordService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户授权记录服务实现
 */
@Service
public class UserConsentRecordServiceImpl extends ServiceImpl<UserConsentRecordDao, UserConsentRecord>
        implements UserConsentRecordService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_GRANTED = 1;
    private static final int STATUS_DENIED = 2;
    private static final int STATUS_WITHDRAWN = 3;

    @Resource
    private UserConsentRecordDao dao;

    @Override
    public UserConsentRecord grant(Integer userId, ConsentGrantRequest request) {
        int now = nowSeconds();
        UserConsentRecord latest = dao.selectOne(new LambdaQueryWrapper<UserConsentRecord>()
                .eq(UserConsentRecord::getUserId, userId)
                .eq(UserConsentRecord::getScenarioCode, request.getScenarioCode())
                .orderByDesc(UserConsentRecord::getId)
                .last("limit 1"));

        if (latest == null) {
            latest = new UserConsentRecord();
            latest.setTenantId(0)
                    .setUserId(userId)
                    .setStoreId(request.getStoreId() == null ? 0 : request.getStoreId())
                    .setScenarioCode(request.getScenarioCode())
                    .setCreatedAt(now);
        }

        latest.setDataScopeJson(request.getDataScopeJson())
                .setPurposeCodesJson(request.getPurposeCodesJson())
                .setPolicyVersion(request.getPolicyVersion())
                .setConsentTextHash(request.getConsentTextHash())
                .setConsentStatus(STATUS_GRANTED)
                .setGrantedAt(now)
                .setWithdrawnAt(null)
                .setExpireAt(request.getExpireAt())
                .setSourceChannel(StrUtil.isBlank(request.getSourceChannel()) ? "miniapp" : request.getSourceChannel())
                .setOperatorType(1)
                .setOperatorId(userId)
                .setUpdatedAt(now);

        if (latest.getId() == null) {
            dao.insert(latest);
        } else {
            dao.updateById(latest);
        }
        return latest;
    }

    @Override
    public Boolean withdraw(Integer userId, Long consentId) {
        UserConsentRecord record = dao.selectById(consentId);
        if (record == null || !record.getUserId().equals(userId)) {
            return false;
        }
        if (record.getConsentStatus() == STATUS_WITHDRAWN || record.getConsentStatus() == STATUS_DENIED) {
            return true;
        }
        int now = nowSeconds();
        record.setConsentStatus(STATUS_WITHDRAWN)
                .setWithdrawnAt(now)
                .setUpdatedAt(now);
        return dao.updateById(record) > 0;
    }

    @Override
    public List<UserConsentRecord> getByUser(Integer userId, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        return dao.selectList(new LambdaQueryWrapper<UserConsentRecord>()
                .eq(UserConsentRecord::getUserId, userId)
                .orderByDesc(UserConsentRecord::getId));
    }

    @Override
    public List<UserConsentRecord> getAdminList(String scenarioCode, Integer status, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<UserConsentRecord> lqw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(scenarioCode)) {
            lqw.eq(UserConsentRecord::getScenarioCode, scenarioCode);
        }
        if (status != null) {
            lqw.eq(UserConsentRecord::getConsentStatus, status);
        }
        lqw.orderByDesc(UserConsentRecord::getId);
        return dao.selectList(lqw);
    }

    @Override
    public Boolean hasActiveConsent(Integer userId, String scenarioCode) {
        int now = nowSeconds();
        Integer count = dao.selectCount(new LambdaQueryWrapper<UserConsentRecord>()
                .eq(UserConsentRecord::getUserId, userId)
                .eq(UserConsentRecord::getScenarioCode, scenarioCode)
                .eq(UserConsentRecord::getConsentStatus, STATUS_GRANTED)
                .and(w -> w.isNull(UserConsentRecord::getExpireAt)
                        .or()
                        .eq(UserConsentRecord::getExpireAt, 0)
                        .or()
                        .gt(UserConsentRecord::getExpireAt, now)));
        return count != null && count > 0;
    }

    private int nowSeconds() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}
