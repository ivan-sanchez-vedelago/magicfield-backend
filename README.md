# Magic Field - Backend

Esta carpeta contiene la **base** del backend para el proyecto Magic Field.

Objetivo: generar una API REST mínima y compilable con las siguientes características:

- Java 17 + Spring Boot
- Spring Security con verificación de Firebase ID Token
- JPA/Hibernate con PostgreSQL
- Estructura mínima y explícita, lista para extender conforme al README raíz

## Stack Tecnológico

- **Lenguaje**: Java 17
- **Framework**: Spring Boot 3.x
- **Base de Datos**: PostgreSQL
- **ORM**: JPA/Hibernate
- **Seguridad**: Spring Security + Firebase Auth
- **Build Tool**: Maven
- **Contenedor**: Docker (para desarrollo)

## Archivos Clave

- `pom.xml` - Dependencias y configuración de build
- `src/main/java/com/magicfield/backend` - Código fuente Java
- `src/main/resources/application.yml` - Configuración de aplicación
- `docker-compose.yml` - Base de datos PostgreSQL para desarrollo
- `Dockerfile` - Contenedorización de la aplicación

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/magicfield/backend/
│   │   ├── MagicFieldApplication.java      # Clase principal
│   │   ├── config/                         # Configuraciones
│   │   ├── controller/                     # Controladores REST
│   │   ├── dto/                            # Objetos de Transferencia de Datos
│   │   ├── entity/                         # Entidades JPA
│   │   ├── exception/                      # Manejo de excepciones
│   │   ├── repository/                     # Repositorios JPA
│   │   ├── security/                       # Configuración de seguridad
│   │   └── service/                        # Lógica de negocio
│   └── resources/
│       └── application.yml                 # Configuración
└── test/                                   # Tests (por implementar)
```

## Variables de Entorno / Configuración

### Base de Datos
- `SPRING_DATASOURCE_URL` (por defecto: `jdbc:postgresql://localhost:5432/magicfield`)
- `SPRING_DATASOURCE_USERNAME` (por defecto: `postgres`)
- `SPRING_DATASOURCE_PASSWORD` (por defecto: `postgres`)

### Firebase Authentication
- `FIREBASE_SERVICE_ACCOUNT_PATH` - Ruta al archivo JSON de service account
- `FIREBASE_SERVICE_ACCOUNT_JSON` - JSON inline del service account (alternativo)

### Email (para notificaciones)
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`

### Aplicación
- `PORT` (por defecto: 8080)
- `APP_ADMIN_EMAIL`
- `APP_EMAIL_FROM`

### Firebase Storage
- `FIREBASE_BUCKET`

### Resend API (para emails)
- `RESEND_API_KEY`

## Instalación y Configuración

### Prerrequisitos
- Java 17+
- Maven 3.6+
- Docker (opcional, para PostgreSQL)

### 1. Clonar/Configurar
```bash
cd magicfield-back
```

### 2. Configurar Base de Datos
Opciones:

**Opción A: Docker (Recomendado)**
```bash
docker-compose up -d
```

**Opción B: PostgreSQL Local**
Asegurar que PostgreSQL esté corriendo en puerto 5432 con usuario `postgres` y password `postgres`.

### 3. Configurar Variables de Entorno
Crear archivo `.env` en la raíz del proyecto:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/magicfield
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/serviceAccountKey.json

SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password

APP_ADMIN_EMAIL=admin@magicfield.com
APP_EMAIL_FROM=noreply@magicfield.com

FIREBASE_BUCKET=magicfield.appspot.com
RESEND_API_KEY=your-resend-api-key
```

## Ejecutar en Desarrollo

### Opción 1: Con Docker (Recomendado)

1. **Levantar el contenedor de PostgreSQL**
   ```bash
   docker-compose up -d
   ```

2. **Ejecutar Spring Boot con Maven**
   ```powershell
   # Windows PowerShell
   $env:JAVA_TOOL_OPTIONS='-Duser.timezone=UTC'; mvn -f pom.xml spring-boot:run
   ```
   
   ```bash
   # Linux/Mac
   export JAVA_TOOL_OPTIONS='-Duser.timezone=UTC' && mvn -f pom.xml spring-boot:run
   ```

### Opción 2: Build completo
```bash
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

### Opción 3: Sin Docker (PostgreSQL local)
```bash
mvn spring-boot:run
```

### Verificar
La aplicación estará disponible en: `http://localhost:8080`

Endpoint de health check: `GET /health`

## API Endpoints

### Productos
- `GET /api/products` - Listar productos
- `GET /api/products/{id}` - Obtener producto por ID
- `POST /api/products` - Crear producto
- `PUT /api/products/{id}` - Actualizar producto
- `DELETE /api/products/{id}` - Eliminar producto

### Órdenes
- `POST /api/orders/checkout` - Procesar checkout

### Subida de Imágenes
La API acepta subida de productos con imágenes vía multipart/form-data.

**Ejemplo con curl:**
```bash
curl -X POST "http://localhost:8080/api/products" \
  -F 'product={"name":"Camiseta","description":"Camiseta de algodón","price":19.99,"stock":50};type=application/json' \
  -F "images=@/path/to/img1.jpg" \
  -F "images=@/path/to/img2.png"
```

**Postman:**
1. Método: POST, URL: `http://localhost:8080/api/products`
2. Body > form-data
3. Key `product` (Text): `{"name":"Camiseta","description":"...","price":19.99,"stock":50}`
4. Keys `images` (File): Seleccionar archivos de imagen

**Notas:**
- El campo `product` debe ser JSON válido
- `images` es opcional, acepta múltiples archivos
- Las imágenes se almacenan como LOBs en la tabla `images`

## Configuración de Seguridad

### Firebase Authentication
- La aplicación verifica ID Tokens de Firebase
- Requiere configuración de service account
- Si no está configurado, la verificación falla silenciosamente

### CORS
- Configurado para permitir orígenes del frontend
- Ajustar en `config/CorsConfig.java` según necesidad

## Base de Datos

### Esquema
- `products` - Productos del catálogo
- `users` - Usuarios (por implementar)
- `orders` - Órdenes de compra
- `order_items` - Items de órdenes
- `payments` - Pagos
- `images` - Imágenes de productos (LOB)

### Migraciones
- `hibernate.ddl-auto: update` (desarrollo)
- Cambiar a `validate` o `none` en producción
- Usar Flyway o Liquibase para migraciones controladas

## Desarrollo

### Agregar Nuevos Endpoints
1. Crear DTO en `dto/`
2. Crear entidad en `entity/` (si aplica)
3. Crear repositorio en `repository/`
4. Crear servicio en `service/`
5. Crear controlador en `controller/`

### Manejo de Errores
- Excepciones globales en `exception/`
- `@ControllerAdvice` para respuestas consistentes

---

## Telemetría y Observabilidad

El proyecto implementa una estrategia de telemetría completa en tres capas: backend, frontend y admin. Toda la infraestructura opera en free tiers ($0/mes).

### Arquitectura

```
Frontend (Vercel)              Admin (Expo)
  │  │                           │
  │  └── Sentry ──────────┐     │ GET /api/admin/dashboard-stats
  │                        │     │
  └── Vercel Analytics     │     ▼
      + Speed Insights     │   Spring Boot Backend
                           │     │   │
                           │     │   ├── /actuator/prometheus → Grafana Cloud
                           │     │   ├── /actuator/health     → Health checks
                           │     │   └── Logback JSON         → Grafana Loki
                           │     │
                           └─────┴── Sentry (errores server-side)
```

---

### 1. Backend — Grafana Cloud (Métricas + Logs + Trazas)

**Acceso:** https://grafana.com → tu stack gratuito

**Endpoints locales para verificar:**
- `GET /actuator/health` — Estado de salud (DB, disk, etc.)
- `GET /actuator/prometheus` — Métricas en formato Prometheus (scrape target)
- `GET /actuator/metrics` — Lista de métricas disponibles

**Métricas disponibles:**
| Métrica | Descripción |
|---|---|
| `http_server_requests_seconds` | Latencia por endpoint (con percentiles) |
| `jvm_memory_used_bytes` | Uso de memoria JVM |
| `hikaricp_connections_active` | Conexiones activas a PostgreSQL |
| `business_checkout_completed_total` | Checkouts exitosos |
| `business_checkout_failed_total` | Checkouts fallidos |
| `business_auth_failed_total` | Autenticaciones fallidas |
| `business_external_dollar_service_seconds` | Latencia del servicio de dólar |
| `business_external_scryfall_service_seconds` | Latencia de Scryfall |

**Variables de entorno requeridas en Railway (producción):**
```env
# Grafana Cloud OTLP (obtener de grafana.com → Connections → OpenTelemetry)
OTEL_EXPORTER_OTLP_ENDPOINT=https://otlp-gateway-prod-sa-east-1.grafana.net/otlp
OTEL_EXPORTER_OTLP_HEADERS=Authorization=Basic <base64_de_instanceId:apiKey>
OTEL_SERVICE_NAME=magicfield-backend

# Activar perfil de logging JSON en producción
SPRING_PROFILES_ACTIVE=prod
```

**Setup Grafana Cloud (una sola vez):**
1. Crear cuenta gratuita en https://grafana.com
2. Ir a Connections → Add new connection → OpenTelemetry (OTLP)
3. Copiar el endpoint y generar API key
4. Configurar las variables de entorno en Railway
5. Importar dashboard pre-hecho: ID `4701` (JVM Micrometer) y `11378` (Spring Boot)

---

### 2. Frontend — Vercel Analytics + Sentry

#### Vercel Analytics (Tráfico)

**Acceso:** https://vercel.com → tu proyecto → pestaña **Analytics**

**Lo que muestra:**
- Page views por ruta
- Visitantes únicos (día/semana/mes)
- Países, dispositivos, navegadores
- Top referrers (de dónde llega el tráfico)
- Core Web Vitals: LCP, CLS, FID, TTFB

**Activación:** Se activa automáticamente al deployar. No requiere variables de entorno. El componente `<Analytics />` y `<SpeedInsights />` ya están en `layout.tsx`.

#### Sentry (Errores y Performance)

**Acceso:** https://sentry.io → tu proyecto `magicfield-frontend`

**Lo que muestra:**
- Errores agrupados con stack trace completo
- Contexto del usuario (URL, browser, OS)
- Breadcrumbs (últimas acciones antes del error)
- Performance traces de server components
- Alertas por email cuando aparece un error nuevo

**Variables de entorno requeridas:**
```env
# En Vercel → Settings → Environment Variables
NEXT_PUBLIC_SENTRY_DSN=https://xxxxx@o123.ingest.sentry.io/456
SENTRY_DSN=https://xxxxx@o123.ingest.sentry.io/456
SENTRY_ORG=tu-org
SENTRY_PROJECT=magicfield-frontend
SENTRY_AUTH_TOKEN=sntrys_xxxxx  # Para source maps upload
```

**Setup Sentry (una sola vez):**
1. Crear cuenta en https://sentry.io (free: 5,000 errores/mes)
2. Crear proyecto → Platform: Next.js
3. Copiar el DSN
4. Crear auth token en Settings → Auth Tokens
5. Configurar las 4 variables en Vercel

---

### 3. Admin Dashboard — Métricas de Negocio

**Acceso:** App admin → pantalla Dashboard (primera pantalla al abrir)

**Endpoint:** `GET /api/admin/dashboard-stats`

**Lo que muestra:**
- Total productos, stock, valor del inventario, productos sin stock
- Órdenes y revenue de hoy y esta semana
- Estado de órdenes (pendientes/completadas/canceladas)
- Top 5 productos más vendidos

**Pull-to-refresh:** Deslizar hacia abajo para actualizar datos.

---

### Resumen de Accesos

| Herramienta | URL | Qué ves |
|---|---|---|
| Grafana Cloud | grafana.com | Métricas backend, logs, trazas |
| Vercel Analytics | vercel.com → Analytics | Tráfico del e-commerce |
| Sentry | sentry.io | Errores frontend + backend traces |
| Admin Dashboard | App móvil → Dashboard | KPIs de negocio |
| Health Check | `/actuator/health` | Estado del servicio |
| Prometheus | `/actuator/prometheus` | Métricas crudas (scrape) |

### Logging
- Configurado para DEBUG en paquetes de la aplicación
- Ajustar niveles en `application.yml`

## Despliegue

### Docker
```bash
# Build
docker build -t magicfield-backend .

# Run
docker run -p 8080:8080 magicfield-backend
```

### Producción
- Configurar variables de entorno
- Usar `hibernate.ddl-auto: validate`
- Configurar logging apropiado
- Implementar health checks
- Configurar métricas (Spring Boot Actuator)

## Notas Importantes

- Se dejó `hibernate.ddl-auto: update` para facilitar desarrollo. Cambiar a migraciones en producción.
- La verificación de tokens requiere inicializar Firebase con una service account; si no está configurada, el servidor arrancará pero la verificación fallará (se documenta con TODOs).
- No se añadieron features fuera del alcance del README principal.

---

Desarrollado para ser una base clara, explícita y fácil de explicar en entrevistas técnicas.