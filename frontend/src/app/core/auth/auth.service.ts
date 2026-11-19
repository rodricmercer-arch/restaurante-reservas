import { HttpClient } from '@angular/common/http';
import { Injectable, OnDestroy, computed, inject, signal } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  LoginRequest,
  RegistroRequest,
  RegistroResponse,
  Rol,
  Sesion,
  TokenResponse,
  UsuarioActual,
  esRol,
  esSesion,
} from '../../models/auth.model';
import { API_URL } from '../config/api.config';

/** Clave de localStorage donde vive la sesión (token, dni, rol y expiración). */
export const CLAVE_SESION = 'restaurante.sesion';

/** setTimeout no admite más de 2^31-1 ms (unos 24,8 días). */
const MAX_TIMEOUT_MS = 2_147_483_647;

@Injectable({ providedIn: 'root' })
export class AuthService implements OnDestroy {
  private readonly http = inject(HttpClient);

  private readonly sesion = signal<Sesion | null>(null);
  private temporizador: ReturnType<typeof setTimeout> | null = null;

  /** Usuario actual como signal: null si es visitante. Úsalo en plantillas con usuario(). */
  readonly usuario = computed<UsuarioActual | null>(() => {
    const s = this.sesion();
    return s ? { dni: s.dni, rol: s.rol } : null;
  });

  constructor() {
    this.restaurarSesion();
  }

  ngOnDestroy(): void {
    this.cancelarTemporizador();
  }

  /** POST /api/auth/login. Guarda la sesión y devuelve el usuario para decidir a dónde navegar. */
  login(dni: string, password: string): Observable<UsuarioActual> {
    const cuerpo: LoginRequest = { dni, password };
    return this.http
      .post<TokenResponse>(`${API_URL}/auth/login`, cuerpo)
      .pipe(map((respuesta) => this.guardarSesion(respuesta)));
  }

  /** POST /api/auth/registro. No inicia sesión: tras registrarse se va al login. */
  registro(datos: RegistroRequest): Observable<RegistroResponse> {
    return this.http.post<RegistroResponse>(`${API_URL}/auth/registro`, datos);
  }

  /** Borra la sesión (memoria y localStorage). La navegación la decide quien llama. */
  logout(): void {
    this.cancelarTemporizador();
    this.sesion.set(null);
    try {
      localStorage.removeItem(CLAVE_SESION);
    } catch {
      // Sin acceso a localStorage no hay nada que borrar.
    }
  }

  /** Token vigente, o null si no hay sesión o ya venció. Lo usa el authInterceptor. */
  token(): string | null {
    const s = this.sesion();
    return s !== null && Date.now() < s.expiraEn ? s.token : null;
  }

  /** true si hay sesión y no ha vencido (la expiración se compara con la hora actual). */
  isLoggedIn(): boolean {
    return this.token() !== null;
  }

  /** true si hay sesión vigente y el rol es alguno de los indicados: hasRole('ADMIN'), hasRole('CLIENTE', 'ADMIN'). */
  hasRole(...roles: Rol[]): boolean {
    const s = this.sesion();
    return this.isLoggedIn() && s !== null && roles.includes(s.rol);
  }

  // ---------- Internos ----------

  private guardarSesion(respuesta: TokenResponse): UsuarioActual {
    if (!esRol(respuesta.rol)) {
      throw new Error(`El servidor devolvió un rol desconocido: "${respuesta.rol}".`);
    }
    const sesion: Sesion = {
      token: respuesta.token,
      dni: respuesta.dni,
      rol: respuesta.rol,
      expiraEn: Date.now() + respuesta.expiraEnSegundos * 1000,
    };
    try {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
    } catch {
      // Sin localStorage la sesión vive solo en memoria (se pierde al recargar).
    }
    this.establecerSesion(sesion);
    return { dni: sesion.dni, rol: sesion.rol };
  }

  /** Al arrancar la app: recupera la sesión guardada solo si es válida y no ha vencido. */
  private restaurarSesion(): void {
    const guardada = this.leerSesionGuardada();
    if (guardada) {
      this.establecerSesion(guardada);
    } else {
      try {
        localStorage.removeItem(CLAVE_SESION);
      } catch {
        // Nada que limpiar.
      }
    }
  }

  private leerSesionGuardada(): Sesion | null {
    try {
      const crudo = localStorage.getItem(CLAVE_SESION);
      if (crudo === null) {
        return null;
      }
      const dato: unknown = JSON.parse(crudo);
      return esSesion(dato) && Date.now() < dato.expiraEn ? dato : null;
    } catch {
      return null; // JSON corrupto o localStorage no disponible
    }
  }

  private establecerSesion(sesion: Sesion): void {
    this.sesion.set(sesion);
    this.programarVencimiento(sesion.expiraEn);
  }

  /** Cuando llega la hora de expiración, la sesión se limpia sola y la barra de navegación vuelve a modo visitante. */
  private programarVencimiento(expiraEn: number): void {
    this.cancelarTemporizador();
    const restante = expiraEn - Date.now();
    if (restante > 0 && restante < MAX_TIMEOUT_MS) {
      this.temporizador = setTimeout(() => this.logout(), restante);
    }
  }

  private cancelarTemporizador(): void {
    if (this.temporizador !== null) {
      clearTimeout(this.temporizador);
      this.temporizador = null;
    }
  }
}