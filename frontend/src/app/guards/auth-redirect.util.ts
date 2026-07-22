import { Router, RouterStateSnapshot } from '@angular/router';


export function redirectToLogin(router: Router, state: RouterStateSnapshot, reason: string): false {
  router.navigate(['/login'], {
    queryParams: { reason, returnUrl: state.url }
  }).catch(() => undefined);

  return false;
}
