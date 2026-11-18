# Assistia

**Sistema de escritorio para la gestión de pacientes y el seguimiento de tratamientos**

## 1. Descripción del proyecto y datos del estudiante

Assistia es una aplicación de escritorio orientada al seguimiento de tratamientos en pacientes adultos mayores. El proyecto busca reunir en una misma aplicación los registros de pacientes, cuidadores, médicos y medicamentos para organizar los tratamientos y consultar el estado de las tomas.

| Información académica | Datos |
| --- | --- |
| Asignatura | Programación de Aplicaciones de Escritorio |
| Docente | José Durán |
| Estudiante | Diego Silva |
| Integrantes del equipo | Mia Flores, Davis Rivera, Claudia Lira y Diego Silva |

El objetivo general es desarrollar un sistema que centralice la información de los pacientes y facilite el seguimiento de sus tratamientos. El alcance del equipo incluye el registro de tomas, la búsqueda de medicamentos con RxNorm y el reporte PDF del historial con JasperReports.

La rama `feat/Diego` contiene los módulos de **Pacientes** y **Cuidadores**, con el registro y la eliminación de asignaciones entre ambos. Los catálogos de **Medicamentos** y **Médicos** están integrados en `main`. Los modelos de **Tratamientos** y **Registro de tomas** se describen como parte del alcance del equipo y corresponden a sus respectivas ramas de desarrollo.

## 2. Requisitos

| Requisito | Uso |
| --- | --- |
| JDK 21 | Compilar y ejecutar el código Java. |
| Maven | Resolver dependencias y ejecutar el proyecto. El repositorio incluye Maven Wrapper (`mvnw` y `mvnw.cmd`), por lo que puedes usarlo sin instalar Maven por separado. |
| PostgreSQL | Almacenar los registros. El servidor debe estar en ejecución y el usuario de conexión debe poder crear tablas en la base local. |
| Git | Clonar el repositorio y consultar las ramas. |

La interfaz utiliza **JavaFX y FXML**. La persistencia se implementa con **Jakarta Persistence y Hibernate ORM**.

### Crear la base de datos local

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

También puedes crear el rol con permiso de inicio de sesión y asignarlo como propietario de `assistia` desde pgAdmin. Si ya tienes una base local de desarrollo, usa su nombre y el usuario correspondiente al configurar la aplicación. Hibernate crea o actualiza las tablas de las entidades registradas cuando se inicializa la conexión.

## 3. Cómo ejecutar el proyecto

1. **Abre el repositorio.** Si aún no tienes una copia, clónalo. Si ya lo tienes, abre una terminal en la carpeta que contiene `pom.xml`.

   ```powershell
   git clone https://github.com/silvadiego-08/PAE_Assistia.git
   Set-Location PAE_Assistia
   ```

2. **Elige la rama.** El clon abre la rama predeterminada del repositorio. Para reproducir el módulo de Diego, cambia a su rama.

   ```powershell
   git switch feat/Diego
   ```

3. **Configura la conexión.** Si aún no tienes `db.properties`, copia la plantilla.

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

4. **Compila e inicia Assistia.** Ejecuta estos comandos en PowerShell desde la carpeta de `pom.xml`.

   ```powershell
   .\mvnw.cmd clean compile
   .\mvnw.cmd javafx:run
   ```

   Desde IntelliJ IDEA, puedes abrir `pom.xml` como proyecto Maven, seleccionar JDK 21 y ejecutar el objetivo `javafx:run`. Para verificar la persistencia en `feat/Diego`, crea un paciente y un cuidador ficticios, vincúlalos, reinicia Assistia y comprueba que la asignación se conserva.

## 4. Modelo de datos

**PK** identifica un registro de forma única y **FK** referencia la clave de otra tabla. El estado indica dónde se verificó el modelo.

| Entidad | Tabla | Atributos principales | Relaciones | Estado |
| --- | --- | --- | --- | --- |
| `Paciente` | `pacientes` | `id` (PK), `nombre`, `fechaNacimiento`, `telefono`. | Un paciente puede aparecer en varias asignaciones y tratamientos. | `feat/Diego` y `main` |
| `Cuidador` | `cuidadores` | `id` (PK), `nombre`, `telefono`, `correo`. | Un cuidador puede aparecer en varias asignaciones de pacientes. | `feat/Diego` y `main` |
| `AsignacionCuidador` | `asignaciones_cuidador` | `id` (PK), `paciente_id` (FK), `cuidador_id` (FK), `parentesco`, `responsablePrincipal`. | Cada asignación referencia un paciente y un cuidador. Su combinación es única. | `feat/Diego` y `main` |
| `Medico` | `medico` | `id` (PK), `nombre`, `especialidad`, `telefono`. | Un médico puede estar asociado a varios tratamientos. | `main` |
| `Medicamento` | `medicamento` | `id` (PK), `nombre`, `principioActivo`, `presentacion`, `concentracion`. | Un medicamento puede aparecer en varios detalles de tratamiento. | `main` |
| `Tratamiento` | `tratamientos` | `id` (PK), `paciente_id` (FK), `medico_id` (FK), `fechaInicio`, `fechaFin`, `activo`. | Pertenece a un paciente y a un médico. Puede contener varios detalles. | `feat/Mia` |
| `DetalleTratamiento` | `detalles_tratamiento` | `id` (PK), `tratamiento_id` (FK), `medicamento_id` (FK), `dosisIndicada`, `unidad`, `instrucciones`. | Pertenece a un tratamiento y referencia un medicamento. Puede tener varias tomas. | `feat/Mia` |
| `RegistroToma` | `registro_toma` | `id` (PK), `detalle_tratamiento_id` (FK), `fechaHoraProgramada`, `fechaHoraReal`, `estado`, `observacion`. | Cada toma referencia un detalle de tratamiento. La combinación de detalle y hora programada es única. | `feat/registro-tomas`, pendiente de integración |

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
