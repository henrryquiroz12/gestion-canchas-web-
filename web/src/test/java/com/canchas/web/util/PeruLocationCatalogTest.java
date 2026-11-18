package com.canchas.web.util;

import com.canchas.web.model.PeruDepartment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PeruLocationCatalogTest {

    @Test
    public void testAll25DepartmentsHaveProvinces() {
        int totalProvinces = 0;
        for (PeruDepartment dept : PeruDepartment.values()) {
            List<String> provinces = PeruLocationCatalog.getProvincesForDepartment(dept);
            assertNotNull(provinces, "Las provincias no deben ser null para: " + dept);
            assertFalse(provinces.isEmpty(), "Debe existir al menos 1 provincia para: " + dept);
            totalProvinces += provinces.size();
        }
        assertEquals(196, totalProvinces, "El catálogo oficial del Perú debe contener exactamente 196 provincias");
    }

    @Test
    public void testLaLibertadProvinces() {
        List<String> laLibertadProvinces = PeruLocationCatalog.getProvincesForDepartment(PeruDepartment.LA_LIBERTAD);
        assertTrue(laLibertadProvinces.contains("Trujillo"));
        assertTrue(laLibertadProvinces.contains("Chepén"));
        assertTrue(laLibertadProvinces.contains("Pacasmayo"));
        assertTrue(laLibertadProvinces.contains("Virú"));
        assertTrue(laLibertadProvinces.contains("Gran Chimú"));
        assertTrue(laLibertadProvinces.contains("Sánchez Carrión"));
    }

    @Test
    public void testIsValidProvince_Success() {
        assertTrue(PeruLocationCatalog.isValidProvince(PeruDepartment.LA_LIBERTAD, "Trujillo"));
        assertTrue(PeruLocationCatalog.isValidProvince(PeruDepartment.LA_LIBERTAD, "trujillo"));
        assertTrue(PeruLocationCatalog.isValidProvince(PeruDepartment.LA_LIBERTAD, "  TRUJILLO  "));
        assertTrue(PeruLocationCatalog.isValidProvince(PeruDepartment.LA_LIBERTAD, "Sanchez Carrion"));
        assertTrue(PeruLocationCatalog.isValidProvince(PeruDepartment.LIMA, "Lima"));
        assertTrue(PeruLocationCatalog.isValidProvince(PeruDepartment.CALLAO, "Callao"));
        assertTrue(PeruLocationCatalog.isValidProvince(PeruDepartment.CUSCO, "Urubamba"));
    }

    @Test
    public void testIsValidProvince_MismatchDepartment() {
        assertFalse(PeruLocationCatalog.isValidProvince(PeruDepartment.LA_LIBERTAD, "Lima"));
        assertFalse(PeruLocationCatalog.isValidProvince(PeruDepartment.LIMA, "Trujillo"));
        assertFalse(PeruLocationCatalog.isValidProvince(PeruDepartment.AREQUIPA, "Cusco"));
    }

    @Test
    public void testIsValidProvince_NullOrBlank() {
        assertFalse(PeruLocationCatalog.isValidProvince(null, "Trujillo"));
        assertFalse(PeruLocationCatalog.isValidProvince(PeruDepartment.LA_LIBERTAD, null));
        assertFalse(PeruLocationCatalog.isValidProvince(PeruDepartment.LA_LIBERTAD, ""));
        assertFalse(PeruLocationCatalog.isValidProvince(PeruDepartment.LA_LIBERTAD, "   "));
    }

    @Test
    public void testCanonicalProvinceName() {
        assertEquals("Sánchez Carrión", PeruLocationCatalog.getCanonicalProvinceName(PeruDepartment.LA_LIBERTAD, "sanchez carrion"));
        assertEquals("Trujillo", PeruLocationCatalog.getCanonicalProvinceName(PeruDepartment.LA_LIBERTAD, "TRUJILLO"));
        assertEquals("Chepén", PeruLocationCatalog.getCanonicalProvinceName(PeruDepartment.LA_LIBERTAD, "Chepen"));
    }

    @Test
    public void testAllProvincesByDepartmentCode() {
        var map = PeruLocationCatalog.getAllProvincesByDepartmentCode();
        assertEquals(25, map.size());
        assertTrue(map.containsKey("LA_LIBERTAD"));
        assertTrue(map.get("LA_LIBERTAD").contains("Trujillo"));
    }
}
