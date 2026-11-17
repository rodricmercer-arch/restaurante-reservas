import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { NavbarComponent } from './navbar.component';

/** Marco común de las pantallas con barra de navegación (login y registro quedan fuera). */
@Component({
  selector: 'app-shell',
  imports: [NavbarComponent, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-navbar />
    <main class="container-narrow pagina">
      <router-outlet />
    </main>
  `,
})
export class ShellComponent {}