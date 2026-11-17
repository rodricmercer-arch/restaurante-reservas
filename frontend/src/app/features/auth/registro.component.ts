import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../models/api-error.model';
import { extraerApiError } from '../../shared/api-error';
import { confirmacionCoincide, mensajeError, noEnBlanco, returnUrlSegura } from '../../shared/validadores';

@Component({
  selector: 'app-registro',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="auth-fondo"></div>
    <div class="auth-contenedor">
      <div class="auth-card">
        <div class="auth-logo"><i class="fas fa-utensils"></i></div>
        <h1 class="auth-titulo">Crear cuenta</h1>
        <p class="auth-subtitulo">Regístrate para reservar tu mesa</p>

        @if (errorApi(); as e) {
          <div class="auth-alerta auth-alerta-error" role="alert">
            <i class="fas fa-exclamation-circle"></i> {{ e.mensaje }}
            @if (e.errores.length > 0) {
              <ul>
                @for (linea of e.errores; track linea) {
                  <li>{{ linea }}</li>
                }
              </ul>
            }
          </div>
        }

        <form class="auth-form" [formGroup]="form" (ngSubmit)="enviar()" novalidate>
          <div class="auth-campo">
            <label class="auth-label" for="dni">DNI</label>
            <input id="dni" type="text" class="auth-input" formControlName="dni" [class.invalido]="!!mensajeDni()"
                   placeholder="8 dígitos" maxlength="8" inputmode="numeric" autocomplete="username" autofocus />
            @if (mensajeDni(); as mensaje) {
              <div class="auth-error-campo">{{ mensaje }}</div>
            }
          </div>

          <div class="auth-campo">
            <label class="auth-label" for="nombre">Nombre completo</label>
            <input id="nombre" type="text" class="auth-input" formControlName="nombre" [class.invalido]="!!mensajeNombre()"
                   placeholder="Tu nombre completo" autocomplete="name" />
            @if (mensajeNombre(); as mensaje) {
              <div class="auth-error-campo">{{ mensaje }}</div>
            }
          </div>

          <div class="auth-campo">
            <label class="auth-label" for="password">Contraseña</label>
            <div class="auth-grupo">
              <input id="password" class="auth-input" formControlName="password" [class.invalido]="!!mensajePassword()"
                     [type]="verPassword() ? 'text' : 'password'" placeholder="Mínimo 8 caracteres" autocomplete="new-password" />
              <button type="button" class="auth-toggle" (click)="verPassword.set(!verPassword())"
                      [attr.aria-label]="verPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                <i class="fas" [class.fa-eye]="!verPassword()" [class.fa-eye-slash]="verPassword()"></i>
              </button>
            </div>
            @if (mensajePassword(); as mensaje) {
              <div class="auth-error-campo">{{ mensaje }}</div>
            }
          </div>

          <div class="auth-campo">
            <label class="auth-label" for="confirmacion">Confirmar contraseña</label>
            <div class="auth-grupo">
              <input id="confirmacion" class="auth-input" formControlName="confirmacion" [class.invalido]="!!mensajeConfirmacion()"
                     [type]="verConfirmacion() ? 'text' : 'password'" placeholder="Repite la contraseña" autocomplete="new-password" />
              <button type="button" class="auth-toggle" (click)="verConfirmacion.set(!verConfirmacion())"
                      [attr.aria-label]="verConfirmacion() ? 'Ocultar confirmación' : 'Mostrar confirmación'">
                <i class="fas" [class.fa-eye]="!verConfirmacion()" [class.fa-eye-slash]="verConfirmacion()"></i>
              </button>
            </div>
            @if (mensajeConfirmacion(); as mensaje) {
              <div class="auth-error-campo">{{ mensaje }}</div>
            }
          </div>

          <button type="submit" class="auth-boton" [disabled]="enviando()">
            {{ enviando() ? 'Creando cuenta…' : 'Crear cuenta' }}
          </button>
        </form>

        <div class="auth-enlaces">
          <a routerLink="/login" [queryParams]="{ returnUrl: returnUrl }">¿Ya tienes cuenta? <strong>Inicia sesión</strong></a>
          <a routerLink="/"><i class="fas fa-arrow-left"></i> Volver al inicio</a>
        </div>
      </div>
    </div>
  `,
})
export class RegistroComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly returnUrl = returnUrlSegura(this.ruta.snapshot.queryParamMap.get('returnUrl'));

  protected readonly form = this.fb.group(
    {
      dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
      nombre: ['', [Validators.required, noEnBlanco, Validators.maxLength(100)]],
      password: ['', [Validators.required, noEnBlanco, Validators.minLength(8)]],
      confirmacion: ['', [Validators.required]],
    },
    { validators: [confirmacionCoincide] },
  );

  protected readonly verPassword = signal(false);
  protected readonly verConfirmacion = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errorApi = signal<ApiError | null>(null);

  protected mensajeDni(): string | null {
    return mensajeError(this.form.controls.dni, {
      required: 'El DNI es obligatorio',
      pattern: 'El DNI debe tener 8 dígitos',
    });
  }

  protected mensajeNombre(): string | null {
    return mensajeError(this.form.controls.nombre, {
      required: 'El nombre es obligatorio',
      maxlength: 'El nombre no puede superar los 100 caracteres',
    });
  }

  protected mensajePassword(): string | null {
    return mensajeError(this.form.controls.password, {
      required: 'La contraseña es obligatoria',
      minlength: 'La contraseña debe tener al menos 8 caracteres',
    });
  }

  protected mensajeConfirmacion(): string | null {
    const control = this.form.controls.confirmacion;
    const propio = mensajeError(control, { required: 'Debes confirmar la contraseña' });
    if (propio !== null) {
      return propio;
    }
    return control.touched && this.form.hasError('noCoincide') ? 'La confirmación no coincide con la contraseña.' : null;
  }

  protected enviar(): void {
    if (this.enviando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.errorApi.set(null);

    const { dni, nombre, password, confirmacion } = this.form.getRawValue();
    this.auth.registro({ dni, nombre: nombre.trim(), password, confirmacion }).subscribe({
      next: () => {
        this.enviando.set(false);
        void this.router.navigate(['/login'], {
          queryParams: { registrado: 'true', returnUrl: this.returnUrl },
        });
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.errorApi.set(this.repartirErrores(extraerApiError(e)));
      },
    });
  }

  /**
   * Un 400 trae líneas "campo: mensaje" (por ejemplo "dni: El DNI debe tener 8 dígitos"):
   * las que corresponden a un campo del formulario se muestran bajo ese campo y el resto queda en la alerta.
   * Como siempre se actualiza errorApi después, la vista se refresca con los errores de los campos.
   */
  private repartirErrores(error: ApiError): ApiError {
    const sinCampo: string[] = [];
    for (const linea of error.errores) {
      const corte = linea.indexOf(': ');
      const control = corte > 0 ? this.form.get(linea.slice(0, corte)) : null;
      if (control) {
        control.setErrors({ servidor: linea.slice(corte + 2) });
        control.markAsTouched();
      } else {
        sinCampo.push(linea);
      }
    }
    return { ...error, errores: sinCampo };
  }
}