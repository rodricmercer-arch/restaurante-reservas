import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { PerfilService } from '../../core/services/perfil.service';
import { ApiError } from '../../models/api-error.model';
import { Perfil } from '../../models/perfil.model';
import { AlertaApiComponent } from '../../shared/alerta-api.component';
import { extraerApiError, repartirErroresEnFormulario } from '../../shared/api-error';
import { confirmacionCoincideNueva, mensajeError, noEnBlanco } from '../../shared/validadores';

/** /perfil (ADMIN y CLIENTE): datos del usuario y cambio de la contraseña propia. */
@Component({
  selector: 'app-perfil',
  imports: [ReactiveFormsModule, AlertaApiComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-header">
      <div class="eyebrow">Mi cuenta</div>
      <h2>Mi perfil</h2>
    </div>

    <app-alerta-api [error]="errorPerfil()" [conReintento]="true" (reintentar)="cargar()" />

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando perfil…</p>
    } @else if (perfil(); as p) {
      <div class="perfil-card">
        <dl class="perfil-datos">
          <dt>Nombre</dt>
          <dd>{{ p.nombre }}</dd>
          <dt>DNI</dt>
          <dd>{{ p.dni }}</dd>
          <dt>Rol</dt>
          <dd>{{ p.rol }}</dd>
          @if (p.totalReservas !== undefined) {
            <dt>Reservas</dt>
            <dd>{{ p.totalReservas }}</dd>
          }
        </dl>
      </div>

      <h3 class="perfil-subtitulo">Cambiar contraseña</h3>

      @if (exito(); as m) {
        <div class="alerta alerta-exito" role="status"><i class="fas fa-circle-check"></i> {{ m }}</div>
      }
      <app-alerta-api [error]="errorApi()" />

      <form class="form-card" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        <div class="campo">
          <label class="campo-label" for="actual">Contraseña actual</label>
          <input id="actual" type="password" class="campo-input" formControlName="actual"
                 [class.invalido]="!!mensajeActual()" autocomplete="current-password" />
          @if (mensajeActual(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="nueva">Nueva contraseña</label>
          <input id="nueva" type="password" class="campo-input" formControlName="nueva"
                 [class.invalido]="!!mensajeNueva()" autocomplete="new-password" />
          <div class="campo-ayuda">Mínimo 8 caracteres.</div>
          @if (mensajeNueva(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="confirmacion">Confirmar nueva contraseña</label>
          <input id="confirmacion" type="password" class="campo-input" formControlName="confirmacion"
                 [class.invalido]="!!mensajeConfirmacion()" autocomplete="new-password" />
          @if (mensajeConfirmacion(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="form-acciones">
          <button type="submit" class="btn-admin btn-admin-primario" [disabled]="enviando()">
            {{ enviando() ? 'Guardando…' : 'Guardar' }}
          </button>
        </div>
      </form>
    }
  `,
})
export class PerfilComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly servicio = inject(PerfilService);

  protected readonly form = this.fb.group(
    {
      actual: ['', [Validators.required, noEnBlanco]],
      nueva: ['', [Validators.required, noEnBlanco, Validators.minLength(8)]],
      confirmacion: ['', [Validators.required]],
    },
    { validators: [confirmacionCoincideNueva] },
  );

  protected readonly perfil = signal<Perfil | null>(null);
  protected readonly cargando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly exito = signal<string | null>(null);
  protected readonly errorPerfil = signal<ApiError | null>(null);
  protected readonly errorApi = signal<ApiError | null>(null);

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorPerfil.set(null);
    this.servicio.obtener().subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.errorPerfil.set(extraerApiError(e));
        this.cargando.set(false);
      },
    });
  }

  protected mensajeActual(): string | null {
    return mensajeError(this.form.controls.actual, { required: 'La contraseña actual es obligatoria' });
  }

  protected mensajeNueva(): string | null {
    return mensajeError(this.form.controls.nueva, {
      required: 'La nueva contraseña es obligatoria',
      minlength: 'La contraseña debe tener al menos 8 caracteres.',
    });
  }

  protected mensajeConfirmacion(): string | null {
    const control = this.form.controls.confirmacion;
    const propio = mensajeError(control, { required: 'Debes confirmar la nueva contraseña' });
    if (propio !== null) {
      return propio;
    }
    return control.touched && this.form.hasError('noCoincide') ? 'La confirmación no coincide con la nueva contraseña.' : null;
  }

  protected guardar(): void {
    if (this.enviando()) {
      return;
    }
    this.exito.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.errorApi.set(null);

    this.servicio.cambiarPassword(this.form.getRawValue()).subscribe({
      next: () => {
        this.enviando.set(false);
        this.form.reset(); // vacía los tres campos y quita el estado "tocado"
        this.exito.set('Contraseña actualizada correctamente.');
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        // 400: "La contraseña actual es incorrecta." y similares van a la alerta; los "campo: mensaje" bajo su input.
        this.errorApi.set(repartirErroresEnFormulario(this.form, extraerApiError(e)));
      },
    });
  }
}