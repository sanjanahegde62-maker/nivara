import { useEffect, useState, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { propertyService, unitService } from '../../services/propertyService';
import type { PropertyDetailResponse, UnitResponse } from '../../types/api';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ErrorMessage from '../../components/ui/ErrorMessage';
import EmptyState from '../../components/ui/EmptyState';
import UnitFormModal from './UnitFormModal';
import ManagerAssignModal from './ManagerAssignModal';
import clsx from 'clsx';
import {
  ArrowLeft,
  Building2,
  MapPin,
  Home,
  DollarSign,
  Plus,
  Users,
  Pencil,
  Trash2,
  CheckCircle,
} from 'lucide-react';

const STATUS_PILL: Record<string, string> = {
  VACANT: 'bg-sage-100 text-sage-500',
  OCCUPIED: 'bg-terracotta-100 text-terracotta-600',
  MAINTENANCE: 'bg-amber-50 text-amber-700',
};

export default function PropertyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const propertyId = Number(id);
  const { user } = useAuth();
  const navigate = useNavigate();

  const [detail, setDetail] = useState<PropertyDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notFound, setNotFound] = useState(false);

  const [showUnitModal, setShowUnitModal] = useState(false);
  const [editingUnit, setEditingUnit] = useState<UnitResponse | null>(null);
  const [showManagerModal, setShowManagerModal] = useState(false);

  const isOwner = user?.role === 'OWNER';

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    setNotFound(false);
    propertyService
      .get(propertyId)
      .then(setDetail)
      .catch((err) => {
        const status = (err as { response?: { status?: number } })?.response?.status;
        if (status === 404 || status === 403) {
          setNotFound(true);
        } else {
          setError('Failed to load property details.');
        }
      })
      .finally(() => setLoading(false));
  }, [propertyId]);

  useEffect(() => { load(); }, [load]);

  async function handleDeleteUnit(unitId: number) {
    if (!confirm('Delete this unit? This cannot be undone.')) return;
    try {
      await unitService.delete(propertyId, unitId);
      load();
    } catch {
      alert('Failed to delete unit.');
    }
  }

  if (loading) return <LoadingSpinner />;

  if (notFound) {
    return (
      <div className="flex flex-col items-center gap-4 py-20 text-center">
        <Building2 size={40} className="text-warm-300" />
        <h2 className="font-display text-xl font-semibold text-warm-900">Property not found</h2>
        <p className="text-sm text-warm-600">This property doesn't exist or you don't have access.</p>
        <Link to="/properties" className="btn-primary mt-2">
          <ArrowLeft size={16} /> Back to properties
        </Link>
      </div>
    );
  }

  if (error) return <ErrorMessage message={error} onRetry={load} />;
  if (!detail) return null;

  return (
    <div>
      {/* Breadcrumb + back */}
      <div className="mb-6 flex items-center gap-2 text-sm text-warm-600">
        <Link to="/properties" className="flex items-center gap-1 hover:text-terracotta-500 transition-colors">
          <ArrowLeft size={14} /> Properties
        </Link>
        <span>/</span>
        <span className="text-warm-900 font-medium truncate">{detail.name}</span>
      </div>

      {/* Header card */}
      <div className="card overflow-hidden mb-6">
        <div className="h-1.5 bg-gradient-to-r from-terracotta-400 to-terracotta-500" />
        <div className="p-6">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="flex items-start gap-4">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-terracotta-100 text-terracotta-500">
                <Building2 size={26} />
              </div>
              <div>
                <h1 className="font-display text-2xl font-semibold text-warm-900">{detail.name}</h1>
                <div className="mt-1 flex items-center gap-1.5 text-sm text-warm-600">
                  <MapPin size={13} className="text-warm-400 shrink-0" />
                  {detail.address}
                </div>
                <span className="mt-1.5 inline-block rounded-full bg-ivory-200 px-2.5 py-0.5 text-xs font-semibold text-warm-700 capitalize">
                  {detail.propertyType.charAt(0) + detail.propertyType.slice(1).toLowerCase()}
                </span>
                {detail.description && (
                  <p className="mt-2 text-sm text-warm-600 max-w-lg">{detail.description}</p>
                )}
              </div>
            </div>

            {isOwner && (
              <div className="flex gap-2 shrink-0">
                <Link
                  to="/properties"
                  onClick={(e) => { e.preventDefault(); navigate('/properties'); }}
                  className="btn-ghost text-sm"
                >
                  <Pencil size={14} /> Edit
                </Link>
              </div>
            )}
          </div>

          {/* Stat row */}
          <div className="mt-5 grid grid-cols-3 gap-3 border-t border-border pt-5">
            <div className="text-center">
              <p className="font-display text-2xl font-semibold text-warm-900">{detail.totalUnits}</p>
              <p className="text-xs font-medium text-warm-600 mt-0.5">Total Units</p>
            </div>
            <div className="text-center border-x border-border">
              <p className="font-display text-2xl font-semibold text-terracotta-500">{detail.occupiedUnits}</p>
              <p className="text-xs font-medium text-warm-600 mt-0.5">Occupied</p>
            </div>
            <div className="text-center">
              <p className="font-display text-2xl font-semibold text-sage-400">{detail.vacantUnits}</p>
              <p className="text-xs font-medium text-warm-600 mt-0.5">Vacant</p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        {/* Units section — takes 2/3 width on large screens */}
        <div className="lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-display text-lg font-semibold text-warm-900">
              Units <span className="ml-1.5 text-sm font-normal text-warm-600">({detail.units.length})</span>
            </h2>
            {isOwner && (
              <button
                onClick={() => { setEditingUnit(null); setShowUnitModal(true); }}
                className="btn-primary py-1.5 text-xs"
              >
                <Plus size={14} /> Add unit
              </button>
            )}
          </div>

          {detail.units.length === 0 ? (
            <EmptyState message="No units yet. Add the first unit." />
          ) : (
            <div className="space-y-3">
              {detail.units.map((u) => (
                <div key={u.id} className="card flex items-center gap-4 px-5 py-4 hover:shadow-card-hover transition-shadow">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-ivory-200 text-warm-600">
                    <Home size={18} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="font-semibold text-warm-900">Unit {u.unitNumber}</p>
                      <span
                        className={clsx(
                          'rounded-full px-2.5 py-0.5 text-xs font-semibold',
                          STATUS_PILL[u.status] ?? 'bg-ivory-200 text-warm-700'
                        )}
                      >
                        {u.status}
                      </span>
                    </div>
                    <p className="text-xs text-warm-600 mt-0.5">{u.unitType}</p>
                  </div>
                  <div className="flex items-center gap-1 font-display font-semibold text-warm-900 shrink-0">
                    <DollarSign size={14} className="text-terracotta-500" />
                    ₹{Number(u.monthlyRent).toLocaleString()}
                    <span className="text-xs font-normal text-warm-600">/mo</span>
                  </div>
                  {isOwner && (
                    <div className="flex gap-1 shrink-0">
                      <button
                        onClick={() => { setEditingUnit(u); setShowUnitModal(true); }}
                        className="btn-ghost p-1.5 text-xs"
                        title="Edit unit"
                      >
                        <Pencil size={14} />
                      </button>
                      <button
                        onClick={() => handleDeleteUnit(u.id)}
                        className="rounded-xl p-1.5 text-red-400 hover:bg-red-50 transition-colors"
                        title="Delete unit"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Managers section — sidebar */}
        <div>
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-display text-lg font-semibold text-warm-900">
              Managers
            </h2>
            {isOwner && (
              <button
                onClick={() => setShowManagerModal(true)}
                className="btn-ghost text-xs flex items-center gap-1"
              >
                <Users size={14} /> Manage
              </button>
            )}
          </div>

          {detail.managers.length === 0 ? (
            <div className="card p-5 text-center">
              <p className="text-sm text-warm-600">No managers assigned.</p>
              {isOwner && (
                <button
                  onClick={() => setShowManagerModal(true)}
                  className="btn-primary mt-3 text-xs py-1.5"
                >
                  <UserPlusIcon /> Assign manager
                </button>
              )}
            </div>
          ) : (
            <div className="space-y-3">
              {detail.managers.map((m) => (
                <div key={m.managerId} className="card flex items-center gap-3 p-4">
                  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-sage-100 text-xs font-bold text-sage-500 uppercase">
                    {m.managerName.slice(0, 2)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-warm-900 truncate">{m.managerName}</p>
                    <p className="text-xs text-warm-600 truncate">{m.managerEmail}</p>
                  </div>
                  <CheckCircle size={16} className="text-sage-400 shrink-0" />
                </div>
              ))}
            </div>
          )}

          {/* Occupancy visual */}
          {detail.totalUnits > 0 && (
            <div className="card mt-4 p-4">
              <p className="text-xs font-semibold uppercase tracking-wide text-warm-700 mb-3">Occupancy</p>
              <div className="flex h-3 w-full overflow-hidden rounded-full bg-ivory-200">
                <div
                  className="h-full rounded-full bg-terracotta-400 transition-all"
                  style={{ width: `${(detail.occupiedUnits / detail.totalUnits) * 100}%` }}
                />
              </div>
              <div className="mt-2 flex justify-between text-xs text-warm-600">
                <span className="flex items-center gap-1">
                  <span className="inline-block h-2 w-2 rounded-full bg-terracotta-400" />
                  {detail.occupiedUnits} occupied
                </span>
                <span className="flex items-center gap-1">
                  <span className="inline-block h-2 w-2 rounded-full bg-ivory-300" />
                  {detail.vacantUnits} vacant
                </span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Unit CRUD modal */}
      {showUnitModal && (
        <UnitFormModal
          propertyId={propertyId}
          existing={editingUnit}
          onClose={() => setShowUnitModal(false)}
          onSaved={() => { setShowUnitModal(false); load(); }}
        />
      )}

      {/* Manager assignment modal — OWNER only */}
      {showManagerModal && isOwner && (
        <ManagerAssignModal
          propertyId={propertyId}
          currentManagers={detail.managers}
          onClose={() => setShowManagerModal(false)}
          onChanged={() => { setShowManagerModal(false); load(); }}
        />
      )}
    </div>
  );
}

// inline icon helper to avoid extra import
function UserPlusIcon() {
  return <Users size={14} className="mr-1" />;
}
