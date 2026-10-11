import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { billingService } from '../../services/billingService';
import type { LeaseChargesSummary, LedgerEntryResponse } from '../../types/api';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ErrorMessage from '../../components/ui/ErrorMessage';
import StatusBadge, { statusForCharge } from '../../components/ui/StatusBadge';
import { format } from 'date-fns';
import { ArrowLeft, Plus, TrendingDown, TrendingUp, Wallet } from 'lucide-react';
import CreateChargeModal from './CreateChargeModal';
import RecordPaymentModal from './RecordPaymentModal';

export default function BillingPage() {
  const { leaseId } = useParams<{ leaseId: string }>();
  const lid = Number(leaseId);
  const { user } = useAuth();
  const [summary, setSummary] = useState<LeaseChargesSummary | null>(null);
  const [ledger, setLedger] = useState<LedgerEntryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCharge, setShowCharge] = useState(false);
  const [chargeIdForPayment, setChargeIdForPayment] = useState<number | null>(null);

  function load() {
    setLoading(true);
    Promise.all([
      billingService.getCharges(lid),
      billingService.getLedger(lid),
    ])
      .then(([s, l]) => { setSummary(s); setLedger(l); })
      .catch(() => setError('Failed to load billing data.'))
      .finally(() => setLoading(false));
  }
  useEffect(() => { load(); }, [lid]);

  const isOwnerOrManager = user?.role === 'OWNER' || user?.role === 'MANAGER';
  const isTenant = user?.role === 'TENANT';

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} onRetry={load} />;

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link to="/leases" className="btn-ghost p-2"><ArrowLeft size={18} /></Link>
          <div>
            <h1 className="section-title">Billing</h1>
            <p className="section-subtitle">Lease #{lid}</p>
          </div>
        </div>
        {isOwnerOrManager && (
          <button onClick={() => setShowCharge(true)} className="btn-primary">
            <Plus size={16} /> Create charge
          </button>
        )}
      </div>

      {/* Summary cards */}
      {summary && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3 mb-8">
          <div className="card p-5 flex items-center gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-terracotta-100 text-terracotta-500">
              <TrendingDown size={20} />
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-warm-700">Total Charged</p>
              <p className="mt-0.5 font-display text-xl font-semibold text-warm-900">
                ₹{Number(summary.totalCharged).toLocaleString()}
              </p>
            </div>
          </div>
          <div className="card p-5 flex items-center gap-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-sage-100 text-sage-400">
              <TrendingUp size={20} />
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-warm-700">Total Paid</p>
              <p className="mt-0.5 font-display text-xl font-semibold text-warm-900">
                ₹{Number(summary.totalPaid).toLocaleString()}
              </p>
            </div>
          </div>
          <div className={`card p-5 flex items-center gap-4 ${Number(summary.outstandingBalance) > 0 ? 'border-red-200' : ''}`}>
            <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${Number(summary.outstandingBalance) > 0 ? 'bg-red-50 text-red-500' : 'bg-ivory-200 text-warm-600'}`}>
              <Wallet size={20} />
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-warm-700">Outstanding</p>
              <p className={`mt-0.5 font-display text-xl font-semibold ${Number(summary.outstandingBalance) > 0 ? 'text-red-600' : 'text-warm-900'}`}>
                ₹{Number(summary.outstandingBalance).toLocaleString()}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Charges table */}
      <h2 className="font-display text-lg font-semibold text-warm-900 mb-3">Rent Charges</h2>
      {summary && summary.charges.length > 0 ? (
        <div className="table-wrapper mb-8">
          <table className="data-table">
            <thead>
              <tr>
                <th>Billing Period</th>
                <th>Amount</th>
                <th>Due Date</th>
                <th>Status</th>
                {isTenant && <th>Action</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {summary.charges.map((charge) => (
                <tr key={charge.id}>
                  <td className="font-medium">{charge.billingPeriod}</td>
                  <td>₹{Number(charge.amount).toLocaleString()}</td>
                  <td className="text-xs text-warm-600">{format(new Date(charge.dueDate), 'MMM d, yyyy')}</td>
                  <td><StatusBadge label={charge.status} variant={statusForCharge(charge.status)} /></td>
                  {isTenant && (
                    <td>
                      {charge.status !== 'PAID' && (
                        <button
                          onClick={() => setChargeIdForPayment(charge.id)}
                          className="rounded-lg bg-terracotta-100 px-3 py-1.5 text-xs font-semibold text-terracotta-600 hover:bg-terracotta-200 transition-colors"
                        >
                          Pay now
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="mb-8 text-sm text-warm-600">No charges yet.</p>
      )}

      {/* Ledger */}
      <h2 className="font-display text-lg font-semibold text-warm-900 mb-3">Ledger</h2>
      {ledger.length > 0 ? (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Description</th>
                <th>Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {ledger.map((entry) => (
                <tr key={entry.id}>
                  <td className="text-xs text-warm-600">{format(new Date(entry.createdAt), 'MMM d, yyyy')}</td>
                  <td>
                    <span className={`text-xs font-semibold uppercase ${
                      entry.entryType === 'PAYMENT' ? 'text-sage-500' :
                      entry.entryType === 'LATE_FEE' ? 'text-red-500' :
                      'text-warm-700'
                    }`}>{entry.entryType.replace('_', ' ')}</span>
                  </td>
                  <td className="text-sm text-warm-700">{entry.description}</td>
                  <td className={`font-semibold ${entry.entryType === 'PAYMENT' ? 'text-sage-500' : 'text-warm-900'}`}>
                    {entry.entryType === 'PAYMENT' ? '+' : '-'}₹{Number(entry.amount).toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-warm-600">No ledger entries yet.</p>
      )}

      {showCharge && (
        <CreateChargeModal
          leaseId={lid}
          onClose={() => setShowCharge(false)}
          onCreated={() => { setShowCharge(false); load(); }}
        />
      )}
      {chargeIdForPayment !== null && (
        <RecordPaymentModal
          rentChargeId={chargeIdForPayment}
          onClose={() => setChargeIdForPayment(null)}
          onRecorded={() => { setChargeIdForPayment(null); load(); }}
        />
      )}
    </div>
  );
}
