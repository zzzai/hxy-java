package cn.iocoder.yudao.server.config.crmeb;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * CRMEB 兼容接口统一控制配置。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CrmebCompatProperties.class)
public class CrmebCompatWebConfiguration implements WebMvcConfigurer {

    private static final List<String> DEFAULT_INCLUDE_PATHS = Arrays.asList(
            "/api/front/**",
            "/api/admin/**"
    );

    private final CrmebCompatProperties properties;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void logCompatConfig() {
        log.info("[crmeb-compat][config] enabled={}, paymentCoreOnlyMode={}, includePaths={}, excludePaths={}, paymentCorePaths={}",
                properties.isEnabled(),
                properties.isPaymentCoreOnlyMode(),
                effectiveIncludePaths(),
                properties.getExcludePaths(),
                properties.getPaymentCorePaths());
    }

    @Bean
    public FilterRegistrationBean<CrmebCompatFilter> crmebCompatFilterRegistration() {
        FilterRegistrationBean<CrmebCompatFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new CrmebCompatFilter(properties, objectMapper));
        bean.setName("crmebCompatFilter");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.addUrlPatterns("/*");
        return bean;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (properties.getExcludePaths() == null || properties.getExcludePaths().isEmpty()) {
            registry.addInterceptor(new CrmebCompatInterceptor(properties, objectMapper))
                    .addPathPatterns(effectiveIncludePaths());
            return;
        }
        registry.addInterceptor(new CrmebCompatInterceptor(properties, objectMapper))
                .addPathPatterns(effectiveIncludePaths())
                .excludePathPatterns(properties.getExcludePaths());
    }

    private List<String> effectiveIncludePaths() {
        if (properties.getIncludePaths() == null || properties.getIncludePaths().isEmpty()) {
            return DEFAULT_INCLUDE_PATHS;
        }
        return properties.getIncludePaths();
    }

}
