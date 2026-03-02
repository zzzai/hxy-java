package cn.iocoder.yudao.module.trade.service.aftersale;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.trade.controller.admin.aftersale.vo.rule.AfterSaleRefundRuleSaveReqVO;
import cn.iocoder.yudao.module.trade.convert.aftersale.AfterSaleRefundRuleConvert;
import cn.iocoder.yudao.module.trade.dal.dataobject.aftersale.AfterSaleRefundRuleConfigDO;
import cn.iocoder.yudao.module.trade.dal.mysql.aftersale.AfterSaleRefundRuleConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 售后退款规则配置 Service 实现
 *
 * @author HXY
 */
@Service
@Validated
public class AfterSaleRefundRuleConfigServiceImpl implements AfterSaleRefundRuleConfigService {

    private static final DateTimeFormatter VERSION_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private AfterSaleRefundRuleConfigMapper afterSaleRefundRuleConfigMapper;

    @Override
    public AfterSaleRefundRuleConfigDO getLatestDbRule() {
        return afterSaleRefundRuleConfigMapper.selectLatest();
    }

    @Override
    public Long saveRule(AfterSaleRefundRuleSaveReqVO reqVO) {
        AfterSaleRefundRuleConfigDO latest = getLatestDbRule();
        AfterSaleRefundRuleConfigDO saveObj = AfterSaleRefundRuleConvert.INSTANCE.convert(reqVO);
        saveObj.setEnabled(ObjectUtil.defaultIfNull(saveObj.getEnabled(), Boolean.TRUE));
        saveObj.setAutoRefundMaxPrice(ObjectUtil.defaultIfNull(saveObj.getAutoRefundMaxPrice(), 5000));
        saveObj.setUserDailyApplyLimit(ObjectUtil.defaultIfNull(saveObj.getUserDailyApplyLimit(), 3));
        saveObj.setBlacklistUserIds(normalizeLongList(saveObj.getBlacklistUserIds()));
        saveObj.setSuspiciousOrderKeywords(normalizeStringList(saveObj.getSuspiciousOrderKeywords()));
        saveObj.setRuleVersion(resolveRuleVersion(saveObj.getRuleVersion(), latest));
        saveObj.setRemark(StrUtil.blankToDefault(StrUtil.trim(saveObj.getRemark()), ""));
        afterSaleRefundRuleConfigMapper.insert(saveObj);
        return saveObj.getId();
    }

    private String resolveRuleVersion(String requestVersion, AfterSaleRefundRuleConfigDO latest) {
        if (StrUtil.isNotBlank(requestVersion)) {
            return requestVersion.trim();
        }
        String latestVersion = latest != null ? latest.getRuleVersion() : null;
        if (StrUtil.startWithIgnoreCase(latestVersion, "v")
                && StrUtil.isNumeric(StrUtil.subSuf(latestVersion, 1))) {
            long current = Long.parseLong(StrUtil.subSuf(latestVersion, 1));
            return "v" + (current + 1);
        }
        return "v" + LocalDateTime.now().format(VERSION_TIME_FORMATTER);
    }

    private List<Long> normalizeLongList(List<Long> source) {
        if (CollUtil.isEmpty(source)) {
            return Collections.emptyList();
        }
        Set<Long> results = new LinkedHashSet<>();
        for (Long value : source) {
            if (value != null && value > 0) {
                results.add(value);
            }
        }
        return results.stream().collect(Collectors.toList());
    }

    private List<String> normalizeStringList(List<String> source) {
        if (CollUtil.isEmpty(source)) {
            return Collections.emptyList();
        }
        Set<String> results = new LinkedHashSet<>();
        for (String value : source) {
            if (StrUtil.isNotBlank(value)) {
                results.add(value.trim());
            }
        }
        return results.stream().collect(Collectors.toList());
    }

}
