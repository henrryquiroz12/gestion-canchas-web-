package com.canchas.web.service;

import com.canchas.web.dto.ClientProfileDTO;
import com.canchas.web.dto.UserRegistrationDTO;
import com.canchas.web.exception.DuplicateDataException;
import com.canchas.web.model.Gender;
import com.canchas.web.model.PeruDepartment;
import com.canchas.web.model.User;
import com.canchas.web.repository.UserRepository;
import com.canchas.web.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.canchas.web.repository.UserRoleRepository userRoleRepository;

    @Mock
    private com.canchas.web.repository.RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    public void setup() {
        lenient().when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        lenient().when(roleRepository.findByName(anyString())).thenAnswer(inv -> Optional.of(new com.canchas.web.model.Role(inv.getArgument(0), inv.getArgument(0))));
        lenient().when(roleRepository.save(any(com.canchas.web.model.Role.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private UserRegistrationDTO createValidDto(String dni) {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setDni(dni);
        dto.setUsername("testuser");
        dto.setPassword("123456");
        dto.setConfirmPassword("123456");
        dto.setFullName("Test User");
        dto.setEmail("test@correo.com");
        dto.setPhoneNumber("987654321");
        dto.setDepartment(PeruDepartment.LIMA);
        dto.setProvince("Lima");
        dto.setGender(Gender.MALE);
        dto.setAddress("Av. Larco 123");
        return dto;
    }

    @Test
    public void testRegister_Valid8DigitDni() {
        UserRegistrationDTO dto = createValidDto("12345678");
        when(userRepository.existsByDni("12345678")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@correo.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User registered = userService.registerClient(dto);

        assertNotNull(registered);
        assertEquals("12345678", registered.getDni());
        assertEquals(User.Role.CLIENTE, registered.getRole());
        assertEquals(PeruDepartment.LIMA, registered.getDepartment());
        assertEquals("Lima", registered.getProvince());
        assertEquals(Gender.MALE, registered.getGender());
        assertTrue(registered.isActive());
    }

    @Test
    public void testRegister_ValidDepartmentAndProvince_Success() {
        UserRegistrationDTO dto = createValidDto("87654321");
        dto.setDepartment(PeruDepartment.LA_LIBERTAD);
        dto.setProvince("Trujillo");

        when(userRepository.existsByDni("87654321")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@correo.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User registered = userService.registerClient(dto);

        assertNotNull(registered);
        assertEquals(PeruDepartment.LA_LIBERTAD, registered.getDepartment());
        assertEquals("Trujillo", registered.getProvince());
    }

    @Test
    public void testRegister_DepartmentAndProvinceMismatch_Rejected() {
        UserRegistrationDTO dto = createValidDto("87654321");
        dto.setDepartment(PeruDepartment.LA_LIBERTAD);
        dto.setProvince("Lima"); // Lima pertenece al departamento LIMA, no a LA_LIBERTAD

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.registerClient(dto));
        assertEquals("La provincia no pertenece al departamento seleccionado.", ex.getMessage());
    }

    @Test
    public void testRegister_EmptyProvinceForNewClient_Rejected() {
        UserRegistrationDTO dtoNull = createValidDto("87654321");
        dtoNull.setProvince(null);
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () -> userService.registerClient(dtoNull));
        assertEquals("La provincia es obligatoria para el cliente.", exNull.getMessage());

        UserRegistrationDTO dtoBlank = createValidDto("87654321");
        dtoBlank.setProvince("   ");
        IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class, () -> userService.registerClient(dtoBlank));
        assertEquals("La provincia es obligatoria para el cliente.", exBlank.getMessage());
    }

    @Test
    public void testRegister_7DigitDni_Invalid() {
        UserRegistrationDTO dto = createValidDto("1234567");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.registerClient(dto));
        assertEquals("El DNI debe contener exactamente 8 dígitos.", ex.getMessage());
    }

    @Test
    public void testRegister_9DigitDni_Invalid() {
        UserRegistrationDTO dto = createValidDto("123456789");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.registerClient(dto));
        assertEquals("El DNI debe contener exactamente 8 dígitos.", ex.getMessage());
    }

    @Test
    public void testRegister_LetterDni_Invalid() {
        UserRegistrationDTO dto = createValidDto("1234ABCD");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.registerClient(dto));
        assertEquals("El DNI debe contener exactamente 8 dígitos.", ex.getMessage());
    }

    @Test
    public void testRegister_DuplicateDni_Rejected() {
        UserRegistrationDTO dto = createValidDto("12345678");
        when(userRepository.existsByDni("12345678")).thenReturn(true);

        DuplicateDataException ex = assertThrows(DuplicateDataException.class, () -> userService.registerClient(dto));
        assertEquals("El DNI ya se encuentra registrado.", ex.getMessage());
    }

    @Test
    public void testRegister_DepartmentMandatoryForClient() {
        UserRegistrationDTO dto = createValidDto("12345678");
        dto.setDepartment(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.registerClient(dto));
        assertEquals("El departamento es obligatorio para el cliente.", ex.getMessage());
    }

    @Test
    public void testRegister_GenderOptional() {
        UserRegistrationDTO dto = createValidDto("12345678");
        dto.setGender(null);

        when(userRepository.existsByDni("12345678")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@correo.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User registered = userService.registerClient(dto);
        assertNotNull(registered);
        assertEquals(Gender.PREFER_NOT_TO_SAY, registered.getGender());
    }

    @Test
    public void testRegister_AllGendersSupported() {
        for (Gender g : Gender.values()) {
            UserRegistrationDTO dto = createValidDto("12345678");
            dto.setGender(g);
            when(userRepository.existsByDni("12345678")).thenReturn(false);
            when(userRepository.existsByUsername("testuser")).thenReturn(false);
            when(userRepository.existsByEmail("test@correo.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            User registered = userService.registerClient(dto);
            assertNotNull(registered);
            assertEquals(g, registered.getGender());
            reset(userRepository);
        }
    }

    @Test
    public void testUpdateProfile_ValidDepartmentAndProvince_Success() {
        User client = new User();
        client.setId(5L);
        client.setEmail("viejo@correo.com");
        client.setDepartment(PeruDepartment.LA_LIBERTAD);
        client.setProvince("Trujillo");

        when(userRepository.findById(5L)).thenReturn(Optional.of(client));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        ClientProfileDTO dto = new ClientProfileDTO();
        dto.setFullName("Juan Perez");
        dto.setEmail("nuevo@correo.com");
        dto.setPhoneNumber("912345678");
        dto.setDepartment(PeruDepartment.LA_LIBERTAD);
        dto.setProvince("Chepén");

        User updated = userService.updateProfile(5L, dto);
        assertNotNull(updated);
        assertEquals(PeruDepartment.LA_LIBERTAD, updated.getDepartment());
        assertEquals("Chepén", updated.getProvince());
    }

    @Test
    public void testUpdateProfile_MismatchProvince_Rejected() {
        User client = new User();
        client.setId(5L);
        client.setEmail("cliente@correo.com");
        client.setDepartment(PeruDepartment.LA_LIBERTAD);
        client.setProvince("Trujillo");

        when(userRepository.findById(5L)).thenReturn(Optional.of(client));

        // Cliente cambia departamento a LIMA pero envía provincia Trujillo
        ClientProfileDTO dto = new ClientProfileDTO();
        dto.setFullName("Juan Perez");
        dto.setEmail("cliente@correo.com");
        dto.setPhoneNumber("912345678");
        dto.setDepartment(PeruDepartment.LIMA);
        dto.setProvince("Trujillo");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.updateProfile(5L, dto));
        assertEquals("La provincia no pertenece al departamento seleccionado.", ex.getMessage());
    }

    @Test
    public void testLegacyClientWithNullProvince_DoesNotBreak() {
        User legacyClient = new User();
        legacyClient.setId(99L);
        legacyClient.setUsername("legacyClient");
        legacyClient.setDni("11223344");
        legacyClient.setFullName("Cliente Antiguo");
        legacyClient.setEmail("antiguo@fredyqueensoccer.com");
        legacyClient.setDepartment(PeruDepartment.LA_LIBERTAD);
        legacyClient.setProvince(null); // Columna null para registros existentes
        legacyClient.setRole(User.Role.CLIENTE);
        legacyClient.setActive(true);

        assertNull(legacyClient.getProvince());
        assertNull(legacyClient.getProvincia());
        assertEquals("Cliente Antiguo", legacyClient.getFullName());
    }

    @Test
    public void testFindActiveClientByDni_ExistingAndActive() {
        User client = new User();
        client.setId(10L);
        client.setDni("72345678");
        client.setRole(User.Role.CLIENTE);
        client.setActive(true);

        when(userRepository.findByDniAndActiveTrue("72345678")).thenReturn(Optional.of(client));

        Optional<User> found = userService.findActiveClientByDni("72345678");
        assertTrue(found.isPresent());
        assertEquals("72345678", found.get().getDni());
    }

    @Test
    public void testFindActiveClientByDni_NotFound() {
        when(userRepository.findByDniAndActiveTrue("99999999")).thenReturn(Optional.empty());

        Optional<User> found = userService.findActiveClientByDni("99999999");
        assertTrue(found.isEmpty());
    }

    @Test
    public void testFindActiveClientByDni_NotClient_ReturnsEmpty() {
        User admin = new User();
        admin.setId(1L);
        admin.setDni("72345678");
        admin.setRole(User.Role.ADMIN);
        admin.setActive(true);

        when(userRepository.findByDniAndActiveTrue("72345678")).thenReturn(Optional.of(admin));

        Optional<User> found = userService.findActiveClientByDni("72345678");
        assertTrue(found.isEmpty());
    }
}
