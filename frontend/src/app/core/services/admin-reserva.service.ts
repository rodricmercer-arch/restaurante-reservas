import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { FiltroReservas, MoverReservaRequest, Reserva } from '../../models/reserva.model';
import { API_URL } from '../config/api.config';

/** Reservas vistas por el ADMIN: /api/admin/reservas. */
@Injectable({ providedIn: 'root' })
export class AdminReservaService {
  private readonly http = inject(HttpClient);

  /** GET /api/admin/reservas con filtros opcionales (dni, estado, desde, hasta); los vacíos no se envían. */
  listar(filtros: FiltroReservas = {}): Observable<Reserva[]> {
    let params = new HttpParams();
    if (filtros.dni) {
      params = params.set('dni', filtros.dni);
    }
    if (filtros.estado) {
      params = params.set('estado', filtros.estado);
    }
    if (filtros.desde) {
      params = params.set('desde', filtros.desde);
    }
    if (filtros.hasta) {
      params = params.set('hasta', filtros.hasta);
    }
    return this.http.get<Reserva[]>(`${API_URL}/admin/reservas`, { params });
  }

  /** POST /api/admin/reservas/{id}/cancelar (204): 400 si no existe; 409 si ya está cancelada. */
  cancelar(id: number): Observable<void> {
    return this.http.post<void>(`${API_URL}/admin/reservas/${id}/cancelar`, null);
  }

  /** POST /api/admin/reservas/{id}/mover (204): 409 si está cancelada, misma mesa, mesa ocupada o modificación simultánea. */
  mover(id: number, nuevaMesaId: number): Observable<void> {
    const cuerpo: MoverReservaRequest = { nuevaMesaId };
    return this.http.post<void>(`${API_URL}/admin/reservas/${id}/mover`, cuerpo);
  }
}