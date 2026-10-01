package com.shortliner.analytics.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unknownPathReturns404() throws Exception {
        mockMvc.perform(get("/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(post("/api/analytics/abc/summary"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownShortCodeReturns404() throws Exception {
        mockMvc.perform(get("/api/analytics/unknown/summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Short Code Not Found"));
    }
}
