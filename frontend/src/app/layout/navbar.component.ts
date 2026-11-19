import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="navbar-restaurante" aria-label="Navegación principal">
      <a class="navbar-brand" routerLink="/">
        <span class="brand-badge"><i class="fas fa-utensils"></i></span>
        Restaurante
      </a>

      <div class="navbar-nav">
        @if (auth.usuario(); as usuario) {
          @if (usuario.rol === 'CLIENTE') {
            <a class="nav-link" routerLink="/mesas" routerLinkActive="activo">Mesas</a>
            <a class="nav-link" routerLink="/cliente/mi-reserva" routerLinkActive="activo">Mi reserva</a>
          } @else {
            <a class="nav-link" routerLink="/admin/dashboard" routerLinkActive="activo">Panel admin</a>
          }
          <a class="nav-link" routerLink="/perfil" routerLinkActive="activo">Mi perfil</a>
        } @else {
          <a class="nav-link" routerLink="/carta" routerLinkActive="activo">Carta</a>
          <a class="nav-link" routerLink="/mesas" routerLinkActive="activo">Mesas</a>
        }
      </div>

      <div class="navbar-acciones">
        @if (auth.usuario()) {
          <button class="btn-logout" type="button" (click)="salir()">
            <i class="fas fa-arrow-right-from-bracket"></i> Salir
          </button>
        } @else {
          <a class="btn-nav-login" routerLink="/login">Iniciar sesión</a>
          <a class="btn-nav-registro" routerLink="/registro">Registrarse</a>
        }
      </div>
    </nav>
  `,
})
export class NavbarComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected salir(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/');
  }
}