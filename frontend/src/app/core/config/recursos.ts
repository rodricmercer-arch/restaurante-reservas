import { API_URL } from './api.config';

/** Origen del servidor (http://localhost:8080), que sirve las imágenes estáticas (/img/...). Se deriva de API_URL: no es otra constante. */
const ORIGEN_SERVIDOR = API_URL.replace(/\/api$/, '');

/** Convierte la ruta guardada en la BD ("img/platos/x.jpg" o una URL absoluta) en una URL que el navegador pueda cargar. */
export function urlRecurso(ruta: string): string {
  return /^https?:\/\//i.test(ruta) ? ruta : `${ORIGEN_SERVIDOR}/${ruta.replace(/^\/+/, '')}`;
}