import api from '../lib/api';
import type { AuthUser, LoginRequest, RegisterRequest, ChangePasswordRequest } from '../types/api';

export const authService = {
  login: (data: LoginRequest) =>
    api.post<AuthUser>('/auth/login', data).then((r) => r.data),

  register: (data: RegisterRequest) =>
    api.post<AuthUser>('/auth/register', data).then((r) => r.data),

  changePassword: (data: ChangePasswordRequest) =>
    api.post<void>('/auth/change-password', data),
};
