import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { Rol } from '../../models/auth.model';

/** Exige sesión y que el rol sea alguno de los indicados: canActivate: [roleGuard(['CLIENTE'])]. Sin sesión -> /login; rol incorrecto -> /403. */
export const roleGuard =
  (roles: Rol[]): CanActivateFn =>
  (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isLoggedIn()) {
      auth.logout();
      return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    return auth.hasRole(...roles) ? true : router.createUrlTree(['/403']);
  };