import { Rol } from './auth.model';

/** Respuesta de GET /api/perfil (PerfilResponseDTO). totalReservas solo viene si el rol es CLIENTE. */
export interface Perfil {
  dni: string;
  nombre: string;
  rol: Rol;
  totalReservas?: number;
}

/** Body de PUT /api/perfil/password (CambiarPasswordRequestDTO). El DNI sale del token, nunca del body. */
export interface CambiarPasswordRequest {
  actual: string;
  nueva: string;
  confirmacion: string;
}