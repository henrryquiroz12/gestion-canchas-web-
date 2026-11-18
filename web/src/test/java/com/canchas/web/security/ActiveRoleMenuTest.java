package com.canchas.web.security;

import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ActiveRoleMenuTest {
    @Test
    void adminMenuContainsOnlySwitchAndLogout() {
        String menu = renderMenu("ADMIN", true);
        assertTrue(menu.contains("Cambiar de rol"));
        assertTrue(menu.contains("Cerrar Sesión"));
        assertFalse(menu.contains("/cliente/"));
        assertEquals(2, menu.split("role=\"menuitem\"", -1).length - 1);
    }

    @Test
    void clientMenuContainsOnlyProfileReservationsAndLogout() {
        String menu = renderMenu("CLIENTE", false);
        assertTrue(menu.contains("Mi Perfil"));
        assertTrue(menu.contains("Mis Reservas"));
        assertTrue(menu.contains("Cerrar Sesión"));
        assertFalse(menu.contains("/admin/"));
        assertFalse(menu.contains("/seleccionar-rol"));
        assertEquals(3, menu.split("role=\"menuitem\"", -1).length - 1);
    }

    @Test
    void administratorInClientModeCanReturnToRoleSelector() {
        String menu = renderMenu("CLIENTE", true);
        assertTrue(menu.contains("Cambiar de rol"));
        assertTrue(menu.contains("/seleccionar-rol"));
        assertTrue(menu.contains("Mi Perfil"));
        assertTrue(menu.contains("Mis Reservas"));
        assertTrue(menu.contains("Cerrar Sesión"));
        assertFalse(menu.contains("/admin/"));
        assertEquals(4, menu.split("role=\"menuitem\"", -1).length - 1);
    }

    private String renderMenu(String role, boolean canSwitchRole) {
        var resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode(TemplateMode.HTML);
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        // Context-relative URLs can be rendered without a servlet for this fragment test.
        engine.setLinkBuilder(new StandardLinkBuilder() {
            @Override
            protected String computeContextPath(org.thymeleaf.context.IExpressionContext context,
                                                String base, java.util.Map<String, Object> parameters) {
                return "";
            }
        });
        var context = new Context();
        context.setVariable("activeRole", role);
        context.setVariable("canSwitchRole", canSwitchRole);
        context.setVariable("isAdminSection", "ADMIN".equals(role));
        context.setVariable("activeSection", "inicio");
        context.setVariable("displayName", "Usuario de prueba");
        String html = engine.process(new TemplateSpec("fragments/layout", Set.of("navbar"), TemplateMode.HTML, null), context);
        int start = html.indexOf("id=\"fqsUserDropdown\"");
        return html.substring(start, html.indexOf("</ul>", start));
    }
}
