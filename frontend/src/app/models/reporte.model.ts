/** Un plato del ranking (ReporteDTO.PlatoTop). */
export interface PlatoTop {
  nombre: string;
  unidades: number;
}

/** Respuesta de GET /api/admin/reportes (ReporteDTO). Los porcentajes NO vienen de la API: se calculan en la pantalla. */
export interface Reporte {
  reservasPorEstado: Record<string, number>;
  mesasPorEstado: Record<string, number>;
  platosPorCategoria: Record<string, number>;
  topPlatos: PlatoTop[];
  ingresoEstimado: number;
}