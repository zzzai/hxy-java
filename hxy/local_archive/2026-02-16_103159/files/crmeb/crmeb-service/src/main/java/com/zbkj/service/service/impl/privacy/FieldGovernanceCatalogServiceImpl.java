package com.zbkj.service.service.impl.privacy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.zbkj.common.model.privacy.FieldGovernanceCatalog;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.service.dao.privacy.FieldGovernanceCatalogDao;
import com.zbkj.service.service.privacy.FieldGovernanceCatalogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 字段治理目录服务实现
 */
@Service
public class FieldGovernanceCatalogServiceImpl extends ServiceImpl<FieldGovernanceCatalogDao, FieldGovernanceCatalog>
        implements FieldGovernanceCatalogService {

    @Resource
    private FieldGovernanceCatalogDao dao;

    @Override
    public List<FieldGovernanceCatalog> getList(Integer necessityLevel, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        LambdaQueryWrapper<FieldGovernanceCatalog> lqw = new LambdaQueryWrapper<>();
        if (necessityLevel != null) {
            lqw.eq(FieldGovernanceCatalog::getNecessityLevel, necessityLevel);
        }
        lqw.orderByAsc(FieldGovernanceCatalog::getDomain)
                .orderByAsc(FieldGovernanceCatalog::getFieldCode);
        return dao.selectList(lqw);
    }
}

