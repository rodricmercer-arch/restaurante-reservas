import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';

/** Exige sesión vigente; si no, manda a /login conservando la ruta pedida en returnUrl. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }
  auth.logout(); // limpia una sesión vencida que siguiera guardada
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};