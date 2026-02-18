package com.zbkj.service.service.impl.privacy;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.model.privacy.LabelPolicy;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.privacy.LabelPolicyDao;
import com.zbkj.service.service.privacy.LabelPolicyService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 标签策略门禁服务实现
 */
@Service
public class LabelPolicyServiceImpl extends ServiceImpl<LabelPolicyDao, LabelPolicy> implements LabelPolicyService {

    @Resource
    private LabelPolicyDao dao;

    @Override
    public List<LabelPolicy> getList(Integer riskLevel, Integer enabled, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<LabelPolicy> lqw = new LambdaQueryWrapper<>();
        if (riskLevel != null) {
            lqw.eq(LabelPolicy::getRiskLevel, riskLevel);
        }
        if (enabled != null) {
            lqw.eq(LabelPolicy::getEnabled, enabled);
        }
        lqw.orderByAsc(LabelPolicy::getRiskLevel).orderByAsc(LabelPolicy::getLabelKey);
        return dao.selectList(lqw);
    }

    @Override
    public Boolean updateStatus(Long id, Integer enabled, String remarks) {
        LabelPolicy policy = dao.selectById(id);
        if (policy == null) {
            return false;
        }
        int now = nowSeconds();
        policy.setEnabled(enabled)
                .setRemarks(remarks)
                .setUpdatedAt(now);
        return dao.updateById(policy) > 0;
    }

    @Override
    public Boolean isAllowed(String labelKey, String purposeCode) {
        if (StrUtil.isBlank(labelKey)) {
            return false;
        }
        LabelPolicy policy = dao.selectOne(new LambdaQueryWrapper<LabelPolicy>()
                .eq(LabelPolicy::getLabelKey, labelKey)
                .eq(LabelPolicy::getEnabled, 1)
                .last("limit 1"));
        if (policy == null) {
            return false;
        }
        if (StrUtil.isBlank(policy.getPurposeWhitelistJson())) {
            return false;
        }
        JSONArray whitelist = JSONArray.parseArray(policy.getPurposeWhitelistJson());
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        if (StrUtil.isBlank(purposeCode)) {
            return false;
        }
        return whitelist.stream().anyMatch(item -> purposeCode.equals(String.valueOf(item)));
    }

    private int nowSeconds() {
        return (int) (System.currentTimeMillis() / 1000);
    }
}

