package com.zbkj.common.config;

import com.alibaba.fastjson.parser.ParserConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * fastjson 过渡期安全兜底配置：
 * 在彻底迁移到 Jackson/fastjson2 前，先禁用 autoType 并开启安全模式。
 */
@Slf4j
@Configuration
public class FastjsonSecurityConfig {

    @PostConstruct
    public void init() {
        ParserConfig parserConfig = ParserConfig.getGlobalInstance();
        parserConfig.setAutoTypeSupport(false);
        parserConfig.setSafeMode(true);
        log.info("Fastjson security mode enabled: autoType=false, safeMode=true");
    }
}

