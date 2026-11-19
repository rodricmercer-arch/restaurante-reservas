import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { esRutaSinToken, esUrlDeLaApi } from '../config/api.config';

/**
 * 401 en una ruta protegida -> limpia la sesión y va a /login con aviso (y returnUrl para volver).
 * 403 -> /403. El error se vuelve a lanzar para que cada pantalla pueda mostrar su propio mensaje.
 * Se ignoran login, registro y /api/publico/**: ahí un 401 significa "clave incorrecta", no "sesión vencida".
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      const aplica = esUrlDeLaApi(req.url) && !esRutaSinToken(req.url);

      if (aplica && error instanceof HttpErrorResponse) {
        if (error.status === 401) {
          const teniaSesion = auth.token() !== null;
          const destino = router.url;
          auth.logout();
          // Los valores null se omiten de la URL: solo hay aviso si de verdad había sesión.
          void router.navigate(['/login'], {
            queryParams: {
              aviso: teniaSesion ? 'sesion-vencida' : null,
              returnUrl: destino && !destino.startsWith('/login') ? destino : null,
            },
          });
        } else if (error.status === 403) {
          void router.navigateByUrl('/403');
        }
      }
      return throwError(() => error);
    }),
  );
};