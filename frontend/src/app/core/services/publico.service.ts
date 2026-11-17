import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Mesa } from '../../models/mesa.model';
import { CategoriaPlato, Plato } from '../../models/plato.model';
import { API_URL } from '../config/api.config';

/** Endpoints públicos (sin token): GET /api/publico/carta y GET /api/publico/mesas. */
@Injectable({ providedIn: 'root' })
export class PublicoService {
  private readonly http = inject(HttpClient);

  carta(categoria?: CategoriaPlato): Observable<Plato[]> {
    const params = categoria ? new HttpParams().set('categoria', categoria) : undefined;
    return this.http.get<Plato[]>(`${API_URL}/publico/carta`, { params });
  }

  mesas(): Observable<Mesa[]> {
    return this.http.get<Mesa[]>(`${API_URL}/publico/mesas`);
  }
}