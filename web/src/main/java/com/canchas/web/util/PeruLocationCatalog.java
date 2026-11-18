package com.canchas.web.util;

import com.canchas.web.model.PeruDepartment;
import java.text.Normalizer;
import java.util.*;

public final class PeruLocationCatalog {

    private static final Map<PeruDepartment, List<String>> PROVINCES_BY_DEPARTMENT;
    private static final Map<String, List<String>> PROVINCES_BY_CODE;

    static {
        Map<PeruDepartment, List<String>> map = new EnumMap<>(PeruDepartment.class);

        map.put(PeruDepartment.AMAZONAS, List.of(
            "Chachapoyas", "Bagua", "Bongará", "Condorcanqui", "Luya", "Rodríguez de Mendoza", "Utcubamba"
        ));
        map.put(PeruDepartment.ANCASH, List.of(
            "Huaraz", "Aija", "Antonio Raymondi", "Asunción", "Bolognesi", "Carhuaz", "Carlos Fermín Fitzcarrald",
            "Casma", "Corongo", "Huari", "Huarmey", "Huaylas", "Mariscal Luzuriaga", "Ocros", "Pallasca",
            "Pomabamba", "Recuay", "Santa", "Sihuas", "Yungay"
        ));
        map.put(PeruDepartment.APURIMAC, List.of(
            "Abancay", "Andahuaylas", "Antabamba", "Aymaraes", "Cotabambas", "Chincheros", "Grau"
        ));
        map.put(PeruDepartment.AREQUIPA, List.of(
            "Arequipa", "Camaná", "Caravelí", "Castilla", "Caylloma", "Condesuyos", "Islay", "La Unión"
        ));
        map.put(PeruDepartment.AYACUCHO, List.of(
            "Huamanga", "Cangallo", "Huanca Sancos", "Huanta", "La Mar", "Lucanas", "Parinacochas",
            "Páucar del Sara Sara", "Sucre", "Víctor Fajardo", "Vilcas Huamán"
        ));
        map.put(PeruDepartment.CAJAMARCA, List.of(
            "Cajamarca", "Cajabamba", "Celendín", "Chota", "Contumazá", "Cutervo", "Hualgayoc", "Jaén",
            "San Ignacio", "San Marcos", "San Miguel", "San Pablo", "Santa Cruz"
        ));
        map.put(PeruDepartment.CALLAO, List.of(
            "Callao"
        ));
        map.put(PeruDepartment.CUSCO, List.of(
            "Cusco", "Acomayo", "Anta", "Calca", "Canas", "Canchis", "Chumbivilcas", "Espinar",
            "La Convención", "Paruro", "Paucartambo", "Quispicanchi", "Urubamba"
        ));
        map.put(PeruDepartment.HUANCAVELICA, List.of(
            "Huancavelica", "Acobamba", "Angaraes", "Castrovirreyna", "Churcampa", "Huaytará", "Tayacaja"
        ));
        map.put(PeruDepartment.HUANUCO, List.of(
            "Huánuco", "Ambo", "Dos de Mayo", "Huacaybamba", "Huamalíes", "Leoncio Prado", "Marañón",
            "Pachitea", "Puerto Inca", "Lauricocha", "Yarowilca"
        ));
        map.put(PeruDepartment.ICA, List.of(
            "Ica", "Chincha", "Nazca", "Palpa", "Pisco"
        ));
        map.put(PeruDepartment.JUNIN, List.of(
            "Huancayo", "Concepción", "Chanchamayo", "Jauja", "Junín", "Satipo", "Tarma", "Yauli", "Chupaca"
        ));
        map.put(PeruDepartment.LA_LIBERTAD, List.of(
            "Trujillo", "Ascope", "Bolívar", "Chepén", "Gran Chimú", "Julcán", "Otuzco", "Pacasmayo",
            "Pataz", "Sánchez Carrión", "Santiago de Chuco", "Virú"
        ));
        map.put(PeruDepartment.LAMBAYEQUE, List.of(
            "Chiclayo", "Ferreñafe", "Lambayeque"
        ));
        map.put(PeruDepartment.LIMA, List.of(
            "Lima", "Barranca", "Cajatambo", "Canta", "Cañete", "Huaral", "Huarochirí", "Huaura", "Oyón", "Yauyos"
        ));
        map.put(PeruDepartment.LORETO, List.of(
            "Maynas", "Alto Amazonas", "Loreto", "Mariscal Ramón Castilla", "Requena", "Ucayali",
            "Datem del Marañón", "Putumayo"
        ));
        map.put(PeruDepartment.MADRE_DE_DIOS, List.of(
            "Tambopata", "Manu", "Tahuamanu"
        ));
        map.put(PeruDepartment.MOQUEGUA, List.of(
            "Mariscal Nieto", "General Sánchez Cerro", "Ilo"
        ));
        map.put(PeruDepartment.PASCO, List.of(
            "Pasco", "Daniel Alcides Carrión", "Oxapampa"
        ));
        map.put(PeruDepartment.PIURA, List.of(
            "Piura", "Ayabaca", "Huancabamba", "Morropón", "Paita", "Sullana", "Talara", "Sechura"
        ));
        map.put(PeruDepartment.PUNO, List.of(
            "Puno", "Azángaro", "Carabaya", "Chucuito", "El Collao", "Huancané", "Lampa", "Melgar",
            "Moho", "San Antonio de Putina", "San Román", "Sandia", "Yunguyo"
        ));
        map.put(PeruDepartment.SAN_MARTIN, List.of(
            "Moyobamba", "Bellavista", "El Dorado", "Huallaga", "Lamas", "Mariscal Cáceres",
            "Picota", "Rioja", "San Martín", "Tocache"
        ));
        map.put(PeruDepartment.TACNA, List.of(
            "Tacna", "Candarave", "Jorge Basadre", "Tarata"
        ));
        map.put(PeruDepartment.TUMBES, List.of(
            "Tumbes", "Contralmirante Villar", "Zarumilla"
        ));
        map.put(PeruDepartment.UCAYALI, List.of(
            "Coronel Portillo", "Atalaya", "Padre Abad", "Purús"
        ));

        PROVINCES_BY_DEPARTMENT = Collections.unmodifiableMap(map);

        Map<String, List<String>> codeMap = new LinkedHashMap<>();
        for (Map.Entry<PeruDepartment, List<String>> entry : map.entrySet()) {
            codeMap.put(entry.getKey().name(), entry.getValue());
        }
        PROVINCES_BY_CODE = Collections.unmodifiableMap(codeMap);
    }

    private PeruLocationCatalog() {
    }

    public static List<String> getProvincesForDepartment(PeruDepartment department) {
        if (department == null) {
            return Collections.emptyList();
        }
        return PROVINCES_BY_DEPARTMENT.getOrDefault(department, Collections.emptyList());
    }

    public static Map<String, List<String>> getAllProvincesByDepartmentCode() {
        return PROVINCES_BY_CODE;
    }

    public static boolean isValidProvince(PeruDepartment department, String province) {
        if (department == null || province == null || province.isBlank()) {
            return false;
        }
        String normalized = normalize(province);
        for (String p : getProvincesForDepartment(department)) {
            if (normalize(p).equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static String getCanonicalProvinceName(PeruDepartment department, String province) {
        if (department == null || province == null || province.isBlank()) {
            return null;
        }
        String normalized = normalize(province);
        for (String p : getProvincesForDepartment(department)) {
            if (normalize(p).equalsIgnoreCase(normalized)) {
                return p;
            }
        }
        return province.trim();
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
