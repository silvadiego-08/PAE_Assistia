## Base de datos local (PostgreSQL + Hibernate)

### 1. Requisitos
JDK 21, PostgreSQL 15 o 16, IntelliJ IDEA, Maven.

### 2. Crear la base de datos local
Cada integrante crea su propia base. Con un superusuario de PostgreSQL:

```bash
psql -U postgres -f scripts/crear-bd-local.sql
```

El script crea el usuario `app_med` (contraseña de ejemplo: cámbiela) y la base `gestor_medicacion`
con `app_med` como propietario. Ese propietario es necesario en PostgreSQL 15+ para que Hibernate
pueda crear las tablas.

Las tablas se crean solas al iniciar la aplicación (`hibernate.hbm2ddl.auto=update`).

### 3. Configurar las variables de entorno en IntelliJ
1. `Run > Edit Configurations…`, y elija la configuración de `Launcher` (créela con `+ > Application` si no existe; clase principal `ni.edu.uam.assistia.Launcher`).
2. En **Environment variables** escriba (reemplace por sus valores):

```
DB_URL=jdbc:postgresql://localhost:5432/gestor_medicacion;DB_USER=app_med;DB_PASSWORD=su_clave_local
```

`.env.example` muestra los nombres; **no** suba ningún archivo con credenciales reales.

Desde terminal también funciona:

```bash
# Linux / macOS
export DB_URL=jdbc:postgresql://localhost:5432/gestor_medicacion DB_USER=app_med DB_PASSWORD=su_clave_local
mvn javafx:run

# Windows PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5432/gestor_medicacion"; $env:DB_USER="app_med"; $env:DB_PASSWORD="su_clave_local"; mvn javafx:run
```

### 4. Comprobar la conexión
- Al ejecutar, la consola muestra `Conectado a: PostgreSQL 16…`.
- En `psql -U app_med -d gestor_medicacion` ejecute `\dt`: deben aparecer `medicamento` y `medico`.
- Si falta una variable o la conexión falla, la aplicación muestra un mensaje claro y se cierra.

### 5. Agregar entidades de otros módulos
Registre la clase en la lista `ENTIDADES` de `ni/edu/uam/assistia/util/HibernateUtil.java` (una línea por entidad).

### 6. Reiniciar el esquema local (si cambian columnas)
`hbm2ddl=update` no modifica columnas existentes. Para empezar de cero en su base local:

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```
