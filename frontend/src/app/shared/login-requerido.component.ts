import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ModalComponent } from './modal.component';

/** "Inicia sesión para continuar": enlaces a /login y /registro conservando returnUrl. */
@Component({
  selector: 'app-login-requerido',
  imports: [ModalComponent, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal [abierto]="abierto()" titulo="Inicia sesión para continuar" (cerrar)="cerrar.emit()">
      <div class="modal-icono"><i class="fas fa-lock"></i></div>
      <h2 class="modal-titulo">Inicia sesión para continuar</h2>
      <p class="modal-texto">Para reservar una mesa o ver tu reserva necesitas una cuenta.</p>
      <div class="modal-acciones">
        <a class="btn-modal btn-modal-primario" routerLink="/login" [queryParams]="{ returnUrl: returnUrl() }" (click)="cerrar.emit()">Iniciar sesión</a>
        <a class="btn-modal btn-modal-secundario" routerLink="/registro" [queryParams]="{ returnUrl: returnUrl() }" (click)="cerrar.emit()">Crear cuenta</a>
        <button type="button" class="btn-modal btn-modal-enlace" (click)="cerrar.emit()">Seguir explorando</button>
      </div>
    </app-modal>
  `,
})
export class LoginRequeridoComponent {
  readonly abierto = input.required<boolean>();
  /** Ruta a la que se vuelve después de iniciar sesión o registrarse. */
  readonly returnUrl = input('/mesas');
  readonly cerrar = output<void>();
}