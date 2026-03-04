package cn.iocoder.yudao.module.product.controller.admin.store;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogPageReqVO;
import cn.iocoder.yudao.module.product.controller.admin.store.vo.ProductStoreLifecycleBatchLogRespVO;
import cn.iocoder.yudao.module.product.dal.dataobject.store.ProductStoreLifecycleBatchLogDO;
import cn.iocoder.yudao.module.product.service.store.ProductStoreLifecycleBatchLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 门店生命周期批量执行台账")
@RestController
@RequestMapping("/product/store/lifecycle-batch-log")
@Validated
public class ProductStoreLifecycleBatchLogController {

    @Resource
    private ProductStoreLifecycleBatchLogService lifecycleBatchLogService;

    @GetMapping("/page")
    @Operation(summary = "分页查询门店生命周期批量执行台账")
    @PreAuthorize("@ss.hasPermission('product:store:query')")
    public CommonResult<PageResult<ProductStoreLifecycleBatchLogRespVO>> pageLifecycleBatchLog(
            @Valid ProductStoreLifecycleBatchLogPageReqVO reqVO) {
        PageResult<ProductStoreLifecycleBatchLogDO> pageResult = lifecycleBatchLogService.getLifecycleBatchLogPage(reqVO);
        return success(BeanUtils.toBean(pageResult, ProductStoreLifecycleBatchLogRespVO.class));
    }
}
