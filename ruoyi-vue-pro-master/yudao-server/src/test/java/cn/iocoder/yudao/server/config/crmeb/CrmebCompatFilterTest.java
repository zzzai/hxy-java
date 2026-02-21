package cn.iocoder.yudao.server.config.crmeb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CrmebCompatFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldReturn410WhenCompatDisabled() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setEnabled(false);
        CrmebCompatFilter filter = new CrmebCompatFilter(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/order/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(410, response.getStatus());
        assertEquals("disabled", response.getHeader("X-Compat-Status"));
        assertTrue(response.getContentAsString().contains("CRMEB"));
    }

    @Test
    public void shouldDisableNonCorePathInPaymentCoreMode() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setPaymentCoreOnlyMode(true);
        CrmebCompatFilter filter = new CrmebCompatFilter(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/order/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);
        assertEquals(410, response.getStatus());
    }

    @Test
    public void shouldPassCorePathInPaymentCoreMode() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setPaymentCoreOnlyMode(true);
        CrmebCompatFilter filter = new CrmebCompatFilter(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/pay/get/config");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
        assertEquals("deprecated", response.getHeader("X-Compat-Status"));
    }

    @Test
    public void shouldFallbackToDefaultIncludePathsWhenIncludePathEmpty() throws Exception {
        CrmebCompatProperties properties = new CrmebCompatProperties();
        properties.setEnabled(false);
        properties.setIncludePaths(Collections.emptyList());
        CrmebCompatFilter filter = new CrmebCompatFilter(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/front/order/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);
        assertEquals(410, response.getStatus());
    }
}
