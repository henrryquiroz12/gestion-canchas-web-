package com.canchas.web.validation;

public record ClientSearchCriteria(String criterion, String value) {
    public static ClientSearchCriteria parse(String criterion, String value) {
        if (!"DNI".equals(criterion) && !"NOMBRE".equals(criterion)) {
            throw new IllegalArgumentException("Selecciona DNI o Nombre.");
        }
        if (value == null || value.length() > 100) {
            throw new IllegalArgumentException("La búsqueda no puede superar 100 caracteres.");
        }
        if (value.isEmpty()) return new ClientSearchCriteria(criterion, value);
        if ("DNI".equals(criterion)) {
            if (!value.matches("[0-9]{1,8}")) {
                throw new IllegalArgumentException("Escribe de 1 a 8 dígitos, sin espacios ni signos.");
            }
        } else {
            if (!value.matches("[\\p{L}\\p{M} ]+")) {
                throw new IllegalArgumentException("Usa solo letras y espacios para buscar por nombre o apellidos.");
            }
            value = value.trim().replaceAll(" +", " ");
        }
        return new ClientSearchCriteria(criterion, value);
    }
}
