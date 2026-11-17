import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AdminUsuarioService } from '../../../core/services/admin-usuario.service';
import { ApiError } from '../../../models/api-error.model';
import { ROLES, Rol } from '../../../models/auth.model';
import { UsuarioRequest } from '../../../models/usuario.model';
import { AlertaApiComponent } from '../../../shared/alerta-api.component';
import { extraerApiError, repartirErroresEnFormulario } from '../../../shared/api-error';
import { dniValido, mensajeError, noEnBlanco } from '../../../shared/validadores';

/**
 * Crear (/admin/usuarios/nuevo) y editar (/admin/usuarios/:id/editar).
 * Reglas: DNI de 8 dígitos (no editable al editar), nombre y rol obligatorios,
 * contraseña obligatoria al crear y opcional al editar (mínimo 8 si se escribe).
 */
@Component({
  selector: 'app-admin-usuario-form',
  imports: [ReactiveFormsModule, RouterLink, AlertaApiComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="btn-back" routerLink="/admin/usuarios"><i class="fas fa-arrow-left"></i> Volver a usuarios</a>
    <div class="page-header">
      <div class="eyebrow">Usuarios</div>
      <h2>{{ esEdicion ? 'Editar usuario' : 'Nuevo usuario' }}</h2>
    </div>

    <app-alerta-api [error]="errorApi()" />

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando usuario…</p>
    } @else if (!noEncontrado()) {
      <form class="form-card" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
        <div class="campo">
          <label class="campo-label" for="dni">DNI</label>
          <input id="dni" type="text" class="campo-input" formControlName="dni" [class.invalido]="!!mensajeDni()"
                 maxlength="8" inputmode="numeric" autocomplete="off" />
          @if (esEdicion) {
            <div class="campo-ayuda">El DNI no se puede modificar.</div>
          }
          @if (mensajeDni(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="nombre">Nombre</label>
          <input id="nombre" type="text" class="campo-input" formControlName="nombre" [class.invalido]="!!mensajeNombre()"
                 autocomplete="off" />
          @if (mensajeNombre(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="rol">Rol</label>
          <select id="rol" class="campo-input" formControlName="rol" [class.invalido]="!!mensajeRol()">
            <option value="">-- Seleccione --</option>
            @for (r of roles; track r) {
              <option [value]="r">{{ r }}</option>
            }
          </select>
          @if (mensajeRol(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="campo">
          <label class="campo-label" for="password">Contraseña</label>
          <input id="password" type="password" class="campo-input" formControlName="password"
                 [class.invalido]="!!mensajePassword()" autocomplete="new-password" />
          <div class="campo-ayuda">
            {{ esEdicion ? 'Déjela vacía para mantener la actual.' : 'Obligatoria, mínimo 8 caracteres.' }}
          </div>
          @if (mensajePassword(); as mensaje) {
            <div class="campo-error">{{ mensaje }}</div>
          }
        </div>

        <div class="form-acciones">
          <button type="submit" class="btn-admin btn-admin-primario" [disabled]="enviando()">
            {{ enviando() ? 'Guardando…' : 'Guardar' }}
          </button>
          <a class="btn-admin btn-admin-secundario" routerLink="/admin/usuarios">Cancelar</a>
        </div>
      </form>
    }
  `,
})
export class AdminUsuarioFormComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly servicio = inject(AdminUsuarioService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  private readonly idParam = this.ruta.snapshot.paramMap.get('id');
  /** null = crear; número = editar. */
  protected readonly id: number | null = this.idParam === null ? null : Number(this.idParam);
  protected readonly esEdicion = this.id !== null;

  protected readonly roles = ROLES;

  protected readonly form = this.fb.group({
    dni: ['', [Validators.required, dniValido]],
    nombre: ['', [Validators.required, noEnBlanco]],
    rol: this.fb.control<Rol | ''>('', [Validators.required]),
    // Crear: obligatoria. Editar: opcional, pero si se escribe debe tener al menos 8 caracteres.
    password: ['', this.esEdicion ? [Validators.minLength(8)] : [Validators.required, noEnBlanco, Validators.minLength(8)]],
  });

  protected readonly cargando = signal(this.esEdicion);
  protected readonly noEncontrado = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errorApi = signal<ApiError | null>(null);

  constructor() {
    if (this.id !== null) {
      this.form.controls.dni.disable(); // el DNI no se modifica al editar
      this.cargarUsuario(this.id);
    }
  }

  /** La API no tiene GET /api/admin/usuarios/{id}: se busca el usuario en la lista. */
  private cargarUsuario(id: number): void {
    this.servicio.listar().subscribe({
      next: (lista) => {
        const usuario = lista.find((u) => u.id === id);
        if (usuario) {
          this.form.patchValue({ dni: usuario.dni, nombre: usuario.nombre, rol: usuario.rol });
        } else {
          this.noEncontrado.set(true);
          this.errorApi.set({ estado: 404, mensaje: 'El usuario que intentas editar no existe.', errores: [] });
        }
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.noEncontrado.set(true);
        this.errorApi.set(extraerApiError(e));
        this.cargando.set(false);
      },
    });
  }

  protected mensajeDni(): string | null {
    return mensajeError(this.form.controls.dni, {
      required: 'El DNI es obligatorio',
      dni: 'El DNI debe tener 8 dígitos',
    });
  }

  protected mensajeNombre(): string | null {
    return mensajeError(this.form.controls.nombre, { required: 'El nombre es obligatorio' });
  }

  protected mensajeRol(): string | null {
    return mensajeError(this.form.controls.rol, { required: 'El rol es obligatorio' });
  }

  protected mensajePassword(): string | null {
    return mensajeError(this.form.controls.password, {
      required: 'La contraseña es obligatoria.',
      minlength: 'La contraseña debe tener al menos 8 caracteres.',
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
    // getRawValue incluye el DNI aunque esté deshabilitado: la API lo exige en el PUT (no lo modifica).
    const { dni, nombre, rol, password } = this.form.getRawValue();
    if (rol === '') {
      return;
    }
    this.enviando.set(true);
    this.errorApi.set(null);

    const datos: UsuarioRequest = { dni, nombre: nombre.trim(), rol };
    if (password.trim() !== '') {
      datos.password = password; // sin password, la clave actual no cambia
    }
    const peticion = this.id === null ? this.servicio.crear(datos) : this.servicio.actualizar(this.id, datos);
    peticion.subscribe({
      next: (usuario) => {
        this.enviando.set(false);
        const texto = this.esEdicion ? `Usuario ${usuario.nombre} actualizado.` : `Usuario ${usuario.nombre} creado.`;
        void this.router.navigate(['/admin/usuarios'], { state: { mensaje: texto } });
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        // 400: errores por campo bajo cada input; 409 (DNI duplicado, último administrador, clave corta) y 404: alerta.
        this.errorApi.set(repartirErroresEnFormulario(this.form, extraerApiError(e)));
      },
    });
  }
}