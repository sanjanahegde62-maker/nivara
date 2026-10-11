import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { propertyService } from '../../services/propertyService';
import type { PropertyResponse } from '../../types/api';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ErrorMessage from '../../components/ui/ErrorMessage';
import EmptyState from '../../components/ui/EmptyState';
import PropertyFormModal from './PropertyFormModal';
import { Plus, MapPin, Building2, ChevronRight } from 'lucide-react';

export default function PropertiesPage() {
  const [properties, setProperties] = useState<PropertyResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState<PropertyResponse | null>(null);

  function load() {
    setLoading(true);
    setError('');
    propertyService
      .list()
      .then(setProperties)
      .catch(() => setError('Failed to load properties.'))
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, []);

  function openCreate() {
    setEditing(null);
    setShowModal(true);
  }
  function openEdit(p: PropertyResponse) {
    setEditing(p);
    setShowModal(true);
  }
  async function handleDelete(id: number) {
    if (!confirm('Delete this property?')) return;
    await propertyService.delete(id);
    setProperties((prev) => prev.filter((p) => p.id !== id));
  }

  return (
    <div>
      <div className="page-header flex items-center justify-between">
        <div>
          <h1 className="section-title">Properties</h1>
          <p className="section-subtitle">Manage your property portfolio</p>
        </div>
        <button onClick={openCreate} className="btn-primary">
          <Plus size={16} /> Add property
        </button>
      </div>

      {loading && <LoadingSpinner />}
      {error && <ErrorMessage message={error} onRetry={load} />}
      {!loading && !error && properties.length === 0 && (
        <EmptyState message="No properties yet. Add your first property to get started." />
      )}

      {!loading && !error && properties.length > 0 && (
        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {properties.map((p) => (
            <div key={p.id} className="card overflow-hidden group hover:shadow-card-hover transition-shadow">
              {/* Color band */}
              <div className="h-2 bg-gradient-to-r from-terracotta-400 to-terracotta-500" />
              <div className="p-5">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-terracotta-100 text-terracotta-500">
                      <Building2 size={20} />
                    </div>
                    <div>
                      <h3 className="font-semibold text-warm-900 line-clamp-1">{p.name}</h3>
                      <span className="text-xs text-warm-600 capitalize">{p.propertyType}</span>
                    </div>
                  </div>
                </div>

                <div className="mt-3 flex items-start gap-1.5 text-xs text-warm-600">
                  <MapPin size={13} className="mt-0.5 shrink-0 text-warm-400" />
                  <span className="line-clamp-2">{p.address}</span>
                </div>

                {p.description && (
                  <p className="mt-2 text-xs text-warm-600 line-clamp-2">{p.description}</p>
                )}

                <div className="mt-4 flex items-center gap-2 border-t border-border pt-4">
                  <Link
                    to={`/properties/${p.id}`}
                    className="btn-secondary flex-1 py-1.5 text-xs"
                  >
                    View details <ChevronRight size={13} />
                  </Link>
                  <button
                    onClick={() => openEdit(p)}
                    className="btn-ghost text-xs px-3 py-1.5"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(p.id)}
                    className="rounded-xl px-3 py-1.5 text-xs font-medium text-red-500 hover:bg-red-50 transition-colors"
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <PropertyFormModal
          existing={editing}
          onClose={() => setShowModal(false)}
          onSaved={() => { setShowModal(false); load(); }}
        />
      )}
    </div>
  );
}
