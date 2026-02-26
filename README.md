# Magic Field - Backend (Base)

Esta carpeta contiene la **base** del backend para el proyecto Magic Field.

Objetivo: generar una API REST mínima y compilable con las siguientes características:

- Java 17 + Spring Boot
- Spring Security con verificación de Firebase ID Token
- JPA/Hibernate con PostgreSQL
- Estructura mínima y explícita, lista para extender conforme al README raíz

## Archivos clave

- `pom.xml` - Dependencias y build
- `src/main/java/com/magicfield/backend` - Código fuente
- `src/main/resources/application.yml` - Configuración (usar variables de entorno)
- `docker-compose.yml` - Desarrollo con PostgreSQL (En la carpeta docker)

## Variables de entorno / configuración

- `DB_URL` (opcional, por defecto `jdbc:postgresql://localhost:5432/magicfield`)
- `DB_USER` (por defecto `postgres`)
- `DB_PASSWORD` (por defecto `postgres`)
- `FIREBASE_SERVICE_ACCOUNT_PATH` - Ruta al JSON del service account para verificar tokens

## Ejecutar en desarrollo

1. Levantar PostgreSQL:

   docker-compose up -d

2. Configurar `FIREBASE_SERVICE_ACCOUNT_PATH` si se desea verificar tokens con Firebase.

3. Construir y ejecutar:

   mvn -f back/pom.xml clean package
   java -jar back/target/backend-0.0.1-SNAPSHOT.jar

## Notas importantes

- Se dejó `hibernate.ddl-auto: update` para facilitar desarrollo. Cambiar a migraciones en producción.
- La verificación de tokens requiere inicializar Firebase con una service account; si no está configurada, el servidor arrancará pero la verificación fallará (se documenta con TODOs).
- No se añadieron features fuera del alcance del README principal.

---

Desarrollado para ser una base clara, explícita y fácil de explicar en entrevistas técnicas.