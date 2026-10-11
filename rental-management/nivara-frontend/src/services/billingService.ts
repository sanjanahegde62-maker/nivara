import api from '../lib/api';
import type {
  LeaseChargesSummary,
  LedgerEntryResponse,
  PaymentRequest,
  PaymentResponse,
  RentChargeResponse,
} from '../types/api';

export const billingService = {
  /** OWNER / MANAGER: create a rent charge for a billing month (yyyy-MM) */
  createCharge: (leaseId: number, billingMonth: string) =>
    api
      .post<RentChargeResponse>(`/leases/${leaseId}/charges`, null, {
        params: { billingMonth },
      })
      .then((r) => r.data),

  /** All roles: get charges + balance summary */
  getCharges: (leaseId: number) =>
    api.get<LeaseChargesSummary>(`/leases/${leaseId}/charges`).then((r) => r.data),

  /** All roles: full ledger */
  getLedger: (leaseId: number) =>
    api.get<LedgerEntryResponse[]>(`/leases/${leaseId}/ledger`).then((r) => r.data),

  /** TENANT: record a payment */
  recordPayment: (data: PaymentRequest) =>
    api.post<PaymentResponse>('/payments', data).then((r) => r.data),

  getPayment: (paymentId: number) =>
    api.get<PaymentResponse>(`/payments/${paymentId}`).then((r) => r.data),
};
