package com.zbkj.service.service.privacy;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.privacy.LabelPolicy;
import com.zbkj.common.request.PageParamRequest;

import java.util.List;

/**
 * 标签策略门禁服务
 */
public interface LabelPolicyService extends IService<LabelPolicy> {

    /**
     * 列表
     */
    List<LabelPolicy> getList(Integer riskLevel, Integer enabled, PageParamRequest pageParamRequest);

    /**
     * 更新启用状态
     */
    Boolean updateStatus(Long id, Integer enabled, String remarks);

    /**
     * 判断标签是否允许在指定用途下使用
     */
    Boolean isAllowed(String labelKey, String purposeCode);
}

