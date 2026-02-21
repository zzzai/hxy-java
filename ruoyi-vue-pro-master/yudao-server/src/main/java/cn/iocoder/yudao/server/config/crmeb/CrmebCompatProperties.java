package cn.iocoder.yudao.server.config.crmeb;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CRMEB 兼容层的运行开关与收敛策略。
 *
 * 目标是支持“先兼容、后切流、再下线”的迁移节奏。
 */
@Data
@ConfigurationProperties(prefix = "yudao.compat.crmeb")
public class CrmebCompatProperties {

    /**
     * 是否启用 CRMEB 兼容接口。
     */
    private boolean enabled = true;

    /**
     * 是否在兼容接口响应中添加弃用提示 Header。
     */
    private boolean emitDeprecationHeaders = true;

    /**
     * 是否打印兼容接口访问审计日志。
     */
    private boolean auditEnabled = true;

    /**
     * 兼容接口 Sunset 时间（HTTP-date 格式）。
     */
    private String sunset = "";

    /**
     * 兼容接口关闭时返回的业务错误码。
     */
    private Integer disableCode = 410001;

    /**
     * 兼容接口关闭时返回的错误提示。
     */
    private String disableMessage = "CRMEB 兼容接口已关闭，请切换至 /app-api 或 /admin-api。";

    /**
     * 需要被兼容层拦截的路径。
     */
    private List<String> includePaths = new ArrayList<>(Arrays.asList(
            "/api/front/**",
            "/api/admin/**"
    ));

    /**
     * 需要从兼容层拦截中排除的路径。
     */
    private List<String> excludePaths = new ArrayList<>();

    /**
     * 分阶段下线旧接口时，按路径匹配直接返回 410。
     * 示例：/api/front/order/**, /api/admin/store/order/**
     */
    private List<String> disabledPaths = new ArrayList<>();

    /**
     * 快迁模式：仅保留支付核心接口，其它兼容接口统一 410。
     */
    private boolean paymentCoreOnlyMode = false;

    /**
     * 快迁模式下允许放行的支付核心接口路径。
     */
    private List<String> paymentCorePaths = new ArrayList<>(Arrays.asList(
            "/api/front/wechat/authorize/program/login",
            "/api/front/order/pre/order",
            "/api/front/order/load/pre/**",
            "/api/front/order/computed/price",
            "/api/front/order/create",
            "/api/front/order/get/pay/config",
            "/api/front/order/refund",
            "/api/front/order/refund/reason",
            "/api/front/order/apply/refund/**",
            "/api/front/pay/payment",
            "/api/front/pay/queryPayResult",
            "/api/front/pay/get/config",
            "/api/admin/payment/callback/wechat",
            "/api/admin/payment/callback/wechat/refund",
            "/api/admin/system/config/**",
            "/api/admin/store/order/refund/**"
    ));

}
