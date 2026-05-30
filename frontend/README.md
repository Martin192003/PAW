# Frontend

Angular SPA served from the WAR. The API base path is `/api`.

## Requisitos

- Node.js 20+ (o la version usada en el equipo)
- npm 11+

## Instalacion

```bash
npm install
```

## Desarrollo

El frontend espera la API bajo `/api`. Para desarrollo local podes:

1. Servir la SPA desde el WAR (misma origin que la API).
2. Usar `ng serve` con un proxy hacia el backend.

Ejemplo de proxy (archivo local no versionado):

```json
{
  "/api": {
	"target": "http://localhost:8080/webapp_war_exploded",
	"secure": false,
	"changeOrigin": true
  }
}
```

Luego:

```bash
ng serve --proxy-config proxy.conf.json
```

## Build

```bash
npm run build
```

## Tests

```bash
npm run test -- --watch=false --browsers=ChromeHeadless
```
