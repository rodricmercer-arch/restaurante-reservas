import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl } from '@angular/forms';

import { ApiError } from '../models/api-error.model';

function esApiError(valor: unknown): valor is ApiError {
  if (typeof valor !== 'object' || valor === null) {
    return false;
  }
  const v = valor as Record<string, unknown>;
  return typeof v['estado'] === 'number' && typeof v['mensaje'] === 'string';
}

function mensajePorEstado(estado: number): string {
  switch (estado) {
    case 400:
      return 'La solicitud no es válida.';
    case 401:
      return 'Tu sesión no es válida. Inicia sesión de nuevo.';
    case 403:
      return 'No tienes permiso para hacer esto.';
    case 404:
      return 'No se encontró lo que buscas.';
    case 409:
      return 'La operación no se pudo completar.';
    default:
      return estado >= 500 ? 'El servidor tuvo un problema. Inténtalo más tarde.' : 'Ocurrió un error inesperado.';
  }
}

/** Convierte cualquier error de HttpClient en el formato único {estado, mensaje, errores} del backend. */
export function extraerApiError(error: unknown): ApiError {
  if (error instanceof HttpErrorResponse) {
    if (esApiError(error.error)) {
      return {
        estado: error.error.estado,
        mensaje: error.error.mensaje,
        errores: Array.isArray(error.error.errores) ? error.error.errores : [],
      };
    }
    if (error.status === 0) {
      return { estado: 0, mensaje: 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.', errores: [] };
    }
    return { estado: error.status, mensaje: mensajePorEstado(error.status), errores: [] };
  }
  return { estado: -1, mensaje: error instanceof Error ? error.message : 'Ocurrió un error inesperado.', errores: [] };
}

/**
 * Un 400 trae líneas "campo: mensaje". Las que corresponden a un control del formulario se marcan en ese control
 * (error "servidor", que mensajeError() ya sabe mostrar) y las demás se devuelven para la alerta general.
 */
export function repartirErroresEnFormulario(form: AbstractControl, error: ApiError): ApiError {
  const sinCampo: string[] = [];
  for (const linea of error.errores) {
    const corte = linea.indexOf(': ');
    const control = corte > 0 ? form.get(linea.slice(0, corte)) : null;
    if (control) {
      control.setErrors({ servidor: linea.slice(corte + 2) });
      control.markAsTouched();
    } else {
      sinCampo.push(linea);
    }
  }
  return { ...error, errores: sinCampo };
}