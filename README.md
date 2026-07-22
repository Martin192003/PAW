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

El login REST no usa un endpoint dedicado y no existe `/users/me`. Las
credenciales se envian como metadata en el header `Authorization: Basic ...`
contra cualquier recurso protegido; si son validas, el backend emite un token
Bearer en headers:

```http
GET /api/bookings
Authorization: Basic base64(usuario@example.com:password)
```

```http
Paw-Auth-Token: <token>
Paw-Token-Type: Bearer
Paw-Token-Expires-In: <segundos>
```

El cliente decodifica el `userId` del claim `sub` del JWT y luego pide el
recurso canonico del usuario usando el Bearer:

```http
GET /api/users/{userId}
Authorization: Bearer <token>
```

Los endpoints protegidos subsiguientes deben usar:

```http
Authorization: Bearer <token>
```

No hay usuarios demo versionados en seeds. Para crear un usuario normal, usar `POST /api/users`. Para pruebas de rol admin, promover un usuario existente en la base:

```sql
UPDATE users SET role = 'ADMIN' WHERE user_name = 'admin';
```

Todas las passwords nuevas pasan por BCrypt. Si existe una base vieja con passwords en texto plano, esos usuarios deben migrarse a BCrypt porque el fallback plaintext fue eliminado.

## Endpoints principales

- Login: `Authorization: Basic ...` contra cualquier recurso protegido emite headers JWT frescos. No hay `/users/me`: el cliente resuelve `/api/users/{userId}` a partir del claim `sub` del JWT.
- `POST /api/auth/challenges`: solicita desafios de autenticacion como reenvio de verificacion o reset de password.
- `PUT /api/users/{userId}/verification`: verifica la cuenta con el token recibido por mail; devuelve el usuario en el body y la sesión exclusivamente en headers `Paw-*`.
- `PUT /api/users/credentials/password`: resetea password con token de mail; devuelve el usuario en el body y la sesión exclusivamente en headers `Paw-*`.
- `GET /api/users/{userId}`: usuario publico, o privado si es el propio usuario.
- `PUT /api/users/{userId}/credentials/password`: cambia password del usuario autenticado.
- `GET /api/clubs`: busqueda/listado de clubes.
- `GET /api/clubs?ownerId={userId}`: clubes por duenio/admin.
- `GET /api/clubs/{clubId}`: detalle de club.
- `GET /api/events`: eventos globales; `?clubId={clubId}` filtra la misma colección por club.
- `POST /api/events?clubId={clubId}`: crea un evento; su URI canónica es `/api/events/{eventId}`.
- `GET /api/reviews?clubId={clubId}` o `?userId={userId}`: filtra la colección canónica de reviews. Se pueden combinar ambos filtros.
- `POST /api/reviews?clubId={clubId}`: crea una review; su URI canónica es `/api/reviews/{reviewId}`.
- `GET /api/bookings?userId={userId}`: reservas del usuario autenticado.
- `GET /api/bookings?managed=true&clubId={clubId}`: reservas administradas, opcionalmente filtradas por club.
- `POST /api/bookings?clubId={clubId}`: crea una reserva; su URI canónica es `/api/bookings/{bookingId}`.
- `GET /api/bookings/{bookingId}`: detalle canonico de reserva propia o administrada, segun permisos.
- `PUT /api/bookings/{bookingId}`: confirma o cancela una reserva administrada.
- `DELETE /api/bookings/{bookingId}`: cancela una reserva propia o administrada.
- `GET|PUT|DELETE /api/bookings/{bookingId}/receipt`: recibo asociado a la reserva canónica.
- `GET|PUT|DELETE /api/events/{eventId}/photo`: imagen asociada al evento canónico.
- `GET|DELETE /api/photos/{photoId}`: lectura o borrado de una foto de galería mediante la misma URI.

## Decisiones REST/HATEOAS

- La API usa JAX-RS/Jersey bajo `/api/*`.
- Los DTOs principales exponen `links` con `rel`, `href` y `method`.
- El frontend prioriza links HATEOAS en flujos principales y conserva fallback solo donde todavia hace falta compatibilidad.
- Los endpoints de autenticacion viven bajo `/api/auth/*`; `/api/users` queda reservado para el recurso usuario.
- Las colecciones relevantes mantienen `X-Total-Count` y agregan `Link` pagination cuando aplica.
- Los assets Angular hasheados reciben `Cache-Control: public, max-age=31536000, immutable`; `index.html` e `index.csr.html` usan `no-store`, y assets estaticos no hasheados se revalidan con `no-cache`.
- Las imagenes modificables guardadas en base usan cache condicional (`Cache-Control: no-cache` + `ETag`). Los recibos privados usan `private, no-store`.
- Las colecciones REST devuelven links `rel=item`; la paginacion viaja por headers `Link` y `X-Total-Count`.

## Aclaraciones conocidas

- No versionar `target/`, `out/`, `frontend/node/`, `.env`, `application.properties`, `mail.properties`, WARs ni credenciales.
- Si hubo credenciales reales en el historial Git, rotarlas aunque ya no esten en el arbol actual.
- El frontend requiere npm para verificar build TypeScript/Angular; los tests backend se ejecutan con Maven.
