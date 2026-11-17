import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { PublicoService } from '../../core/services/publico.service';
import { Plato } from '../../models/plato.model';
import { extraerApiError } from '../../shared/api-error';
import { LoginRequeridoComponent } from '../../shared/login-requerido.component';
import { PlatoImagenComponent } from '../../shared/plato-imagen.component';

@Component({
  selector: 'app-carta',
  imports: [DecimalPipe, RouterLink, LoginRequeridoComponent, PlatoImagenComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-login-requerido [abierto]="modalAbierto()" returnUrl="/mesas" (cerrar)="modalAbierto.set(false)" />

    <div class="page-header">
      <div class="eyebrow">Restaurante</div>
      <h2>Nuestra carta</h2>
      <p>Conoce nuestros platos, postres y bebidas.</p>
    </div>

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando la carta…</p>
    } @else if (error()) {
      <div class="alerta alerta-error" role="alert">
        {{ error() }}
        <span class="alerta-acciones"><button class="btn-reintentar" type="button" (click)="cargar()">Reintentar</button></span>
      </div>
    } @else {
      @for (seccion of secciones(); track seccion.titulo) {
        <div class="section-label">{{ seccion.titulo }}</div>
        <div class="grid-platos">
          @for (plato of seccion.platos; track plato.id) {
            <article class="dish-public">
              <app-plato-imagen [categoria]="plato.categoria" [imagen]="plato.imagen" [nombre]="plato.nombre" clase="dish-banner" />
              <div class="dish-public-body">
                <div class="dish-name">{{ plato.nombre }}</div>
                <p class="dish-desc">{{ plato.descripcion }}</p>
                <div class="dish-price">S/ {{ plato.precio | number: '1.2-2' }}</div>
              </div>
            </article>
          } @empty {
            <p class="texto-muted">Pronto agregaremos opciones.</p>
          }
        </div>
      }

      <div class="cta-carta">
        <p class="texto-muted">¿Listo para disfrutar?</p>
        @if (auth.usuario(); as usuario) {
          @if (usuario.rol === 'CLIENTE') {
            <a class="btn-reservar btn-cta" routerLink="/mesas">Reservar una mesa</a>
          }
        } @else {
          <button type="button" class="btn-reservar btn-cta" (click)="modalAbierto.set(true)">Reservar una mesa</button>
        }
      </div>
    }
  `,
})
export class CartaComponent {
  private readonly publico = inject(PublicoService);
  protected readonly auth = inject(AuthService);

  protected readonly platos = signal<Plato[]>([]);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly modalAbierto = signal(false);

  protected readonly secciones = computed(() => [
    { titulo: 'Platos principales', platos: this.platos().filter((p) => p.categoria === 'PRINCIPAL') },
    { titulo: 'Postres', platos: this.platos().filter((p) => p.categoria === 'POSTRE') },
    { titulo: 'Bebidas', platos: this.platos().filter((p) => p.categoria === 'BEBIDA') },
  ]);

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.publico.carta().subscribe({
      next: (lista) => {
        this.platos.set(lista);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.error.set(extraerApiError(e).mensaje);
        this.cargando.set(false);
      },
    });
  }
}