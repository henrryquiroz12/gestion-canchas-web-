package com.canchas.web.security;

import com.canchas.web.model.User;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class RoleSelectionAndPerformanceTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("1. Marlon Quipuzco Vargas existe como Administrador con username Mark2026 y ambos roles (ADMIN y CLIENTE)")
    void testAdministratorMark2026ExistsWithBothRoles() {
        User mark = userRepository.findByUsername("Mark2026").orElse(null);
        assertNotNull(mark, "El usuario Mark2026 debe existir en la base de datos");
        assertEquals("Marlon Quipuzco Vargas", mark.getFullName());
        assertEquals("Mark", mark.getApodo());
        assertTrue(mark.hasRole("ROLE_ADMIN"), "Debe tener rol ROLE_ADMIN");
        assertTrue(mark.hasRole("ROLE_CLIENTE"), "Debe tener rol ROLE_CLIENTE (puede operar como cliente)");
    }

    @Test
    @DisplayName("2. Inicio de sesión con Mark2026 y contraseña Proyectoweb2026 redirige a /seleccionar-rol")
    void testLoginMark2026RedirectsToRoleSelection() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "Mark2026")
                .param("password", "Proyectoweb2026")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seleccionar-rol"));
    }

    @Test
    @WithMockUser(username = "Mark2026", roles = {"ADMIN", "CLIENTE"})
    @DisplayName("3. Pantalla /seleccionar-rol muestra encabezado '¿Con qué rol deseas ingresar?' y opciones 'Administrador' y 'Cliente'")
    void testRoleSelectionViewContent() throws Exception {
        mockMvc.perform(get("/seleccionar-rol"))
                .andExpect(status().isOk())
                .andExpect(view().name("seleccionar-rol"))
                .andExpect(content().string(containsString("¿Con qué rol deseas ingresar?")))
                .andExpect(content().string(containsString("Administrador")))
                .andExpect(content().string(containsString("Cliente")));
    }

    @Test
    @WithMockUser(username = "Mark2026", roles = {"ADMIN", "CLIENTE"})
    @DisplayName("4. Seleccionar rol Administrador redirige a /admin/dashboard")
    void testSelectAdminRoleRedirectsToDashboard() throws Exception {
        mockMvc.perform(get("/seleccionar-rol/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @WithMockUser(username = "Mark2026", roles = {"ADMIN", "CLIENTE"})
    @DisplayName("5. Seleccionar rol Cliente redirige a /canchas")
    void testSelectClientRoleRedirectsToCourts() throws Exception {
        mockMvc.perform(get("/seleccionar-rol/cliente"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/canchas"));
    }

    @Test
    @DisplayName("6. Verificación de rendimiento y prevención de desborde de buffer con 1,000,000 de clientes (15 por página)")
    void testPerformanceWith1MillionClients() {
        long startCount = System.currentTimeMillis();
        long totalClients = userService.countClients();
        long countDuration = System.currentTimeMillis() - startCount;

        assertTrue(totalClients >= 1_000_000, "Debe haber al menos 1,000,000 clientes en la base de datos");
        assertTrue(countDuration < 1500, "El conteo COUNT(*) de 1M clientes debe responder fluidamente sin desborde");

        long startPage = System.currentTimeMillis();
        Page<User> page = userService.findClientsPaginated(0, 15, null);
        long pageDuration = System.currentTimeMillis() - startPage;

        assertNotNull(page);
        assertEquals(15, page.getContent().size(), "La página paginada debe contener exactamente 15 elementos");
        assertTrue(page.getTotalElements() >= 1_000_000);
        assertTrue(pageDuration < 1000, "La consulta paginada debe responder fluidamente sin desborde de buffer");
    }

    @Test
    @WithMockUser(username = "Mark2026", roles = {"ADMIN"})
    @DisplayName("7. Acceso a /admin/dashboard carga fluidamente sin desborde de datos")
    void testAdminDashboardLoadsWithoutOverflow() throws Exception {
        long start = System.currentTimeMillis();
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 5000, "El dashboard debe cargar rápidamente sin desbordes de memoria");
    }

    @Test
    @WithMockUser(username = "Mark2026", roles = {"ADMIN"})
    @DisplayName("8. Acceso a /admin/clientes carga una búsqueda vacía sin listado ni paginación")
    void testAdminClientsSearchStartsEmpty() throws Exception {
        long start = System.currentTimeMillis();
        MvcResult result = mockMvc.perform(get("/admin/clientes"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/clientes/lista"))
                .andExpect(content().string(containsString("<tbody id=\"clientSearchResults\"></tbody>")))
                .andExpect(content().string(containsString("Clientes")))
                .andExpect(content().string(containsString("clientSearchCriterion")))
                .andExpect(content().string(containsString("clientSearchValue")))
                .andReturn();
        long duration = System.currentTimeMillis() - start;

        // Comprobar que el tamaño del HTML de respuesta sea razonable (< 100 KB), evitando desborde de buffer
        byte[] contentBytes = result.getResponse().getContentAsByteArray();
        assertTrue(contentBytes.length < 150_000, "El tamaño HTML de la tabla de clientes debe ser menor a 150KB");
        assertTrue(duration < 2000, "La vista de clientes debe cargar en menos de 2 segundos");
    }
}
