import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ApiError } from '../models/api-error.model';

/** Alerta de error de la API: muestra `mensaje` y, si existe, la lista `errores`. Opcionalmente ofrece "Reintentar". */
@Component({
  selector: 'app-alerta-api',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (error(); as e) {
      <div class="alerta alerta-error" role="alert">
        <i class="fas fa-circle-exclamation"></i> {{ e.mensaje }}
        @if (conReintento()) {
          <span class="alerta-acciones">
            <button class="btn-reintentar" type="button" (click)="reintentar.emit()">Reintentar</button>
          </span>
        }
        @if (e.errores.length > 0) {
          <ul>
            @for (linea of e.errores; track linea) {
              <li>{{ linea }}</li>
            }
          </ul>
        }
      </div>
    }
  `,
})
export class AlertaApiComponent {
  readonly error = input<ApiError | null>(null);
  readonly conReintento = input(false);
  readonly reintentar = output<void>();
}