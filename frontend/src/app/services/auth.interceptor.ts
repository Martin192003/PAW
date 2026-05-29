import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStateService } from './auth-state.service';
import { environment } from '../../environments/environment';
import { isApiUrl } from '../utils/api-url.util';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authState = inject(AuthStateService);
  const apiBaseUrl = environment.apiBaseUrl;

  if (!isApiUrl(req.url, apiBaseUrl)) {
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

  if (!token) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    })
  );
};
