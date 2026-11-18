package com.canchas.web.controller.admin;

import com.canchas.web.repository.*;
import com.canchas.web.service.UserService;
import com.canchas.web.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminClientSearchTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserServiceImpl service = new UserServiceImpl(repository, mock(UserRoleRepository.class),
            mock(RoleRepository.class), mock(org.springframework.security.crypto.password.PasswordEncoder.class));

    @Test
    void openingSectionDoesNotQueryClientsEvenWithOldSearchParameters() throws Exception {
        UserService users = mock(UserService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminClientController(users))
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/", ".html")).build();
        mvc.perform(get("/admin/clientes").param("q", "123")).andExpect(status().isOk())
                .andExpect(view().name("admin/clientes/lista"));
        verifyNoInteractions(users);
    }

    @Test
    void prefixAndExactDniUseDatabaseLimitOfTen() {
        when(repository.searchClientsByDniPrefix(eq("001"), any())).thenReturn(List.of());
        when(repository.searchClientsByExactDni(eq("00123456"), any())).thenReturn(List.of());
        service.searchClients("DNI", "001");
        service.searchClients("DNI", "00123456");
        verify(repository).searchClientsByDniPrefix(eq("001"), argThat(p -> p.getPageSize() == 10 && p.getOffset() == 0));
        verify(repository).searchClientsByExactDni(eq("00123456"), argThat(p -> p.getPageSize() == 10 && p.getOffset() == 0));
    }

    @Test
    void nameAllowsAccentsAndSpacesAndUsesDatabaseLimit() {
        when(repository.searchClientsByName(eq("María Peña"), any())).thenReturn(List.of());
        service.searchClients("NOMBRE", " María  Peña ");
        verify(repository).searchClientsByName(eq("María Peña"), argThat(p -> p.getPageSize() == 10));
    }

    @Test
    void invalidSearchesNeverReachDatabaseAndEmptySearchIsEmpty() throws Exception {
        for (String value : List.of("123456789", "12 3", "12a", "%", "１２３")) {
            assertThrows(IllegalArgumentException.class, () -> service.searchClients("DNI", value));
        }
        for (String value : List.of("Ana1", "Ana%", "Ana_Peña", "Ana-Peña", "a".repeat(101))) {
            assertThrows(IllegalArgumentException.class, () -> service.searchClients("NOMBRE", value));
        }
        assertThrows(IllegalArgumentException.class, () -> service.searchClients("EMAIL", "ana"));
        assertTrue(service.searchClients("DNI", "").isEmpty());
        assertTrue(service.searchClients("NOMBRE", "   ").isEmpty());
        verifyNoInteractions(repository);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminClientApiController(service)).build();
        mvc.perform(get("/admin/api/clientes/buscar").param("criterio", "EMAIL").param("valor", "ana"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.valor").exists());
        mvc.perform(get("/admin/api/clientes/buscar").param("criterio", "DNI").param("valor", "1"))
                .andExpect(status().isOk());
    }
}
