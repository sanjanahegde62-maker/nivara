// ─── Auth ─────────────────────────────────────────────────────────────────────
export type Role = 'OWNER' | 'MANAGER' | 'TENANT' | 'MAINTENANCE_STAFF';

export interface LoginRequest {
  email: string;
  password: string;
}
export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  phone?: string;
  role: Role;
}
export interface AuthUser {
  id: number;
  name: string;
  email: string;
  role: Role;
  token: string;
}
export interface ChangePasswordRequest {
  newPassword: string;
}

// ─── Property ─────────────────────────────────────────────────────────────────
export interface PropertyRequest {
  name: string;
  address: string;
  propertyType: string;
  description?: string;
}
export interface PropertyResponse {
  id: number;
  name: string;
  address: string;
  propertyType: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

// ─── Unit ─────────────────────────────────────────────────────────────────────
export interface UnitRequest {
  unitNumber: string;
  unitType: string;
  monthlyRent: number;
  status: string;
}
export interface UnitResponse {
  id: number;
  propertyId: number;
  unitNumber: string;
  unitType: string;
  monthlyRent: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

// ─── Application ──────────────────────────────────────────────────────────────
export interface ApplicationResponse {
  id: number;
  unitId: number;
  unitNumber: string;
  tenantId: number;
  tenantName: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  appliedAt: string;
  reviewedAt?: string;
}

// ─── Lease ────────────────────────────────────────────────────────────────────
export interface LeaseResponse {
  id: number;
  tenantId: number;
  tenantName: string;
  unitId: number;
  unitNumber: string;
  propertyId: number;
  propertyName: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

// ─── Billing ──────────────────────────────────────────────────────────────────
export interface RentChargeResponse {
  id: number;
  leaseId: number;
  billingPeriod: string;
  amount: number;
  dueDate: string;
  status: 'PENDING' | 'OVERDUE' | 'PAID';
  createdAt: string;
  updatedAt: string;
}
export interface LeaseChargesSummary {
  leaseId: number;
  totalCharged: number;
  totalPaid: number;
  outstandingBalance: number;
  charges: RentChargeResponse[];
}
export interface LedgerEntryResponse {
  id: number;
  leaseId: number;
  entryType: 'RENT_CHARGE' | 'PAYMENT' | 'LATE_FEE' | 'LEASE_EXPIRY_NOTICE';
  amount: number;
  description: string;
  rentChargeId?: number;
  paymentId?: number;
  createdAt: string;
}
export interface PaymentRequest {
  rentChargeId: number;
  amount: number;
  paymentMethod: string;
  reference?: string;
}
export interface PaymentResponse {
  id: number;
  rentChargeId: number;
  leaseId: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  status: 'COMPLETED' | 'REFUNDED';
  reference?: string;
  createdAt: string;
}

// ─── Maintenance ──────────────────────────────────────────────────────────────
export interface MaintenanceResponse {
  id: number;
  unitId: number;
  tenantId: number;
  assignedStaffId?: number;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  dueAt: string;
  slaBreached: boolean;
  resolvedAt?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
}

// ─── Reports ──────────────────────────────────────────────────────────────────
export interface IncomeReportRow {
  propertyId: number;
  propertyName: string;
  propertyAddress: string;
  from?: string;
  to?: string;
  totalIncome: number;
  paymentCount: number;
}
export interface ExpenseReportRow {
  propertyId: number;
  propertyName: string;
  propertyAddress: string;
  from?: string;
  to?: string;
  totalLateFees: number;
  lateFeeCount: number;
}
