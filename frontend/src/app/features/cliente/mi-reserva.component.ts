import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { PublicoService } from '../../core/services/publico.service';
import { ReservaClienteService } from '../../core/services/reserva-cliente.service';
import { ApiError } from '../../models/api-error.model';
import { Reserva } from '../../models/reserva.model';
import { extraerApiError } from '../../shared/api-error';
import { ModalComponent } from '../../shared/modal.component';
import { PlatoImagenComponent } from '../../shared/plato-imagen.component';

const ETIQUETA_CATEGORIA = { PRINCIPAL: 'Principal', POSTRE: 'Postre', BEBIDA: 'Bebida' } as const;

@Component({
  selector: 'app-mi-reserva',
  imports: [DatePipe, DecimalPipe, RouterLink, ModalComponent, PlatoImagenComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-header">
      <div class="eyebrow">Tu cuenta</div>
      <h2>Mi reserva</h2>
    </div>

    @if (errorApi(); as e) {
      <div class="alerta alerta-error" role="alert">
        <i class="fas fa-circle-exclamation"></i> {{ e.mensaje }}
        <span class="alerta-acciones"><button class="btn-reintentar" type="button" (click)="cargar()">Reintentar</button></span>
      </div>
    }

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando tu reserva…</p>
    } @else if (reserva(); as r) {
      <article class="reserva-card">
        <div class="rc-header">
          <div>
            <div class="eyebrow">Reserva #{{ r.id }}</div>
            <h3>Mesa {{ r.mesa.numero }}</h3>
          </div>
          <span class="badge-pill" [class]="'badge-pill ' + r.estado.toLowerCase()">{{ r.estado }}</span>
        </div>

        <div class="rc-body">
          <div class="rc-meta">
            <div>
              <div class="lbl">Fecha y hora</div>
              <div class="val">{{ r.fechaHora | date: 'dd/MM/yyyy HH:mm' }}</div>
            </div>
            <div>
              <div class="lbl">Capacidad</div>
              <div class="val">{{ r.mesa.capacidad }} personas</div>
            </div>
            <div>
              <div class="lbl">Cliente</div>
              <div class="val">{{ r.cliente.nombre }}</div>
            </div>
          </div>

          <div class="rc-titulo">Tu pedido</div>
          @for (d of lineas(); track d.platoId) {
            <div class="rc-pedido-line">
              <div class="rc-plato">
                <app-plato-imagen [categoria]="d.categoria" [imagen]="d.imagen" [nombre]="d.nombre" clase="dish-mini" />
                <div>
                  <span class="cat">{{ d.etiqueta }}</span>
                  {{ d.nombre }} @if (d.cantidad > 1) { <span class="texto-muted">× {{ d.cantidad }}</span> }
                </div>
              </div>
              <strong>S/ {{ d.subtotal | number: '1.2-2' }}</strong>
            </div>
          }
          <div class="rc-total">
            <span>Total</span>
            <span class="monto">S/ {{ total() | number: '1.2-2' }}</span>
          </div>

          @if (r.estado !== 'CANCELADA') {
            <button type="button" class="btn-cancelar-reserva" (click)="confirmacionAbierta.set(true)">
              <i class="fas fa-xmark"></i> Cancelar reserva
            </button>
          }
        </div>
      </article>
    } @else if (!errorApi()) {
      <div class="empty-state">
        <div class="icon-wrap"><i class="fas fa-calendar-xmark"></i></div>
        <h5>No tienes reservas activas</h5>
        <p>Elige una mesa y arma tu pedido para crear tu reserva.</p>
        <a class="btn-explore" routerLink="/mesas">Ver mesas</a>
      </div>
    }

    <app-modal [abierto]="confirmacionAbierta()" titulo="Confirmar cancelación" (cerrar)="cerrarConfirmacion()">
      <div class="modal-icono"><i class="fas fa-triangle-exclamation"></i></div>
      <h2 class="modal-titulo">¿Cancelar tu reserva?</h2>
      <p class="modal-texto">La mesa quedará libre para otros clientes. Esta acción no se puede deshacer.</p>
      <div class="modal-acciones">
        <button type="button" class="btn-modal btn-modal-primario" [disabled]="cancelando()" (click)="cancelarReserva()">
          {{ cancelando() ? 'Cancelando…' : 'Sí, cancelar reserva' }}
        </button>
        <button type="button" class="btn-modal btn-modal-secundario" [disabled]="cancelando()" (click)="cerrarConfirmacion()">
          No, conservarla
        </button>
      </div>
    </app-modal>
  `,
})
export class MiReservaComponent {
  private readonly reservas = inject(ReservaClienteService);
  private readonly publico = inject(PublicoService);
  private readonly router = inject(Router);

  protected readonly reserva = signal<Reserva | null>(null);
  protected readonly imagenes = signal<ReadonlyMap<number, string | null>>(new Map());
  protected readonly cargando = signal(true);
  protected readonly cancelando = signal(false);
  protected readonly confirmacionAbierta = signal(false);
  protected readonly errorApi = signal<ApiError | null>(null);

  /** Detalles con etiqueta, imagen (cruzada con la carta) y subtotal. */
  protected readonly lineas = computed(() =>
    (this.reserva()?.detalles ?? []).map((d) => ({
      ...d,
      etiqueta: ETIQUETA_CATEGORIA[d.categoria],
      imagen: this.imagenes().get(d.platoId) ?? null,
      subtotal: d.precio * d.cantidad,
    })),
  );
  protected readonly total = computed(() => this.lineas().reduce((suma, l) => suma + l.subtotal, 0));

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorApi.set(null);
    this.reservas.reservaActiva().subscribe({
      next: (reserva) => {
        this.reserva.set(reserva); // 204 -> null
        this.cargando.set(false);
        if (reserva !== null) {
          this.cargarImagenes();
        }
      },
      error: (e: unknown) => {
        this.errorApi.set(extraerApiError(e));
        this.cargando.set(false);
      },
    });
  }

  /** El detalle no trae imagen: se recupera de la carta. Si falla, quedan los íconos. */
  private cargarImagenes(): void {
    this.publico.carta().subscribe({
      next: (platos) => this.imagenes.set(new Map(platos.map((p) => [p.id, p.imagen] as const))),
      error: () => undefined,
    });
  }

  protected cerrarConfirmacion(): void {
    if (!this.cancelando()) {
      this.confirmacionAbierta.set(false);
    }
  }

  protected cancelarReserva(): void {
    const actual = this.reserva();
    if (actual === null || this.cancelando()) {
      return;
    }
    this.cancelando.set(true);
    this.errorApi.set(null);

    this.reservas.cancelar(actual.id).subscribe({
      next: () => {
        this.cancelando.set(false);
        this.confirmacionAbierta.set(false);
        void this.router.navigateByUrl('/mesas');
      },
      error: (e: unknown) => {
        this.cancelando.set(false);
        this.confirmacionAbierta.set(false);
        this.errorApi.set(extraerApiError(e));
      },
    });
  }
}