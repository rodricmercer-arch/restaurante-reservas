export type CategoriaPlato = 'PRINCIPAL' | 'POSTRE' | 'BEBIDA';

/** PlatoResponseDTO. descripcion e imagen pueden venir null. */
export interface Plato {
  id: number;
  nombre: string;
  descripcion: string | null;
  precio: number;
  categoria: CategoriaPlato;
  imagen: string | null;
}

/** Valores del enum CategoriaPlato del backend, para los selectores. */
export const CATEGORIAS_PLATO: readonly CategoriaPlato[] = ['PRINCIPAL', 'POSTRE', 'BEBIDA'];

/** Body de POST /api/platos y PUT /api/platos/{id} (PlatoRequestDTO). descripcion e imagen son opcionales (null si van vacías). */
export interface PlatoRequest {
  nombre: string;
  descripcion: string | null;
  precio: number;
  categoria: CategoriaPlato;
  imagen: string | null;
}

/** Filtros opcionales de GET /api/platos. Los vacíos no se envían. */
export interface FiltroPlatos {
  texto?: string;
  categoria?: CategoriaPlato | '';
  precioMin?: number | null;
  precioMax?: number | null;
}