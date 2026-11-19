import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { PlatoService } from '../../../core/services/plato.service';
import { ApiError } from '../../../models/api-error.model';
import { CATEGORIAS_PLATO, CategoriaPlato, FiltroPlatos, Plato } from '../../../models/plato.model';
import { AlertaApiComponent } from '../../../shared/alerta-api.component';
import { extraerApiError } from '../../../shared/api-error';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog.component';
import { leerMensajeFlash } from '../../../shared/flash';
import { PlatoImagenComponent } from '../../../shared/plato-imagen.component';
import { rangoOrdenado } from '../../../shared/validadores';

@Component({
  selector: 'app-admin-platos',
  imports: [ReactiveFormsModule, RouterLink, DecimalPipe, AlertaApiComponent, ConfirmDialogComponent, PlatoImagenComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="admin-cabecera">
      <h2>Carta del restaurante</h2>
      <a class="btn-admin btn-admin-primario" routerLink="/admin/platos/nuevo">
        <i class="fas fa-plus"></i> Nuevo plato
      </a>
    </div>

    @if (mensaje(); as m) {
      <div class="alerta alerta-exito" role="status"><i class="fas fa-circle-check"></i> {{ m }}</div>
    }
    <app-alerta-api [error]="errorApi()" [conReintento]="true" (reintentar)="cargar()" />

    <form class="filtros" [formGroup]="filtroForm" (ngSubmit)="aplicar()" novalidate>
      <div class="campo">
        <label class="campo-label" for="f-texto">Nombre</label>
        <input id="f-texto" type="text" class="campo-input" formControlName="texto" placeholder="Ej. lomo" />
      </div>
      <div class="campo">
        <label class="campo-label" for="f-categoria">Categoría</label>
        <select id="f-categoria" class="campo-input" formControlName="categoria">
          <option value="">Todas</option>
          @for (c of categorias; track c) {
            <option [value]="c">{{ c }}</option>
          }
        </select>
      </div>
      <div class="campo">
        <label class="campo-label" for="f-min">Precio mín.</label>
        <input id="f-min" type="number" class="campo-input" formControlName="precioMin" min="0" step="0.01" />
      </div>
      <div class="campo">
        <label class="campo-label" for="f-max">Precio máx.</label>
        <input id="f-max" type="number" class="campo-input" formControlName="precioMax" min="0" step="0.01" />
      </div>
      <div class="filtros-acciones">
        <button type="submit" class="btn-admin btn-admin-primario">Filtrar</button>
        <button type="button" class="btn-admin btn-admin-secundario" (click)="limpiar()">Limpiar</button>
      </div>
    </form>
    @if (filtroForm.hasError('rangoInvalido')) {
      <div class="alerta alerta-aviso" role="alert">El precio mínimo no puede ser mayor que el máximo.</div>
    } @else if (filtroForm.controls.precioMin.invalid || filtroForm.controls.precioMax.invalid) {
      <div class="alerta alerta-aviso" role="alert">Los precios no pueden ser negativos.</div>
    }

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando platos…</p>
    } @else if (platos().length > 0) {
      <div class="tabla-wrap">
        <table class="tabla-admin">
          <thead>
            <tr><th>Imagen</th><th>Nombre</th><th>Descripción</th><th>Precio</th><th>Categoría</th><th></th></tr>
          </thead>
          <tbody>
            @for (p of platos(); track p.id) {
              <tr>
                <td><app-plato-imagen [categoria]="p.categoria" [imagen]="p.imagen" [nombre]="p.nombre" clase="dish-mini" /></td>
                <td>{{ p.nombre }}</td>
                <td class="celda-texto">{{ p.descripcion }}</td>
                <td>S/ {{ p.precio | number: '1.2-2' }}</td>
                <td>{{ p.categoria }}</td>
                <td class="col-acciones">
                  <a class="btn-admin btn-admin-secundario btn-admin-sm" [routerLink]="['/admin/platos', p.id, 'editar']">Editar</a>
                  <button type="button" class="btn-admin btn-admin-peligro btn-admin-sm" (click)="pedirEliminar(p)">Eliminar</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else if (!errorApi()) {
      <div class="empty-state">
        <div class="icon-wrap"><i class="fas fa-utensils"></i></div>
        <h5>{{ hayFiltros() ? 'Ningún plato coincide con los filtros' : 'Aún no hay platos' }}</h5>
        <p>{{ hayFiltros() ? 'Prueba con otros criterios o pulsa Limpiar.' : 'Crea el primer plato de la carta.' }}</p>
      </div>
    }

    <app-confirm-dialog
      [abierto]="porEliminar() !== null"
      titulo="¿Eliminar plato?"
      [mensaje]="mensajeEliminar()"
      textoConfirmar="Sí, eliminar"
      textoCancelar="No, conservarlo"
      [procesando]="eliminando()"
      (confirmar)="eliminar()"
      (cancelar)="porEliminar.set(null)"
    />
  `,
})
export class AdminPlatosComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly servicio = inject(PlatoService);

  protected readonly categorias = CATEGORIAS_PLATO;

  protected readonly filtroForm = this.fb.group(
    {
      texto: [''],
      categoria: this.fb.control<CategoriaPlato | ''>(''),
      precioMin: this.fb.control<number | null>(null, [Validators.min(0)]),
      precioMax: this.fb.control<number | null>(null, [Validators.min(0)]),
    },
    { validators: [rangoOrdenado('precioMin', 'precioMax')] },
  );

  /** Filtros YA aplicados (los que se enviaron al pulsar Filtrar); lo escrito sin enviar no afecta las recargas. */
  private readonly filtros = signal<FiltroPlatos>({});
  protected readonly hayFiltros = computed(() => {
    const f = this.filtros();
    return !!f.texto || !!f.categoria || (f.precioMin ?? null) !== null || (f.precioMax ?? null) !== null;
  });

  protected readonly platos = signal<Plato[]>([]);
  protected readonly cargando = signal(true);
  protected readonly eliminando = signal(false);
  protected readonly porEliminar = signal<Plato | null>(null);
  protected readonly mensaje = signal<string | null>(leerMensajeFlash());
  protected readonly errorApi = signal<ApiError | null>(null);

  protected readonly mensajeEliminar = computed(() => {
    const plato = this.porEliminar();
    return plato ? `Se eliminará "${plato.nombre}". Esta acción no se puede deshacer.` : '';
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorApi.set(null);
    this.servicio.listar(this.filtros()).subscribe({
      next: (lista) => {
        this.platos.set(lista);
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
    this.servicio.listar(this.filtros()).subscribe({
      next: (lista) => this.platos.set(lista),
      error: (e: unknown) => this.errorApi.set(extraerApiError(e)),
    });
  }

  protected aplicar(): void {
    if (this.filtroForm.invalid) {
      this.filtroForm.markAllAsTouched();
      return;
    }
    const v = this.filtroForm.getRawValue();
    this.filtros.set({ texto: v.texto.trim(), categoria: v.categoria, precioMin: v.precioMin, precioMax: v.precioMax });
    this.mensaje.set(null);
    this.cargar();
  }

  protected limpiar(): void {
    this.filtroForm.reset();
    this.filtros.set({});
    this.mensaje.set(null);
    this.cargar();
  }

  protected pedirEliminar(plato: Plato): void {
    this.mensaje.set(null);
    this.errorApi.set(null);
    this.porEliminar.set(plato);
  }

  protected eliminar(): void {
    const plato = this.porEliminar();
    if (plato === null || this.eliminando()) {
      return;
    }
    this.eliminando.set(true);
    this.servicio.eliminar(plato.id).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.porEliminar.set(null);
        this.mensaje.set(`Plato "${plato.nombre}" eliminado.`);
        this.refrescar();
      },
      error: (e: unknown) => {
        const error = extraerApiError(e);
        this.eliminando.set(false);
        this.porEliminar.set(null);
        this.errorApi.set(error); // 409: plato usado en reservas
        if (error.estado === 404) {
          this.refrescar();
        }
      },
    });
  }
}