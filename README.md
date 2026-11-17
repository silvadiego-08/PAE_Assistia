# PAE_Assistia

## Ejecutar los CRUD de pacientes y cuidadores

Abre el proyecto Maven en IntelliJ IDEA con JDK 21. En PostgreSQL, crea una
base de datos local llamada `assistia` (o usa otra base de desarrollo) y
asigna como propietario un usuario local de PostgreSQL.

Desde la carpeta que contiene `pom.xml`, copia la plantilla:

```powershell
Copy-Item db.properties.example db.properties
.\mvnw.cmd javafx:run
```

Antes de ejecutar el segundo comando, abre `db.properties` y completa
`DB_URL`, `DB_USER` y `DB_PASSWORD` con tus datos locales. Git ignora ese
archivo. Si la contraseña contiene una barra invertida, escríbela como
`\\` en el archivo.

En IntelliJ, ejecuta el objetivo Maven `javafx:run` desde la carpeta que
contiene `pom.xml`. Las variables de entorno que ya tengas configuradas
tienen prioridad sobre los valores del archivo. Hibernate crea o actualiza
las tablas de desarrollo. Las entidades se registran en `HibernateConfig.java`.

Para comprobar el CRUD, guarda un cuidador ficticio, selecciónalo y edita su
teléfono, reinicia y verifica el cambio. Selecciona el registro, elimínalo y
confirma que desaparece de la tabla. Prueba guardar un nombre vacío o un
correo inválido. Si un cuidador tiene una asignación, la pantalla debe
informar que hay que quitarla antes de eliminarlo.

En **Pacientes**, guarda un paciente ficticio con una fecha de nacimiento,
edítalo y comprueba que el cambio permanece al reiniciar. Selecciona al paciente,
elige un cuidador y pulsa **Asignar**. Comprueba que la misma pareja no se pueda
registrar dos veces y que no puedas eliminar un paciente con una asignación.
Quita la asignación y verifica que el paciente y el cuidador sigan en sus
respectivas tablas. Luego elimina los registros de prueba.
