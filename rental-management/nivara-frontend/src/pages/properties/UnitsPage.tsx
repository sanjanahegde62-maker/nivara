import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { unitService } from '../../services/propertyService';
import type { UnitResponse } from '../../types/api';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ErrorMessage from '../../components/ui/ErrorMessage';
import EmptyState from '../../components/ui/EmptyState';
import UnitFormModal from './UnitFormModal';
import { ArrowLeft, Plus, Home, DollarSign } from 'lucide-react';
import clsx from 'clsx';

const STATUS_COLORS: Record<string, string> = {
  VACANT: 'bg-sage-100 text-sage-500',
  OCCUPIED: 'bg-terracotta-100 text-terracotta-600',
  MAINTENANCE: 'bg-amber-50 text-amber-700',
};

export default function UnitsPage() {
  const { propertyId } = useParams<{ propertyId: string }>();
  const pid = Number(propertyId);
  const [units, setUnits] = useState<UnitResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<UnitResponse | null>(null);

  function load() {
    setLoading(true);
    unitService
      .list(pid)
      .then(setUnits)
      .catch(() => setError('Failed to load units.'))
      .finally(() => setLoading(false));
  }
  useEffect(() => { load(); }, [pid]);

  async function handleDelete(unitId: number) {
    if (!confirm('Delete this unit?')) return;
    await unitService.delete(pid, unitId);
    setUnits((prev) => prev.filter((u) => u.id !== unitId));
  }

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link to="/properties" className="btn-ghost p-2">
            <ArrowLeft size={18} />
          </Link>
          <div>
            <h1 className="section-title">Units</h1>
            <p className="section-subtitle">Property #{pid}</p>
          </div>
        </div>
        <button onClick={() => { setEditing(null); setShowModal(true); }} className="btn-primary">
          <Plus size={16} /> Add unit
        </button>
      </div>

      {loading && <LoadingSpinner />}
      {error && <ErrorMessage message={error} onRetry={load} />}
      {!loading && !error && units.length === 0 && (
        <EmptyState message="No units yet. Add the first unit for this property." />
      )}

      {!loading && !error && units.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {units.map((u) => (
            <div key={u.id} className="card p-5 hover:shadow-card-hover transition-shadow">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-ivory-200 text-warm-600">
                    <Home size={18} />
                  </div>
                  <div>
                    <p className="font-semibold text-warm-900">Unit {u.unitNumber}</p>
                    <p className="text-xs text-warm-600 capitalize">{u.unitType}</p>
                  </div>
                </div>
                <span
                  className={clsx(
                    'rounded-full px-2.5 py-0.5 text-xs font-semibold',
                    STATUS_COLORS[u.status] ?? 'bg-ivory-200 text-warm-700'
                  )}
                >
                  {u.status}
                </span>
              </div>
              <div className="mt-4 flex items-center gap-1.5 text-lg font-display font-semibold text-warm-900">
                <DollarSign size={16} className="text-terracotta-500" />
                ₹{Number(u.monthlyRent).toLocaleString()}
                <span className="text-sm font-normal text-warm-600">/mo</span>
              </div>
              <div className="mt-3 flex gap-2 border-t border-border pt-3">
                <button
                  onClick={() => { setEditing(u); setShowModal(true); }}
                  className="btn-ghost text-xs flex-1"
                >
                  Edit
                </button>
                <button
                  onClick={() => handleDelete(u.id)}
                  className="flex-1 rounded-xl px-3 py-1.5 text-xs font-medium text-red-500 hover:bg-red-50 transition-colors"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <UnitFormModal
          propertyId={pid}
          existing={editing}
          onClose={() => setShowModal(false)}
          onSaved={() => { setShowModal(false); load(); }}
        />
      )}
    </div>
  );
}
