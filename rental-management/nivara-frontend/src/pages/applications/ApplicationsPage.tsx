import { useEffect, useState } from 'react';
import { applicationService } from '../../services/applicationService';
import type { ApplicationResponse } from '../../types/api';
import { useAuth } from '../../context/AuthContext';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ErrorMessage from '../../components/ui/ErrorMessage';
import EmptyState from '../../components/ui/EmptyState';
import StatusBadge, { statusForApplication } from '../../components/ui/StatusBadge';
import { format } from 'date-fns';
import { CheckCircle, XCircle, FileText } from 'lucide-react';
import CreateLeaseModal from '../leases/CreateLeaseModal';

export default function ApplicationsPage() {
  const { user } = useAuth();
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionPending, setActionPending] = useState<number | null>(null);
  const [leaseApp, setLeaseApp] = useState<ApplicationResponse | null>(null);

  function load() {
    setLoading(true);
    const promise =
      user?.role === 'TENANT'
        ? applicationService.myApplications()
        : applicationService.propertyApplications();
    promise
      .then(setApplications)
      .catch(() => setError('Failed to load applications.'))
      .finally(() => setLoading(false));
  }
  useEffect(() => { load(); }, [user]);

  async function handleReview(id: number, decision: 'APPROVED' | 'REJECTED') {
    setActionPending(id);
    try {
      const updated = await applicationService.review(id, decision);
      setApplications((prev) => prev.map((a) => (a.id === id ? updated : a)));
    } catch {
      alert('Action failed. Please try again.');
    } finally {
      setActionPending(null);
    }
  }

  const isOwnerOrManager = user?.role === 'OWNER' || user?.role === 'MANAGER';

  return (
    <div>
      <div className="page-header">
        <h1 className="section-title">Applications</h1>
        <p className="section-subtitle">
          {user?.role === 'TENANT'
            ? 'Track your rental applications'
            : 'Review tenant applications for your properties'}
        </p>
      </div>

      {loading && <LoadingSpinner />}
      {error && <ErrorMessage message={error} onRetry={load} />}
      {!loading && !error && applications.length === 0 && (
        <EmptyState message="No applications found." />
      )}

      {!loading && !error && applications.length > 0 && (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Tenant</th>
                <th>Unit</th>
                <th>Applied</th>
                <th>Status</th>
                {isOwnerOrManager && <th>Actions</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {applications.map((app) => (
                <tr key={app.id}>
                  <td className="font-medium">{app.tenantName}</td>
                  <td>
                    <span className="text-warm-600">Unit</span>{' '}
                    <span className="font-medium">{app.unitNumber}</span>
                    <span className="ml-1.5 text-xs text-warm-400">#{app.unitId}</span>
                  </td>
                  <td className="text-warm-600 text-xs">
                    {format(new Date(app.appliedAt), 'MMM d, yyyy')}
                  </td>
                  <td>
                    <StatusBadge label={app.status} variant={statusForApplication(app.status)} />
                  </td>
                  {isOwnerOrManager && (
                    <td>
                      <div className="flex items-center gap-2">
                        {app.status === 'PENDING' && (
                          <>
                            <button
                              onClick={() => handleReview(app.id, 'APPROVED')}
                              disabled={actionPending === app.id}
                              className="flex items-center gap-1 rounded-lg bg-sage-100 px-2.5 py-1.5 text-xs font-semibold text-sage-500 hover:bg-sage-200 transition-colors disabled:opacity-50"
                            >
                              <CheckCircle size={13} /> Approve
                            </button>
                            <button
                              onClick={() => handleReview(app.id, 'REJECTED')}
                              disabled={actionPending === app.id}
                              className="flex items-center gap-1 rounded-lg bg-red-50 px-2.5 py-1.5 text-xs font-semibold text-red-600 hover:bg-red-100 transition-colors disabled:opacity-50"
                            >
                              <XCircle size={13} /> Reject
                            </button>
                          </>
                        )}
                        {app.status === 'APPROVED' && (
                          <button
                            onClick={() => setLeaseApp(app)}
                            className="flex items-center gap-1 rounded-lg bg-terracotta-100 px-2.5 py-1.5 text-xs font-semibold text-terracotta-600 hover:bg-terracotta-200 transition-colors"
                          >
                            <FileText size={13} /> Create lease
                          </button>
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {leaseApp && (
        <CreateLeaseModal
          application={leaseApp}
          onClose={() => setLeaseApp(null)}
          onCreated={() => { setLeaseApp(null); load(); }}
        />
      )}
    </div>
  );
}
