# Base de datos local

Sigue las instrucciones del [README principal](../README.md) para configurar PostgreSQL con `db.properties` o con variables de entorno.

Assistia usa una sola configuración de Hibernate para Pacientes, Cuidadores, Medicamentos y Médicos. Registra las entidades nuevas en `HibernateConfig.java`.

Para verificar los catálogos, abre las pestañas **Medicamentos** y **Médicos**. Crea un registro ficticio en cada una, cierra la aplicación y vuelve a abrirla para confirmar que ambos registros siguen disponibles. Luego prueba editar y eliminar los registros de prueba.
