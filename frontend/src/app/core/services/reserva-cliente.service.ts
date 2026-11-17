import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CrearReservaRequest, Reserva } from '../../models/reserva.model';
import { API_URL } from '../config/api.config';

/** Endpoints del CLIENTE: reserva activa, crear y cancelar. */
@Injectable({ providedIn: 'root' })
export class ReservaClienteService {
  private readonly http = inject(HttpClient);

  /** GET /api/cliente/reserva-activa: el 204 (sin reserva) llega como null. */
  reservaActiva(): Observable<Reserva | null> {
    return this.http.get<Reserva | null>(`${API_URL}/cliente/reserva-activa`);
  }

  /** POST /api/cliente/reservas. */
  crear(datos: CrearReservaRequest): Observable<Reserva> {
    return this.http.post<Reserva>(`${API_URL}/cliente/reservas`, datos);
  }

  /** POST /api/cliente/reservas/{id}/cancelar (204). */
  cancelar(id: number): Observable<void> {
    return this.http.post<void>(`${API_URL}/cliente/reservas/${id}/cancelar`, null);
  }
}