import { CategoriaPlato } from './plato.model';

export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA';

/** ReservaResponseDTO.MesaResumen. */
export interface MesaResumen {
  id: number;
  numero: number;
  capacidad: number;
}

/** ReservaResponseDTO.ClienteResumen. */
export interface ClienteResumen {
  dni: string;
  nombre: string;
}

/** ReservaResponseDTO.DetalleResumen. El precio es el precio actual del plato (el detalle no guarda uno propio). */
export interface DetalleReserva {
  platoId: number;
  nombre: string;
  categoria: CategoriaPlato;
  cantidad: number;
  precio: number;
}

/** ReservaResponseDTO. fechaHora llega como texto ISO, por ejemplo "2026-11-10T14:30:12.123456". */
export interface Reserva {
  id: number;
  fechaHora: string;
  estado: EstadoReserva;
  mesa: MesaResumen;
  cliente: ClienteResumen;
  detalles: DetalleReserva[];
}

/** Body de POST /api/cliente/reservas (CrearReservaRequestDTO). Postre y bebida son opcionales. */
export interface CrearReservaRequest {
  mesaId: number;
  platoPrincipalId: number;
  platoPostreId?: number | null;
  platoBebidaId?: number | null;
}

/** Valores del enum EstadoReserva del backend, para los selectores. */
export const ESTADOS_RESERVA: readonly EstadoReserva[] = ['PENDIENTE', 'CONFIRMADA', 'CANCELADA'];

/** Body de POST /api/admin/reservas/{id}/mover (MoverReservaRequestDTO). */
export interface MoverReservaRequest {
  nuevaMesaId: number;
}

/** Filtros opcionales de GET /api/admin/reservas. desde y hasta en formato yyyy-MM-dd. Los vacíos no se envían. */
export interface FiltroReservas {
  dni?: string;
  estado?: EstadoReserva | '';
  desde?: string;
  hasta?: string;
}