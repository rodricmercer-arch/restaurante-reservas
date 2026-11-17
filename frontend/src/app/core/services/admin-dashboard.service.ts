import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Dashboard } from '../../models/dashboard.model';
import { API_URL } from '../config/api.config';

/** GET /api/admin/dashboard (solo ADMIN). */
@Injectable({ providedIn: 'root' })
export class AdminDashboardService {
  private readonly http = inject(HttpClient);

  obtener(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${API_URL}/admin/dashboard`);
  }
}