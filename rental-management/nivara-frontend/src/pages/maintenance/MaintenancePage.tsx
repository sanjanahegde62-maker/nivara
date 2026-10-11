import { useEffect, useState } from 'react';
import { maintenanceService } from '../../services/maintenanceService';
import type { MaintenanceResponse } from '../../types/api';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ErrorMessage from '../../components/ui/ErrorMessage';
import EmptyState from '../../components/ui/EmptyState';
import StatusBadge, { statusForMaintenance } from '../../components/ui/StatusBadge';
import { format } from 'date-fns';
import { Plus, AlertTriangle } from 'lucide-react';
import CreateMaintenanceModal from './CreateMaintenanceModal';
import clsx from 'clsx';

const STATUS_FLOW = ['OPEN', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

const PRIORITY_COLORS: Record<string, string> = {
  HIGH: 'bg-red-100 text-red-600',
  MEDIUM: 'bg-amber-50 text-amber-700',
  LOW: 'bg-ivory-200 text-warm-600',
};

export default function MaintenancePage() {
  const { user } = useAuth();
  const [requests, setRequests] = useState<MaintenanceResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [advancing, setAdvancing] = useState<number | null>(null);

  function load() {
    setLoading(true);
    maintenanceService
      .list()
      .then(setRequests)
      .catch(() => setError('Failed to load maintenance requests.'))
      .finally(() => setLoading(false));
  }
  useEffect(() => { load(); }, []);

  async function handleAdvance(req: MaintenanceResponse) {
    const idx = STATUS_FLOW.indexOf(req.status);
    if (idx < 0 || idx >= STATUS_FLOW.length - 1) return;
    const next = STATUS_FLOW[idx + 1];
    setAdvancing(req.id);
    try {
      const updated = await maintenanceService.updateStatus(req.id, next);
      setRequests((prev) => prev.map((r) => (r.id === req.id ? updated : r)));
    } catch {
      alert('Failed to update status.');
    } finally {
      setAdvancing(null);
    }
  }

  const canAdvance = user?.role === 'OWNER' || user?.role === 'MANAGER' || user?.role === 'MAINTENANCE_STAFF';
  const canCreate = user?.role === 'TENANT';

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="section-title">Maintenance Requests</h1>
          <p className="section-subtitle">Track and manage property maintenance</p>
        </div>
        {canCreate && (
          <button onClick={() => setShowCreate(true)} className="btn-primary">
            <Plus size={16} /> New request
          </button>
        )}
      </div>

      {loading && <LoadingSpinner />}
      {error && <ErrorMessage message={error} onRetry={load} />}
      {!loading && !error && requests.length === 0 && (
        <EmptyState message="No maintenance requests." />
      )}

      {!loading && !error && requests.length > 0 && (
        <div className="space-y-3">
          {requests.map((req) => (
            <div
              key={req.id}
              className={clsx(
                'card p-5 hover:shadow-card-hover transition-shadow',
                req.slaBreached && 'border-red-200'
              )}
            >
              <div className="flex items-start gap-4">
                <div
                  className={clsx(
                    'mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl',
                    PRIORITY_COLORS[req.priority] ?? 'bg-ivory-200 text-warm-600'
                  )}
                >
                  <AlertTriangle size={16} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-semibold text-warm-900 line-clamp-1">{req.description}</p>
                    {req.slaBreached && (
                      <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-semibold text-red-600">
                        SLA Breached
                      </span>
                    )}
                  </div>
                  <div className="mt-1 flex flex-wrap gap-x-3 gap-y-0.5 text-xs text-warm-600">
                    <span>Unit #{req.unitId}</span>
                    <span className={clsx('font-semibold', PRIORITY_COLORS[req.priority])}>
                      {req.priority}
                    </span>
                    <span>Created {format(new Date(req.createdAt), 'MMM d, yyyy')}</span>
                    {req.dueAt && (
                      <span>Due {format(new Date(req.dueAt), 'MMM d, yyyy')}</span>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <StatusBadge label={req.status} variant={statusForMaintenance(req.status)} />
                  {canAdvance && req.status !== 'CLOSED' && (
                    <button
                      onClick={() => handleAdvance(req)}
                      disabled={advancing === req.id}
                      className="rounded-lg bg-terracotta-100 px-2.5 py-1.5 text-xs font-semibold text-terracotta-600 hover:bg-terracotta-200 transition-colors disabled:opacity-50"
                    >
                      {advancing === req.id ? '…' : '→ Advance'}
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <CreateMaintenanceModal
          onClose={() => setShowCreate(false)}
          onCreated={() => { setShowCreate(false); load(); }}
        />
      )}
    </div>
  );
}
