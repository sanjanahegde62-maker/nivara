import api from '../lib/api';
import type { IncomeReportRow, ExpenseReportRow } from '../types/api';

export const reportService = {
  income: (from?: string, to?: string) =>
    api.get<IncomeReportRow[]>('/reports/income', { params: { from, to } }).then((r) => r.data),

  expenses: (from?: string, to?: string) =>
    api.get<ExpenseReportRow[]>('/reports/expenses', { params: { from, to } }).then((r) => r.data),
};
