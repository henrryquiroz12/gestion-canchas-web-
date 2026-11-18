package com.canchas.web.model;

public enum PeruDepartment {
    AMAZONAS("Amazonas"),
    ANCASH("Áncash"),
    APURIMAC("Apurímac"),
    AREQUIPA("Arequipa"),
    AYACUCHO("Ayacucho"),
    CAJAMARCA("Cajamarca"),
    CALLAO("Callao"),
    CUSCO("Cusco"),
    HUANCAVELICA("Huancavelica"),
    HUANUCO("Huánuco"),
    ICA("Ica"),
    JUNIN("Junín"),
    LA_LIBERTAD("La Libertad"),
    LAMBAYEQUE("Lambayeque"),
    LIMA("Lima"),
    LORETO("Loreto"),
    MADRE_DE_DIOS("Madre de Dios"),
    MOQUEGUA("Moquegua"),
    PASCO("Pasco"),
    PIURA("Piura"),
    PUNO("Puno"),
    SAN_MARTIN("San Martín"),
    TACNA("Tacna"),
    TUMBES("Tumbes"),
    UCAYALI("Ucayali");

    private final String label;

    PeruDepartment(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getDisplayName() {
        return label;
    }

    public static PeruDepartment fromLabel(String text) {
        if (text == null || text.isBlank()) return null;
        for (PeruDepartment d : values()) {
            if (d.label.equalsIgnoreCase(text.trim()) || d.name().equalsIgnoreCase(text.trim())) {
                return d;
            }
        }
        return null;
    }
}