package com.zbkj.service.service.privacy;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.privacy.FieldGovernanceCatalog;
import com.zbkj.common.request.PageParamRequest;

import java.util.List;

/**
 * 字段治理目录服务
 */
public interface FieldGovernanceCatalogService extends IService<FieldGovernanceCatalog> {

    /**
     * 列表
     */
    List<FieldGovernanceCatalog> getList(Integer necessityLevel, PageParamRequest pageParamRequest);
}

