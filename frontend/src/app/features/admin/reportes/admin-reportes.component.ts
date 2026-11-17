import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';

import { AdminReporteService } from '../../../core/services/admin-reporte.service';
import { ApiError } from '../../../models/api-error.model';
import { Reporte } from '../../../models/reporte.model';
import { AlertaApiComponent } from '../../../shared/alerta-api.component';
import { extraerApiError } from '../../../shared/api-error';

interface FilaReporte {
  etiqueta: string;
  valor: number;
  porcentaje: number;
}

interface TablaReporte {
  titulo: string;
  columna: string;
  filas: FilaReporte[];
}

/** Mismo cálculo que ReporteDTO.porcentaje: entero (división truncada) del valor dentro de su grupo. */
function porcentaje(valor: number, total: number): number {
  return total === 0 ? 0 : Math.floor((valor * 100) / total);
}

function filasDeMapa(mapa: Record<string, number>): FilaReporte[] {
  const entradas = Object.entries(mapa);
  const total = entradas.reduce((suma, [, valor]) => suma + valor, 0);
  return entradas.map(([etiqueta, valor]) => ({ etiqueta, valor, porcentaje: porcentaje(valor, total) }));
}

/** Réplica de /reportes (Thymeleaf): sin filtros, porque ni la plantilla ni GET /api/admin/reportes los tienen. Barras con HTML/CSS. */
@Component({
  selector: 'app-admin-reportes',
  imports: [AlertaApiComponent, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="admin-cabecera">
      <h2>Reportes</h2>
    </div>

    <app-alerta-api [error]="errorApi()" [conReintento]="true" (reintentar)="cargar()" />

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando reporte…</p>
    } @else if (datos(); as d) {
      <div class="reporte-ingreso">
        <div class="lbl">Ingreso estimado</div>
        <div class="reporte-monto">S/ {{ d.ingresoEstimado | number: '1.2-2' : 'en-US' }}</div>
        <p class="reporte-nota">Suma de cantidad × precio de los platos pedidos, sin contar reservas canceladas.</p>
      </div>

      <div class="reporte-grid">
        @for (t of tablas(); track t.titulo) {
          <div>
            <h5 class="reporte-titulo">{{ t.titulo }}</h5>
            <div class="tabla-wrap">
              <table class="tabla-admin">
                <thead>
                  <tr><th>{{ t.columna }}</th><th>Cant.</th><th>Proporción</th></tr>
                </thead>
                <tbody>
                  @for (f of t.filas; track f.etiqueta) {
                    <tr>
                      <td>{{ f.etiqueta }}</td>
                      <td>{{ f.valor }}</td>
                      <td>
                        <div class="celda-barra">
                          <div class="barra" role="progressbar" aria-valuemin="0" aria-valuemax="100" [attr.aria-valuenow]="f.porcentaje">
                            <div class="barra-relleno" [style.width.%]="f.porcentaje"></div>
                          </div>
                          <span class="pct">{{ f.porcentaje }}%</span>
                        </div>
                      </td>
                    </tr>
                  } @empty {
                    <tr><td colspan="3" class="sin-datos">Sin datos</td></tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        }
      </div>

      <h5 class="reporte-titulo">Top 5 platos más reservados</h5>
      <div class="tabla-wrap">
        <table class="tabla-admin">
          <thead>
            <tr><th>#</th><th>Plato</th><th>Unidades</th><th>Frente al más pedido</th></tr>
          </thead>
          <tbody>
            @for (f of topFilas(); track $index; let i = $index) {
              <tr>
                <td>{{ i + 1 }}</td>
                <td>{{ f.etiqueta }}</td>
                <td>{{ f.valor }}</td>
                <td>
                  <div class="celda-barra">
                    <div class="barra" role="progressbar" aria-valuemin="0" aria-valuemax="100" [attr.aria-valuenow]="f.porcentaje">
                      <div class="barra-relleno" [style.width.%]="f.porcentaje"></div>
                    </div>
                    <span class="pct">{{ f.porcentaje }}%</span>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="4" class="sin-datos">Aún no hay reservas con platos.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class AdminReportesComponent {
  private readonly servicio = inject(AdminReporteService);

  protected readonly datos = signal<Reporte | null>(null);
  protected readonly cargando = signal(true);
  protected readonly errorApi = signal<ApiError | null>(null);

  protected readonly tablas = computed<TablaReporte[]>(() => {
    const d = this.datos();
    if (d === null) {
      return [];
    }
    return [
      { titulo: 'Reservas por estado', columna: 'Estado', filas: filasDeMapa(d.reservasPorEstado) },
      { titulo: 'Mesas por estado', columna: 'Estado', filas: filasDeMapa(d.mesasPorEstado) },
      { titulo: 'Platos por categoría', columna: 'Categoría', filas: filasDeMapa(d.platosPorCategoria) },
    ];
  });

  /** Mismo cálculo que ReporteDTO.porcentajeTop: cada plato frente al primero del ranking. */
  protected readonly topFilas = computed<FilaReporte[]>(() => {
    const top = this.datos()?.topPlatos ?? [];
    const max = top.length > 0 ? top[0].unidades : 0;
    return top.map((p) => ({ etiqueta: p.nombre, valor: p.unidades, porcentaje: porcentaje(p.unidades, max) }));
  });

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