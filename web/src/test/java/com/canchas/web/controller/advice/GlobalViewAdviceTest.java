package com.canchas.web.controller.advice;

import com.canchas.web.model.BusinessConfiguration;
import com.canchas.web.model.User;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.service.BusinessConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GlobalViewAdviceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BusinessConfigurationService configurationService;

    @InjectMocks
    private GlobalViewAdvice advice;

    private BusinessConfiguration config;

    @BeforeEach
    void setUp() {
        config = new BusinessConfiguration();
        config.setBusinessName("Fredy Queen Soccer");
        config.setContactPhone("949356493");
        config.setContactEmail("freddyquinnsoccer@gmail.com");
        lenient().when(configurationService.getConfiguration()).thenReturn(config);
    }

    @Test
    void testUsuarioAutenticado_ExtraePrimerNombreYPrimerApellidoYConfig() {
        User user = new User();
        user.setUsername("pepe");
        user.setFullName("Pepe Pérez González");
        user.setEmail("pepe@example.com");

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(user));

        Authentication auth = new UsernamePasswordAuthenticationToken("pepe", "password", Collections.emptyList());
        Model model = new ConcurrentModel();

        advice.addGlobalAttributes(model, auth);

        assertEquals("Pepe Pérez", model.getAttribute("displayName"));
        assertEquals(user, model.getAttribute("currentUser"));
        assertEquals(config, model.getAttribute("companyConfig"));
        assertEquals(Boolean.FALSE, model.getAttribute("isAdminSection"));
    }

    @Test
    void testVisitante_NoAgregaUsuarioPeroSiConfig() {
        Model model = new ConcurrentModel();

        advice.addGlobalAttributes(model, null);

        assertNull(model.getAttribute("displayName"));
        assertNull(model.getAttribute("currentUser"));
        assertEquals(config, model.getAttribute("companyConfig"));
        assertEquals(Boolean.FALSE, model.getAttribute("isAdminSection"));
    }

    @Test
    void testIsAdminSection_RutaAdmin_RetornaTrue() {
        Model model = new ConcurrentModel();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/clientes");

        advice.addGlobalAttributes(model, null, request);

        assertEquals(Boolean.TRUE, model.getAttribute("isAdminSection"));
    }

    @Test
    void testIsAdminSection_RutaPublica_RetornaFalse() {
        Model model = new ConcurrentModel();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/inicio");

        advice.addGlobalAttributes(model, null, request);

        assertEquals(Boolean.FALSE, model.getAttribute("isAdminSection"));
    }

    @Test
    void testFormatDisplayName_CuatroPalabras() {
        assertEquals("Juan Pérez", GlobalViewAdvice.formatDisplayName("Juan José Pérez García", "juanp"));
        assertEquals("Marlon Quipuzco", GlobalViewAdvice.formatDisplayName("Marlon Robinson Quipuzco Vargas", "marlon"));
    }

    @Test
    void testFormatDisplayName_TresPalabras() {
        assertEquals("Pepe Pérez", GlobalViewAdvice.formatDisplayName("Pepe Pérez González", "pepe"));
    }

    @Test
    void testFormatDisplayName_DosPalabras() {
        assertEquals("Juan Pérez", GlobalViewAdvice.formatDisplayName("Juan Pérez", "juanp"));
    }

    @Test
    void testFormatDisplayName_UnaPalabra() {
        assertEquals("Juan", GlobalViewAdvice.formatDisplayName("Juan", "juan123"));
    }

    @Test
    void testFormatDisplayName_NullOBlank_FallbackUsername() {
        assertEquals("cliente1", GlobalViewAdvice.formatDisplayName(null, "cliente1"));
        assertEquals("cliente1", GlobalViewAdvice.formatDisplayName("", "cliente1"));
        assertEquals("cliente1", GlobalViewAdvice.formatDisplayName("   ", "cliente1"));
    }
}