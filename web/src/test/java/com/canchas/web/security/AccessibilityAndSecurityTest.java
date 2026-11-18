package com.canchas.web.security;

import com.canchas.web.model.Gender;
import com.canchas.web.model.PeruDepartment;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class AccessibilityAndSecurityTest {

    @Test
    public void testPeruDepartmentsComplete() {
        assertEquals(25, PeruDepartment.values().length, "Deben existir exactamente 25 departamentos/regiones del Perú");
        assertNotNull(PeruDepartment.fromLabel("Lima"));
        assertNotNull(PeruDepartment.fromLabel("Cusco"));
        assertNotNull(PeruDepartment.fromLabel("Áncash"));
    }

    @Test
    public void testGenderValuesInclusive() {
        assertEquals(3, Gender.values().length, "Deben existir 3 opciones de género");
        assertNotNull(Gender.valueOf("MALE"));
        assertNotNull(Gender.valueOf("FEMALE"));
        assertNotNull(Gender.valueOf("PREFER_NOT_TO_SAY"));
    }

    @Test
    public void testLayoutContainsSkipLinkAndMainContent() throws IOException {
        Path layoutPath = Paths.get("src/main/resources/templates/fragments/layout.html");
        assertTrue(Files.exists(layoutPath), "El fragmento layout.html debe existir");

        String layoutContent = Files.readString(layoutPath);
        assertTrue(layoutContent.contains("fqs-skip-link") || layoutContent.contains("href=\"#main-content\""),
                "El layout debe incluir un enlace skip-link a #main-content para WCAG 2.4.1");
        assertTrue(layoutContent.contains("main-content") || layoutContent.contains("id=\"main-content\""),
                "El layout debe definir el ancla main-content");
    }

    @Test
    public void testInicioContainsAccessibleCarouselControls() throws IOException {
        Path inicioPath = Paths.get("src/main/resources/templates/inicio.html");
        assertTrue(Files.exists(inicioPath), "inicio.html debe existir");

        String content = Files.readString(inicioPath);
        assertTrue(content.contains("fqsCarouselTogglePlay") || content.contains("hero-carousel-toggle"),
                "El carrusel de inicio debe contar con control play/pause accesible para WCAG 2.2.2");
    }

    @Test
    public void testRegistroContainsAccessibleWizardAndFieldset() throws IOException {
        Path registroPath = Paths.get("src/main/resources/templates/registro.html");
        assertTrue(Files.exists(registroPath), "registro.html debe existir");

        String content = Files.readString(registroPath);
        assertTrue(content.contains("fieldset"), "El formulario de registro debe agrupar radios con fieldset accesible");
        assertTrue(content.contains("wizard"), "El formulario de registro debe contener estructura wizard");
        assertTrue(content.contains("id=\"department\""), "Debe existir selector de departamento");
        assertTrue(content.contains("id=\"province\""), "Debe existir selector de provincia");
    }

    @Test
    public void testTorneosTemplatesExist() {
        assertTrue(Files.exists(Paths.get("src/main/resources/templates/torneos/lista.html")));
        assertTrue(Files.exists(Paths.get("src/main/resources/templates/torneos/como-funciona.html")));
        assertTrue(Files.exists(Paths.get("src/main/resources/templates/torneos/detalle.html")));
        assertTrue(Files.exists(Paths.get("src/main/resources/templates/admin/torneos/lista.html")));
        assertTrue(Files.exists(Paths.get("src/main/resources/templates/nosotros.html")));
    }

    @Test
    public void testTorneoDetalleContainsBackLinkAndNoNumberedBreadcrumb() throws IOException {
        Path detallePath = Paths.get("src/main/resources/templates/torneos/detalle.html");
        assertTrue(Files.exists(detallePath), "El template detalle.html debe existir");

        String content = Files.readString(detallePath);
        assertTrue(content.contains("Volver a torneos"), "Debe contener el enlace comprensible 'Volver a torneos'");
        assertTrue(content.contains("th:href=\"@{/torneos}\""), "El enlace debe apuntar exactamente a /torneos");
        assertTrue(content.contains("class=\"fqs-back-link\""), "Debe utilizar la clase accesible fqs-back-link");
        assertFalse(content.contains("fqs-breadcrumb"), "No debe existir la estructura de breadcrumb numerado");
        assertFalse(content.contains("<li><a th:href=\"@{/inicio}\">Inicio</a></li>"), "No debe tener lista de breadcrumbs a inicio");
    }

    @Test
    public void testFooterContainsComplexInfoAndNoNavegacionLinks() throws IOException {
        Path layoutPath = Paths.get("src/main/resources/templates/fragments/layout.html");
        assertTrue(Files.exists(layoutPath), "layout.html debe existir");

        String content = Files.readString(layoutPath);
        assertTrue(content.contains("Información del Complejo"), "El footer debe contener la columna 'Información del Complejo'");
        assertTrue(content.contains("10 canchas sintéticas"), "Debe indicar 10 canchas sintéticas");
        assertTrue(content.contains("8 normales y 2 grandes"), "Debe indicar 8 normales y 2 grandes");
        assertTrue(content.contains("Reservas de 60 minutos"), "Debe indicar reservas de 60 minutos");
        assertTrue(content.contains("Iluminación desde 18:00"), "Debe indicar iluminación desde 18:00");
        assertFalse(content.contains("<h3 class=\"fqs-footer-title\">Navegación</h3>"), "La columna Navegación debe estar eliminada");
    }

    @Test
    public void testAdminDropdownAndClientSearchAccessibility() throws IOException {
        Path layoutPath = Paths.get("src/main/resources/templates/fragments/layout.html");
        assertTrue(Files.exists(layoutPath), "layout.html debe existir");

        String layoutContent = Files.readString(layoutPath);
        assertTrue(layoutContent.contains("th:if=\"${canSwitchRole}\""), "El selector de rol depende de los roles asignados");
        assertFalse(layoutContent.contains("Mi Panel"), "El menú no debe mezclar accesos adicionales");
        assertTrue(layoutContent.contains("Cerrar Sesión"), "Debe contener Cerrar Sesión");

        Path clientesPath = Paths.get("src/main/resources/templates/admin/clientes/lista.html");
        assertTrue(Files.exists(clientesPath), "lista.html de clientes debe existir");

        String clientesContent = Files.readString(clientesPath);
        assertTrue(clientesContent.contains("for=\"clientSearchCriterion\""), "El selector debe tener una etiqueta");
        assertTrue(clientesContent.contains("for=\"clientSearchValue\""), "La búsqueda debe tener una etiqueta");
        assertTrue(clientesContent.contains("aria-live=\"polite\""), "Los resultados deben anunciarse sin mover el foco");
    }
}
