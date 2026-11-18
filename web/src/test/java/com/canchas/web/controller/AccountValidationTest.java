package com.canchas.web.controller;

import com.canchas.web.dto.*;
import com.canchas.web.exception.DuplicateDataException;
import com.canchas.web.service.UserService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountValidationTest {
    private final UserService users = mock(UserService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(users))
            .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/templates/", ".html"))
            .build();

    private MockHttpServletRequestBuilder registration() {
        return post("/registro").param("fullName", "María Peña Gómez")
                .param("dni", "00123456").param("gender", "PREFER_NOT_TO_SAY")
                .param("phoneNumber", "912345678").param("department", "LIMA")
                .param("province", "Lima").param("email", "maria@GMAIL.COM")
                .param("username", "maria").param("password", "abcdef")
                .param("confirmPassword", "abcdef");
    }

    @Test
    void allAccountDtosShareFieldRules() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            for (Object dto : List.of(new UserRegistrationDTO(), new UserRequestDTO(), new ClientProfileDTO())) {
                var bean = new BeanWrapperImpl(dto);
                for (String name : List.of("María Peña", "Álvaro Muñoz", "José Luis", "Jose\u0301 Peña")) {
                    bean.setPropertyValue("fullName", name);
                    assertTrue(validator.validateProperty(dto, "fullName").isEmpty(), name);
                }
                for (String name : List.of("Ana1", "Ana-Pérez", "Ana@", " Ana", "Ana ", "Ana\tPérez")) {
                    bean.setPropertyValue("fullName", name);
                    assertFalse(validator.validateProperty(dto, "fullName").isEmpty(), name);
                }
                for (String email : List.of("ana@gmail.com", "ana@HOTMAIL.COM", "ana.pena@UTP.EDU.PE")) {
                    bean.setPropertyValue("email", email);
                    assertTrue(validator.validateProperty(dto, "email").isEmpty(), email);
                }
                for (String email : List.of("ana@@gmail.com", "ana@sub.gmail.com", "ana@gmail", "ana@gmail.com ", "a b@gmail.com", "ana@yahoo.com", "a..b@gmail.com")) {
                    bean.setPropertyValue("email", email);
                    assertFalse(validator.validateProperty(dto, "email").isEmpty(), email);
                }
                for (String phone : List.of("812345678", "91234567", "9123456789", "91234567a", "912345678 ", "+912345678")) {
                    bean.setPropertyValue("phoneNumber", phone);
                    assertFalse(validator.validateProperty(dto, "phoneNumber").isEmpty(), phone);
                }
                if (bean.isWritableProperty("dni")) {
                    bean.setPropertyValue("dni", "00123456");
                    assertTrue(validator.validateProperty(dto, "dni").isEmpty());
                    for (String dni : List.of("1234567", "123456789", "1234567a", "12345678 ", "１２３４５６７８", "+1234567")) {
                        bean.setPropertyValue("dni", dni);
                        assertFalse(validator.validateProperty(dto, "dni").isEmpty(), dni);
                    }
                }
            }
        }
    }

    @Test
    void dniCheckCanBeCorrectedAndDoesNotCache() throws Exception {
        when(users.existsByDni("00123456")).thenReturn(true);
        mvc.perform(get("/registro/validar-dni").param("dni", "00123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/registro/validar-dni").param("dni", "00123457"))
                .andExpect(jsonPath("$.available").value(true));
        mvc.perform(get("/registro/validar-dni").param("dni", "00123456 "))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.dni").exists());
    }

    @Test
    void duplicateDniPreservesInputsAndBindsFieldError() throws Exception {
        when(users.existsByDni("00123456")).thenReturn(true);
        mvc.perform(registration()).andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrors("registroDto", "dni"))
                .andExpect(model().attribute("registroDto", org.hamcrest.Matchers.hasProperty("fullName", org.hamcrest.Matchers.is("María Peña Gómez"))));
        verify(users, never()).registerClient(any());
    }

    @Test
    void directInvalidPostNeverReachesPersistence() throws Exception {
        var request = registration();
        request.param("fullName", "Ana9");
        // setParam replaces the existing value rather than appending a second value.
        mvc.perform(request.with(r -> {
            r.setParameter("fullName", "Ana9");
            r.setParameter("dni", "00123456 ");
            r.setParameter("phoneNumber", "812345678");
            r.setParameter("email", "ana@sub.gmail.com");
            return r;
        })).andExpect(model().attributeHasFieldErrors("registroDto", "fullName", "dni", "phoneNumber", "email"));
        verify(users, never()).registerClient(any());
    }

    @Test
    void duplicateBetweenCheckAndSaveIsAFieldError() throws Exception {
        when(users.registerClient(any())).thenThrow(new DuplicateDataException("dni", "El DNI ya se encuentra registrado."));
        mvc.perform(registration()).andExpect(model().attributeHasFieldErrors("registroDto", "dni"));
    }

    @Test
    void uniqueConstraintRaceIsAFieldError() throws Exception {
        when(users.existsByDni("00123456")).thenReturn(false, true);
        when(users.registerClient(any())).thenThrow(new DataIntegrityViolationException("unique dni"));
        mvc.perform(registration()).andExpect(model().attributeHasFieldErrors("registroDto", "dni"));
    }

    @Test
    void sixCharacterPasswordAndUppercaseDomainKeepWorking() throws Exception {
        mvc.perform(registration()).andExpect(redirectedUrl("/login?registroExitoso=true"));
        verify(users).registerClient(any());
    }

    @Test
    void directInvalidEditsReturnErrorsBesideTheirFields() throws Exception {
        var editMvc = MockMvcBuilders.standaloneSetup(new ClientController(users),
                new com.canchas.web.controller.admin.AdminClientController(users)).build();
        var client = new com.canchas.web.model.User();
        client.setId(1L);
        when(users.findByUsername("cliente")).thenReturn(client);
        for (String url : List.of("/cliente/perfil/editar", "/admin/clientes/1/editar")) {
            editMvc.perform(post(url)
                    .principal(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("cliente", ""))
                    .param("username", "cliente").param("role", "CLIENTE")
                    .param("gender", "MALE").param("department", "LIMA").param("province", "Lima")
                    .param("fullName", "Ana9").param("email", "ana@sub.gmail.com")
                    .param("phoneNumber", "812345678"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fullName").exists())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.phoneNumber").exists());
        }
        verify(users, never()).update(any(), any());
        verify(users, never()).updateProfile(any(), any());
    }
}
