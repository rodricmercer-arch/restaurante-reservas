/** GET /api/admin/dashboard (DashboardDTO). */
export interface Dashboard {
  nombre: string;
  totalMesas: number;
  totalReservas: number;
}