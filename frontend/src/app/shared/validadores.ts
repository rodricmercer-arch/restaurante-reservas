import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';


/** Equivale a @NotBlank: un texto de solo espacios cuenta como vacío (Validators.required no lo detecta). */
export const noEnBlanco: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && control.value.trim().length === 0 ? { required: true } : null;

/** DNI de 8 dígitos (misma regla del backend: \d{8}). Un valor vacío no es error aquí: eso lo decide Validators.required. Clave de error: "dni". */
export const dniValido: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const valor: unknown = control.value;
  if (valor === null || valor === undefined || valor === '') {
    return null;
  }
  return typeof valor === 'string' && /^\d{8}$/.test(valor) ? null : { dni: true };
};


/**
 * Validador de GRUPO para rangos "desde <= hasta" (precios numéricos o fechas ISO yyyy-MM-dd, que se ordenan como texto).
 * Si alguno de los dos está vacío no hay error. Clave de error en el grupo: "rangoInvalido".
 */
export function rangoOrdenado(campoDesde: string, campoHasta: string): ValidatorFn {
  return (grupo: AbstractControl): ValidationErrors | null => {
    const desde: unknown = grupo.get(campoDesde)?.value;
    const hasta: unknown = grupo.get(campoHasta)?.value;
    if (typeof desde === 'number' && typeof hasta === 'number') {
      return desde > hasta ? { rangoInvalido: true } : null;
    }
    if (typeof desde === 'string' && typeof hasta === 'string' && desde !== '' && hasta !== '') {
      return desde > hasta ? { rangoInvalido: true } : null;
    }
    return null;
  };
}

/** Validador del grupo de registro: la confirmación debe ser igual a la contraseña. */
export const confirmacionCoincide: ValidatorFn = (grupo: AbstractControl): ValidationErrors | null => {
  const password = grupo.get('password')?.value as unknown;
  const confirmacion = grupo.get('confirmacion')?.value as unknown;
  return typeof password === 'string' && typeof confirmacion === 'string' && confirmacion !== '' && password !== confirmacion
    ? { noCoincide: true }
    : null;
};

/** Variante para el cambio de contraseña: la confirmación debe ser igual al campo "nueva". Clave de error en el grupo: "noCoincide". */
export const confirmacionCoincideNueva: ValidatorFn = (grupo: AbstractControl): ValidationErrors | null => {
  const nueva = grupo.get('nueva')?.value as unknown;
  const confirmacion = grupo.get('confirmacion')?.value as unknown;
  return typeof nueva === 'string' && typeof confirmacion === 'string' && confirmacion !== '' && nueva !== confirmacion
    ? { noCoincide: true }
    : null;
};

/** Mensaje del primer error del control que tenga texto en `mensajes`; si no, el error "servidor" (viene del backend). Solo si ya fue tocado. */
export function mensajeError(control: AbstractControl, mensajes: Record<string, string>): string | null {
  if (!control.touched || !control.errors) {
    return null;
  }
  const clave = Object.keys(control.errors).find((k) => k in mensajes);
  if (clave !== undefined) {
    return mensajes[clave];
  }
  const servidor: unknown = control.errors['servidor'];
  return typeof servidor === 'string' ? servidor : null;
}

/** Solo acepta rutas internas ("/algo"), nunca "//otro-sitio" ni URLs absolutas (evita redirecciones abiertas). */
export function returnUrlSegura(valor: string | null | undefined): string | null {
  return valor && valor.startsWith('/') && !valor.startsWith('//') ? valor : null;
}