/** Roles que maneja el backend (claim "roles" del JWT y campo "rol" del login). */
export const ROLES = ['ADMIN', 'CLIENTE'] as const;
export type Rol = (typeof ROLES)[number];

export function esRol(valor: unknown): valor is Rol {
  return typeof valor === 'string' && (ROLES as readonly string[]).includes(valor);
}

/** Body de POST /api/auth/login (LoginRequestDTO). */
export interface LoginRequest {
  dni: string;
  password: string;
}

/** Respuesta de POST /api/auth/login (TokenResponseDTO). */
export interface TokenResponse {
  token: string;
  tipo: string; // siempre "Bearer"
  expiraEnSegundos: number;
  dni: string;
  rol: string; // "ADMIN" | "CLIENTE"; se valida con esRol() antes de usarlo
}

/** Body de POST /api/auth/registro (RegistroFormDTO). El rol lo fija siempre el servidor. */
export interface RegistroRequest {
  dni: string;
  nombre: string;
  password: string;
  confirmacion: string;
}

/** Respuesta de POST /api/auth/registro (RegistroResponseDTO). Nunca incluye la contraseña. */
export interface RegistroResponse {
  dni: string;
  nombre: string;
}

/** Lo que se guarda en localStorage. expiraEn = marca de tiempo en milisegundos (Date.now() + expiración). */
export interface Sesion {
  token: string;
  dni: string;
  rol: Rol;
  expiraEn: number;
}

/** Usuario actual expuesto a la interfaz (sin el token). */
export interface UsuarioActual {
  dni: string;
  rol: Rol;
}

export function esSesion(valor: unknown): valor is Sesion {
  if (typeof valor !== 'object' || valor === null) {
    return false;
  }
  const s = valor as Record<string, unknown>;
  return (
    typeof s['token'] === 'string' &&
    typeof s['dni'] === 'string' &&
    esRol(s['rol']) &&
    typeof s['expiraEn'] === 'number'
  );
}