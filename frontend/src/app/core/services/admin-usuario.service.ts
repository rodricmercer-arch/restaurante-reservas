import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Usuario, UsuarioRequest } from '../../models/usuario.model';
import { API_URL } from '../config/api.config';

/** CRUD de usuarios (solo ADMIN): /api/admin/usuarios. La API no tiene GET por id; la edición usa la lista. */
@Injectable({ providedIn: 'root' })
export class AdminUsuarioService {
  private readonly http = inject(HttpClient);

  /** GET /api/admin/usuarios (ordenados por nombre). */
  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${API_URL}/admin/usuarios`);
  }

  /** POST /api/admin/usuarios: 201; 400 si hay datos inválidos; 409 si el DNI ya existe o falta/es corta la contraseña. */
  crear(datos: UsuarioRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${API_URL}/admin/usuarios`, datos);
  }

  /** PUT /api/admin/usuarios/{id}: 200; 404 si no existe; 409 si deja al sistema sin administradores o la clave es menor a 8. El DNI se exige pero no se modifica. */
  actualizar(id: number, datos: UsuarioRequest): Observable<Usuario> {
    return this.http.put<Usuario>(`${API_URL}/admin/usuarios/${id}`, datos);
  }

  /** DELETE /api/admin/usuarios/{id}: 204; 404 si no existe; 409 si es tu propia cuenta, el último administrador o tiene reservas. */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/usuarios/${id}`);
  }
}