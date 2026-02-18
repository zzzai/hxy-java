package com.zbkj.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.model.wechat.WechatPayInfo;
import com.zbkj.service.dao.WechatPayInfoDao;
import com.zbkj.service.service.WechatPayInfoService;
import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 *  WechatPayInfoServiceImpl 接口实现
 *  +----------------------------------------------------------------------
 *  | CRMEB [ CRMEB赋能开发者，助力企业发展 ]
 *  +----------------------------------------------------------------------
 *  | Copyright (c) 2016~2025 https://www.crmeb.com All rights reserved.
 *  +----------------------------------------------------------------------
 *  | Licensed CRMEB并不是自由软件，未经许可不能去掉CRMEB相关版权
 *  +----------------------------------------------------------------------
 *  | Author: CRMEB Team <admin@crmeb.com>
 *  +----------------------------------------------------------------------
 */
@Service
public class WechatPayInfoServiceImpl extends ServiceImpl<WechatPayInfoDao, WechatPayInfo> implements WechatPayInfoService {

    @Resource
    private WechatPayInfoDao dao;

    /**
     * 获取详情（商户订单号）
     * @param outTradeNo 商户订单号
     * @return WechatPayInfo
     */
    @Override
    public WechatPayInfo getByNo(String outTradeNo) {
        List<WechatPayInfo> list = dao.selectList(
                Wrappers.<WechatPayInfo>lambdaQuery()
                        .eq(WechatPayInfo::getOutTradeNo, outTradeNo)
                        .orderByDesc(WechatPayInfo::getId)
                        .last("limit 1")
        );
        return CollUtil.isEmpty(list) ? null : list.get(0);
    }
}
