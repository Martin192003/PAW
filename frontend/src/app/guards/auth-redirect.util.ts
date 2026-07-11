import { Router, RouterStateSnapshot } from '@angular/router';

/**
 * Redirige a /login preservando la URL a la que el usuario intentaba
 * navegar (`returnUrl`), para poder volver ahí automáticamente después de
 * un login exitoso (ver LoginPage.onSubmit).
 *
 * Centralizado acá porque más de un guard (adminGuard, authGuard) necesita
 * el mismo comportamiento de "no hay sesión -> mandar a login y recordar
 * a dónde volver". Evita reimplementar el mismo router.navigate en cada guard.
 */
export function redirectToLogin(router: Router, state: RouterStateSnapshot, reason: string): false {
  router.navigate(['/login'], {
    queryParams: { reason, returnUrl: state.url }
  }).catch(() => undefined);

  return false;
}
