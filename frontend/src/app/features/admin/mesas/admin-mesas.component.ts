import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AdminMesaService } from '../../../core/services/admin-mesa.service';
import { ApiError } from '../../../models/api-error.model';
import { Mesa } from '../../../models/mesa.model';
import { AlertaApiComponent } from '../../../shared/alerta-api.component';
import { extraerApiError } from '../../../shared/api-error';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog.component';
import { leerMensajeFlash } from '../../../shared/flash';

@Component({
  selector: 'app-admin-mesas',
  imports: [RouterLink, AlertaApiComponent, ConfirmDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="admin-cabecera">
      <h2>Mesas</h2>
      <a class="btn-admin btn-admin-primario" routerLink="/admin/mesas/nueva">
        <i class="fas fa-plus"></i> Nueva mesa
      </a>
    </div>

    @if (mensaje(); as m) {
      <div class="alerta alerta-exito" role="status"><i class="fas fa-circle-check"></i> {{ m }}</div>
    }
    <app-alerta-api [error]="errorApi()" [conReintento]="true" (reintentar)="cargar()" />

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando mesas…</p>
    } @else if (mesas().length > 0) {
      <div class="tabla-wrap">
        <table class="tabla-admin">
          <thead>
            <tr><th>N.º</th><th>Capacidad</th><th>Estado</th><th></th></tr>
          </thead>
          <tbody>
            @for (m of mesas(); track m.id) {
              <tr>
                <td>{{ m.numero }}</td>
                <td>{{ m.capacidad }} personas</td>
                <td>
                  <span class="badge-estado" [class.libre]="m.estado === 'LIBRE'" [class.reservada]="m.estado === 'RESERVADA'">
                    <span class="dot"></span> {{ m.estado }}
                  </span>
                </td>
                <td class="col-acciones">
                  <a class="btn-admin btn-admin-secundario btn-admin-sm" [routerLink]="['/admin/mesas', m.id, 'editar']">Editar</a>
                  <button type="button" class="btn-admin btn-admin-peligro btn-admin-sm" (click)="pedirEliminar(m)">Eliminar</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else if (!errorApi()) {
      <div class="empty-state">
        <div class="icon-wrap"><i class="fas fa-chair"></i></div>
        <h5>Aún no hay mesas</h5>
        <p>Crea la primera mesa para que los clientes puedan reservar.</p>
      </div>
    }

    <app-confirm-dialog
      [abierto]="porEliminar() !== null"
      titulo="¿Eliminar mesa?"
      [mensaje]="mensajeEliminar()"
      textoConfirmar="Sí, eliminar"
      textoCancelar="No, conservarla"
      [procesando]="eliminando()"
      (confirmar)="eliminar()"
      (cancelar)="porEliminar.set(null)"
    />
  `,
})
export class AdminMesasComponent {
  private readonly servicio = inject(AdminMesaService);

  protected readonly mesas = signal<Mesa[]>([]);
  protected readonly cargando = signal(true);
  protected readonly eliminando = signal(false);
  protected readonly porEliminar = signal<Mesa | null>(null);
  protected readonly mensaje = signal<string | null>(leerMensajeFlash());
  protected readonly errorApi = signal<ApiError | null>(null);

  protected readonly mensajeEliminar = computed(() => {
    const mesa = this.porEliminar();
    return mesa ? `Se eliminará la mesa ${mesa.numero}. Esta acción no se puede deshacer.` : '';
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorApi.set(null);
    this.servicio.listar().subscribe({
      next: (lista) => {
        this.mesas.set(lista);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.errorApi.set(extraerApiError(e));
        this.cargando.set(false);
      },
    });
  }

  /** Recarga sin spinner ni limpiar mensajes (después de eliminar). */
  private refrescar(): void {
    this.servicio.listar().subscribe({
      next: (lista) => this.mesas.set(lista),
      error: (e: unknown) => this.errorApi.set(extraerApiError(e)),
    });
  }

  protected pedirEliminar(mesa: Mesa): void {
    this.mensaje.set(null);
    this.errorApi.set(null);
    this.porEliminar.set(mesa);
  }

  protected eliminar(): void {
    const mesa = this.porEliminar();
    if (mesa === null || this.eliminando()) {
      return;
    }
    this.eliminando.set(true);
    this.servicio.eliminar(mesa.id).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.porEliminar.set(null);
        this.mensaje.set(`Mesa ${mesa.numero} eliminada.`);
        this.refrescar();
      },
      error: (e: unknown) => {
        const error = extraerApiError(e);
        this.eliminando.set(false);
        this.porEliminar.set(null);
        this.errorApi.set(error); // 409: mesa con reservas
        if (error.estado === 404) {
          this.refrescar(); // ya no existía: la lista se actualiza sola
        }
      },
    });
  }
}