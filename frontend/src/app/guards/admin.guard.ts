import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';

export const adminGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  const token = authState.getToken();
  if (!token) {
    router.navigate(['/login'], { queryParams: { reason: 'auth' } }).catch(() => undefined);
    return false;
  }

  const user = authState.getUser();
  if (!user) {
    router.navigate(['/login'], { queryParams: { reason: 'auth' } }).catch(() => undefined);
    return false;
  }

  const role = (user.role || '').toUpperCase();
  if (role !== 'ADMIN') {
    router.navigate(['/forbidden']).catch(() => undefined);
    return false;
  }

  return true;
};
