import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';

import { AdminReservaService } from '../../../core/services/admin-reserva.service';
import { ApiError } from '../../../models/api-error.model';
import { ESTADOS_RESERVA, EstadoReserva, FiltroReservas, Reserva } from '../../../models/reserva.model';
import { AlertaApiComponent } from '../../../shared/alerta-api.component';
import { extraerApiError } from '../../../shared/api-error';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog.component';
import { rangoOrdenado } from '../../../shared/validadores';
import { MoverReservaDialogComponent } from './mover-reserva-dialog.component';

@Component({
  selector: 'app-admin-reservas',
  imports: [ReactiveFormsModule, DatePipe, AlertaApiComponent, ConfirmDialogComponent, MoverReservaDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="admin-cabecera">
      <h2>Todas las reservas</h2>
    </div>

    @if (mensaje(); as m) {
      <div class="alerta alerta-exito" role="status"><i class="fas fa-circle-check"></i> {{ m }}</div>
    }
    <app-alerta-api [error]="errorApi()" [conReintento]="true" (reintentar)="cargar()" />

    <form class="filtros" [formGroup]="filtroForm" (ngSubmit)="aplicar()" novalidate>
      <div class="campo">
        <label class="campo-label" for="f-dni">DNI del cliente</label>
        <input id="f-dni" type="text" class="campo-input" formControlName="dni" maxlength="8" inputmode="numeric" placeholder="Ej. 12345678" />
      </div>
      <div class="campo">
        <label class="campo-label" for="f-estado">Estado</label>
        <select id="f-estado" class="campo-input" formControlName="estado">
          <option value="">Todos</option>
          @for (e of estados; track e) {
            <option [value]="e">{{ e }}</option>
          }
        </select>
      </div>
      <div class="campo">
        <label class="campo-label" for="f-desde">Desde</label>
        <input id="f-desde" type="date" class="campo-input" formControlName="desde" />
      </div>
      <div class="campo">
        <label class="campo-label" for="f-hasta">Hasta</label>
        <input id="f-hasta" type="date" class="campo-input" formControlName="hasta" />
      </div>
      <div class="filtros-acciones">
        <button type="submit" class="btn-admin btn-admin-primario">Filtrar</button>
        <button type="button" class="btn-admin btn-admin-secundario" (click)="limpiar()">Limpiar</button>
      </div>
    </form>
    @if (filtroForm.hasError('rangoInvalido')) {
      <div class="alerta alerta-aviso" role="alert">La fecha "Desde" no puede ser posterior a "Hasta".</div>
    }

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando reservas…</p>
    } @else if (reservas().length > 0) {
      <div class="tabla-wrap">
        <table class="tabla-admin">
          <thead>
            <tr><th>Cliente</th><th>Mesa</th><th>Fecha</th><th>Estado</th><th>Pedido</th><th>Acciones</th></tr>
          </thead>
          <tbody>
            @for (r of reservas(); track r.id) {
              <tr>
                <td>{{ r.cliente.nombre }} ({{ r.cliente.dni }})</td>
                <td>{{ r.mesa.numero }}</td>
                <td>{{ r.fechaHora | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>
                  <span class="badge-estado" [class.pendiente]="r.estado === 'PENDIENTE'" [class.confirmada]="r.estado === 'CONFIRMADA'" [class.cancelada]="r.estado === 'CANCELADA'">
                    <span class="dot"></span> {{ r.estado }}
                  </span>
                </td>
                <td class="celda-texto">{{ pedido(r) }}</td>
                <td class="col-acciones">
                  @if (r.estado !== 'CANCELADA') {
                    <button type="button" class="btn-admin btn-admin-secundario btn-admin-sm" (click)="pedirMover(r)">Mover</button>
                    <button type="button" class="btn-admin btn-admin-peligro btn-admin-sm" (click)="pedirCancelar(r)">Cancelar</button>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else if (!errorApi()) {
      <div class="empty-state">
        <div class="icon-wrap"><i class="fas fa-calendar-xmark"></i></div>
        <h5>{{ hayFiltros() ? 'Ninguna reserva coincide con los filtros' : 'Aún no hay reservas' }}</h5>
        <p>{{ hayFiltros() ? 'Prueba con otros criterios o pulsa Limpiar.' : 'Las reservas de los clientes aparecerán aquí.' }}</p>
      </div>
    }

    <app-confirm-dialog
      [abierto]="porCancelar() !== null"
      titulo="¿Cancelar reserva?"
      [mensaje]="mensajeCancelar()"
      textoConfirmar="Sí, cancelar reserva"
      textoCancelar="No, conservarla"
      [procesando]="cancelando()"
      (confirmar)="cancelar()"
      (cancelar)="porCancelar.set(null)"
    />

    @if (reservaAMover(); as r) {
      <app-mover-reserva-dialog
        [reserva]="r"
        (movida)="alMover(r, $event)"
        (fallida)="alFallarMover($event)"
        (cerrar)="reservaAMover.set(null)"
      />
    }
  `,
})
export class AdminReservasComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly servicio = inject(AdminReservaService);

  protected readonly estados = ESTADOS_RESERVA;

  protected readonly filtroForm = this.fb.group(
    {
      dni: [''],
      estado: this.fb.control<EstadoReserva | ''>(''),
      desde: [''],
      hasta: [''],
    },
    { validators: [rangoOrdenado('desde', 'hasta')] },
  );

  /** Filtros YA aplicados (los enviados al pulsar Filtrar); las recargas tras cancelar o mover los reutilizan. */
  private readonly filtros = signal<FiltroReservas>({});
  protected readonly hayFiltros = computed(() => {
    const f = this.filtros();
    return !!f.dni || !!f.estado || !!f.desde || !!f.hasta;
  });

  protected readonly reservas = signal<Reserva[]>([]);
  protected readonly cargando = signal(true);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly errorApi = signal<ApiError | null>(null);

  protected readonly porCancelar = signal<Reserva | null>(null);
  protected readonly cancelando = signal(false);
  protected readonly reservaAMover = signal<Reserva | null>(null);

  protected readonly mensajeCancelar = computed(() => {
    const r = this.porCancelar();
    return r ? `Se cancelará la reserva #${r.id} de ${r.cliente.nombre} y la mesa ${r.mesa.numero} quedará libre.` : '';
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorApi.set(null);
    this.servicio.listar(this.filtros()).subscribe({
      next: (lista) => {
        this.reservas.set(lista);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.errorApi.set(extraerApiError(e));
        this.cargando.set(false);
      },
    });
  }

  /** Recarga sin spinner y sin borrar mensajes ni alertas (tras cancelar, mover o un conflicto). */
  private refrescar(): void {
    this.servicio.listar(this.filtros()).subscribe({
      next: (lista) => this.reservas.set(lista),
      error: (e: unknown) => this.errorApi.set(extraerApiError(e)),
    });
  }

  protected aplicar(): void {
    if (this.filtroForm.invalid) {
      this.filtroForm.markAllAsTouched();
      return;
    }
    const v = this.filtroForm.getRawValue();
    this.filtros.set({ dni: v.dni.trim(), estado: v.estado, desde: v.desde, hasta: v.hasta });
    this.mensaje.set(null);
    this.cargar();
  }

  protected limpiar(): void {
    this.filtroForm.reset();
    this.filtros.set({});
    this.mensaje.set(null);
    this.cargar();
  }

  protected pedido(r: Reserva): string {
    return r.detalles.length > 0 ? r.detalles.map((d) => d.nombre).join(', ') : '—';
  }

  // ---------- Cancelar ----------

  protected pedirCancelar(r: Reserva): void {
    this.mensaje.set(null);
    this.errorApi.set(null);
    this.porCancelar.set(r);
  }

  protected cancelar(): void {
    const r = this.porCancelar();
    if (r === null || this.cancelando()) {
      return;
    }
    this.cancelando.set(true);
    this.servicio.cancelar(r.id).subscribe({
      next: () => {
        this.cancelando.set(false);
        this.porCancelar.set(null);
        this.mensaje.set(`Reserva #${r.id} cancelada. La mesa ${r.mesa.numero} quedó libre.`);
        this.refrescar();
      },
      error: (e: unknown) => {
        this.cancelando.set(false);
        this.porCancelar.set(null);
        this.mostrarFallo(extraerApiError(e));
      },
    });
  }

  // ---------- Mover ----------

  protected pedirMover(r: Reserva): void {
    this.mensaje.set(null);
    this.errorApi.set(null);
    this.reservaAMover.set(r);
  }

  protected alMover(r: Reserva, numeroMesa: number): void {
    this.reservaAMover.set(null);
    this.errorApi.set(null);
    this.mensaje.set(`Reserva #${r.id} movida a la mesa ${numeroMesa}.`);
    this.refrescar();
  }

  protected alFallarMover(error: ApiError): void {
    this.reservaAMover.set(null);
    this.mostrarFallo({ ...error, mensaje: `No se pudo mover la reserva. ${error.mensaje}` });
  }

  /** 400, 404 y 409 (incluye la modificación simultánea) indican datos desactualizados: se avisa y se recarga la lista. */
  private mostrarFallo(error: ApiError): void {
    this.mensaje.set(null);
    if (error.estado === 400 || error.estado === 404 || error.estado === 409) {
      this.errorApi.set({ ...error, errores: [...error.errores, 'Se actualizó la lista con los datos más recientes.'] });
      this.refrescar();
    } else {
      this.errorApi.set(error);
    }
  }
}