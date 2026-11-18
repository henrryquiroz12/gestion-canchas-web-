package com.canchas.web.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class BulkClientGenerator {

    private static final String[] FIRST_NAMES = {
        "Carlos", "Juan", "Jose", "Luis", "Miguel", "Jorge", "Victor", "Cesar", "Manuel", "David",
        "Pedro", "Alejandro", "Javier", "Daniel", "Diego", "Fernando", "Ricardo", "Eduardo", "Raul", "Mario",
        "Maria", "Ana", "Rosa", "Carmen", "Lucia", "Elena", "Patricia", "Sofia", "Valeria", "Camila"
    };

    private static final String[] LAST_NAMES_1 = {
        "Quispe", "Flores", "Rodriguez", "Sanchez", "Garcia", "Mendoza", "Rojas", "Diaz", "Torres", "Castillo",
        "Morales", "Gutierrez", "Chavez", "Ramos", "Espinoza", "Vargas", "Vasquez", "Fernandez", "Mamani", "Alvarez",
        "Romero", "Navarro", "Campos", "Vega", "Perez", "Cruz", "Silva", "Huaman", "Cabrera", "Ruiz"
    };

    private static final String[] LAST_NAMES_2 = {
        "Herrera", "Medina", "Salazar", "Castro", "Paredes", "Gomez", "Guerrero", "Reyes", "Cardenas", "Aguilar",
        "Benitez", "Bravo", "Carrasco", "Delgado", "Estrada", "Falcon", "Gallardo", "Hidalgo", "Ibañez", "Juarez",
        "Lara", "Montes", "Noriega", "Olivares", "Palacios", "Quintana", "Rios", "Serrano", "Tello", "Urteaga"
    };

    private static final String[] DEPARTMENTS = {
        "LA_LIBERTAD", "LIMA", "AREQUIPA", "LAMBAYEQUE", "PIURA", "ANCASH", "CAJAMARCA", "CUSCO"
    };

    private static final String[] PROVINCES = {
        "Trujillo", "Lima", "Arequipa", "Chiclayo", "Piura", "Santa", "Cajamarca", "Cusco"
    };

    // BCrypt for password 'Proyectoweb2026'
    private static final String DEFAULT_PASSWORD_HASH = "$2a$10$jeHX8uTiYT3uQc4uHXF4rO9DtY15MGT8Ks77TmPFpt0IxzDZab0a6";

    public static void main(String[] args) {
        String url = System.getProperty("db.url", "jdbc:mysql://localhost:3306/canchas_web?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima&characterEncoding=UTF-8&rewriteBatchedStatements=true");
        String user = System.getProperty("db.user", "root");
        String pass = System.getProperty("db.pass", "admin123");
        int targetTotal = 1_000_000;

        System.out.println("🚀 Iniciando registro masivo de " + targetTotal + " clientes...");
        long startTime = System.currentTimeMillis();

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(false);

            try (Statement st = conn.createStatement()) {
                st.execute("SET foreign_key_checks = 0;");
                st.execute("SET unique_checks = 0;");
            }

            // Check current max id
            long startId = 2; // Marlon is id 1

            String sqlPersona = "INSERT INTO personas (id, nombres, apellido_paterno, apellido_materno, dni, email, telefono, direccion, genero, departamento, provincia, fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '2026-11-11')";
            String sqlUsuario = "INSERT INTO usuarios (id, persona_id, apodo, usuario, password, activo) VALUES (?, ?, ?, ?, ?, 1)";
            String sqlRoles = "INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (?, 2)";

            PreparedStatement psPersona = conn.prepareStatement(sqlPersona);
            PreparedStatement psUsuario = conn.prepareStatement(sqlUsuario);
            PreparedStatement psRoles = conn.prepareStatement(sqlRoles);

            int batchSize = 10_000;
            int batches = targetTotal / batchSize;

            for (int b = 0; b < batches; b++) {
                for (int i = 0; i < batchSize; i++) {
                    long currentId = startId + (long) b * batchSize + i;
                    int idx = (int) (currentId - 2);

                    String nombre = FIRST_NAMES[idx % FIRST_NAMES.length];
                    String apPaterno = LAST_NAMES_1[(idx / FIRST_NAMES.length) % LAST_NAMES_1.length];
                    String apMaterno = LAST_NAMES_2[(idx / (FIRST_NAMES.length * LAST_NAMES_1.length)) % LAST_NAMES_2.length];

                    // DNI 8 digits unique: 10000000 + idx
                    String dni = String.format("%08d", 10000000 + (idx % 90000000));
                    String email = "cliente" + currentId + "@canchasweb.pe";
                    String telefono = "9" + String.format("%08d", (idx * 31 + 1234567) % 100000000);
                    String direccion = "Av. Deportiva " + (100 + (idx % 900));
                    String genero = (idx % 2 == 0) ? "MALE" : "FEMALE";
                    int deptIdx = idx % DEPARTMENTS.length;
                    String dept = DEPARTMENTS[deptIdx];
                    String prov = PROVINCES[deptIdx];

                    // Persona
                    psPersona.setLong(1, currentId);
                    psPersona.setString(2, nombre);
                    psPersona.setString(3, apPaterno);
                    psPersona.setString(4, apMaterno);
                    psPersona.setString(5, dni);
                    psPersona.setString(6, email);
                    psPersona.setString(7, telefono);
                    psPersona.setString(8, direccion);
                    psPersona.setString(9, genero);
                    psPersona.setString(10, dept);
                    psPersona.setString(11, prov);
                    psPersona.addBatch();

                    // Usuario
                    String username = "cliente" + currentId;
                    String apodo = "Cli" + currentId;
                    psUsuario.setLong(1, currentId);
                    psUsuario.setLong(2, currentId);
                    psUsuario.setString(3, apodo);
                    psUsuario.setString(4, username);
                    psUsuario.setString(5, DEFAULT_PASSWORD_HASH);
                    psUsuario.addBatch();

                    // Roles (ROLE_CLIENTE = 2)
                    psRoles.setLong(1, currentId);
                    psRoles.addBatch();
                }

                psPersona.executeBatch();
                psUsuario.executeBatch();
                psRoles.executeBatch();
                conn.commit();

                int insertedSoFar = (b + 1) * batchSize;
                if (insertedSoFar % 100_000 == 0 || insertedSoFar == targetTotal) {
                    double elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0;
                    System.out.printf("  ✓ Insertados %d / %d clientes (%.1f s)...%n", insertedSoFar, targetTotal, elapsedSec);
                }
            }

            try (Statement st = conn.createStatement()) {
                st.execute("SET foreign_key_checks = 1;");
                st.execute("SET unique_checks = 1;");
            }
            conn.commit();

            long totalTime = System.currentTimeMillis() - startTime;
            System.out.printf("🎉 ¡1,000,000 clientes insertados exitosamente en %.2f segundos!%n", totalTime / 1000.0);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error en la generación masiva: " + e.getMessage());
        }
    }
}

