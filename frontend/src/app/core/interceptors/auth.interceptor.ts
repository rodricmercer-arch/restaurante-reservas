import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from '../auth/auth.service';
import { esRutaSinToken, esUrlDeLaApi } from '../config/api.config';

/** Agrega Authorization: Bearer <token> a las peticiones protegidas de la API (no a login, registro ni /api/publico/**). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).token();

  if (token === null || !esUrlDeLaApi(req.url) || esRutaSinToken(req.url)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};