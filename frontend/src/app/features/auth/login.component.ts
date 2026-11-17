import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../models/api-error.model';
import { extraerApiError } from '../../shared/api-error';
import { mensajeError, noEnBlanco, returnUrlSegura } from '../../shared/validadores';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="auth-fondo"></div>
    <div class="auth-contenedor">
      <div class="auth-card">
        <div class="auth-logo"><i class="fas fa-utensils"></i></div>
        <h1 class="auth-titulo">Restaurante</h1>
        <p class="auth-subtitulo">Reserva tu mesa e ingresa a tu cuenta</p>

        @if (registrado) {
          <div class="auth-alerta auth-alerta-ok" role="status">
            <i class="fas fa-check-circle"></i> Cuenta creada. Ya puedes iniciar sesión.
          </div>
        }
        @if (aviso === 'sesion-vencida') {
          <div class="auth-alerta auth-alerta-aviso" role="alert">
            <i class="fas fa-clock"></i> Tu sesión venció o ya no es válida. Inicia sesión de nuevo.
          </div>
        }
        @if (errorApi(); as e) {
          <div class="auth-alerta auth-alerta-error" role="alert">
            <i class="fas fa-exclamation-circle"></i> {{ e.mensaje }}
          </div>
        }

        <form class="auth-form" [formGroup]="form" (ngSubmit)="enviar()" novalidate>
          <div class="auth-campo">
            <label class="auth-label" for="dni">DNI</label>
            <input id="dni" type="text" class="auth-input" formControlName="dni" [class.invalido]="!!mensajeDni()"
                   placeholder="Ingresa tu DNI" maxlength="8" inputmode="numeric" autocomplete="username" autofocus />
            @if (mensajeDni(); as mensaje) {
              <div class="auth-error-campo">{{ mensaje }}</div>
            }
          </div>

          <div class="auth-campo">
            <label class="auth-label" for="password">Contraseña</label>
            <div class="auth-grupo">
              <input id="password" class="auth-input" formControlName="password" [class.invalido]="!!mensajePassword()"
                     [type]="verPassword() ? 'text' : 'password'" placeholder="••••••••" autocomplete="current-password" />
              <button type="button" class="auth-toggle" (click)="verPassword.set(!verPassword())"
                      [attr.aria-label]="verPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'">
                <i class="fas" [class.fa-eye]="!verPassword()" [class.fa-eye-slash]="verPassword()"></i>
              </button>
            </div>
            @if (mensajePassword(); as mensaje) {
              <div class="auth-error-campo">{{ mensaje }}</div>
            }
          </div>

          <button type="submit" class="auth-boton" [disabled]="enviando()">
            {{ enviando() ? 'Ingresando…' : 'Iniciar Sesión' }}
          </button>
        </form>

        <div class="auth-enlaces">
          <a routerLink="/registro" [queryParams]="{ returnUrl: returnUrl }">¿No tienes cuenta? <strong>Regístrate</strong></a>
          <a routerLink="/"><i class="fas fa-arrow-left"></i> Volver al inicio</a>
        </div>

        <div class="auth-demo">
          <p>Demo admin: DNI 00000000 / admin123</p>
          <p>Demo cliente: DNI 11111111 / cliente123</p>
        </div>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  protected readonly returnUrl = returnUrlSegura(this.ruta.snapshot.queryParamMap.get('returnUrl'));
  protected readonly aviso = this.ruta.snapshot.queryParamMap.get('aviso');
  protected readonly registrado = this.ruta.snapshot.queryParamMap.get('registrado') === 'true';

  protected readonly form = this.fb.group({
    dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    password: ['', [Validators.required, noEnBlanco]],
  });

  protected readonly verPassword = signal(false);
  protected readonly enviando = signal(false);
  protected readonly errorApi = signal<ApiError | null>(null);

  protected mensajeDni(): string | null {
    return mensajeError(this.form.controls.dni, {
      required: 'El DNI es obligatorio',
      pattern: 'El DNI debe tener 8 dígitos',
    });
  }

  protected mensajePassword(): string | null {
    return mensajeError(this.form.controls.password, { required: 'La contraseña es obligatoria' });
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

    const { dni, password } = this.form.getRawValue();
    this.auth.login(dni, password).subscribe({
      next: (usuario) => {
        this.enviando.set(false);
        const destino = this.returnUrl ?? (usuario.rol === 'ADMIN' ? '/admin/dashboard' : '/mesas');
        void this.router.navigateByUrl(destino);
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.errorApi.set(extraerApiError(e));
      },
    });
  }
}