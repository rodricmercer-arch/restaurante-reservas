export type EstadoMesa = 'LIBRE' | 'RESERVADA';

/** MesaResponseDTO. */
export interface Mesa {
  id: number;
  numero: number;
  capacidad: number;
  estado: EstadoMesa;
}

/** Body de POST /api/admin/mesas y PUT /api/admin/mesas/{id} (MesaRequestDTO). El estado nunca viaja. */
export interface MesaRequest {
  numero: number;
  capacidad: number;
}