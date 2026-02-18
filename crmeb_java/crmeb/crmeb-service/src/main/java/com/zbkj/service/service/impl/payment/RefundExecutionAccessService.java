package com.zbkj.service.service.impl.payment;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.zbkj.common.constants.SysConfigConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.system.SystemAdmin;
import com.zbkj.common.utils.ConfigSwitchUtil;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.service.service.SystemConfigService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * 手工退款执行权限策略：支持总部专属开关 + 管理员/角色白名单。
 */
@Component
public class RefundExecutionAccessService {

    @Resource
    private SystemConfigService systemConfigService;

    public void assertManualRefundAllowed(SystemAdmin currentAdmin) {
        if (canExecuteManualRefund(currentAdmin)) {
            return;
        }
        throw new CrmebException("当前账号无总部退款执行权限，请提交退款工单由总部处理");
    }

    public boolean canExecuteManualRefund(SystemAdmin currentAdmin) {
        if (ObjectUtil.isNull(currentAdmin) || ObjectUtil.isNull(currentAdmin.getId())) {
            return false;
        }
        if (!isHqOnlyEnabled()) {
            return true;
        }
        if (isSuperAdmin(currentAdmin)) {
            return true;
        }
        if (inAllowAdminIdList(currentAdmin.getId())) {
            return true;
        }
        return inAllowRoleList(currentAdmin.getRoles());
    }

    private boolean isHqOnlyEnabled() {
        String raw = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_HQ_ONLY_ENABLE);
        return ConfigSwitchUtil.isOn(StrUtil.blankToDefault(raw, "1"));
    }

    private boolean isSuperAdmin(SystemAdmin currentAdmin) {
        return parseIntList(currentAdmin.getRoles()).contains(1);
    }

    private boolean inAllowAdminIdList(Integer adminId) {
        List<Integer> allowAdminIds = parseIntList(
                systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_HQ_ADMIN_IDS));
        return allowAdminIds.contains(adminId);
    }

    private boolean inAllowRoleList(String roleIdsRaw) {
        List<Integer> allowRoleIds = parseIntList(
                systemConfigService.getValueByKey(SysConfigConstants.CONFIG_PAY_REFUND_HQ_ROLE_IDS));
        if (allowRoleIds.isEmpty() || StrUtil.isBlank(roleIdsRaw)) {
            return false;
        }
        List<Integer> adminRoleIds = parseIntList(roleIdsRaw);
        for (Integer roleId : adminRoleIds) {
            if (allowRoleIds.contains(roleId)) {
                return true;
            }
        }
        return false;
    }

    private List<Integer> parseIntList(String raw) {
        if (StrUtil.isBlank(raw)) {
            return Collections.emptyList();
        }
        try {
            return CrmebUtil.stringToArray(raw.trim());
        } catch (Exception ignore) {
            return Collections.emptyList();
        }
    }
}
