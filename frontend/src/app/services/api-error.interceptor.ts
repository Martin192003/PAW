import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { isApiUrl } from '../utils/api-url.util';
import { AuthStateService } from './auth-state.service';
import { ApiErrorService } from './api-error.service';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authState = inject(AuthStateService);
  const apiErrors = inject(ApiErrorService);
  const apiBaseUrl = environment.apiBaseUrl;

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!isApiUrl(req.url, apiBaseUrl)) {
        return throwError(() => error);
      }

      const status = error.status;
      if (status === 401) {
        authState.clearSession();
        router.navigate(['/login'], { queryParams: { reason: 'expired' } }).catch(() => undefined);
      } else if (status === 403) {
        router.navigate(['/forbidden']).catch(() => undefined);
      } else if (status === 404) {
        router.navigate(['/not-found']).catch(() => undefined);
      } else if (status === 409 || status === 415 || status >= 500) {
        apiErrors.setError({
          status,
          message: extractErrorMessage(error) ?? defaultStatusMessage(status),
        });
        router.navigate(['/error']).catch(() => undefined);
      }

      return throwError(() => error);
    })
  );
};

function extractErrorMessage(error: HttpErrorResponse): string | null {
  const payload = error.error;
  if (payload && typeof payload === 'object' && 'message' in payload) {
    const message = (payload as { message?: unknown }).message;
    return typeof message === 'string' ? message : null;
  }
  if (typeof payload === 'string' && payload.trim()) {
    return payload.trim();
  }
  return null;
}

function defaultStatusMessage(status: number): string {
  switch (status) {
    case 404:
      return 'Recurso no encontrado.';
    case 409:
      return 'Conflicto al procesar la solicitud.';
    case 415:
      return 'Formato de contenido no soportado.';
    default:
      return 'Ocurrio un error inesperado.';
  }
}
