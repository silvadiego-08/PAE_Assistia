-- Ejecutar en Query Tool sobre postgres con un administrador y Auto-commit activado.
-- El rol assistia_local debe existir antes de ejecutar este archivo.
-- Si assistia ya existe, usa esa base y verifica quién es su propietario.
CREATE DATABASE assistia OWNER assistia_local;
