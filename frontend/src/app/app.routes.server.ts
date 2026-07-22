import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [

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
    path: 'profile/**',
    renderMode: RenderMode.Client
  },
  {
    path: 'bookings/**',
    renderMode: RenderMode.Client
  },
  {
    path: 'admin/**',
    renderMode: RenderMode.Client
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
