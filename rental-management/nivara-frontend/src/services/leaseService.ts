import api from '../lib/api';
import type { LeaseResponse } from '../types/api';

export const leaseService = {
  /** Role-aware list: TENANT → own; OWNER/MANAGER → their properties */
  list: () =>
    api.get<LeaseResponse[]>('/leases').then((r) => r.data),

  get: (leaseId: number) =>
    api.get<LeaseResponse>(`/leases/${leaseId}`).then((r) => r.data),

  /** OWNER / MANAGER: create lease from approved application */
  create: (applicationId: number, startDate: string, endDate: string) =>
    api
      .post<LeaseResponse>(`/leases/applications/${applicationId}`, null, {
        params: { startDate, endDate },
      })
      .then((r) => r.data),

  /** Download lease PDF as blob */
  downloadDocument: (leaseId: number) =>
    api.get(`/leases/${leaseId}/document`, { responseType: 'blob' }).then((r) => r.data as Blob),
};
