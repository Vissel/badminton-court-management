package com.badminton.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /logout must be served by AuthenController (so the refresh token is
 * revoked). Spring Security's default LogoutFilter used to answer 302 ->
 * /login?logout, and the browser's follow-up GET /login (POST-only) rendered a
 * 405 error page.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LogoutFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void logoutIsHandledByTheControllerAndDoesNotRedirect() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist("Location"));
    }
}
