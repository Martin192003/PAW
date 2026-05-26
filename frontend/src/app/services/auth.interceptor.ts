import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthStateService } from './auth-state.service';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  // Si la request ya tiene Authorization (p.ej. Basic Auth del login), dejar pasar sin tocar.
  // El componente de login maneja sus propios errores 401/403; no redirigir aquí.
  if (req.headers.has('Authorization')) {
    return next(req);
  }

  // Solo agregar Bearer token a requests hacia la API, no a assets ni i18n
  const isApiRequest = req.url.startsWith(environment.apiBaseUrl) || req.url.startsWith('/api');

  if (!isApiRequest) {
    return next(req);
  }

  const rawToken = authState.getToken();
  let token = rawToken?.trim() ?? '';

  if (token.startsWith('Bearer ')) {
    token = token.substring(7).trim();
  }

  if (token.startsWith('"') && token.endsWith('"') && token.length > 1) {
    token = token.slice(1, -1).trim();
  }

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => handleError(error, authState, router))
  );
};

function handleError(
  error: HttpErrorResponse,
  authState: AuthStateService,
  router: Router
) {
  if (error.status === 401) {
    // Token expirado o credenciales inválidas: limpiar sesión y redirigir al login
    authState.logout();
    router.navigate(['/login']);
  } else if (error.status === 403) {
    // Autenticado pero sin permisos suficientes
    router.navigate(['/forbidden']);
  }
  return throwError(() => error);
}
