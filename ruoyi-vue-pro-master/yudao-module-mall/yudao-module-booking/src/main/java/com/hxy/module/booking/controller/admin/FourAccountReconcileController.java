package com.hxy.module.booking.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcilePageReqVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileRespVO;
import com.hxy.module.booking.controller.admin.vo.FourAccountReconcileRunReqVO;
import com.hxy.module.booking.dal.dataobject.FourAccountReconcileDO;
import com.hxy.module.booking.service.FourAccountReconcileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;

@Tag(name = "管理后台 - 四账对账")
@RestController
@RequestMapping("/booking/four-account-reconcile")
@Validated
public class FourAccountReconcileController {

    @Resource
    private FourAccountReconcileService reconcileService;

    @GetMapping("/page")
    @Operation(summary = "分页查询四账对账记录")
    @PreAuthorize("@ss.hasPermission('booking:commission:query')")
    public CommonResult<PageResult<FourAccountReconcileRespVO>> page(@Valid FourAccountReconcilePageReqVO reqVO) {
        PageResult<FourAccountReconcileDO> pageResult = reconcileService.getReconcilePage(reqVO);
        return success(BeanUtils.toBean(pageResult, FourAccountReconcileRespVO.class));
    }

    @PostMapping("/run")
    @Operation(summary = "手工触发四账对账")
    @PreAuthorize("@ss.hasPermission('booking:commission:settlement')")
    public CommonResult<Long> run(@Valid @RequestBody FourAccountReconcileRunReqVO reqVO) {
        return success(reconcileService.runReconcile(reqVO.getBizDate(), reqVO.getSource(), resolveOperator()));
    }

    private String resolveOperator() {
        String nickname = getLoginUserNickname();
        if (nickname != null && !nickname.trim().isEmpty()) {
            return nickname.trim();
        }
        Long userId = getLoginUserId();
        return userId == null ? null : String.valueOf(userId);
    }
}

