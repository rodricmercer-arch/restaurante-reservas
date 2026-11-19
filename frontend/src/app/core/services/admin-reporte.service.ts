import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Reporte } from '../../models/reporte.model';
import { API_URL } from '../config/api.config';

/** GET /api/admin/reportes (solo ADMIN). Sin parámetros: la API no admite filtros. */
@Injectable({ providedIn: 'root' })
export class AdminReporteService {
  private readonly http = inject(HttpClient);

  obtener(): Observable<Reporte> {
    return this.http.get<Reporte>(`${API_URL}/admin/reportes`);
  }
}