package com.shortliner.analytics.controller;

import com.shortliner.analytics.entity.ClickEvent;
import com.shortliner.analytics.repository.ClickEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnalyticsControllerSecurityTest {

    private static final String OWNER = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @BeforeEach
    void setUp() {
        saveClick("own1", OWNER);
        saveClick("own1", OWNER);
        saveClick("other1", OTHER);
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/analytics/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsOnlyCallersOwnLinks() throws Exception {
        mockMvc.perform(get("/api/analytics/me").with(user(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].shortCode").value("own1"))
                .andExpect(jsonPath("$.content[0].clickCount").value(2));
    }

    @Test
    void userEndpointRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/analytics/user/" + OWNER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userEndpointForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/analytics/user/" + OWNER).with(user(OTHER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userEndpointAllowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/analytics/user/" + OWNER).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].shortCode").value("own1"));
    }

    @Test
    void perLinkStatsAreAnonymous() throws Exception {
        mockMvc.perform(get("/api/analytics/own1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(2));
        mockMvc.perform(get("/api/analytics/own1/daily"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealthAndPrometheusStayOpen() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
        // Prometheus export is off in tests (endpoint 404s); only assert security doesn't block it
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    private static RequestPostProcessor user(String sub) {
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_user"));
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_user"), new SimpleGrantedAuthority("ROLE_admin"));
    }

    private void saveClick(String shortCode, String userId) {
        ClickEvent event = new ClickEvent();
        event.setShortCode(shortCode);
        event.setUserId(userId);
        event.setTimestamp(Instant.now());
        event.setIp("10.0.0.1");
        event.setEventHash(UUID.randomUUID().toString().replace("-", ""));
        clickEventRepository.save(event);
    }
}
