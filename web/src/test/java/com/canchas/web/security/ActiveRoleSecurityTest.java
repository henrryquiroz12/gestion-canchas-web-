package com.canchas.web.security;

import com.canchas.web.config.SecurityConfig;
import com.canchas.web.controller.RoleSelectionController;
import com.canchas.web.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(classes = {SecurityConfig.class, ActiveRoleSecurityTest.TestConfig.class,
        RoleSelectionController.class})
@WithMockUser(roles = {"ADMIN", "CLIENTE"})
class ActiveRoleSecurityTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void adminContextBlocksClientViewsAndTournamentRegistration() throws Exception {
        mvc.perform(get("/admin/dashboard").sessionAttr("ACTIVE_ROLE", "ADMIN")).andExpect(status().isOk());
        for (String path : new String[]{"/cliente/perfil", "/cliente/reservas"}) {
            mvc.perform(get(path).sessionAttr("ACTIVE_ROLE", "ADMIN")).andExpect(status().isForbidden());
        }
        mvc.perform(get("/torneos/copa/inscribirse").sessionAttr("ACTIVE_ROLE", "ADMIN"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/torneos/copa/inscribirse").with(csrf()).sessionAttr("ACTIVE_ROLE", "ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clientContextBlocksAdminViews() throws Exception {
        mvc.perform(get("/cliente/perfil").sessionAttr("ACTIVE_ROLE", "CLIENTE")).andExpect(status().isOk());
        mvc.perform(get("/cliente/reservas").sessionAttr("ACTIVE_ROLE", "CLIENTE")).andExpect(status().isOk());
        mvc.perform(get("/admin/dashboard").sessionAttr("ACTIVE_ROLE", "CLIENTE"))
                .andExpect(status().isForbidden());
    }

    @Test
    void multiRoleAccountMustSelectContext() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        mvc.perform(get("/cliente/perfil")).andExpect(status().isForbidden());
    }

    @Test
    void switchingRoleChangesSessionAndPermissions() throws Exception {
        var result = mvc.perform(get("/seleccionar-rol/cliente").sessionAttr("ACTIVE_ROLE", "ADMIN"))
                .andExpect(redirectedUrl("/canchas"))
                .andExpect(request().sessionAttribute("ACTIVE_ROLE", "CLIENTE")).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession();
        mvc.perform(get("/cliente/reservas").session(session)).andExpect(status().isOk());
        mvc.perform(get("/admin/dashboard").session(session)).andExpect(status().isForbidden());
        mvc.perform(get("/seleccionar-rol/admin").session(session))
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(request().sessionAttribute("ACTIVE_ROLE", "ADMIN"));
        mvc.perform(get("/admin/dashboard").session(session)).andExpect(status().isOk());
        mvc.perform(get("/cliente/reservas").session(session)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void sessionCannotGrantAnUnassignedRole() throws Exception {
        mvc.perform(get("/admin/dashboard").sessionAttr("ACTIVE_ROLE", "ADMIN"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/seleccionar-rol/admin"))
                .andExpect(redirectedUrl("/error/access-denied"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cannotSelectUnassignedClientRole() throws Exception {
        mvc.perform(get("/seleccionar-rol/cliente")).andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {
        @Bean CustomUserDetailsService userDetailsService() { return mock(CustomUserDetailsService.class); }
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean ProtectedViews protectedViews() { return new ProtectedViews(); }
    }

    @RestController
    static class ProtectedViews {
        @GetMapping({"/admin/dashboard", "/cliente/perfil", "/cliente/reservas"})
        String view() { return "ok"; }
    }
}
