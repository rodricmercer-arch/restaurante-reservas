import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { API_URL } from '../config/api.config';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { errorInterceptor } from '../interceptors/error.interceptor';
import { AuthService, CLAVE_SESION } from './auth.service';

function sembrarSesion(rol: 'ADMIN' | 'CLIENTE', expiraEn = Date.now() + 60_000): void {
  localStorage.setItem(
    CLAVE_SESION,
    JSON.stringify({ token: 'tok-123', dni: '71234567', rol, expiraEn }),
  );
}

/** Siempre se llama DESPUÉS de sembrarSesion(): el servicio lee localStorage al crearse. */
function preparar() {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([{ path: '**', children: [] }]),
      provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  return {
    auth: TestBed.inject(AuthService),
    http: TestBed.inject(HttpClient),
    mock: TestBed.inject(HttpTestingController),
  };
}

describe('AuthService', () => {
  beforeEach(() => localStorage.clear());

  it('login guarda la sesión y expone usuario, isLoggedIn y hasRole', () => {
    const { auth, mock } = preparar();
    let devuelto: unknown;

    auth.login('00000000', 'admin123').subscribe((u) => (devuelto = u));
    const req = mock.expectOne(`${API_URL}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ dni: '00000000', password: 'admin123' });
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ token: 'abc', tipo: 'Bearer', expiraEnSegundos: 3600, dni: '00000000', rol: 'ADMIN' });

    expect(devuelto).toEqual({ dni: '00000000', rol: 'ADMIN' });
    expect(auth.usuario()).toEqual({ dni: '00000000', rol: 'ADMIN' });
    expect(auth.isLoggedIn()).toBe(true);
    expect(auth.hasRole('ADMIN')).toBe(true);
    expect(auth.hasRole('CLIENTE')).toBe(false);
    expect(localStorage.getItem(CLAVE_SESION)).not.toBeNull();
  });

  it('logout limpia la sesión y localStorage', () => {
    sembrarSesion('CLIENTE');
    const { auth } = preparar();
    expect(auth.isLoggedIn()).toBe(true);

    auth.logout();

    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.usuario()).toBeNull();
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('una sesión guardada y vencida se descarta al arrancar', () => {
    sembrarSesion('CLIENTE', Date.now() - 1_000);
    const { auth } = preparar();

    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.usuario()).toBeNull();
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('una sesión corrupta en localStorage se ignora', () => {
    localStorage.setItem(CLAVE_SESION, '{no es json');
    const { auth } = preparar();

    expect(auth.isLoggedIn()).toBe(false);
  });
});

describe('interceptors', () => {
  beforeEach(() => localStorage.clear());

  it('agrega Authorization: Bearer en rutas protegidas', () => {
    sembrarSesion('CLIENTE');
    const { http, mock } = preparar();

    http.get(`${API_URL}/cliente/reserva-activa`).subscribe();

    const req = mock.expectOne(`${API_URL}/cliente/reserva-activa`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer tok-123');
    req.flush(null);
  });

  it('no agrega el token en login, registro ni /api/publico', () => {
    sembrarSesion('CLIENTE');
    const { http, mock } = preparar();

    http.post(`${API_URL}/auth/login`, {}).subscribe();
    http.post(`${API_URL}/auth/registro`, {}).subscribe();
    http.get(`${API_URL}/publico/carta`).subscribe();

    for (const url of ['/auth/login', '/auth/registro', '/publico/carta']) {
      const req = mock.expectOne(`${API_URL}${url}`);
      expect(req.request.headers.has('Authorization')).toBe(false);
      req.flush({});
    }
  });

  it('un 401 en una ruta protegida limpia la sesión', () => {
    sembrarSesion('CLIENTE');
    const { auth, http, mock } = preparar();

    http.get(`${API_URL}/cliente/reserva-activa`).subscribe({ error: () => undefined });
    mock
      .expectOne(`${API_URL}/cliente/reserva-activa`)
      .flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(auth.isLoggedIn()).toBe(false);
    expect(localStorage.getItem(CLAVE_SESION)).toBeNull();
  });

  it('un 401 del login (clave incorrecta) NO toca la sesión', () => {
    sembrarSesion('CLIENTE');
    const { auth, http, mock } = preparar();

    http.post(`${API_URL}/auth/login`, {}).subscribe({ error: () => undefined });
    mock
      .expectOne(`${API_URL}/auth/login`)
      .flush(
        { estado: 401, mensaje: 'DNI o contraseña incorrectos', errores: [] },
        { status: 401, statusText: 'Unauthorized' },
      );

    expect(auth.isLoggedIn()).toBe(true);
  });
});