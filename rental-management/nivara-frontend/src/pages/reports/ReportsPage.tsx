import { useEffect, useState } from 'react';
import { reportService } from '../../services/reportService';
import type { IncomeReportRow, ExpenseReportRow } from '../../types/api';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ErrorMessage from '../../components/ui/ErrorMessage';
import EmptyState from '../../components/ui/EmptyState';
import { BarChart2, TrendingUp, TrendingDown, Calendar } from 'lucide-react';

export default function ReportsPage() {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [income, setIncome] = useState<IncomeReportRow[]>([]);
  const [expenses, setExpenses] = useState<ExpenseReportRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  function load() {
    setLoading(true);
    setError('');
    Promise.all([
      reportService.income(from || undefined, to || undefined),
      reportService.expenses(from || undefined, to || undefined),
    ])
      .then(([i, e]) => { setIncome(i); setExpenses(e); })
      .catch(() => setError('Failed to load reports.'))
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, []);

  const totalIncome = income.reduce((s, r) => s + Number(r.totalIncome), 0);
  const totalExpenses = expenses.reduce((s, r) => s + Number(r.totalLateFees), 0);

  return (
    <div>
      <div className="page-header">
        <h1 className="section-title">Reports</h1>
        <p className="section-subtitle">Financial overview of your portfolio</p>
      </div>

      {/* Date filter */}
      <div className="mb-8 card p-5 flex flex-wrap items-end gap-4">
        <div className="flex items-center gap-2 text-warm-700">
          <Calendar size={16} />
          <span className="text-sm font-semibold">Date range</span>
        </div>
        <div>
          <label className="label">From</label>
          <input type="date" className="input-field w-40" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div>
          <label className="label">To</label>
          <input type="date" className="input-field w-40" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <button onClick={load} className="btn-primary">
          <BarChart2 size={16} /> Generate
        </button>
        <button onClick={() => { setFrom(''); setTo(''); }} className="btn-ghost text-sm">
          Clear
        </button>
      </div>

      {loading && <LoadingSpinner />}
      {error && <ErrorMessage message={error} onRetry={load} />}

      {!loading && !error && (
        <>
          {/* Summary */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 mb-8">
            <div className="card p-5 flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-sage-100 text-sage-400 shrink-0">
                <TrendingUp size={24} />
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-warm-700">Total Income</p>
                <p className="mt-0.5 font-display text-2xl font-semibold text-warm-900">
                  ₹{totalIncome.toLocaleString()}
                </p>
                <p className="text-xs text-warm-600">{income.reduce((s, r) => s + r.paymentCount, 0)} payments</p>
              </div>
            </div>
            <div className="card p-5 flex items-center gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-red-50 text-red-400 shrink-0">
                <TrendingDown size={24} />
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-warm-700">Total Late Fees</p>
                <p className="mt-0.5 font-display text-2xl font-semibold text-warm-900">
                  ₹{totalExpenses.toLocaleString()}
                </p>
                <p className="text-xs text-warm-600">{expenses.reduce((s, r) => s + r.lateFeeCount, 0)} incidents</p>
              </div>
            </div>
          </div>

          {/* Income per property */}
          <h2 className="font-display text-lg font-semibold text-warm-900 mb-3">Income by Property</h2>
          {income.length === 0 ? (
            <EmptyState message="No income data for this period." />
          ) : (
            <div className="table-wrapper mb-8">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Property</th>
                    <th>Address</th>
                    <th>Payments</th>
                    <th>Total Income</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {income.map((row) => (
                    <tr key={row.propertyId}>
                      <td className="font-semibold">{row.propertyName}</td>
                      <td className="text-warm-600 text-xs">{row.propertyAddress}</td>
                      <td>{row.paymentCount}</td>
                      <td className="font-semibold text-sage-500">₹{Number(row.totalIncome).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Late fees per property */}
          <h2 className="font-display text-lg font-semibold text-warm-900 mb-3">Late Fees by Property</h2>
          {expenses.length === 0 ? (
            <EmptyState message="No late fee data for this period." />
          ) : (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Property</th>
                    <th>Address</th>
                    <th>Incidents</th>
                    <th>Total Late Fees</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {expenses.map((row) => (
                    <tr key={row.propertyId}>
                      <td className="font-semibold">{row.propertyName}</td>
                      <td className="text-warm-600 text-xs">{row.propertyAddress}</td>
                      <td>{row.lateFeeCount}</td>
                      <td className="font-semibold text-red-500">₹{Number(row.totalLateFees).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </div>
  );
}
