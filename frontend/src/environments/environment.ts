/**
 * Detecta el context path del WAR dinámicamente y construye la URL base de la API.
 * En desarrollo con Angular CLI usa /api para poder resolverlo con proxy.
 * En producción infiere el context path del WAR desde la URL actual.
 */
function detectApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return '/api'; // SSR / prerender fallback
  }
  const { origin, pathname, port } = window.location;

  if (port === '8080') {
    return '/api';
  }

  const firstSegment = pathname.split('/').filter(Boolean)[0];
  return firstSegment ? `${origin}/${firstSegment}/api` : `${origin}/api`;
}

export const environment = {
  production: false,
  apiBaseUrl: detectApiBaseUrl(),
};
