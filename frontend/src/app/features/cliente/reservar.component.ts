import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, map } from 'rxjs';

import { PublicoService } from '../../core/services/publico.service';
import { ReservaClienteService } from '../../core/services/reserva-cliente.service';
import { ApiError } from '../../models/api-error.model';
import { Mesa } from '../../models/mesa.model';
import { Plato } from '../../models/plato.model';
import { CrearReservaRequest } from '../../models/reserva.model';
import { extraerApiError } from '../../shared/api-error';
import { PlatoImagenComponent } from '../../shared/plato-imagen.component';

@Component({
  selector: 'app-reservar',
  imports: [DecimalPipe, ReactiveFormsModule, RouterLink, PlatoImagenComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a routerLink="/mesas" class="btn-back"><i class="fas fa-arrow-left"></i> Volver a mesas</a>

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando…</p>
    } @else if (errorCarga()) {
      <div class="alerta alerta-error" role="alert">{{ errorCarga() }}</div>
    } @else if (mesa(); as m) {
      <div class="page-header">
        <div class="eyebrow">Mesa {{ m.numero }} &middot; {{ m.capacidad }} personas</div>
        <h2>Arma tu pedido</h2>
        <p>Elige tu plato principal (obligatorio). El postre y la bebida son opcionales.</p>
      </div>

      @if (!mesaLibre()) {
        <div class="alerta alerta-aviso" role="alert">
          <i class="fas fa-triangle-exclamation"></i> Esta mesa ya no está libre. Vuelve a Mesas y elige otra.
        </div>
      }

      @if (errorApi(); as e) {
        <div class="alerta alerta-error" role="alert">
          <i class="fas fa-circle-exclamation"></i> {{ e.mensaje }}
          @if (e.errores.length > 0) {
            <ul>
              @for (linea of e.errores; track linea) {
                <li>{{ linea }}</li>
              }
            </ul>
          }
          @if (e.estado === 409) {
            <span class="alerta-acciones"><a class="btn-reintentar" routerLink="/mesas">Volver a Mesas</a></span>
          }
        </div>
      }

      <form [formGroup]="form" (ngSubmit)="confirmar()" novalidate>
        <div class="reservar-layout">
          <div>
            <div class="section-label">Plato principal <span class="required-pill">Obligatorio</span></div>
            <p class="section-sub">Elige uno para continuar.</p>
            <div class="grid-opciones">
              @for (plato of principales(); track plato.id) {
                <label class="dish-option">
                  <input type="radio" name="principal" formControlName="principal" [value]="plato.id" />
                  <div class="dish-card">
                    <app-plato-imagen [categoria]="plato.categoria" [imagen]="plato.imagen" [nombre]="plato.nombre" clase="dish-thumb" />
                    <div class="dish-info">
                      <div class="dish-name">{{ plato.nombre }}</div>
                      <p class="dish-desc">{{ plato.descripcion }}</p>
                    </div>
                    <div class="dish-lado">
                      <div class="dish-price">S/ {{ plato.precio | number: '1.2-2' }}</div>
                      <div class="check-circle"><i class="fas fa-check"></i></div>
                    </div>
                  </div>
                </label>
              }
            </div>
            @if (form.controls.principal.touched && form.controls.principal.invalid) {
              <p class="campo-error" role="alert">Elige un plato principal para continuar.</p>
            }

            <div class="section-label">Postre <span class="optional-pill">Opcional</span></div>
            <div class="grid-opciones">
              @for (plato of postres(); track plato.id) {
                <label class="dish-option">
                  <input type="radio" name="postre" formControlName="postre" [value]="plato.id" />
                  <div class="dish-card">
                    <app-plato-imagen [categoria]="plato.categoria" [imagen]="plato.imagen" [nombre]="plato.nombre" clase="dish-thumb" />
                    <div class="dish-info"><div class="dish-name">{{ plato.nombre }}</div></div>
                    <div class="dish-lado">
                      <div class="dish-price">S/ {{ plato.precio | number: '1.2-2' }}</div>
                      <div class="check-circle"><i class="fas fa-check"></i></div>
                    </div>
                  </div>
                </label>
              }
              <label class="dish-option">
                <input type="radio" name="postre" formControlName="postre" [value]="null" />
                <div class="dish-card">
                  <div class="dish-name texto-muted">Sin postre</div>
                  <div class="check-circle"><i class="fas fa-check"></i></div>
                </div>
              </label>
            </div>

            <div class="section-label">Bebida <span class="optional-pill">Opcional</span></div>
            <div class="grid-opciones">
              @for (plato of bebidas(); track plato.id) {
                <label class="dish-option">
                  <input type="radio" name="bebida" formControlName="bebida" [value]="plato.id" />
                  <div class="dish-card">
                    <app-plato-imagen [categoria]="plato.categoria" [imagen]="plato.imagen" [nombre]="plato.nombre" clase="dish-thumb" />
                    <div class="dish-info"><div class="dish-name">{{ plato.nombre }}</div></div>
                    <div class="dish-lado">
                      <div class="dish-price">S/ {{ plato.precio | number: '1.2-2' }}</div>
                      <div class="check-circle"><i class="fas fa-check"></i></div>
                    </div>
                  </div>
                </label>
              }
              <label class="dish-option">
                <input type="radio" name="bebida" formControlName="bebida" [value]="null" />
                <div class="dish-card">
                  <div class="dish-name texto-muted">Sin bebida</div>
                  <div class="check-circle"><i class="fas fa-check"></i></div>
                </div>
              </label>
            </div>
          </div>

          <aside class="order-summary" aria-label="Resumen de tu reserva">
            <h5><i class="fas fa-receipt"></i> Resumen de tu reserva</h5>

            <div class="order-line">
              <span><span class="name">Mesa</span>Mesa {{ m.numero }} &middot; {{ m.capacidad }} personas</span>
            </div>
            <div class="order-line">
              <span><span class="name">Principal</span>{{ resumen().principal?.nombre ?? 'Selecciona un plato' }}</span>
              <span>S/ {{ (resumen().principal?.precio ?? 0) | number: '1.2-2' }}</span>
            </div>
            <div class="order-line">
              <span><span class="name">Postre</span>{{ resumen().postre?.nombre ?? 'Sin postre' }}</span>
              <span>S/ {{ (resumen().postre?.precio ?? 0) | number: '1.2-2' }}</span>
            </div>
            <div class="order-line">
              <span><span class="name">Bebida</span>{{ resumen().bebida?.nombre ?? 'Sin bebida' }}</span>
              <span>S/ {{ (resumen().bebida?.precio ?? 0) | number: '1.2-2' }}</span>
            </div>

            <div class="order-total">
              <span>Total</span>
              <span class="amount">S/ {{ resumen().total | number: '1.2-2' }}</span>
            </div>

            <button type="submit" class="btn-confirm" [disabled]="enviando() || !mesaLibre()">
              <i class="fas fa-check-circle"></i> {{ enviando() ? 'Reservando…' : 'Confirmar reserva' }}
            </button>
            <a routerLink="/mesas" class="btn-cancel-link">Cancelar</a>
          </aside>
        </div>
      </form>
    } @else {
      <div class="empty-state">
        <div class="icon-wrap"><i class="fas fa-chair"></i></div>
        <h5>La mesa no existe</h5>
        <p>No encontramos la mesa que quieres reservar.</p>
        <a class="btn-explore" routerLink="/mesas">Ver mesas</a>
      </div>
    }
  `,
})
export class ReservarComponent {
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly publico = inject(PublicoService);
  private readonly reservas = inject(ReservaClienteService);
  private readonly fb = inject(NonNullableFormBuilder);

  private readonly mesaId = Number(this.ruta.snapshot.paramMap.get('mesaId'));

  protected readonly mesa = signal<Mesa | null>(null);
  protected readonly platos = signal<Plato[]>([]);
  protected readonly cargando = signal(true);
  protected readonly errorCarga = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly errorApi = signal<ApiError | null>(null);

  protected readonly form = this.fb.group({
    principal: this.fb.control<number | null>(null, Validators.required),
    postre: this.fb.control<number | null>(null),
    bebida: this.fb.control<number | null>(null),
  });

  /** Valores del formulario como signal: el resumen se recalcula al elegir. */
  private readonly valores = toSignal(
    this.form.valueChanges.pipe(map(() => this.form.getRawValue())),
    { initialValue: this.form.getRawValue() },
  );

  protected readonly principales = computed(() => this.platos().filter((p) => p.categoria === 'PRINCIPAL'));
  protected readonly postres = computed(() => this.platos().filter((p) => p.categoria === 'POSTRE'));
  protected readonly bebidas = computed(() => this.platos().filter((p) => p.categoria === 'BEBIDA'));
  protected readonly mesaLibre = computed(() => this.mesa()?.estado === 'LIBRE');

  protected readonly resumen = computed(() => {
    const valores = this.valores();
    const lista = this.platos();
    const buscar = (id: number | null): Plato | null => (id === null ? null : (lista.find((p) => p.id === id) ?? null));
    const principal = buscar(valores.principal);
    const postre = buscar(valores.postre);
    const bebida = buscar(valores.bebida);
    const total = (principal?.precio ?? 0) + (postre?.precio ?? 0) + (bebida?.precio ?? 0);
    return { principal, postre, bebida, total };
  });

  constructor() {
    forkJoin({ mesas: this.publico.mesas(), platos: this.publico.carta() }).subscribe({
      next: ({ mesas, platos }) => {
        this.mesa.set(mesas.find((m) => m.id === this.mesaId) ?? null);
        this.platos.set(platos);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.errorCarga.set(extraerApiError(e).mensaje);
        this.cargando.set(false);
      },
    });
  }

  protected confirmar(): void {
    if (this.enviando()) {
      return;
    }
    const { principal, postre, bebida } = this.form.getRawValue();
    if (this.form.invalid || principal === null) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.errorApi.set(null);

    const cuerpo: CrearReservaRequest = {
      mesaId: this.mesaId,
      platoPrincipalId: principal,
      platoPostreId: postre,
      platoBebidaId: bebida,
    };
    this.reservas.crear(cuerpo).subscribe({
      next: () => {
        this.enviando.set(false);
        void this.router.navigateByUrl('/cliente/mi-reserva');
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.errorApi.set(extraerApiError(e)); // 409: mesa tomada por otro cliente o ya tienes una reserva activa
      },
    });
  }
}