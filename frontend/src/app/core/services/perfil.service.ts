import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CambiarPasswordRequest, Perfil } from '../../models/perfil.model';
import { API_URL } from '../config/api.config';

/** Perfil del usuario autenticado (CLIENTE o ADMIN): /api/perfil. */
@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly http = inject(HttpClient);

  /** GET /api/perfil: 200; 401 si el token no es válido o el usuario ya no existe. */
  obtener(): Observable<Perfil> {
    return this.http.get<Perfil>(`${API_URL}/perfil`);
  }

  /** PUT /api/perfil/password: 204; 400 si la actual es incorrecta, la confirmación no coincide o la nueva tiene menos de 8 caracteres. */
  cambiarPassword(datos: CambiarPasswordRequest): Observable<void> {
    return this.http.put<void>(`${API_URL}/perfil/password`, datos);
  }
}