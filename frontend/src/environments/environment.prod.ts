function detectApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return '/api';
  }

  const { origin, pathname } = window.location;
  const firstSegment = pathname.split('/').filter(Boolean)[0];
  return firstSegment ? `${origin}/${firstSegment}/api` : `${origin}/api`;
}

export const environment = {
  production: true,
  apiBaseUrl: detectApiBaseUrl(),
};
