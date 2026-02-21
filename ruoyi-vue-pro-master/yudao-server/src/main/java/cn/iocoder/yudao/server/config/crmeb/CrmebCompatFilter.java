package cn.iocoder.yudao.server.config.crmeb;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * CRMEB 兼容路径前置过滤器。
 *
 * 说明：
 * 1. 过滤器在 Spring Security 之前执行，避免“鉴权先返回 401/200，导致 410 下线规则失效”。
 * 2. 与拦截器复用同一套配置，保证阶段切换语义一致。
 */
@Slf4j
@RequiredArgsConstructor
public class CrmebCompatFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> DEFAULT_INCLUDE_PATHS = Arrays.asList(
            "/api/front/**",
            "/api/admin/**"
    );

    private final CrmebCompatProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        Iterable<String> includePaths = properties.getIncludePaths();
        if (includePaths == null || !includePaths.iterator().hasNext()) {
            includePaths = DEFAULT_INCLUDE_PATHS;
        }
        Iterable<String> excludePaths = properties.getExcludePaths();
        if (excludePaths == null) {
            excludePaths = Collections.emptyList();
        }
        return !isPathMatched(includePaths, uri)
                || isPathMatched(excludePaths, uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
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

        if (!shouldDisable(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
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
    }

    private boolean shouldDisable(String uri) {
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
