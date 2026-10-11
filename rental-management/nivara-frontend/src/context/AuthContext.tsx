import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import type { AuthUser, LoginRequest, RegisterRequest } from '../types/api';
import { authService } from '../services/authService';

interface AuthContextValue {
  user: AuthUser | null;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function loadUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem('nivara_user');
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(loadUser);

  const login = useCallback(async (data: LoginRequest) => {
    const authUser = await authService.login(data);
    localStorage.setItem('nivara_token', authUser.token);
    localStorage.setItem('nivara_user', JSON.stringify(authUser));
    setUser(authUser);
  }, []);

  const register = useCallback(async (data: RegisterRequest) => {
    const authUser = await authService.register(data);
    localStorage.setItem('nivara_token', authUser.token);
    localStorage.setItem('nivara_user', JSON.stringify(authUser));
    setUser(authUser);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('nivara_token');
    localStorage.removeItem('nivara_user');
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, register, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
