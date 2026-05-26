/**
 * Detecta el context path del WAR dinámicamente y construye la URL base de la API.
 * Funciona tanto en desarrollo (http://localhost:8080/webapp_war_exploded/)
 * como en producción (http://pawserver.../paw-2025b-06/).
 * No requiere hardcodear el nombre del contexto.
 */
function detectApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return '/api'; // SSR / prerender fallback
  }
  const { origin, pathname, port } = window.location;

  // Angular CLI dev server corre en el puerto 4200; el backend Tomcat está en 8080.
  // En este caso el context path no puede inferirse del pathname (que es una ruta Angular),
  // así que apuntamos directamente al WAR en Tomcat.
  if (port === '4200') {
    return 'http://localhost:8080/webapp_war_exploded/api';
  }

  // En producción o cuando se sirve desde el propio WAR (puerto 8080, etc.),
  // el primer segmento del pathname es el context path del WAR.
  // Ej: /webapp_war_exploded/clubs -> primer segmento = webapp_war_exploded
  // Ej: /paw-2025b-06/            -> primer segmento = paw-2025b-06
  const firstSegment = pathname.split('/').filter(Boolean)[0];
  return firstSegment ? `${origin}/${firstSegment}/api` : `${origin}/api`;
}

export const environment = {
  production: false,
  apiBaseUrl: detectApiBaseUrl(),
};
