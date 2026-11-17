import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Mesa, MesaRequest } from '../../models/mesa.model';
import { API_URL } from '../config/api.config';

/** CRUD de mesas (solo ADMIN): /api/admin/mesas. La API no tiene GET por id; la edición usa la lista. */
@Injectable({ providedIn: 'root' })
export class AdminMesaService {
  private readonly http = inject(HttpClient);

  /** GET /api/admin/mesas (ordenadas por número). */
  listar(): Observable<Mesa[]> {
    return this.http.get<Mesa[]>(`${API_URL}/admin/mesas`);
  }

  /** POST /api/admin/mesas: 201; 409 si el número ya existe. */
  crear(datos: MesaRequest): Observable<Mesa> {
    return this.http.post<Mesa>(`${API_URL}/admin/mesas`, datos);
  }

  /** PUT /api/admin/mesas/{id}: 200; 404 si no existe; 409 si el número ya existe. */
  actualizar(id: number, datos: MesaRequest): Observable<Mesa> {
    return this.http.put<Mesa>(`${API_URL}/admin/mesas/${id}`, datos);
  }

  /** DELETE /api/admin/mesas/{id}: 204; 404 si no existe; 409 si tiene reservas. */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/admin/mesas/${id}`);
  }
}