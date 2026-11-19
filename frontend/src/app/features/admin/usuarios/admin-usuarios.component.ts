import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';
import { AdminUsuarioService } from '../../../core/services/admin-usuario.service';
import { ApiError } from '../../../models/api-error.model';
import { Usuario } from '../../../models/usuario.model';
import { AlertaApiComponent } from '../../../shared/alerta-api.component';
import { extraerApiError } from '../../../shared/api-error';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog.component';
import { leerMensajeFlash } from '../../../shared/flash';

@Component({
  selector: 'app-admin-usuarios',
  imports: [RouterLink, AlertaApiComponent, ConfirmDialogComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="admin-cabecera">
      <h2>Usuarios</h2>
      <a class="btn-admin btn-admin-primario" routerLink="/admin/usuarios/nuevo">
        <i class="fas fa-plus"></i> Nuevo usuario
      </a>
    </div>

    @if (mensaje(); as m) {
      <div class="alerta alerta-exito" role="status"><i class="fas fa-circle-check"></i> {{ m }}</div>
    }
    <app-alerta-api [error]="errorApi()" [conReintento]="true" (reintentar)="cargar()" />

    @if (cargando()) {
      <p class="estado-carga" role="status">Cargando usuarios…</p>
    } @else if (usuarios().length > 0) {
      <div class="tabla-wrap">
        <table class="tabla-admin">
          <thead>
            <tr><th>DNI</th><th>Nombre</th><th>Rol</th><th></th></tr>
          </thead>
          <tbody>
            @for (u of usuarios(); track u.id) {
              <tr>
                <td>{{ u.dni }}</td>
                <td>
                  {{ u.nombre }}
                  @if (u.dni === dniActual()) {
                    <span class="etiqueta-yo">Tú</span>
                  }
                </td>
                <td>
                  <span class="badge-estado" [class.admin]="u.rol === 'ADMIN'" [class.cliente]="u.rol === 'CLIENTE'">
                    <span class="dot"></span> {{ u.rol }}
                  </span>
                </td>
                <td class="col-acciones">
                  <a class="btn-admin btn-admin-secundario btn-admin-sm" [routerLink]="['/admin/usuarios', u.id, 'editar']">Editar</a>
                  <button type="button" class="btn-admin btn-admin-peligro btn-admin-sm" (click)="pedirEliminar(u)">Eliminar</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else if (!errorApi()) {
      <div class="empty-state">
        <div class="icon-wrap"><i class="fas fa-users"></i></div>
        <h5>Aún no hay usuarios</h5>
        <p>Crea el primer usuario del sistema.</p>
      </div>
    }

    <app-confirm-dialog
      [abierto]="porEliminar() !== null"
      titulo="¿Eliminar usuario?"
      [mensaje]="mensajeEliminar()"
      textoConfirmar="Sí, eliminar"
      textoCancelar="No, conservarlo"
      [procesando]="eliminando()"
      (confirmar)="eliminar()"
      (cancelar)="porEliminar.set(null)"
    />
  `,
})
export class AdminUsuariosComponent {
  private readonly servicio = inject(AdminUsuarioService);
  private readonly auth = inject(AuthService);

  protected readonly usuarios = signal<Usuario[]>([]);
  protected readonly cargando = signal(true);
  protected readonly eliminando = signal(false);
  protected readonly porEliminar = signal<Usuario | null>(null);
  protected readonly mensaje = signal<string | null>(leerMensajeFlash());
  protected readonly errorApi = signal<ApiError | null>(null);

  /** DNI de la sesión: solo sirve para marcar tu propia fila; la regla "no puedes eliminarte" la aplica el servidor (409). */
  protected readonly dniActual = computed(() => this.auth.usuario()?.dni ?? null);

  protected readonly mensajeEliminar = computed(() => {
    const usuario = this.porEliminar();
    return usuario ? `Se eliminará al usuario ${usuario.nombre} (DNI ${usuario.dni}). Esta acción no se puede deshacer.` : '';
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorApi.set(null);
    this.servicio.listar().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.cargando.set(false);
      },
      error: (e: unknown) => {
        this.errorApi.set(extraerApiError(e));
        this.cargando.set(false);
      },
    });
  }

  /** Recarga sin spinner ni limpiar mensajes (después de eliminar). */
  private refrescar(): void {
    this.servicio.listar().subscribe({
      next: (lista) => this.usuarios.set(lista),
      error: (e: unknown) => this.errorApi.set(extraerApiError(e)),
    });
  }

  protected pedirEliminar(usuario: Usuario): void {
    this.mensaje.set(null);
    this.errorApi.set(null);
    this.porEliminar.set(usuario);
  }

  protected eliminar(): void {
    const usuario = this.porEliminar();
    if (usuario === null || this.eliminando()) {
      return;
    }
    this.eliminando.set(true);
    this.servicio.eliminar(usuario.id).subscribe({
      next: () => {
        this.eliminando.set(false);
        this.porEliminar.set(null);
        this.mensaje.set(`Usuario ${usuario.nombre} eliminado.`);
        this.refrescar();
      },
      error: (e: unknown) => {
        const error = extraerApiError(e);
        this.eliminando.set(false);
        this.porEliminar.set(null);
        this.errorApi.set(error); // 409: tu propia cuenta, último administrador o con reservas
        if (error.estado === 404) {
          this.refrescar(); // ya no existía: la lista se actualiza sola
        }
      },
    });
  }
}