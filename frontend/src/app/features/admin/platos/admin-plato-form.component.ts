import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { PlatoService } from '../../../core/services/plato.service';
import { ApiError } from '../../../models/api-error.model';
import { CATEGORIAS_PLATO, CategoriaPlato, PlatoRequest } from '../../../models/plato.model';
import { AlertaApiComponent } from '../../../shared/alerta-api.component';
import { extraerApiError, repartirErroresEnFormulario } from '../../../shared/api-error';
import { mensajeError, noEnBlanco } from '../../../shared/validadores';

/** Crear (/admin/platos/nuevo) y editar (/admin/platos/:id/editar). Reglas del backend (PlatoRequestDTO). */
@Component({
  selector: 'app-admin-plato-form',
  imports: [ReactiveFormsModule, RouterLink, AlertaApiComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="btn-back" routerLink="/admin/platos"><i class="fas fa-arrow-left"></i> Volver a la carta</a>
    <div class="page-header">
      <div class="eyebrow">Carta</div>
      <h2>{{ esEdicion ? 'Editar plato' : 'Nuevo plato' }}</h2>
    </div>

    <app-alerta-api [error]="errorApi()" />

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando plato…</p>
    } @else if (!noEncontrado()) {
      <form class="form-card" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        <div class="campo">
          <label class="campo-label" for="nombre">Nombre</label>
          <input id="nombre" type="text" class="campo-input" formControlName="nombre" [class.invalido]="!!mensajeNombre()" />
          @if (mensajeNombre(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="descripcion">Descripción</label>
          <input id="descripcion" type="text" class="campo-input" formControlName="descripcion" [class.invalido]="!!mensajeDescripcion()" />
          <div class="campo-ayuda">Opcional. Máximo 255 caracteres.</div>
          @if (mensajeDescripcion(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="imagen">Imagen (URL o ruta)</label>
          <input id="imagen" type="text" class="campo-input" formControlName="imagen" [class.invalido]="!!mensajeImagen()"
                 placeholder="https://... o img/platos/mi-plato.jpg" />
          <div class="campo-ayuda">Opcional. URL completa o ruta dentro de static/ (sin barra inicial).</div>
          @if (mensajeImagen(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="precio">Precio (S/)</label>
          <input id="precio" type="number" class="campo-input" formControlName="precio" [class.invalido]="!!mensajePrecio()"
                 min="0.01" step="0.01" inputmode="decimal" />
          @if (mensajePrecio(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="categoria">Categoría</label>
          <select id="categoria" class="campo-input" formControlName="categoria" [class.invalido]="!!mensajeCategoria()">
            <option value="">-- Seleccione --</option>
            @for (c of categorias; track c) {
              <option [value]="c">{{ c }}</option>
            }
          </select>
          @if (mensajeCategoria(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="form-acciones">
          <button type="submit" class="btn-admin btn-admin-primario" [disabled]="enviando()">
            {{ enviando() ? 'Guardando…' : 'Guardar' }}
          </button>
          <a class="btn-admin btn-admin-secundario" routerLink="/admin/platos">Cancelar</a>
        </div>
      </form>
    }
  `,
})
export class AdminPlatoFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly servicio = inject(PlatoService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  private readonly idParam = this.ruta.snapshot.paramMap.get('id');
  /** null = crear; número = editar. */
  protected readonly id: number | null = this.idParam === null ? null : Number(this.idParam);
  protected readonly esEdicion = this.id !== null;
  protected readonly categorias = CATEGORIAS_PLATO;

  protected readonly form = this.fb.group({
    nombre: ['', [Validators.required, noEnBlanco, Validators.maxLength(100)]],
    descripcion: ['', [Validators.maxLength(255)]],
    imagen: ['', [Validators.maxLength(255)]],
    precio: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01), Validators.pattern(/^\d+(\.\d{1,2})?$/)]),
    categoria: this.fb.control<CategoriaPlato | ''>('', [Validators.required]),
  });

  protected readonly cargando = signal(this.esEdicion);
  protected readonly noEncontrado = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errorApi = signal<ApiError | null>(null);

  constructor() {
    if (this.id !== null) {
      if (Number.isInteger(this.id)) {
        this.cargarPlato(this.id);
      } else {
        this.marcarNoEncontrado({ estado: 404, mensaje: 'El plato que intentas editar no existe.', errores: [] });
      }
    }
  }

  private cargarPlato(id: number): void {
    this.servicio.obtener(id).subscribe({
      next: (plato) => {
        this.form.patchValue({
          nombre: plato.nombre,
          descripcion: plato.descripcion ?? '',
          imagen: plato.imagen ?? '',
          precio: plato.precio,
          categoria: plato.categoria,
        });
        this.cargando.set(false);
      },
      error: (e: unknown) => this.marcarNoEncontrado(extraerApiError(e)),
    });
  }

  private marcarNoEncontrado(error: ApiError): void {
    this.noEncontrado.set(true);
    this.errorApi.set(error);
    this.cargando.set(false);
  }

  protected mensajeNombre(): string | null {
    return mensajeError(this.form.controls.nombre, {
      required: 'El nombre es obligatorio',
      maxlength: 'El nombre no puede superar los 100 caracteres',
    });
  }

  protected mensajeDescripcion(): string | null {
    return mensajeError(this.form.controls.descripcion, {
      maxlength: 'La descripción no puede superar los 255 caracteres',
    });
  }

  protected mensajeImagen(): string | null {
    return mensajeError(this.form.controls.imagen, {
      maxlength: 'La ruta de la imagen no puede superar los 255 caracteres',
    });
  }

  protected mensajePrecio(): string | null {
    return mensajeError(this.form.controls.precio, {
      required: 'El precio es obligatorio',
      min: 'El precio debe ser mayor a 0',
      pattern: 'El precio admite hasta 2 decimales',
    });
  }

  protected mensajeCategoria(): string | null {
    return mensajeError(this.form.controls.categoria, { required: 'La categoría es obligatoria' });
  }

  protected guardar(): void {
    if (this.enviando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (v.precio === null || v.categoria === '') {
      return;
    }
    const descripcion = v.descripcion.trim();
    const imagen = v.imagen.trim();
    const datos: PlatoRequest = {
      nombre: v.nombre.trim(),
      descripcion: descripcion === '' ? null : descripcion,
      precio: v.precio,
      categoria: v.categoria,
      imagen: imagen === '' ? null : imagen,
    };

    this.enviando.set(true);
    this.errorApi.set(null);
    const peticion = this.id === null ? this.servicio.crear(datos) : this.servicio.actualizar(this.id, datos);
    peticion.subscribe({
      next: (plato) => {
        this.enviando.set(false);
        const texto = this.esEdicion ? `Plato "${plato.nombre}" actualizado.` : `Plato "${plato.nombre}" creado.`;
        void this.router.navigate(['/admin/platos'], { state: { mensaje: texto } });
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        // 400: errores bajo cada campo; 409 (nombre duplicado) y 404: alerta general.
        this.errorApi.set(repartirErroresEnFormulario(this.form, extraerApiError(e)));
      },
    });
  }
}