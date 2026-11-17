import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

const OPCIONES_MENU = [
  { ruta: '/admin/dashboard', etiqueta: 'Dashboard', icono: 'fas fa-gauge' },
  { ruta: '/admin/mesas', etiqueta: 'Mesas', icono: 'fas fa-chair' },
  { ruta: '/admin/platos', etiqueta: 'Platos', icono: 'fas fa-utensils' },
  { ruta: '/admin/reservas', etiqueta: 'Reservas', icono: 'fas fa-calendar-check' },
  { ruta: '/admin/usuarios', etiqueta: 'Usuarios', icono: 'fas fa-users' },
  { ruta: '/admin/reportes', etiqueta: 'Reportes', icono: 'fas fa-chart-pie' },
] as const;

/** Marco común de /admin/**: menú del panel y la pantalla activa debajo. */
@Component({
  selector: 'app-admin-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="admin-menu" aria-label="Menú de administración">
      @for (opcion of opciones; track opcion.ruta) {
        <a [routerLink]="opcion.ruta" routerLinkActive="activo">
          <i [class]="opcion.icono"></i> {{ opcion.etiqueta }}
        </a>
      }
    </nav>
    <router-outlet />
  `,
})
export class AdminLayoutComponent {
  protected readonly opciones = OPCIONES_MENU;
}