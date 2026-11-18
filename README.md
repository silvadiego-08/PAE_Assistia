# Assistia

**Sistema de escritorio para la gestión de pacientes y el seguimiento de tratamientos**

## 1. Descripción del proyecto y datos del estudiante

Assistia es una aplicación de escritorio orientada al seguimiento de tratamientos en pacientes adultos mayores. El proyecto busca reunir en una misma aplicación los registros de pacientes, cuidadores, médicos y medicamentos para organizar los tratamientos y consultar el estado de las tomas.

| Información académica | Datos |
| --- | --- |
| Asignatura | Programación de Aplicaciones de Escritorio |
| Docente | José Durán |
| Integrantes del equipo | Mia Flores, Davis Rivera, Claudia Lira y Diego Silva |

El objetivo general es desarrollar un sistema que centralice la información de los pacientes y facilite el seguimiento de sus tratamientos. El alcance del equipo incluye el registro de tomas, la búsqueda de medicamentos con RxNorm y el reporte PDF del historial con JasperReports.


## 2. Requisitos

| Requisito | Uso |
| --- | --- |
| JDK 21 | Compilar y ejecutar el código Java. |
| Maven | Resolver dependencias y ejecutar el proyecto. El repositorio incluye Maven Wrapper (`mvnw` y `mvnw.cmd`), por lo que puedes usarlo sin instalar Maven por separado. |
| PostgreSQL | Almacenar los registros. El servidor debe estar en ejecución y el usuario de conexión debe poder crear tablas en la base local. |
| Git | Clonar el repositorio y consultar las ramas. |

La interfaz utiliza **JavaFX y FXML**. La persistencia se implementa con **Jakarta Persistence y Hibernate ORM**.

### Crear la base de datos local

**Con pgAdmin**

1. Conéctate como administrador y abre **Query Tool** sobre la base `postgres`. Ejecuta [crear-rol-local.sql](scripts/crear-rol-local.sql) si `assistia_local` aún no existe.
2. En **Login/Group Roles**, actualiza la lista, abre las propiedades de `assistia_local` y configura su contraseña en la pestaña **Definition**.
3. En un **Query Tool** nuevo sobre `postgres`, activa **Auto-commit** y ejecuta [crear-bd-local.sql](scripts/crear-bd-local.sql). Este archivo contiene únicamente la sentencia `CREATE DATABASE`.
4. Actualiza **Databases** y abre otro **Query Tool** sobre `assistia` con el usuario de `DB_USER` (`assistia_local` en una instalación nueva con estos pasos). Ejecuta [crear-tablas.sql](scripts/crear-tablas.sql) completo. Si la base ya tiene tablas, usa una conexión cuyo usuario sea propietario de las tablas existentes y pueda crear objetos en `public`. Las ocho tablas de la rama aparecen en **Schemas → public → Tables** después de actualizar el árbol.

Omite la creación del rol o de la base si ya existen. Para usar los scripts con otro nombre de base o de rol, adapta las sentencias y los valores de `db.properties`. El script de tablas puede ejecutarse de nuevo sobre el mismo esquema. Si las tablas ya existen, el script conserva su estructura.

**Con psql**

Conéctate al servidor PostgreSQL como administrador usando `psql`. Ajusta `postgres` si tu administrador tiene otro nombre.

```powershell
psql -U postgres -d postgres
```

Si todavía no tienes un usuario y una base para Assistia, ejecuta estas líneas dentro de `psql`. El comando `\password` te pedirá escribir y confirmar la contraseña del usuario nuevo.

```sql
CREATE ROLE assistia_local LOGIN;
\password assistia_local
CREATE DATABASE assistia OWNER assistia_local;
\q
```

Luego ejecuta el script de tablas desde la raíz del proyecto.

```powershell
psql -U assistia_local -d assistia -v ON_ERROR_STOP=1 -f scripts/crear-tablas.sql
```

Hibernate también puede crear o actualizar las tablas de las entidades registradas al inicializar la conexión. Si ya tienes una base local con tratamientos, vuelve a ejecutar [crear-tablas.sql](scripts/crear-tablas.sql) para agregar `registro_toma`.

## 3. Cómo ejecutar el proyecto

1. **Abre el repositorio.** Si aún no tienes una copia, clónalo. Si ya lo tienes, abre una terminal en la carpeta que contiene `pom.xml`.

   ```powershell
   git clone https://github.com/silvadiego-08/PAE_Assistia.git
   Set-Location PAE_Assistia
   ```

2. **Configura la conexión.** Si aún no tienes `db.properties`, copia la plantilla.

   ```powershell
   Copy-Item db.properties.example db.properties
   ```

   Abre `db.properties` y ajusta los datos a tu instalación. Completa la contraseña en tu archivo local.

   ```properties
   DB_URL=jdbc:postgresql://localhost:5432/assistia
   DB_USER=assistia_local
   DB_PASSWORD=
   ```

   Git omite `db.properties`. Si tienes valores definidos en las variables de entorno `DB_URL`, `DB_USER` o `DB_PASSWORD`, estos tienen prioridad sobre el archivo.

3. **Compila e inicia Assistia.** Ejecuta estos comandos en PowerShell desde la carpeta de `pom.xml`.

   ```powershell
   .\mvnw.cmd clean compile
   .\mvnw.cmd javafx:run
   ```

   Desde IntelliJ IDEA, puedes abrir `pom.xml` como proyecto Maven, seleccionar JDK 21 y ejecutar el objetivo `javafx:run`.

### Registrar una toma

1. Guarda un tratamiento activo con al menos un medicamento y abre **Tomas**.
2. Selecciona al paciente y al medicamento. Completa la fecha y la hora en formato de 24 horas, por ejemplo `08:30`, y pulsa **Programar toma**.
3. Elige el día programado en **Día de consulta** y selecciona la fila. Para reprogramar una toma pendiente, escribe la nueva fecha y hora y pulsa **Cambiar horario de la toma seleccionada**.
4. Pulsa **Marcar realizada** para guardar la hora actual. La toma conserva su hora programada y sigue visible en el día programado con estado **Realizada**.

## 4. Modelo de datos

| Entidad | Tabla | Atributos principales | Relaciones |  |
| --- | --- | --- | --- | --- |
| `Paciente` | `pacientes` | `id` (PK), `nombre`, `fechaNacimiento`, `telefono`. | Un paciente puede aparecer en varias asignaciones y tratamientos. | |
| `Cuidador` | `cuidadores` | `id` (PK), `nombre`, `telefono`, `correo`. | Un cuidador puede aparecer en varias asignaciones de pacientes. |  |
| `AsignacionCuidador` | `asignaciones_cuidador` | `id` (PK), `paciente_id` (FK), `cuidador_id` (FK), `parentesco`, `responsablePrincipal`. | Cada asignación referencia un paciente y un cuidador. Su combinación es única. |  |
| `Medico` | `medico` | `id` (PK), `nombre`, `especialidad`, `telefono`. | Un médico puede estar asociado a varios tratamientos. |  |
| `Medicamento` | `medicamento` | `id` (PK), `nombre`, `principioActivo`, `presentacion`, `concentracion`. | Un medicamento puede aparecer en varios detalles de tratamiento. | |
| `Tratamiento` | `tratamientos` | `id` (PK), `paciente_id` (FK), `medico_id` (FK), `fechaInicio`, `fechaFin`, `activo`. | Pertenece a un paciente y a un médico. Puede contener varios detalles. |  |
| `DetalleTratamiento` | `detalles_tratamiento` | `id` (PK), `tratamiento_id` (FK), `medicamento_id` (FK), `dosisIndicada`, `unidad`, `instrucciones`. | Pertenece a un tratamiento y referencia un medicamento. Puede tener varias tomas. |  |
| `RegistroToma` | `registro_toma` | `id` (PK), `detalle_tratamiento_id` (FK), `fechaHoraProgramada`, `fechaHoraReal`, `estado`, `observacion`. | Cada toma referencia un detalle de tratamiento. La combinación de detalle y hora programada es única. |  |

## 5. Decisiones de diseño

| Relación | Motivo del mapeo | Decisión sobre cascada |
| --- | --- | --- |
| Pacientes y cuidadores | Un paciente puede tener varios cuidadores y un cuidador puede atender a varios pacientes. Usamos `AsignacionCuidador` con dos relaciones `@ManyToOne` para guardar el parentesco y el indicador de responsable principal en cada vínculo. | Las relaciones de la asignación se guardan sin cascada. Quitar una asignación elimina el vínculo y conserva los registros del paciente y del cuidador. |
| Tratamiento con paciente y médico | Cada tratamiento se asigna a un solo paciente y a un solo médico. Ambos pueden aparecer en otros tratamientos, por eso se usa `@ManyToOne` en cada referencia. | Se vinculan registros existentes sin cascada. Guardar o actualizar un tratamiento solo modifica los datos que pertenecen al tratamiento. |
| Tratamiento y detalles | Un tratamiento agrupa varios detalles y cada detalle pertenece a un solo tratamiento. La colección usa `@OneToMany(mappedBy = "tratamiento")`. El detalle posee la FK con `@ManyToOne`. | `PERSIST` permite guardar los detalles nuevos al persistir el tratamiento. `MERGE` propaga la combinación de cambios cuando Hibernate realiza un `merge` del tratamiento. Las bajas se gestionan de forma explícita para conservar el historial. El mapeo prescinde de `REMOVE` y `orphanRemoval`. |
| Detalle y medicamento | Un medicamento del catálogo puede aparecer en varios tratamientos. Cada detalle usa `@ManyToOne` y guarda su propia dosis e instrucciones. | El medicamento conserva un ciclo de vida independiente. El detalle referencia un medicamento existente sin cascada. |
| Detalle y registro de toma | Un mismo detalle puede generar varias tomas programadas. Cada toma usa `@ManyToOne` hacia un detalle. | Las tomas se registran por separado y conservan su historial. Su relación con el detalle se guarda sin cascada. |

Las referencias `@ManyToOne` de estos módulos se declaran con `FetchType.LAZY`. Cuando una pantalla necesita datos de la relación, el DAO puede cargarlos de forma explícita. Por ejemplo, al consultar las asignaciones de un paciente, el DAO usa `join fetch` para traer el cuidador antes de cerrar la sesión.

## 6. Explicación con mis palabras

- Un ORM conecta las clases que uso en Java con las tablas de una base de datos.
- En Assistia, Hibernate interpreta las anotaciones de mis entidades para saber dónde guardar y consultar sus datos.
- La `Session` es el contexto en el que Hibernate carga entidades, les da seguimiento y detecta cambios.
- La `Transaction` agrupa las operaciones. Con `commit()` se confirman y con `rollback()` se revierten si ocurre un error.
- Una relación `LAZY` se carga cuando hace falta, por ejemplo al acceder al cuidador de una asignación.
- La `LazyInitializationException` aparece si intento acceder a una relación pendiente de cargar después de cerrar la sesión.
- Para evitarla, cargo la relación con `join fetch` dentro de la sesión o paso a la interfaz un objeto con los datos que ya consulté.

## 7. Evidencias

Las cinco capturas están pendientes. Estas son las rutas propuestas para guardarlas en la carpeta `evidencias/` junto al README.

| Captura | Archivo |
| --- | --- |
| 1. Assistia en ejecución, pestaña Pacientes | `01-pacientes.png` |
| 2. Formulario y listado de cuidadores | `02-cuidadores.png` |
| 3. Cuidador asignado a un paciente | `03-asignacion-cuidador.png` |
| 4. Asignación conservada al reiniciar Assistia | `04-persistencia.png` |
| 5. Tablas y claves foráneas del módulo en PostgreSQL | `05-modelo-postgresql.png` |

<!-- Quitar el comentario cuando las cinco capturas estén en evidencias/.

![Assistia en ejecución con la pestaña Pacientes](evidencias/01-pacientes.png)

![Formulario y listado de cuidadores](evidencias/02-cuidadores.png)

![Cuidador asignado a un paciente](evidencias/03-asignacion-cuidador.png)

![Asignación conservada después de reiniciar Assistia](evidencias/04-persistencia.png)

![Tablas y claves foráneas del módulo en PostgreSQL](evidencias/05-modelo-postgresql.png)

-->
