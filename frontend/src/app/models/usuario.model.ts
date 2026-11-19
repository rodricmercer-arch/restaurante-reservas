import { Rol } from './auth.model';

/** UsuarioResponseDTO. Nunca incluye la contraseña. */
export interface Usuario {
  id: number;
  dni: string;
  nombre: string;
  rol: Rol;
}

/**
 * Body de POST /api/admin/usuarios y PUT /api/admin/usuarios/{id} (UsuarioRequestDTO).
 * password: obligatoria al crear; al editar solo se envía si se quiere cambiar la clave.
 */
export interface UsuarioRequest {
  dni: string;
  nombre: string;
  rol: Rol;
  password?: string;
}