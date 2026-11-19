import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // Pantallas completas, sin barra de navegación.
  {
    path: 'login',
    title: 'Iniciar sesión',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'registro',
    title: 'Crear cuenta',
    loadComponent: () => import('./features/auth/registro.component').then((m) => m.RegistroComponent),
  },
  // Todo lo demás va dentro del shell (navbar + contenido).
  {
    path: '',
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'mesas' },
      {
        path: 'carta',
        title: 'Nuestra carta',
        loadComponent: () => import('./features/public/carta.component').then((m) => m.CartaComponent),
      },
      {
        path: 'mesas',
        title: 'Mesas disponibles',
        loadComponent: () => import('./features/public/mesas.component').then((m) => m.MesasComponent),
      },
      {
        path: 'cliente/reservar/:mesaId',
        title: 'Reservar mesa',
        canActivate: [roleGuard(['CLIENTE'])],
        loadComponent: () => import('./features/cliente/reservar.component').then((m) => m.ReservarComponent),
      },
      {
        path: 'cliente/mi-reserva',
        title: 'Mi reserva',
        canActivate: [roleGuard(['CLIENTE'])],
        loadComponent: () => import('./features/cliente/mi-reserva.component').then((m) => m.MiReservaComponent),
      },
      {
        path: 'perfil',
        title: 'Mi perfil',
        canActivate: [authGuard],
        loadComponent: () => import('./features/perfil/perfil.component').then((m) => m.PerfilComponent),
      },
      {
        path: 'admin',
        canActivate: [roleGuard(['ADMIN'])],
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      },
      {
        path: '403',
        title: 'Acceso denegado',
        loadComponent: () => import('./features/public/forbidden.component').then((m) => m.ForbiddenComponent),
      },
      {
        path: '**',
        title: 'Página no encontrada',
        loadComponent: () => import('./features/public/not-found.component').then((m) => m.NotFoundComponent),
      },
    ],
  },
];
