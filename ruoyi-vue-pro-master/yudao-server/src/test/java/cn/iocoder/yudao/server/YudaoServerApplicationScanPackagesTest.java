package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class YudaoServerApplicationScanPackagesTest {

    @Test
    void shouldScanHxyPackages() {
        SpringBootApplication annotation = YudaoServerApplication.class.getAnnotation(SpringBootApplication.class);
        String[] scanBasePackages = annotation.scanBasePackages();

        assertTrue(Arrays.asList(scanBasePackages).contains("com.hxy.server"));
        assertTrue(Arrays.asList(scanBasePackages).contains("com.hxy.module"));
    }

}
