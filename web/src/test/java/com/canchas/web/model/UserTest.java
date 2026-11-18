package com.canchas.web.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void onCreate_shouldAssignRegistrationDateIfNull() {
        User user = new User();
        user.setUsername("testuser");
        user.setRole(User.Role.CLIENTE);
        user.onCreate();
        assertNotNull(user.getRegisteredAt());
        assertEquals(LocalDate.now(), user.getRegisteredAt());
    }

    @Test
    void onCreate_shouldNotOverwriteExistingDate() {
        User user = new User();
        LocalDate originalDate = LocalDate.of(2025, 1, 1);
        user.setRegisteredAt(originalDate);
        user.onCreate();
        assertEquals(originalDate, user.getRegisteredAt());
    }

    @Test
    void testProvinceAndProvinciaAlias() {
        User user = new User();
        assertNull(user.getProvince());
        assertNull(user.getProvincia());

        user.setProvince("Trujillo");
        assertEquals("Trujillo", user.getProvince());
        assertEquals("Trujillo", user.getProvincia());

        user.setProvincia("Chepén");
        assertEquals("Chepén", user.getProvince());
        assertEquals("Chepén", user.getProvincia());
    }
}
