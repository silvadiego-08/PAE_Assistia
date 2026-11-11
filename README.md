# PAE_Assistia - Módulo 4: Registro de tomas

Implementación de `RegistroToma` con Hibernate 7, PostgreSQL y JavaFX.
Incluye la segunda consulta HQL, la pantalla de programación/confirmación de
tomas y el esquema con la restricción de unicidad en PostgreSQL.

## Requisitos

- JDK 21 o superior (probado con JDK 25).
- PostgreSQL 16 o superior (probado con PostgreSQL 18).
- Maven Wrapper incluido en el proyecto.

## 1. Crear la base de datos

Reemplaza `TU_PASSWORD` por la contraseña del usuario `postgres`.

```powershell
$env:ASSISTIA_DB_URL = "jdbc:postgresql://localhost:5432/assistia"
$env:ASSISTIA_DB_USER = "postgres"
$env:ASSISTIA_DB_PASSWORD = "TU_PASSWORD"
$env:PGPASSWORD = $env:ASSISTIA_DB_PASSWORD

$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
& $psql -U postgres -h localhost -c "CREATE DATABASE assistia;"
& $psql -U postgres -h localhost -d assistia -f src/main/resources/db/schema.sql
& $psql -U postgres -h localhost -d assistia -f src/main/resources/db/seed-demo.sql
```

Si la base ya existe, el primer comando muestra un error y puedes ignorarlo.
Las variables de entorno deben estar definidas en la misma terminal donde se
ejecuta la aplicación o en la configuración de ejecución de IntelliJ.

## 2. Compilar y ejecutar

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd javafx:run
```

También se puede ejecutar `HelloApplication` desde IntelliJ, definiendo antes
`ASSISTIA_DB_URL`, `ASSISTIA_DB_USER` y `ASSISTIA_DB_PASSWORD`.

## 3. Demostración con datos ficticios

`seed-demo.sql` crea:

- Paciente **Ana López** con el detalle **Losartán 50 mg**.
- Paciente **Luis Martínez** con el detalle **Metformina 850 mg**.

### Programar una toma

1. Selecciona a **Ana López** y el detalle **Losartán 50 mg**.
2. Elige la fecha `12/11/2026` y la hora `08:00`.
3. Presiona **Programar**.
4. La toma debe aparecer en la tabla de pendientes con estado `PENDIENTE`.

### Marcar como realizada

1. Selecciona la fila de la toma.
2. Presiona **Marcar como realizada** y confirma.
3. La toma desaparece de la tabla de pendientes.
4. Verifica que sigue guardada en PostgreSQL:

```sql
SELECT r.id, p.nombres, m.nombre, r.fecha_hora_programada,
       r.fecha_hora_real, r.estado
FROM registro_toma r
JOIN detalle_tratamiento d ON d.id = r.detalle_tratamiento_id
JOIN tratamiento t ON t.id = d.tratamiento_id
JOIN paciente p ON p.id = t.paciente_id
JOIN medicamento m ON m.id = d.medicamento_id
ORDER BY r.id;
```

La toma debe seguir existiendo con `estado = 'REALIZADA'`, la hora programada
original y una hora real distinta.

### Prueba con dos pacientes

1. Con la misma fecha, cambia a **Luis Martínez**.
2. La toma de Ana no debe aparecer.
3. Programa una toma para Luis y verifica que tampoco aparece para Ana.
4. La consulta HQL filtra por `t.paciente.id`, no solo por la tabla de tomas.

### Prueba de toma a medianoche

1. Programa una toma para Ana el `13/11/2026` a las `00:00`.
2. Debe aparecer al seleccionar la fecha `13/11/2026`.
3. Al cambiar a `12/11/2026` no debe aparecer.
4. El filtro usa `fechaHoraProgramada >= inicio del día` y
   `fechaHoraProgramada < inicio del día siguiente`, por lo que una toma a las
   `00:00` pertenece únicamente al día que empieza.

### Prueba de duplicados

Intenta programar dos veces el mismo detalle, fecha y hora. La aplicación debe
mostrar un mensaje claro y PostgreSQL debe rechazar el duplicado mediante
`uq_registro_toma_detalle_hora`.

## 4. Segunda consulta HQL

```java
select r from RegistroToma r
join fetch r.detalleTratamiento d
join fetch d.medicamento
join fetch d.tratamiento t
where t.paciente.id = :pacienteId
  and r.estado = :estado
  and r.fechaHoraProgramada >= :desde
  and r.fechaHoraProgramada < :hasta
order by r.fechaHoraProgramada
```

Se usa `EstadoToma.PENDIENTE`, con `desde = dia.atStartOfDay()` y
`hasta = dia.plusDays(1).atStartOfDay()`. El DAO devuelve una lista de
`TomaPendienteVista` con los datos ya preparados para JavaFX.

## 5. Reglas implementadas

- `RegistroToma` tiene clave foránea obligatoria a `DetalleTratamiento`.
- `EstadoToma` se guarda como `VARCHAR` mediante `EnumType.STRING`.
- Al marcar como realizada se conserva la hora programada y se guarda la hora
  real por separado.
- Marcar una toma ya realizada no cambia su hora real.
- Solo se pueden reprogramar tomas pendientes.
- `eliminarPendiente` solo borra tomas pendientes; las realizadas se conservan.
- La combinación detalle + hora programada es única en PostgreSQL.

## 6. Estructura del módulo

```text
src/main/java/ni/edu/uam/assistia/
  modelo/          Paciente, Medicamento, Tratamiento,
                   DetalleTratamiento, RegistroToma, EstadoToma
  persistencia/    HibernateUtil, RegistroTomaDAO,
                   TomaPendienteVista, RegistroTomaException
  RegistroTomaController.java
  HelloApplication.java
src/main/resources/
  db/schema.sql
  db/seed-demo.sql
  ni/edu/uam/assistia/registro-toma-view.fxml
```
