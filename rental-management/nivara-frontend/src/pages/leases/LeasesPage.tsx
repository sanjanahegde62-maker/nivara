import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { leaseService } from '../../services/leaseService';
import type { LeaseResponse } from '../../types/api';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ErrorMessage from '../../components/ui/ErrorMessage';
import EmptyState from '../../components/ui/EmptyState';
import StatusBadge, { statusForLease } from '../../components/ui/StatusBadge';
import { format } from 'date-fns';
import { Download, CreditCard } from 'lucide-react';

export default function LeasesPage() {
  const [leases, setLeases] = useState<LeaseResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  function load() {
    setLoading(true);
    leaseService
      .list()
      .then(setLeases)
      .catch(() => setError('Failed to load leases.'))
      .finally(() => setLoading(false));
  }
  useEffect(() => { load(); }, []);

  async function handleDownload(leaseId: number) {
    try {
      const blob = await leaseService.downloadDocument(leaseId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `lease-${leaseId}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      alert('Failed to download PDF.');
    }
  }

  return (
    <div>
      <div className="page-header">
        <h1 className="section-title">Leases</h1>
        <p className="section-subtitle">View and manage lease agreements</p>
      </div>

      {loading && <LoadingSpinner />}
      {error && <ErrorMessage message={error} onRetry={load} />}
      {!loading && !error && leases.length === 0 && (
        <EmptyState message="No leases found." />
      )}

      {!loading && !error && leases.length > 0 && (
        <div className="space-y-4">
          {leases.map((lease) => (
            <div key={lease.id} className="card p-5 hover:shadow-card-hover transition-shadow">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-warm-900">{lease.tenantName}</h3>
                    <StatusBadge label={lease.status} variant={statusForLease(lease.status)} />
                  </div>
                  <p className="mt-0.5 text-sm text-warm-700">
                    <span className="font-medium">{lease.propertyName}</span>
                    {' · '}Unit {lease.unitNumber}
                  </p>
                  <div className="mt-1.5 flex flex-wrap gap-x-4 gap-y-1 text-xs text-warm-600">
                    <span>
                      {format(new Date(lease.startDate), 'MMM d, yyyy')} →{' '}
                      {format(new Date(lease.endDate), 'MMM d, yyyy')}
                    </span>
                    <span className="font-semibold text-warm-900">
                      ₹{Number(lease.monthlyRent).toLocaleString()}/mo
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <Link
                    to={`/billing/${lease.id}`}
                    className="btn-ghost text-xs flex items-center gap-1.5"
                  >
                    <CreditCard size={14} /> Billing
                  </Link>
                  <button
                    onClick={() => handleDownload(lease.id)}
                    className="btn-secondary text-xs flex items-center gap-1.5 py-1.5"
                  >
                    <Download size={14} /> PDF
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
