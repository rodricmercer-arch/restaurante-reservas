import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { urlRecurso } from '../../core/config/recursos';
import { PublicoService } from '../../core/services/publico.service';
import { ReservaClienteService } from '../../core/services/reserva-cliente.service';
import { EstadoMesa, Mesa } from '../../models/mesa.model';
import { Reserva } from '../../models/reserva.model';
import { extraerApiError } from '../../shared/api-error';
import { LoginRequeridoComponent } from '../../shared/login-requerido.component';

type TipoBoton = 'reservable' | 'soloClientes' | 'conReserva' | 'noDisponible';

const DEGRADADO_MESA = 'linear-gradient(to top, rgba(36, 24, 18, 0.55), rgba(36, 24, 18, 0.05))';

@Component({
  selector: 'app-mesas',
  imports: [RouterLink, LoginRequeridoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-login-requerido [abierto]="modalAbierto()" [returnUrl]="returnUrl()" (cerrar)="modalAbierto.set(false)" />

    <section class="hero-section" [style.background-image]="heroFondo">
      <div>
        <div class="hero-eyebrow">Reserva de mesas</div>
        <h1 class="hero-title">Elige tu mesa</h1>
        <p class="hero-subtitle">Selecciona la mesa perfecta para tu velada</p>
      </div>
      <div class="hero-stats">
        <div class="hero-stat libres"><span class="num">{{ libres() }}</span><span class="lbl">Libres</span></div>
        <div class="hero-stat reservadas"><span class="num">{{ reservadas() }}</span><span class="lbl">Reservadas</span></div>
        <div class="hero-stat"><span class="num">{{ mesas().length }}</span><span class="lbl">Total</span></div>
      </div>
    </section>

    @if (reservaActiva(); as activa) {
      <div class="alert-reserva-activa">
        <i class="fas fa-circle-info"></i>
        <div>
          Ya tienes una reserva activa en la <strong>Mesa {{ activa.mesa.numero }}</strong>.
          Ve a <a class="enlace-acento" routerLink="/cliente/mi-reserva">Mi reserva</a> si deseas cancelarla antes de reservar otra.
        </div>
      </div>
    }

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando mesas…</p>
    } @else if (error()) {
      <div class="alerta alerta-error" role="alert">
        {{ error() }}
        <span class="alerta-acciones"><button class="btn-reintentar" type="button" (click)="cargar()">Reintentar</button></span>
      </div>
    } @else {
      <div class="filter-tabs">
        <button type="button" class="filter-tab" [class.active]="filtro() === 'todas'" (click)="filtro.set('todas')">Todas</button>
        <button type="button" class="filter-tab" [class.active]="filtro() === 'LIBRE'" (click)="filtro.set('LIBRE')">
          <span class="dot" style="background: var(--green)"></span>Libres
        </button>
        <button type="button" class="filter-tab" [class.active]="filtro() === 'RESERVADA'" (click)="filtro.set('RESERVADA')">
          <span class="dot" style="background: var(--accent)"></span>Reservadas
        </button>
      </div>

      <div class="grid-mesas">
        @for (mesa of visibles(); track mesa.id) {
          <article class="card-mesa">
            <div class="mesa-img" [style.background-image]="fondoMesa(mesa)">
              <div class="mesa-top">
                <span class="badge-estado" [class.libre]="mesa.estado === 'LIBRE'" [class.reservada]="mesa.estado !== 'LIBRE'">
                  <span class="dot"></span>{{ mesa.estado === 'LIBRE' ? 'Libre' : 'Reservada' }}
                </span>
              </div>
            </div>
            <div class="mesa-cuerpo">
              <div class="mesa-numero">Mesa {{ mesa.numero }}</div>
              <div class="mesa-cap"><i class="fas fa-user-group"></i> {{ mesa.capacidad }} personas</div>
              <div class="mesa-dots">
                @for (punto of puntos(mesa.capacidad); track punto) {
                  <span></span>
                }
              </div>

              @switch (tipoBoton(mesa)) {
                @case ('reservable') {
                  <button type="button" class="btn-reservar" (click)="reservar(mesa)">Reservar mesa</button>
                }
                @case ('soloClientes') {
                  <button type="button" class="btn-nodisponible" disabled>Solo clientes</button>
                }
                @case ('conReserva') {
                  <button type="button" class="btn-nodisponible" disabled>Ya tienes una reserva</button>
                }
                @default {
                  <button type="button" class="btn-nodisponible" disabled>No disponible</button>
                }
              }
            </div>
          </article>
        }
      </div>
      @if (visibles().length === 0) {
        <p class="estado-carga">No hay mesas para mostrar.</p>
      }
    }
  `,
})
export class MesasComponent {
  private readonly publico = inject(PublicoService);
  private readonly reservas = inject(ReservaClienteService);
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);

  protected readonly mesas = signal<Mesa[]>([]);
  protected readonly reservaActiva = signal<Reserva | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filtro = signal<'todas' | EstadoMesa>('todas');
  protected readonly modalAbierto = signal(false);
  protected readonly returnUrl = signal('/mesas');

  protected readonly libres = computed(() => this.mesas().filter((m) => m.estado === 'LIBRE').length);
  protected readonly reservadas = computed(() => this.mesas().filter((m) => m.estado === 'RESERVADA').length);
  protected readonly visibles = computed(() => {
    const filtro = this.filtro();
    return filtro === 'todas' ? this.mesas() : this.mesas().filter((m) => m.estado === filtro);
  });

  /** Degradado + foto del servidor + foto remota de respaldo (como el hero de Thymeleaf). */
  protected readonly heroFondo =
    `linear-gradient(120deg, rgba(36, 24, 18, 0.88), rgba(36, 24, 18, 0.45)), ` +
    `url('${urlRecurso('img/hero.jpg')}'), ` +
    `url('https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1400&q=80')`;

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.publico.mesas().subscribe({
      next: (lista) => {
        this.mesas.set(lista);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.error.set(extraerApiError(e).mensaje);
        this.cargando.set(false);
      },
    });

    // Solo un CLIENTE puede tener reserva activa; si falla, simplemente no se muestra el aviso.
    if (this.auth.hasRole('CLIENTE')) {
      this.reservas.reservaActiva().subscribe({
        next: (reserva) => this.reservaActiva.set(reserva),
        error: () => this.reservaActiva.set(null),
      });
    }
  }

  protected fondoMesa(mesa: Mesa): string {
    const archivo = mesa.capacidad <= 2 ? 'mesa-2' : mesa.capacidad <= 4 ? 'mesa-4' : 'mesa-grupo';
    return `${DEGRADADO_MESA}, url('${urlRecurso(`img/mesas/${archivo}.jpg`)}')`;
  }

  protected puntos(cantidad: number): number[] {
    return Array.from({ length: cantidad }, (_, i) => i);
  }

  protected tipoBoton(mesa: Mesa): TipoBoton {
    if (mesa.estado !== 'LIBRE') {
      return 'noDisponible';
    }
    if (this.reservaActiva() !== null) {
      return 'conReserva';
    }
    const usuario = this.auth.usuario();
    return usuario !== null && usuario.rol !== 'CLIENTE' ? 'soloClientes' : 'reservable';
  }

  /** CLIENTE -> pantalla de reservar; visitante -> ventana emergente con returnUrl hacia esa mesa. */
  protected reservar(mesa: Mesa): void {
    if (this.auth.hasRole('CLIENTE')) {
      void this.router.navigate(['/cliente/reservar', mesa.id]);
      return;
    }
    this.returnUrl.set(`/cliente/reservar/${mesa.id}`);
    this.modalAbierto.set(true);
  }
}