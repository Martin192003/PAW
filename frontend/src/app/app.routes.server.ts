import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
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
