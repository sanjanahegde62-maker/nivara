import api from '../lib/api';
import type { MaintenanceResponse } from '../types/api';

export const maintenanceService = {
  /** TENANT: create a maintenance request */
  create: (unitId: number, description: string, priority: 'HIGH' | 'MEDIUM' | 'LOW') =>
    api
      .post<MaintenanceResponse>(`/maintenance-requests/units/${unitId}`, null, {
        params: { description, priority },
      })
      .then((r) => r.data),

  /** Role-aware list */
  list: () =>
    api.get<MaintenanceResponse[]>('/maintenance-requests').then((r) => r.data),

  get: (id: number) =>
    api.get<MaintenanceResponse>(`/maintenance-requests/${id}`).then((r) => r.data),

  /** OWNER / MANAGER: assign staff */
  assign: (id: number, staffId: number) =>
    api
      .patch<MaintenanceResponse>(`/maintenance-requests/${id}/assign`, null, {
        params: { staffId },
      })
      .then((r) => r.data),

  /** OWNER / MANAGER / STAFF: advance status */
  updateStatus: (id: number, status: string) =>
    api
      .patch<MaintenanceResponse>(`/maintenance-requests/${id}/status`, null, {
        params: { status },
      })
      .then((r) => r.data),
};
