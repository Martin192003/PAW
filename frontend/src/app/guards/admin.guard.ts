import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';
import { redirectToLogin } from './auth-redirect.util';

export const adminGuard: CanActivateFn = (_route, state) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  const token = authState.getToken();
  if (!token) {
    return redirectToLogin(router, state, 'auth');
  }

  const user = authState.getUser();
  if (!user) {
    return redirectToLogin(router, state, 'auth');
  }

  const role = (user.role || '').toUpperCase();
  if (role !== 'ADMIN') {
    router.navigate(['/forbidden']).catch(() => undefined);
    return false;
  }

  return true;
};
