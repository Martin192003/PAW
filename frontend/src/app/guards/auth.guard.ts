import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';
import { redirectToLogin } from './auth-redirect.util';

/**
 * Protege rutas que requieren sesión iniciada, sin exigir ningún rol
 * particular (a diferencia de adminGuard). Pensado para flujos como
 * "crear club", donde cualquier usuario autenticado puede continuar.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  const token = authState.getToken();
  const user = authState.getUser();
  if (!token || !user) {
    return redirectToLogin(router, state, 'auth');
  }

  return true;
};
