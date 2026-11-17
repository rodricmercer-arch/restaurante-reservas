import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { FiltroPlatos, Plato, PlatoRequest } from '../../models/plato.model';
import { API_URL } from '../config/api.config';

/** /api/platos: lectura para CLIENTE y ADMIN; crear, editar y eliminar solo ADMIN. */
@Injectable({ providedIn: 'root' })
export class PlatoService {
  private readonly http = inject(HttpClient);

  /** GET /api/platos con filtros opcionales: los vacíos no se envían (sin filtros la API devuelve todos). */
  listar(filtros: FiltroPlatos = {}): Observable<Plato[]> {
    let params = new HttpParams();
    if (filtros.texto) {
      params = params.set('texto', filtros.texto);
    }
    if (filtros.categoria) {
      params = params.set('categoria', filtros.categoria);
    }
    if (filtros.precioMin !== undefined && filtros.precioMin !== null) {
      params = params.set('precioMin', String(filtros.precioMin));
    }
    if (filtros.precioMax !== undefined && filtros.precioMax !== null) {
      params = params.set('precioMax', String(filtros.precioMax));
    }
    return this.http.get<Plato[]>(`${API_URL}/platos`, { params });
  }

  /** GET /api/platos/{id}: 404 si no existe. */
  obtener(id: number): Observable<Plato> {
    return this.http.get<Plato>(`${API_URL}/platos/${id}`);
  }

  /** POST /api/platos: 201; 400 validación; 409 nombre duplicado. */
  crear(datos: PlatoRequest): Observable<Plato> {
    return this.http.post<Plato>(`${API_URL}/platos`, datos);
  }

  /** PUT /api/platos/{id}: 200; 404 si no existe; 409 nombre duplicado. */
  actualizar(id: number, datos: PlatoRequest): Observable<Plato> {
    return this.http.put<Plato>(`${API_URL}/platos/${id}`, datos);
  }

  /** DELETE /api/platos/{id}: 204; 404 si no existe; 409 si el plato fue usado en reservas. */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/platos/${id}`);
  }
}