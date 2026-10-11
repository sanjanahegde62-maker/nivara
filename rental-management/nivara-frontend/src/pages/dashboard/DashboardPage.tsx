import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { leaseService } from '../../services/leaseService';
import { maintenanceService } from '../../services/maintenanceService';
import { propertyService } from '../../services/propertyService';
import { applicationService } from '../../services/applicationService';
import type { LeaseResponse, MaintenanceResponse, PropertyResponse, ApplicationResponse } from '../../types/api';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import StatusBadge, { statusForLease, statusForMaintenance } from '../../components/ui/StatusBadge';
import { Building2, FileText, Wrench, Home, ArrowRight, TrendingUp } from 'lucide-react';
import { format } from 'date-fns';

function StatCard({
  icon,
  label,
  value,
  to,
  accent,
}: {
  icon: React.ReactNode;
  label: string;
  value: number | string;
  to: string;
  accent?: boolean;
}) {
  return (
    <Link
      to={to}
      className="card flex items-center gap-4 p-6 transition-shadow hover:shadow-card-hover group"
    >
      <div
        className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${
          accent ? 'bg-terracotta-100 text-terracotta-500' : 'bg-ivory-200 text-warm-700'
        }`}
      >
        {icon}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-xs font-semibold uppercase tracking-wide text-warm-700">{label}</p>
        <p className="mt-0.5 text-2xl font-display font-semibold text-warm-900">{value}</p>
      </div>
      <ArrowRight size={16} className="text-warm-300 group-hover:text-terracotta-400 transition-colors" />
    </Link>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [leases, setLeases] = useState<LeaseResponse[]>([]);
  const [maintenance, setMaintenance] = useState<MaintenanceResponse[]>([]);
  const [properties, setProperties] = useState<PropertyResponse[]>([]);
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);

  useEffect(() => {
    if (!user) return;
    const role = user.role;

    Promise.allSettled([
      leaseService.list().then(setLeases).catch(() => {}),
      maintenanceService.list().then(setMaintenance).catch(() => {}),
      role === 'OWNER' || role === 'MANAGER'
        ? propertyService.list().then(setProperties).catch(() => {})
        : Promise.resolve(),
      role === 'TENANT'
        ? applicationService.myApplications().then(setApplications).catch(() => {})
        : role === 'OWNER' || role === 'MANAGER'
        ? applicationService.propertyApplications().then(setApplications).catch(() => {})
        : Promise.resolve(),
    ]).finally(() => setLoading(false));
  }, [user]);

  if (loading) return <LoadingSpinner />;

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
  const openMaintenance = maintenance.filter((m) => m.status === 'OPEN' || m.status === 'IN_PROGRESS');

  return (
    <div>
      {/* Header */}
      <div className="page-header">
        <p className="text-sm font-medium text-terracotta-500">{greeting}</p>
        <h1 className="font-display text-3xl font-semibold text-warm-900 mt-1">
          {user?.name?.split(' ')[0]}, welcome to Nivara
        </h1>
        <p className="section-subtitle">Here's what's happening across your portfolio today.</p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {(user?.role === 'OWNER' || user?.role === 'MANAGER') && (
          <StatCard
            icon={<Building2 size={22} />}
            label="Properties"
            value={properties.length}
            to="/properties"
            accent
          />
        )}
        <StatCard
          icon={<FileText size={22} />}
          label={user?.role === 'TENANT' ? 'My Leases' : 'Active Leases'}
          value={leases.filter((l) => l.status === 'ACTIVE').length}
          to="/leases"
        />
        <StatCard
          icon={<Home size={22} />}
          label="Applications"
          value={applications.filter((a) => a.status === 'PENDING').length}
          to="/applications"
        />
        <StatCard
          icon={<Wrench size={22} />}
          label="Open Requests"
          value={openMaintenance.length}
          to="/maintenance"
          accent={openMaintenance.length > 0}
        />
      </div>

      {/* Recent Leases */}
      <section className="mt-8">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-display text-lg font-semibold text-warm-900">
            {user?.role === 'TENANT' ? 'My Leases' : 'Recent Leases'}
          </h2>
          <Link to="/leases" className="flex items-center gap-1 text-sm font-medium text-terracotta-500 hover:text-terracotta-600">
            View all <ArrowRight size={14} />
          </Link>
        </div>
        {leases.length === 0 ? (
          <div className="card p-6 text-center text-sm text-warm-700">No leases found.</div>
        ) : (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Tenant</th>
                  <th>Property / Unit</th>
                  <th>Dates</th>
                  <th>Rent</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {leases.slice(0, 5).map((lease) => (
                  <tr key={lease.id}>
                    <td className="font-medium">{lease.tenantName}</td>
                    <td>
                      <span className="font-medium">{lease.propertyName}</span>
                      <span className="ml-1.5 text-xs text-warm-600">Unit {lease.unitNumber}</span>
                    </td>
                    <td className="text-warm-600 text-xs">
                      {format(new Date(lease.startDate), 'MMM d, yyyy')} →{' '}
                      {format(new Date(lease.endDate), 'MMM d, yyyy')}
                    </td>
                    <td>₹{Number(lease.monthlyRent).toLocaleString()}/mo</td>
                    <td>
                      <StatusBadge label={lease.status} variant={statusForLease(lease.status)} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Recent Maintenance */}
      {openMaintenance.length > 0 && (
        <section className="mt-8">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="font-display text-lg font-semibold text-warm-900">Open Maintenance Requests</h2>
            <Link to="/maintenance" className="flex items-center gap-1 text-sm font-medium text-terracotta-500 hover:text-terracotta-600">
              View all <ArrowRight size={14} />
            </Link>
          </div>
          <div className="space-y-3">
            {openMaintenance.slice(0, 4).map((req) => (
              <div key={req.id} className="card flex items-start gap-4 p-4">
                <div className={`mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${
                  req.priority === 'HIGH' ? 'bg-red-100 text-red-500' :
                  req.priority === 'MEDIUM' ? 'bg-amber-50 text-amber-600' :
                  'bg-ivory-200 text-warm-600'
                }`}>
                  <Wrench size={16} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-warm-900 line-clamp-1">{req.description}</p>
                  <p className="mt-0.5 text-xs text-warm-600">Unit {req.unitId} · {req.priority} priority</p>
                </div>
                <StatusBadge label={req.status} variant={statusForMaintenance(req.status)} />
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Quick insight */}
      <section className="mt-8">
        <div className="card flex items-center gap-5 bg-gradient-to-r from-terracotta-500 to-terracotta-600 p-6 text-white">
          <TrendingUp size={36} className="opacity-80 shrink-0" />
          <div>
            <p className="font-display text-lg font-semibold">A better way to manage the places we call home</p>
            <p className="mt-1 text-sm text-terracotta-100">
              Nivara helps you track leases, billing, and maintenance — all in one place.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
