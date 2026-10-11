import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { leaseService } from '../../services/leaseService';
import type { LeaseResponse } from '../../types/api';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import EmptyState from '../../components/ui/EmptyState';
import { CreditCard, ArrowRight } from 'lucide-react';

export default function BillingLandingPage() {
  const [leases, setLeases] = useState<LeaseResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    leaseService.list().then(setLeases).finally(() => setLoading(false));
  }, []);

  if (loading) return <LoadingSpinner />;

  return (
    <div>
      <div className="page-header">
        <h1 className="section-title">Billing</h1>
        <p className="section-subtitle">Select a lease to view billing details</p>
      </div>

      {leases.length === 0 ? (
        <EmptyState message="No leases found." />
      ) : (
        <div className="space-y-3">
          {leases.map((lease) => (
            <Link
              key={lease.id}
              to={`/billing/${lease.id}`}
              className="card flex items-center gap-5 p-5 hover:shadow-card-hover transition-shadow group"
            >
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-terracotta-100 text-terracotta-500">
                <CreditCard size={20} />
              </div>
              <div className="flex-1 min-w-0">
                <p className="font-semibold text-warm-900">{lease.tenantName}</p>
                <p className="text-sm text-warm-600">
                  {lease.propertyName} · Unit {lease.unitNumber}
                </p>
              </div>
              <div className="shrink-0 text-right">
                <p className="font-semibold text-warm-900">₹{Number(lease.monthlyRent).toLocaleString()}/mo</p>
                <p className="text-xs text-warm-600">{lease.status}</p>
              </div>
              <ArrowRight size={16} className="text-warm-300 group-hover:text-terracotta-400 transition-colors shrink-0" />
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
