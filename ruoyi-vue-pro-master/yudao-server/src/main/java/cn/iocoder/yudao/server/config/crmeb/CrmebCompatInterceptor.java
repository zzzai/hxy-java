package cn.iocoder.yudao.server.config.crmeb;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * CRMEB 兼容接口拦截器：
 * 1. 给旧接口写入弃用提示头；
 * 2. 输出访问审计；
 * 3. 在切流阶段可一键关闭旧接口入口。
 */
@Slf4j
@RequiredArgsConstructor
public class CrmebCompatInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final CrmebCompatProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        if (properties.isEmitDeprecationHeaders()) {
            response.setHeader("X-Compat-Layer", "crmeb");
            response.setHeader("X-Compat-Status", "deprecated");
            response.setHeader("Deprecation", "true");
            if (properties.getSunset() != null && !properties.getSunset().trim().isEmpty()) {
                response.setHeader("Sunset", properties.getSunset().trim());
            }
        }

        if (properties.isAuditEnabled()) {
            log.info("[crmeb-compat][access] method={}, uri={}, query={}, ip={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    request.getRemoteAddr());
        }

        if (!shouldDisable(request)) {
            return true;
        }
        if (properties.isEmitDeprecationHeaders()) {
            response.setHeader("X-Compat-Status", "disabled");
        }

        CommonResult<Object> body = CommonResult.error(
                properties.getDisableCode(),
                properties.getDisableMessage());
        response.setStatus(HttpStatus.GONE.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }

    private boolean shouldDisable(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!properties.isEnabled()) {
            return true;
        }
        if (properties.isPaymentCoreOnlyMode()
                && !isPathMatched(properties.getPaymentCorePaths(), uri)) {
            return true;
        }
        return isPathMatched(properties.getDisabledPaths(), uri);
    }

    private boolean isPathMatched(Iterable<String> patterns, String uri) {
        if (patterns == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (pattern == null) {
                continue;
            }
            String trimmed = pattern.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (PATH_MATCHER.match(trimmed, uri)) {
                return true;
            }
        }
        return false;
    }

}
