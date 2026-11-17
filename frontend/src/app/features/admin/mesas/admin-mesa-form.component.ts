import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AdminMesaService } from '../../../core/services/admin-mesa.service';
import { ApiError } from '../../../models/api-error.model';
import { AlertaApiComponent } from '../../../shared/alerta-api.component';
import { extraerApiError, repartirErroresEnFormulario } from '../../../shared/api-error';
import { mensajeError } from '../../../shared/validadores';

/** Crear (/admin/mesas/nueva) y editar (/admin/mesas/:id/editar). Reglas del backend: número >= 1, capacidad de 1 a 20. */
@Component({
  selector: 'app-admin-mesa-form',
  imports: [ReactiveFormsModule, RouterLink, AlertaApiComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="btn-back" routerLink="/admin/mesas"><i class="fas fa-arrow-left"></i> Volver a mesas</a>
    <div class="page-header">
      <div class="eyebrow">Mesas</div>
      <h2>{{ esEdicion ? 'Editar mesa' : 'Nueva mesa' }}</h2>
    </div>

    <app-alerta-api [error]="errorApi()" />

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando mesa…</p>
    } @else if (!noEncontrada()) {
      <form class="form-card" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        <div class="campo">
          <label class="campo-label" for="numero">N.º de mesa</label>
          <input id="numero" type="number" class="campo-input" formControlName="numero"
                 [class.invalido]="!!mensajeNumero()" min="1" step="1" inputmode="numeric" />
          @if (mensajeNumero(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="capacidad">Capacidad (1 a 20)</label>
          <input id="capacidad" type="number" class="campo-input" formControlName="capacidad"
                 [class.invalido]="!!mensajeCapacidad()" min="1" max="20" step="1" inputmode="numeric" />
          @if (mensajeCapacidad(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="form-acciones">
          <button type="submit" class="btn-admin btn-admin-primario" [disabled]="enviando()">
            {{ enviando() ? 'Guardando…' : 'Guardar' }}
          </button>
          <a class="btn-admin btn-admin-secundario" routerLink="/admin/mesas">Cancelar</a>
        </div>
      </form>
    }
  `,
})
export class AdminMesaFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly servicio = inject(AdminMesaService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  private readonly idParam = this.ruta.snapshot.paramMap.get('id');
  /** null = crear; número = editar. */
  protected readonly id: number | null = this.idParam === null ? null : Number(this.idParam);
  protected readonly esEdicion = this.id !== null;

  protected readonly form = this.fb.group({
    numero: this.fb.control<number | null>(null, [Validators.required, Validators.min(1), Validators.pattern(/^-?\d+$/)]),
    capacidad: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
      Validators.max(20),
      Validators.pattern(/^-?\d+$/),
    ]),
  });

  protected readonly cargando = signal(this.esEdicion);
  protected readonly noEncontrada = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errorApi = signal<ApiError | null>(null);

  constructor() {
    if (this.id !== null) {
      this.cargarMesa(this.id);
    }
  }

  /** La API no tiene GET /api/admin/mesas/{id}: se busca la mesa en la lista. */
  private cargarMesa(id: number): void {
    this.servicio.listar().subscribe({
      next: (lista) => {
        const mesa = lista.find((m) => m.id === id);
        if (mesa) {
          this.form.patchValue({ numero: mesa.numero, capacidad: mesa.capacidad });
        } else {
          this.noEncontrada.set(true);
          this.errorApi.set({ estado: 404, mensaje: 'La mesa que intentas editar no existe.', errores: [] });
        }
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.noEncontrada.set(true);
        this.errorApi.set(extraerApiError(e));
        this.cargando.set(false);
      },
    });
  }

  protected mensajeNumero(): string | null {
    return mensajeError(this.form.controls.numero, {
      required: 'El número de mesa es obligatorio',
      min: 'El número de mesa debe ser mayor o igual a 1',
      pattern: 'El número de mesa debe ser un entero',
    });
  }

  protected mensajeCapacidad(): string | null {
    return mensajeError(this.form.controls.capacidad, {
      required: 'La capacidad es obligatoria',
      min: 'La capacidad mínima es 1 persona',
      max: 'La capacidad máxima es 20 personas',
      pattern: 'La capacidad debe ser un entero',
    });
  }

  protected guardar(): void {
    if (this.enviando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { numero, capacidad } = this.form.getRawValue();
    if (numero === null || capacidad === null) {
      return;
    }
    this.enviando.set(true);
    this.errorApi.set(null);

    const datos = { numero, capacidad };
    const peticion = this.id === null ? this.servicio.crear(datos) : this.servicio.actualizar(this.id, datos);
    peticion.subscribe({
      next: (mesa) => {
        this.enviando.set(false);
        const texto = this.esEdicion ? `Mesa ${mesa.numero} actualizada.` : `Mesa ${mesa.numero} creada.`;
        void this.router.navigate(['/admin/mesas'], { state: { mensaje: texto } });
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        // 400: errores por campo bajo cada input; 409 (número duplicado) y 404: mensaje en la alerta.
        this.errorApi.set(repartirErroresEnFormulario(this.form, extraerApiError(e)));
      },
    });
  }
}