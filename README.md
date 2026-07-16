# PAW 2025b Grupo 06

Aplicacion web para gestion y reserva de boliches/bares. El backend esta implementado con Java, Spring, Jersey/JAX-RS, Hibernate/JPA y PostgreSQL. El frontend es una SPA Angular integrada al WAR final.

## Modulos

- `model`: entidades y DTOs compartidos.
- `interfaces`: contratos de servicios y DAOs.
- `persistence`: DAOs Hibernate/JPA y schema SQL.
- `service`: logica de negocio, mail y validaciones.
- `webapp`: configuracion web, seguridad, REST API y empaquetado WAR.
- `frontend`: SPA Angular.

## Requisitos

- Java 21.
- Maven 3.9+.
- PostgreSQL.
- Node/npm para el build completo del frontend. El build Maven instala Node `v24.11.1` y npm `11.6.4` mediante `frontend-maven-plugin`.
- Servlet container compatible con WAR, por ejemplo Tomcat.

## Configuracion local

El archivo real `webapp/src/main/resources/application.properties` esta ignorado por Git porque contiene datos locales. Para desarrollo, copiar:

```bash
cp webapp/src/main/resources/application.properties.example webapp/src/main/resources/application.properties
```

Editar los valores `local.db.*`, `jwt.secret`, `security.rememberme.key`, `local.base.url` y `app.base.url` segun el entorno.

La configuracion SMTP puede quedar deshabilitada en desarrollo con `MAIL_OPTIONAL=true`. Si se necesitan mails reales, definir `SMTP_USER` y `SMTP_PASS` en `application.properties` local o en `service/src/main/resources/mail.properties`, que tambien esta ignorado. Nunca commitear credenciales reales.

## Base de datos

Crear una base PostgreSQL y configurar:

```properties
local.db.host=localhost
local.db.port=5432
local.db.name=paw
local.db.user=postgres
local.db.pass=postgres
```

El schema esta en:

```text
persistence/src/main/resources/db/schema.sql
```

Para inicializar automaticamente el schema en local, usar:

```properties
local.schema.init=true
```

En entornos de entrega o produccion conviene aplicar el schema de forma controlada y dejar `local.schema.init=false`.

## Tests

Ejecutar todos los tests backend:

```bash
mvn test
```

Ejecutar solo el bloque necesario para `webapp` y sus dependencias:

```bash
mvn -pl webapp -am test
```

El modulo `webapp` incluye tests unitarios para seguridad REST, Basic Auth, links HATEOAS, paginacion y cache de assets.

## Build

Build completo con frontend y WAR:

```bash
mvn clean package
```

El WAR queda en:

```text
webapp/target/webapp.war
```

El build completo ejecuta `npm install` y `npm run build` dentro del modulo `frontend`.

## Frontend en desarrollo

Desde `frontend`:

```bash
npm install
npm start
```

El frontend usa Angular y consume la API bajo `/api`.

## Deploy del WAR

1. Generar el WAR con `mvn clean package`.
2. Copiar `webapp/target/webapp.war` al directorio `webapps` del servlet container.
3. Asegurar que `application.properties` local este disponible en el classpath del WAR o empaquetado antes del build.
4. Levantar el container y acceder al contexto desplegado, por ejemplo:

```text
http://localhost:8080/webapp
```

La API queda bajo:

```text
http://localhost:8080/webapp/api
```

## Autenticacion y usuarios

El login REST usa:

```http
POST /api/auth/tokens
Content-Type: application/json

{
  "email": "usuario@example.com",
  "password": "password"
}
```

La respuesta devuelve un token Bearer. Los endpoints protegidos deben usar:

```http
Authorization: Bearer <token>
```

Basic Auth esta limitado al endpoint de creacion de token y no se acepta como estrategia global para el resto de `/api`.

No hay usuarios demo versionados en seeds. Para crear un usuario normal, usar `POST /api/users`. Para pruebas de rol admin, promover un usuario existente en la base:

```sql
UPDATE users SET role = 'ADMIN' WHERE user_name = 'admin';
```

Todas las passwords nuevas pasan por BCrypt. Si existe una base vieja con passwords en texto plano, esos usuarios deben migrarse a BCrypt porque el fallback plaintext fue eliminado.

## Endpoints principales

- `POST /api/auth/tokens`: crea token Bearer con grants `password`, `email_verification` o `password_reset`.
- `POST /api/auth/challenges`: solicita desafios de autenticacion como reenvio de verificacion o reset de password.
- `GET /api/users/{userId}`: usuario publico, o privado si es el propio usuario.
- `PUT /api/users/{userId}/credentials/password`: cambia password del usuario autenticado.
- `GET /api/clubs`: busqueda/listado de clubes.
- `GET /api/clubs?ownerId={userId}`: clubes por duenio/admin.
- `GET /api/clubs/{clubId}`: detalle de club.
- `GET /api/events`: eventos globales.
- `GET /api/clubs/{clubId}/events`: eventos de un club.
- `GET /api/clubs/{clubId}/reviews`: reviews paginadas.
- `GET /api/users/{userId}/bookings`: reservas del usuario autenticado.
- `GET /api/managed-bookings`: reservas administradas por el usuario admin.
- `GET /api/clubs/{clubId}/bookings`: reservas administradas de un club especifico.
- `GET /api/bookings/{bookingId}`: detalle canonico de reserva propia o administrada, segun permisos.
- `PUT /api/bookings/{bookingId}`: confirma o cancela una reserva administrada.
- `DELETE /api/bookings/{bookingId}`: cancela una reserva propia o administrada.

## Decisiones REST/HATEOAS

- La API usa JAX-RS/Jersey bajo `/api/*`.
- Los DTOs principales exponen `links` con `rel`, `href` y `method`.
- El frontend prioriza links HATEOAS en flujos principales y conserva fallback solo donde todavia hace falta compatibilidad.
- Los endpoints de autenticacion viven bajo `/api/auth/*`; `/api/users` queda reservado para el recurso usuario.
- Las colecciones relevantes mantienen `X-Total-Count` y agregan `Link` pagination cuando aplica.
- Los assets Angular hasheados reciben `Cache-Control: public, max-age=31536000, immutable`; `index.html` e `index.csr.html` se revalidan con `no-cache`.

## Aclaraciones conocidas

- No versionar `target/`, `out/`, `frontend/node/`, `.env`, `application.properties`, `mail.properties`, WARs ni credenciales.
- Si hubo credenciales reales en el historial Git, rotarlas aunque ya no esten en el arbol actual.
- El frontend requiere npm para verificar build TypeScript/Angular; los tests backend se ejecutan con Maven.
