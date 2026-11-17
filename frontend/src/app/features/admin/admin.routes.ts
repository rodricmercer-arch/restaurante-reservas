import { Routes } from '@angular/router';

/**
 * Rutas hijas de /admin/**. El roleGuard(['ADMIN']) y el lazy loading se declaran una sola vez en app.routes.ts.
 * Todas las secciones del menú del panel tienen ya su pantalla real.
 */
export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./admin-layout.component').then((m) => m.AdminLayoutComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Panel de administración',
        loadComponent: () => import('./dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'mesas',
        title: 'Mesas (admin)',
        loadComponent: () => import('./mesas/admin-mesas.component').then((m) => m.AdminMesasComponent),
      },
      {
        path: 'mesas/nueva',
        title: 'Nueva mesa',
        loadComponent: () => import('./mesas/admin-mesa-form.component').then((m) => m.AdminMesaFormComponent),
      },
      {
        path: 'mesas/:id/editar',
        title: 'Editar mesa',
        loadComponent: () => import('./mesas/admin-mesa-form.component').then((m) => m.AdminMesaFormComponent),
      },
      {
        path: 'platos',
        title: 'Platos (admin)',
        loadComponent: () => import('./platos/admin-platos.component').then((m) => m.AdminPlatosComponent),
      },
      {
        path: 'platos/nuevo',
        title: 'Nuevo plato',
        loadComponent: () => import('./platos/admin-plato-form.component').then((m) => m.AdminPlatoFormComponent),
      },
      {
        path: 'platos/:id/editar',
        title: 'Editar plato',
        loadComponent: () => import('./platos/admin-plato-form.component').then((m) => m.AdminPlatoFormComponent),
      },
      {
        path: 'reservas',
        title: 'Reservas (admin)',
        loadComponent: () => import('./reservas/admin-reservas.component').then((m) => m.AdminReservasComponent),
      },
      {
        path: 'usuarios',
        title: 'Usuarios (admin)',
        loadComponent: () => import('./usuarios/admin-usuarios.component').then((m) => m.AdminUsuariosComponent),
      },
      {
        path: 'usuarios/nuevo',
        title: 'Nuevo usuario',
        loadComponent: () => import('./usuarios/admin-usuario-form.component').then((m) => m.AdminUsuarioFormComponent),
      },
      {
        path: 'usuarios/:id/editar',
        title: 'Editar usuario',
        loadComponent: () => import('./usuarios/admin-usuario-form.component').then((m) => m.AdminUsuarioFormComponent),
      },
      {
        path: 'reportes',
        title: 'Reportes (admin)',
        loadComponent: () => import('./reportes/admin-reportes.component').then((m) => m.AdminReportesComponent),
      },
    ],
  },
];