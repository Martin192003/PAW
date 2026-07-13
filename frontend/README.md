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
2. Usar `ng serve` con el proxy versionado hacia el backend.

Luego:

```bash
npm start
```

## Build

```bash
npm run build
```

## Tests

```bash
npm run test -- --watch=false --browsers=ChromeHeadless
```
