/** Única fuente de la URL base de la API REST (Spring Boot). Todo servicio e interceptor parte de aquí. */
export const API_URL = 'https://restaurante-deploy.onrender.com/api';

/** Rutas que NO llevan token y cuyos 401/403 no cierran la sesión (el 401 del login es "clave incorrecta"). */
const RUTAS_SIN_TOKEN: readonly string[] = [
  `${API_URL}/auth/login`,
  `${API_URL}/auth/registro`,
  `${API_URL}/publico/`,
];

export function esRutaSinToken(url: string): boolean {
  return RUTAS_SIN_TOKEN.some((ruta) => url.startsWith(ruta));
}

/** Evita enviar el token a cualquier otro servidor que algún día llame la app. */
export function esUrlDeLaApi(url: string): boolean {
  return url.startsWith(`${API_URL}/`);
}