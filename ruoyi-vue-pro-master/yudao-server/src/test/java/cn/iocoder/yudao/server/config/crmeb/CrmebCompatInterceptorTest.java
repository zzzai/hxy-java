package cn.iocoder.yudao.server.config.crmeb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class CrmebCompatInterceptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAllowAndSetDeprecationHeadersWhenCompatEnabled() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setEnabled(true);
        properties.setEmitDeprecationHeaders(true);
        properties.setAuditEnabled(false);
        properties.setSunset("Tue, 31 Dec 2030 23:59:59 GMT");
        CrmebCompatInterceptor interceptor = new CrmebCompatInterceptor(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/order/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals("crmeb", response.getHeader("X-Compat-Layer"));
        assertEquals("deprecated", response.getHeader("X-Compat-Status"));
        assertEquals("true", response.getHeader("Deprecation"));
        assertEquals("Tue, 31 Dec 2030 23:59:59 GMT", response.getHeader("Sunset"));
    }

    @Test
    void shouldSkipDeprecationHeadersWhenHeaderDisabled() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setEnabled(true);
        properties.setEmitDeprecationHeaders(false);
        properties.setAuditEnabled(false);
        CrmebCompatInterceptor interceptor = new CrmebCompatInterceptor(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/store/order/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertNull(response.getHeader("X-Compat-Layer"));
        assertNull(response.getHeader("X-Compat-Status"));
        assertNull(response.getHeader("Deprecation"));
        assertNull(response.getHeader("Sunset"));
    }

    @Test
    void shouldRejectWithGoneWhenCompatDisabled() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setEnabled(false);
        properties.setEmitDeprecationHeaders(true);
        properties.setAuditEnabled(false);
        properties.setDisableCode(410123);
        properties.setDisableMessage("compat disabled");
        CrmebCompatInterceptor interceptor = new CrmebCompatInterceptor(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/front/pay/payment");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(410, response.getStatus());
        assertNotNull(response.getContentType());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("disabled", response.getHeader("X-Compat-Status"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(410123, body.path("code").asInt());
        assertEquals("compat disabled", body.path("msg").asText());
    }

    @Test
    void shouldRejectWithGoneWhenDisabledPathMatched() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setEnabled(true);
        properties.setEmitDeprecationHeaders(true);
        properties.setAuditEnabled(false);
        properties.setDisabledPaths(Collections.singletonList("/api/front/order/**"));
        CrmebCompatInterceptor interceptor = new CrmebCompatInterceptor(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/order/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(410, response.getStatus());
        assertEquals("disabled", response.getHeader("X-Compat-Status"));
    }

    @Test
    void shouldAllowWhenDisabledPathNotMatched() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setEnabled(true);
        properties.setEmitDeprecationHeaders(true);
        properties.setAuditEnabled(false);
        properties.setDisabledPaths(Collections.singletonList("/api/front/order/**"));
        CrmebCompatInterceptor interceptor = new CrmebCompatInterceptor(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/pay/get/config");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals("deprecated", response.getHeader("X-Compat-Status"));
    }

    @Test
    void shouldRejectWhenPaymentCoreOnlyModeAndPathNotCore() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setEnabled(true);
        properties.setEmitDeprecationHeaders(true);
        properties.setAuditEnabled(false);
        properties.setPaymentCoreOnlyMode(true);
        properties.setPaymentCorePaths(Collections.singletonList("/api/front/pay/**"));
        CrmebCompatInterceptor interceptor = new CrmebCompatInterceptor(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/order/list");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(410, response.getStatus());
        assertEquals("disabled", response.getHeader("X-Compat-Status"));
    }

    @Test
    void shouldAllowWhenPaymentCoreOnlyModeAndPathIsCore() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setEnabled(true);
        properties.setEmitDeprecationHeaders(true);
        properties.setAuditEnabled(false);
        properties.setPaymentCoreOnlyMode(true);
        properties.setPaymentCorePaths(Arrays.asList("/api/front/pay/**", "/api/admin/payment/**"));
        CrmebCompatInterceptor interceptor = new CrmebCompatInterceptor(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/front/pay/payment");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals("deprecated", response.getHeader("X-Compat-Status"));
    }

}
