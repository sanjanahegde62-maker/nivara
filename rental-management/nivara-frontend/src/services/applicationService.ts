import api from '../lib/api';
import type { ApplicationResponse } from '../types/api';

export const applicationService = {
  /** TENANT: apply to a unit */
  apply: (unitId: number) =>
    api.post<ApplicationResponse>(`/applications/units/${unitId}`).then((r) => r.data),

  /** TENANT: list own applications */
  myApplications: () =>
    api.get<ApplicationResponse[]>('/applications/my').then((r) => r.data),

  /** OWNER / MANAGER: list applications for their properties */
  propertyApplications: () =>
    api.get<ApplicationResponse[]>('/applications/property').then((r) => r.data),

  /** OWNER / MANAGER: approve or reject */
  review: (applicationId: number, decision: 'APPROVED' | 'REJECTED') =>
    api
      .put<ApplicationResponse>(`/applications/${applicationId}/review`, null, {
        params: { decision },
      })
      .then((r) => r.data),
};
