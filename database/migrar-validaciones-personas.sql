-- Ejecutar sobre la base existente. La consulta devuelve los ID y campos incompatibles.
-- Si devuelve filas, revisar con el responsable antes de ejecutar el ALTER.
-- MySQL valida las filas existentes en el ALTER y rechaza toda la operación si alguna incumple.
USE canchas_web;

WITH revision AS (
    SELECT id,
           NOT (REGEXP_LIKE(nombres, '\\A\\p{L}[\\p{L}\\p{M}]*( +\\p{L}[\\p{L}\\p{M}]*)*\\z', 'c')) AS nombres_invalido,
           NOT (CHAR_LENGTH(apellido_paterno) = 0 OR REGEXP_LIKE(apellido_paterno, '\\A\\p{L}[\\p{L}\\p{M}]*( +\\p{L}[\\p{L}\\p{M}]*)*\\z', 'c')) AS apellido_paterno_invalido,
           NOT (CHAR_LENGTH(apellido_materno) = 0 OR REGEXP_LIKE(apellido_materno, '\\A\\p{L}[\\p{L}\\p{M}]*( +\\p{L}[\\p{L}\\p{M}]*)*\\z', 'c')) AS apellido_materno_invalido,
           NOT (dni IS NULL OR (CHAR_LENGTH(dni) = 8 AND NOT REGEXP_LIKE(dni, '[^0-9]', 'c'))) AS dni_invalido,
           NOT (telefono IS NULL OR (CHAR_LENGTH(telefono) = 9 AND LEFT(telefono, 1) = '9' AND NOT REGEXP_LIKE(telefono, '[^0-9]', 'c'))) AS telefono_invalido
    FROM personas
)
SELECT *
FROM revision
WHERE nombres_invalido OR apellido_paterno_invalido OR apellido_materno_invalido
   OR dni_invalido OR telefono_invalido
ORDER BY id;

ALTER TABLE personas
    ADD CONSTRAINT chk_persona_nombres CHECK (REGEXP_LIKE(nombres, '\\A\\p{L}[\\p{L}\\p{M}]*( +\\p{L}[\\p{L}\\p{M}]*)*\\z', 'c')),
    ADD CONSTRAINT chk_persona_apellido_paterno CHECK (CHAR_LENGTH(apellido_paterno) = 0 OR REGEXP_LIKE(apellido_paterno, '\\A\\p{L}[\\p{L}\\p{M}]*( +\\p{L}[\\p{L}\\p{M}]*)*\\z', 'c')),
    ADD CONSTRAINT chk_persona_apellido_materno CHECK (CHAR_LENGTH(apellido_materno) = 0 OR REGEXP_LIKE(apellido_materno, '\\A\\p{L}[\\p{L}\\p{M}]*( +\\p{L}[\\p{L}\\p{M}]*)*\\z', 'c')),
    ADD CONSTRAINT chk_persona_dni CHECK (dni IS NULL OR (CHAR_LENGTH(dni) = 8 AND NOT REGEXP_LIKE(dni, '[^0-9]', 'c')));
