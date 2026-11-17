/** Formato único de error del backend (RestExceptionHandler.ErrorRespuesta). */
export interface ApiError {
  estado: number;
  mensaje: string;
  errores: string[]; // en un 400 de validación trae "campo: mensaje"; en el resto viene vacío
}