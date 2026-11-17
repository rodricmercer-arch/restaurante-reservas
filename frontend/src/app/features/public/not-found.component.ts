import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty-state">
      <div class="icon-wrap"><i class="fas fa-compass"></i></div>
      <h5>404 · Página no encontrada</h5>
      <p>La dirección que buscas no existe o fue movida.</p>
      <a class="btn-explore" routerLink="/">Volver al inicio</a>
    </div>
  `,
})
export class NotFoundComponent {}