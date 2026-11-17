import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AdminDashboardService } from '../../../core/services/admin-dashboard.service';
import { ApiError } from '../../../models/api-error.model';
import { Dashboard } from '../../../models/dashboard.model';
import { extraerApiError } from '../../../shared/api-error';
import { AlertaApiComponent } from '../../../shared/alerta-api.component';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, AlertaApiComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-header">
      <div class="eyebrow">Panel de administración</div>
      @if (datos(); as d) {
        <h2>Bienvenido, {{ d.nombre }}</h2>
      } @else {
        <h2>Bienvenido</h2>
      }
    </div>

    <app-alerta-api [error]="errorApi()" [conReintento]="true" (reintentar)="cargar()" />

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando resumen…</p>
    } @else if (datos(); as d) {
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-num">{{ d.totalMesas }}</div>
          <p class="stat-lbl">Mesas registradas</p>
          <a class="btn-admin btn-admin-peligro btn-admin-sm" routerLink="/admin/mesas">Gestionar mesas</a>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ d.totalReservas }}</div>
          <p class="stat-lbl">Reservas totales</p>
          <a class="btn-admin btn-admin-peligro btn-admin-sm" routerLink="/admin/reservas">Ver reservas</a>
        </div>
        <div class="stat-card">
          <div class="stat-num"><i class="fas fa-utensils"></i></div>
          <p class="stat-lbl">Carta del restaurante</p>
          <a class="btn-admin btn-admin-peligro btn-admin-sm" routerLink="/admin/platos">Gestionar carta</a>
        </div>
      </div>
    }
  `,
})
export class DashboardComponent {
  private readonly servicio = inject(AdminDashboardService);

  protected readonly datos = signal<Dashboard | null>(null);
  protected readonly cargando = signal(true);
  protected readonly errorApi = signal<ApiError | null>(null);

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorApi.set(null);
    this.servicio.obtener().subscribe({
      next: (datos) => {
        this.datos.set(datos);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.errorApi.set(extraerApiError(e));
        this.cargando.set(false);
      },
    });
  }
}