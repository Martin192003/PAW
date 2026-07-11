import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Rutas de autenticación: client-only. No hay contenido SEO que pre-renderizar
  // y el SSR/prerender interfiere con el manejo de formularios interactivos
  // (withEventReplay captura submit events antes de que Angular hidrate).
  {
    path: 'login',
    renderMode: RenderMode.Client
  },
  {
    path: 'register',
    renderMode: RenderMode.Client
  },
  {
    path: 'forgot-password',
    renderMode: RenderMode.Client
  },
  {
    path: 'reset-password',
    renderMode: RenderMode.Client
  },
  {
    path: 'verify-mail',
    renderMode: RenderMode.Client
  },
  {
    path: 'verify-account',
    renderMode: RenderMode.Client
  },
  {
    path: 'clubs',
    renderMode: RenderMode.Client
  },
  {
    path: 'clubs/:id',
    renderMode: RenderMode.Server
  },
  {
    path: 'clubs/handle',
    renderMode: RenderMode.Client
  },
  {
    path: 'clubs/new',
    renderMode: RenderMode.Client
  },
  {
    path: 'profile',
    renderMode: RenderMode.Client
  },
  {
    path: 'bookings/**',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/**',
    renderMode: RenderMode.Client  // ← no SSR para rutas autenticadas
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
