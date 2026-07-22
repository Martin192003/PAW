const SPA_ROUTE_SEGMENTS = new Set([
  'admin',
  'bookings',
  'clubs',
  'error',
  'forbidden',
  'forgot-password',
  'login',
  'not-found',
  'profile',
  'register',
  'reset-password',
  'verify-account',
  'verify-mail',
]);

function detectApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return '/api';
  }

  const { origin, pathname } = window.location;
  const firstSegment = pathname.split('/').filter(Boolean)[0];
  return firstSegment && !SPA_ROUTE_SEGMENTS.has(firstSegment)
    ? `${origin}/${firstSegment}/api`
    : `${origin}/api`;
}

export const environment = {
  production: true,
  apiBaseUrl: detectApiBaseUrl(),
};
