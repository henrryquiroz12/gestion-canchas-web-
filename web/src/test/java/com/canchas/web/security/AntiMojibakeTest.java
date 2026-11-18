package com.canchas.web.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiMojibakeTest {

    private static final String[] MOJIBAKE_PATTERNS = {
        "\u00C3", // Ã
        "\u00C2", // Â
        "\uFFFD", // Replacement character 
        "ï¿½",
        "\u00F0\u0178", // ðŸ (corrupted emoji prefix like in Mis Reservas)
        "ðŸ",
        "âš"
    };

    @Test
    @DisplayName("Los templates Thymeleaf no deben contener secuencias corruptas de mojibake")
    void templatesMustNotContainMojibake() throws IOException {
        Path templatesDir = Path.of("src", "main", "resources", "templates");
        List<String> violations = new ArrayList<>();

        Files.walk(templatesDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().endsWith(".html"))
            .forEach(p -> {
                try {
                    String content = Files.readString(p, StandardCharsets.UTF_8);
                    for (String pattern : MOJIBAKE_PATTERNS) {
                        if (content.contains(pattern)) {
                            violations.add("Mojibake '" + pattern + "' encontrado en template: " + p);
                        }
                    }
                } catch (IOException e) {
                    violations.add("Error leyendo: " + p + ": " + e.getMessage());
                }
            });

        assertTrue(violations.isEmpty(), "Se encontraron secuencias corruptas de mojibake en templates:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("Los recursos estaticos JS y CSS no deben contener secuencias corruptas de mojibake")
    void staticAssetsMustNotContainMojibake() throws IOException {
        Path staticDir = Path.of("src", "main", "resources", "static");
        List<String> violations = new ArrayList<>();

        if (Files.exists(staticDir)) {
            Files.walk(staticDir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".js") || p.toString().endsWith(".css"))
                .forEach(p -> {
                    try {
                        String content = Files.readString(p, StandardCharsets.UTF_8);
                        for (String pattern : MOJIBAKE_PATTERNS) {
                            if (content.contains(pattern)) {
                                violations.add("Mojibake '" + pattern + "' encontrado en recurso: " + p);
                            }
                        }
                    } catch (IOException e) {
                        violations.add("Error leyendo: " + p + ": " + e.getMessage());
                    }
                });
        }

        assertTrue(violations.isEmpty(), "Se encontraron secuencias corruptas de mojibake en static:\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("El archivo SQL unico no debe contener secuencias corruptas de mojibake")
    void sqlMustNotContainMojibake() throws IOException {
        Path sqlPath = Path.of("..", "database", "canchas_web.sql");
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("database", "canchas_web.sql");
        }
        if (Files.exists(sqlPath)) {
            String content = Files.readString(sqlPath, StandardCharsets.UTF_8);
            for (String pattern : MOJIBAKE_PATTERNS) {
                if (content.contains(pattern)) {
                    if (content.contains("\u00C3\u00A1") || content.contains("\u00C3\u00B3") || content.contains("\u00C2\u00B7") || content.contains("ðŸ")) {
                        throw new AssertionError("Mojibake encontrado en SQL: " + pattern);
                    }
                }
            }
        }
    }
}