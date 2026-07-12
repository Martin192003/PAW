# Auditoria de correcciones REST, SPA y frontend

Fecha: 2026-07-11

## Objetivo

Auditar el estado actual del trabajo contra las devoluciones anteriores y los criterios vistos en la cursada, con foco en:

- API RESTful: URNs estables, recursos de dominio, representaciones, metodos HTTP, hipermedia y autenticacion como preocupacion transversal.
- Cache HTTP: cache busting, cache incondicional para assets versionados y cache condicional para recursos modificables.
- SPA: estado reconstruible desde la URL.
- Privacidad, DTOs, tests y build.

La auditoria se basa en las correcciones enviadas, capturas adjuntas de devoluciones/clase y revision del codigo actual del repositorio.

## Resumen ejecutivo

El trabajo corrigio varios puntos importantes de entregas anteriores: hay cache incondicional para assets hasheados, las imagenes dinamicas usan `ETag` con `Cache-Control: no-cache`, el mail publico del usuario fue separado mediante `PublicUserDto`, las pantallas principales de busqueda/paginacion levantan estado desde query params, el modulo `webapp` declara dependencia sobre `frontend`, y no se detectan `GlobalExceptionHandlerAdvice` ni `ErrorController` Spring MVC.

El riesgo mas relevante sigue estando en el diseno REST de autenticacion y usuarios. Verificacion de cuenta y reset de password dejaron de vivir en `/api/email-verification` o `/api/password-reset`, pero siguen expuestos como subrecursos de `/api/users` (`/users/verifications`, `/users/password-resets`). Segun el criterio docente repetido en las devoluciones, esto puede seguir siendo objetado porque verificacion, login y reset no son entidades de dominio sino flujos de autenticacion/autorizacion que deberian resolverse en el modulo de auth/Spring Security.

Tambien persiste el patron de DTOs separados de lectura/escritura (`ClubDto` vs `ClubCreateRequestDto`, `ProductDto` vs `ProductCreateRequestDto`/`ProductUpdateRequestDto`, etc.). La correccion final anterior marco esto como incorrecto porque dos representaciones distintas bajo el mismo recurso/mime type pueden divergir. El repo actual todavia usa ese enfoque.

## Criterios de la cursada aplicados

### REST

Un diseno RESTful no se evalua solamente por usar JSON y verbos HTTP. Los puntos que el profesor remarco en devoluciones anteriores son:

- La URI identifica recursos, no acciones ni versiones artificiales.
- La busqueda, filtrado, orden y paginacion son parte de la negociacion de una coleccion mediante query params, no subrecursos ad hoc.
- La paginacion debe expresarse con headers de navegacion (`Link`) y metadatos, no solo como logica del cliente.
- Las entidades deben poder descubrirse con hipervinculos (`self` y relaciones relevantes).
- La autenticacion no deberia modelarse como entidad de dominio.
- La API no debe exponer informacion sensible como mail, permisos o datos privados de otros usuarios.
- La representacion de una entidad deberia ser una sola, declarando atributos de lectura, escritura o lectoescritura, no duplicando DTOs por caso de uso.

### Cache HTTP

Los criterios repetidos en las correcciones fueron:

- Si un asset estatico tiene cache busting en el nombre, corresponde cache no condicional largo (`public, max-age=31536000, immutable`).
- Si una imagen o recurso binario puede modificarse, corresponde cache condicional (`ETag` + revalidacion), no cache no condicional largo.
- `Vary` solo debe listar headers que cambian la representacion; no debe llenarse con headers de CORS.

### SPA

Una SPA debe poder reconstruir su estado desde la URL:

- Filtros, orden, paginacion, seleccion relevante y deep links deben estar en `path`, query params o fragment.
- Refrescar la pagina, compartir la URL o volver con history no deberia llevar al estado default si la URL contenia estado.

## Matriz de correcciones

| Tema | Estado | Evidencia | Riesgo |
|---|---|---|---|
| Cache incondicional para assets hasheados | Corregido | `frontend/angular.json` usa `outputHashing: "all"` y `StaticAssetsCacheFilter` agrega `Cache-Control: public, max-age=31536000, immutable` para assets hasheados. | Bajo. Verificar en WAR real que todos los assets servidos pasen por el filtro. |
| Imagenes dinamicas con cache condicional | Corregido | `PhotoRestController`, cover, layout y event photo usan `ETag` + `CacheControl#setNoCache(true)`. | Bajo/medio. Correcto para recursos modificables; revisar que no haya otros endpoints binarios fuera de estos. |
| `Vary` con headers CORS | Corregido | Solo se detecta `Vary: Authorization` en `GET /users/{id}` cuando cambia la representacion por usuario autenticado. `CorsFilter` expone headers pero no mete `Vary`. | Bajo. `Vary: Authorization` es defendible si la representacion cambia por caller, aunque idealmente evitar representaciones variables por auth. |
| Mail publico de usuarios | Parcialmente corregido | `PublicUserDto` no expone email ni cellphone; `GET /users/{id}` devuelve `UserDto` solo si el caller es el propio usuario y `PublicUserDto` para terceros. | Medio. `UserDto` sigue teniendo email y cellphone, correcto para owner/profile, pero revisar que no aparezca en listas o links publicos. |
| Registro sin feedback | Corregido | `RegisterPage` redirige a `/verify-mail?email=...` al crear usuario y maneja errores 400/409. | Bajo. La confirmacion depende de que `verify-mail` muestre mensaje claro. |
| URL invalida de registrarse sin web context | Corregido | Angular route `register` existe y los links usan `RouterLink`/router interno. | Bajo. Verificar deploy bajo context path real. |
| SPA no recupera filtros/search desde URL | Corregido para pantallas revisadas | `ClubsPage`, `UserBookingsPage`, `ManagedClubBookingsPage` y `ClubDetailsPage` leen `queryParamMap`/fragment y actualizan query params. | Medio. Auditar pantallas restantes como perfil, activity y handler si tienen estado relevante. |
| Tests frontend superficiales | Mejorado | Hay specs con expectativas de formularios, guards, interceptors, query params, bookings, club creation y handlers. | Medio. No se ejecuto suite en esta auditoria; falta medir coverage real. |
| Dependencia `webapp` -> `frontend` | Corregido | `webapp/pom.xml` declara dependencia `frontend` y empaqueta `../frontend/dist/frontend/browser`. | Bajo. El orden Maven queda mejor garantizado por la dependencia. |
| `GlobalExceptionHandlerAdvice` y `ErrorController` Spring MVC | Corregido | No se detectan clases con esos nombres ni `@ControllerAdvice`/`@ExceptionHandler` Spring MVC en `webapp/src/main/java`. | Bajo. |
| DTOs separados lectura/escritura | Pendiente | Existen multiples `*CreateRequestDto`, `*UpdateRequestDto` y DTOs de lectura por entidad. Controladores usan, por ejemplo, `ClubCreateRequestDto` para crear y actualizar club. | Alto. Fue una correccion conceptual explicita en la final. |
| Verificacion de mail como REST resource | Parcial/Pendiente | Ahora esta bajo `/api/users/verifications`, pero sigue en `UserRestController` como recurso/subrecurso de usuarios. | Alto. El profesor remarco que la verificacion es autenticacion con OTP, no entidad de dominio. |
| Reset de password como REST resource | Parcial/Pendiente | Ahora esta bajo `/api/users/password-reset-requests` y `/api/users/password-resets`, pero sigue modelado como recurso de usuarios. | Alto. Puede seguir siendo objetado igual que la correccion anterior. |
| Autenticacion stateless | Corregido en API | `AuthRestController` emite token bearer y responses de auth usan `Cache-Control: no-store`; no se observa uso conceptual de `JSESSIONID` para API. | Bajo/medio. Confirmar en runtime que no se cree cookie de sesion por filtros Spring. |
| Hipermedia / `self` | Parcialmente corregido | `ClubDto` arma links `self`, `events`, `bookings`, `reviews`, etc.; servicios frontend priorizan links HATEOAS cuando existen. | Medio. Revisar todas las entidades: algunos endpoints globales/colecciones pueden seguir obligando a construir URLs manuales. |
| Recursos no RESTful por nombres operacionales | Parcial | Persisten `/managed-club-bookings` y `/users/{id}/profile`, que son mas de vista/caso de uso que de dominio puro. | Medio/alto segun criterio docente. Conviene redisenarlos como filtros de `/bookings` o representaciones de `/users/{id}`. |

## Hallazgos detallados

### 1. Autenticacion y verificacion siguen siendo el mayor riesgo REST

Evidencia:

- `UserRestController` esta montado en `/users`.
- La verificacion vive en `/users/verifications`.
- El reenvio vive en `/users/verification-requests`.
- El reset vive en `/users/password-reset-requests` y `/users/password-resets`.

Interpretacion:

Mover estos endpoints desde `/api/email-verification` y `/api/password-reset` hacia `/api/users/...` mejora el naming respecto de la devolucion anterior, pero no resuelve del todo el problema conceptual. La devolucion fue clara: verificacion de mail y cambio/reset de password son problemas de autenticacion, no recursos de dominio de la API publica.

Recomendacion:

- Llevar verificacion por OTP/token al componente de auth, por ejemplo como una variante de autenticacion en `/api/auth/tokens` usando credenciales `username + otp` o `token`.
- Mantener `POST /api/auth/tokens` como punto unico de emision de tokens.
- Para reset, modelar el pedido como flujo de auth, no como recurso usuario publico. Si se mantiene endpoint REST, dejar claro que es parte de `/auth` y no de `/users`.

Prioridad: alta.

### 2. DTOs duplicados siguen contradiciendo la correccion final

Evidencia:

- Existen DTOs como `ClubCreateRequestDto`, `ProductCreateRequestDto`, `ProductUpdateRequestDto`, `TableCreateRequestDto`, `TableUpdateRequestDto`, `UserCreateRequestDto`, `ReviewCreateRequestDto`, etc.
- `ClubRestController` usa `ClubCreateRequestDto` tanto para `POST /clubs` como para `PUT /clubs/{clubId}`.
- `UserRestController` usa `UserCreateRequestDto` para crear usuario y tambien para actualizar `/users/{userId}` y `/users/{userId}/profile`.

Interpretacion:

Esto mantiene el patron objetado: varias representaciones para el mismo recurso, con riesgo de que evolucionen distinto. Ademas, usar un DTO llamado `CreateRequest` para update debilita la defensa conceptual.

Recomendacion:

- Definir una representacion por recurso: `ClubDto`, `UserDto`, `ProductDto`, etc.
- Documentar en OpenAPI atributos `readOnly`, `writeOnly` o lectoescritura.
- Usar grupos de validacion si el mismo DTO requiere reglas distintas en `POST`, `PUT` o `PATCH`.
- Si por restricciones de implementacion se mantienen DTOs internos, no exponerlos como representaciones distintas en el contrato ni bajo el mismo media type.

Prioridad: alta.

### 3. Cache de assets estaticos esta alineado con el enunciado

Evidencia:

- `frontend/angular.json` tiene `outputHashing: "all"`.
- `StaticAssetsCacheFilter` detecta assets hasheados y setea `Cache-Control: public, max-age=31536000, immutable`.
- `index.html`, `index.csr.html` y `noscript.html` usan `Cache-Control: no-cache`.
- El filtro esta registrado en `web.xml`.

Interpretacion:

Esto corrige la devolucion de "cache busting sin cache no condicional". La politica es conceptualmente correcta: los archivos versionados por hash pueden cachearse por mucho tiempo; el HTML de entrada debe revalidarse.

Riesgo:

El filtro solo aplica cache largo si el nombre matchea un hash de al menos 8 caracteres antes de la extension. Si hay imagenes estaticas copiadas desde `public/assets` sin hash, no recibiran cache largo. Esto es correcto si no tienen cache busting, pero entonces hay que confirmar que Angular efectivamente hashea esos assets en build productivo o excluirlos de la promesa de cache largo.

Prioridad: media.

### 4. Imagenes dinamicas: cache condicional correcto

Evidencia:

- `/api/photos/{photoId}` genera `ETag` y `Cache-Control: no-cache`.
- `/api/clubs/{clubId}/cover` usa la misma estrategia.
- `/api/clubs/{clubId}/layout/image` usa `ETag` y revalidacion.
- `/api/clubs/{clubId}/events/{eventId}/photo` usa `ETag` y revalidacion.

Interpretacion:

Esto esta alineado con la devolucion: si la imagen puede modificarse, no corresponde `max-age` largo incondicional. `no-cache` no significa "no guardar"; significa "guardar pero revalidar antes de usar". Esa es la politica correcta para imagenes reemplazables.

Prioridad: baja.

### 5. Privacidad de usuario mejoro, pero hay que controlar salidas publicas

Evidencia:

- `PublicUserDto` no expone `email` ni `cellphone`.
- `GET /users/{id}` devuelve `UserDto` solo si el caller autenticado es el mismo usuario; para terceros devuelve `PublicUserDto`.
- La respuesta privada agrega `Cache-Control: private, no-store`, `Pragma: no-cache` y `Vary: Authorization`.

Interpretacion:

El punto "los emails no debieran ser publicos" parece corregido para `GET /users/{id}`. La solucion es defendible, aunque el profesor ya aclaro que variar una entidad segun quien la pide no es ideal. Si se mantiene, `Vary: Authorization` y `no-store` son necesarios.

Recomendacion:

- Auditar todas las respuestas que incluyan usuario: bookings, reviews, admins, clubs, receipts.
- Evitar devolver `UserDto` completo salvo en `/users/{id}` propio o `/users/{id}/profile` autenticado.
- Documentar en OpenAPI que `email`/`cellphone` son privados.

Prioridad: media.

### 6. SPA state desde URL esta corregido en las pantallas criticas revisadas

Evidencia:

- `ClubsPage` lee `route.snapshot.queryParamMap`, se suscribe a `queryParamMap` y actualiza `page`, `size`, `name`, `zone`, `genre`, `price`, `isBar`, `sort`.
- `UserBookingsPage` y `ManagedClubBookingsPage` repiten el mismo patron para filtros/paginacion.
- `ClubDetailsPage` lee `date`, `fromEvent` y fragment `dd-tables`; al cambiar fecha actualiza query params y fragment.

Interpretacion:

Esto corrige el problema conceptual grave de refrescar una busqueda y perder filtros. La mejora esta bien orientada: el estado navegacional relevante vive en URL.

Riesgo:

El uso de `replaceUrl: true` evita llenar history al filtrar. Es defendible si se busca no contaminar history, pero el profesor habia remarcado "history" como parte del estado. Si se quiere maximizar defensa, usar `replaceUrl: false` para cambios explicitos de filtros/pagina puede ser mas natural, porque Back volveria al estado anterior.

Prioridad: media.

### 7. Hipermedia mejoro, pero revisar endpoints de vista

Evidencia:

- `ClubDto` incluye `self`, `events`, `bookings`, `reviews`, `photos`, `review-photos`, `cover`, `layout`, `menu`, `tables`, `owner`, `limits`.
- `ClubService` en frontend prioriza links HATEOAS y solo usa fallback cuando todavia no tiene el recurso padre.

Interpretacion:

Esto responde a una correccion previa: "las entidades no estan hipervinculadas". Para clubes, la solucion es buena.

Riesgo:

Persisten endpoints con naming de vista/caso de uso:

- `/api/managed-club-bookings`
- `/api/managed-club-bookings/{bookingId}`
- `/api/users/{userId}/profile`

Estos nombres pueden ser vistos como recursos artificiales orientados al frontend, no como recursos de dominio. Un rediseno mas RESTful seria:

- `GET /api/bookings?managedBy={userId}` o `GET /api/bookings?clubId={id}` para administracion.
- `GET /api/users/{userId}` con representacion privada si el caller es owner/admin, evitando `/profile`.

Prioridad: media/alta.

### 8. Tests frontend mejoraron, pero falta validar cobertura real

Evidencia:

Hay tests con comportamiento real para:

- Formularios de login y registro.
- Guards de auth.
- Interceptors de auth y errores API.
- Query params de clubes.
- Creacion/edicion de clubes.
- Detalle de club y flujo de reserva.
- Bookings de usuario y admin.

Interpretacion:

La devolucion anterior decia que los tests estaban vacios o solo creaban objetos. Eso ya no parece cierto. Hay expectativas concretas sobre payloads, navegacion, errores y estado.

Riesgo:

No se ejecuto `npm test` ni se midio coverage en esta auditoria por limitacion de tiempo/herramientas. Ademas, los tests estan concentrados en ciertos flujos; falta confirmar cobertura de profile, verify-mail, change-password, activity completa, tablas/productos/eventos y i18n.

Prioridad: media.

### 9. Build multi-modulo corregido

Evidencia:

- El parent lista `frontend` como modulo.
- `webapp/pom.xml` declara dependencia al artifact `frontend`.
- El WAR incluye `../frontend/dist/frontend/browser`.

Interpretacion:

Esto corrige la observacion de que el orden de ejecucion no estaba garantizado. Maven deberia construir `frontend` antes de `webapp` por dependencia.

Riesgo:

Confirmar que el artifact `frontend` se instale correctamente como `pom` y que el plugin de frontend ejecute `npm install/build` en fase adecuada.

Prioridad: baja.

## Recomendaciones priorizadas antes de entregar

1. Redisenar verificacion y reset hacia `/api/auth/...` o integrarlo realmente a Spring Security/auth token flow. Este es el punto mas probable de nueva correccion REST conceptual.
2. Reducir o justificar DTOs duplicados. Como minimo, documentar `readOnly`/`writeOnly` en OpenAPI y evitar nombres `CreateRequestDto` en updates.
3. Revisar `/managed-club-bookings` y `/users/{id}/profile`; migrarlos a filtros/representaciones de recursos existentes si hay tiempo.
4. Ejecutar build completo y tests: `mvn clean package` desde raiz y `npm test` en `frontend`.
5. Verificar con navegador/red que assets hasheados reciban `Cache-Control: public, max-age=31536000, immutable` y `index.csr.html` reciba `no-cache`.
6. Auditar todas las respuestas con usuario para confirmar que ningun endpoint publico devuelve `email`, `cellphone`, roles/permisos sensibles o datos privados.
7. Revisar history UX: decidir si filtros/paginacion deben usar `replaceUrl: false` para que Back vuelva al filtro anterior.

## Conclusion

El proyecto esta bastante mejor alineado que las devoluciones anteriores en cache, SPA URL state, privacidad basica y build. Sin embargo, si el criterio principal del profesor sigue siendo RESTfulness estricto, la entrega todavia tiene dos focos de alto riesgo: autenticacion/verificacion modelada como recurso de usuarios y representaciones duplicadas por DTOs de lectura/escritura.

La recomendacion pragmatica es priorizar esos dos puntos antes de invertir tiempo en ajustes menores de UI. Son los que mas se parecen a las correcciones conceptuales graves que ya bajaron nota en devoluciones previas.
