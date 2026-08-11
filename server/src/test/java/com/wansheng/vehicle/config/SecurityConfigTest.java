package com.wansheng.vehicle.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void allowsLanDeploymentOrigin() {
        SecurityConfig securityConfig = new SecurityConfig(null);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader(HttpHeaders.ORIGIN, "http://192.168.3.110:5173");

        var corsConfiguration = securityConfig.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.checkOrigin("http://192.168.3.110:5173"))
                .isEqualTo("http://192.168.3.110:5173");
    }
}
