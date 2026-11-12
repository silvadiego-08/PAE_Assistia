-- Ejecutar con un superusuario de PostgreSQL:
--   psql -U postgres -f scripts/crear-bd-local.sql
-- Cambien la contraseña de ejemplo por la suya (NO la suban a Git).

CREATE USER app_med WITH PASSWORD 'cambiar_esta_clave';

-- OWNER app_med es necesario en PostgreSQL 15+ para que Hibernate pueda crear tablas en el esquema public.
CREATE DATABASE gestor_medicacion OWNER app_med;
