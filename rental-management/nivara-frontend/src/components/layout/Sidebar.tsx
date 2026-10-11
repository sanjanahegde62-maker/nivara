import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Building2,
  FileText,
  Wrench,
  BarChart2,
  CreditCard,
  LogOut,
  ChevronRight,
  Home,
} from 'lucide-react';
import clsx from 'clsx';
import type { Role } from '../../types/api';

interface NavItem {
  label: string;
  to: string;
  icon: React.ReactNode;
  roles: Role[];
}

const NAV_ITEMS: NavItem[] = [
  {
    label: 'Dashboard',
    to: '/dashboard',
    icon: <LayoutDashboard size={18} />,
    roles: ['OWNER', 'MANAGER', 'TENANT', 'MAINTENANCE_STAFF'],
  },
  {
    label: 'Properties',
    to: '/properties',
    icon: <Building2 size={18} />,
    roles: ['OWNER', 'MANAGER'],
  },
  {
    label: 'Leases',
    to: '/leases',
    icon: <FileText size={18} />,
    roles: ['OWNER', 'MANAGER', 'TENANT'],
  },
  {
    label: 'Applications',
    to: '/applications',
    icon: <Home size={18} />,
    roles: ['OWNER', 'MANAGER', 'TENANT'],
  },
  {
    label: 'Billing',
    to: '/billing',
    icon: <CreditCard size={18} />,
    roles: ['OWNER', 'MANAGER', 'TENANT'],
  },
  {
    label: 'Maintenance',
    to: '/maintenance',
    icon: <Wrench size={18} />,
    roles: ['OWNER', 'MANAGER', 'TENANT', 'MAINTENANCE_STAFF'],
  },
  {
    label: 'Reports',
    to: '/reports',
    icon: <BarChart2 size={18} />,
    roles: ['OWNER', 'MANAGER'],
  },
];

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const visible = NAV_ITEMS.filter(
    (item) => user && item.roles.includes(user.role)
  );

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <aside className="flex h-screen w-64 flex-col bg-white border-r border-border">
      {/* Logo */}
      <div className="flex items-center gap-3 px-6 py-5 border-b border-border">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-terracotta-500">
          <Building2 size={20} className="text-white" />
        </div>
        <span className="font-display text-xl font-semibold text-warm-900">Nivara</span>
      </div>

      {/* User pill */}
      {user && (
        <div className="mx-4 mt-4 flex items-center gap-3 rounded-xl bg-ivory-100 px-4 py-3">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-terracotta-100 text-xs font-bold text-terracotta-600 uppercase shrink-0">
            {user.name.slice(0, 2)}
          </div>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-warm-900">{user.name}</p>
            <p className="truncate text-xs text-warm-700 capitalize">{user.role.toLowerCase().replace('_', ' ')}</p>
          </div>
        </div>
      )}

      {/* Nav */}
      <nav className="mt-4 flex-1 overflow-y-auto px-3">
        <ul className="space-y-0.5">
          {visible.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                className={({ isActive }) =>
                  clsx(
                    'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-150',
                    isActive
                      ? 'bg-terracotta-500 text-white shadow-sm'
                      : 'text-warm-700 hover:bg-ivory-200 hover:text-warm-900'
                  )
                }
              >
                {item.icon}
                <span className="flex-1">{item.label}</span>
                <ChevronRight size={14} className="opacity-40" />
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      {/* Logout */}
      <div className="p-4 border-t border-border">
        <button
          onClick={handleLogout}
          className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-warm-700 transition-all duration-150 hover:bg-red-50 hover:text-red-600"
        >
          <LogOut size={18} />
          Sign out
        </button>
      </div>
    </aside>
  );
}
