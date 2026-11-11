INSERT INTO paciente (id, nombres, apellidos, cedula) VALUES
    (1, 'Ana', 'Lopez', '001-010190-0001A'),
    (2, 'Luis', 'Martinez', '001-020291-0002B')
ON CONFLICT (id) DO NOTHING;

INSERT INTO medicamento (id, nombre, presentacion, concentracion) VALUES
    (1, 'Losartan', 'Tableta', '50 mg'),
    (2, 'Metformina', 'Tableta', '850 mg')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tratamiento (id, paciente_id, descripcion, fecha_inicio, fecha_fin, activo) VALUES
    (1, 1, 'Hipertension arterial', DATE '2026-11-01', DATE '2027-01-31', true),
    (2, 2, 'Diabetes mellitus tipo 2', DATE '2026-11-01', DATE '2027-01-31', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO detalle_tratamiento
    (id, tratamiento_id, medicamento_id, dosis, frecuencia_horas, indicaciones) VALUES
    (1, 1, 1, '1 tableta', 24, 'Tomar despues del desayuno'),
    (2, 2, 2, '1 tableta', 12, 'Tomar con las comidas')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('paciente', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM paciente), 1));
SELECT setval(pg_get_serial_sequence('medicamento', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM medicamento), 1));
SELECT setval(pg_get_serial_sequence('tratamiento', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM tratamiento), 1));
SELECT setval(pg_get_serial_sequence('detalle_tratamiento', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 1) FROM detalle_tratamiento), 1));
