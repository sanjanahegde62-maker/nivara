import api from '../lib/api';
import type {
  ManagerAssignmentResponse,
  PropertyDetailResponse,
  PropertyRequest,
  PropertyResponse,
  UnitRequest,
  UnitResponse,
  UserSummaryResponse,
} from '../types/api';

export const propertyService = {
  list: () =>
    api.get<PropertyResponse[]>('/properties').then((r) => r.data),

  /** Returns PropertyDetailResponse (includes units, managers, stat counts) */
  get: (id: number) =>
    api.get<PropertyDetailResponse>(`/properties/${id}`).then((r) => r.data),

  create: (data: PropertyRequest) =>
    api.post<PropertyResponse>('/properties', data).then((r) => r.data),

  update: (id: number, data: PropertyRequest) =>
    api.put<PropertyResponse>(`/properties/${id}`, data).then((r) => r.data),

  delete: (id: number) =>
    api.delete(`/properties/${id}`),

  listManagers: (propertyId: number) =>
    api.get<ManagerAssignmentResponse[]>(`/properties/${propertyId}/managers`).then((r) => r.data),

  assignManager: (propertyId: number, managerId: number) =>
    api.post(`/properties/${propertyId}/managers/${managerId}`),

  removeManager: (propertyId: number, managerId: number) =>
    api.delete(`/properties/${propertyId}/managers/${managerId}`),
};

export const unitService = {
  list: (propertyId: number) =>
    api.get<UnitResponse[]>(`/properties/${propertyId}/units`).then((r) => r.data),

  create: (propertyId: number, data: UnitRequest) =>
    api.post<UnitResponse>(`/properties/${propertyId}/units`, data).then((r) => r.data),

  update: (propertyId: number, unitId: number, data: UnitRequest) =>
    api.put<UnitResponse>(`/properties/${propertyId}/units/${unitId}`, data).then((r) => r.data),

  delete: (propertyId: number, unitId: number) =>
    api.delete(`/properties/${propertyId}/units/${unitId}`),
};

export const userDirectoryService = {
  listManagers: () =>
    api.get<UserSummaryResponse[]>('/users?role=MANAGER').then((r) => r.data),
};
