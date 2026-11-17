/**
 * Mensaje de éxito que una pantalla deja al navegar a otra:
 * router.navigate([...], { state: { mensaje: 'Mesa creada.' } }) y la pantalla destino llama a leerMensajeFlash().
 * Se borra al leerlo para que no reaparezca al recargar la página.
 */
export function leerMensajeFlash(): string | null {
  const estado: unknown = history.state;
  if (typeof estado !== 'object' || estado === null) {
    return null;
  }
  const mensaje = (estado as Record<string, unknown>)['mensaje'];
  if (typeof mensaje !== 'string') {
    return null;
  }
  history.replaceState({ ...(estado as object), mensaje: null }, '');
  return mensaje;
}