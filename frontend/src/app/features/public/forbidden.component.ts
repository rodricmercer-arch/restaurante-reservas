import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty-state">
      <div class="icon-wrap"><i class="fas fa-lock"></i></div>
      <h5>403 · Acceso denegado</h5>
      <p>No tienes permiso para ver esta página.</p>
      <a class="btn-explore" routerLink="/">Volver al inicio</a>
    </div>
  `,
})
export class ForbiddenComponent {}