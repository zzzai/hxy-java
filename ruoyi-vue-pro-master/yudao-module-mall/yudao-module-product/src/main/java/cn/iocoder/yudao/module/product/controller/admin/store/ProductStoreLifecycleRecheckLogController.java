package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogGetRespVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleRecheckLogRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleRecheckLogDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreLifecycleRecheckLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.product.enums.ErrorCodeConstants.STORE_LIFECYCLE_RECHECK_LOG_NOT_EXISTS;

@Tag(name = "管理后台 - 门店生命周期复核台账")
@RestController
@RequestMapping("/product/store/lifecycle-recheck-log")
@Validated
public class ProductStoreLifecycleRecheckLogController {

    @Resource
    private ProductStoreLifecycleRecheckLogService lifecycleRecheckLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询门店生命周期复核台账")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<PageResult<ProductStoreLifecycleRecheckLogRespVO>> pageLifecycleRecheckLog(
            @Valid ProductStoreLifecycleRecheckLogPageReqVO reqVO) {
        PageResult<ProductStoreLifecycleRecheckLogDO> pageResult = lifecycleRecheckLogService.getLifecycleRecheckLogPage(reqVO);
        return success(BeanUtils.toBean(pageResult, ProductStoreLifecycleRecheckLogRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得门店生命周期复核台账详情")
    @Parameter(name = "id", required = true, description = "台账ID")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<ProductStoreLifecycleRecheckLogGetRespVO> getLifecycleRecheckLog(@RequestParam("id") Long id) {
        ProductStoreLifecycleRecheckLogDO log = lifecycleRecheckLogService.getLifecycleRecheckLog(id);
        if (log == null) {
            throw exception(STORE_LIFECYCLE_RECHECK_LOG_NOT_EXISTS);
        }
        ProductStoreLifecycleRecheckLogGetRespVO respVO =
                BeanUtils.toBean(log, ProductStoreLifecycleRecheckLogGetRespVO.class);
        Map<String, Object> detailView = JsonUtils.parseObjectQuietly(log.getDetailJson(),
                new TypeReference<Map<String, Object>>() {});
        respVO.setDetailView(detailView);
        respVO.setDetailParseError(Boolean.TRUE.equals(log.getDetailParseError())
                || (StringUtils.hasText(log.getDetailJson()) && detailView == null));
        return success(respVO);
    }
}
