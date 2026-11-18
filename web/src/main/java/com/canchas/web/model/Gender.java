package com.canchas.web.model;

public enum Gender {
    MALE("Masculino"),
    FEMALE("Femenino"),
    PREFER_NOT_TO_SAY("Prefiero no decirlo");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}