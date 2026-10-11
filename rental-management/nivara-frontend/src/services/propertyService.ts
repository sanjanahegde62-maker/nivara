import api from '../lib/api';
import type { PropertyRequest, PropertyResponse, UnitRequest, UnitResponse } from '../types/api';

export const propertyService = {
  list: () =>
    api.get<PropertyResponse[]>('/properties').then((r) => r.data),

  get: (id: number) =>
    api.get<PropertyResponse>(`/properties/${id}`).then((r) => r.data),

  create: (data: PropertyRequest) =>
    api.post<PropertyResponse>('/properties', data).then((r) => r.data),

  update: (id: number, data: PropertyRequest) =>
    api.put<PropertyResponse>(`/properties/${id}`, data).then((r) => r.data),

  delete: (id: number) =>
    api.delete(`/properties/${id}`),

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
