import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/auth/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';

// Pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import PropertiesPage from './pages/properties/PropertiesPage';
import UnitsPage from './pages/properties/UnitsPage';
import ApplicationsPage from './pages/applications/ApplicationsPage';
import LeasesPage from './pages/leases/LeasesPage';
import BillingLandingPage from './pages/billing/BillingLandingPage';
import BillingPage from './pages/billing/BillingPage';
import MaintenancePage from './pages/maintenance/MaintenancePage';
import ReportsPage from './pages/reports/ReportsPage';

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Protected app shell */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/dashboard" element={<DashboardPage />} />

            {/* Properties — OWNER and MANAGER only */}
            <Route element={<ProtectedRoute allowedRoles={['OWNER', 'MANAGER']} />}>
              <Route path="/properties" element={<PropertiesPage />} />
              <Route path="/properties/:propertyId/units" element={<UnitsPage />} />
            </Route>

            {/* Applications — all roles except MAINTENANCE_STAFF */}
            <Route
              element={<ProtectedRoute allowedRoles={['OWNER', 'MANAGER', 'TENANT']} />}
            >
              <Route path="/applications" element={<ApplicationsPage />} />
            </Route>

            {/* Leases — OWNER, MANAGER, TENANT */}
            <Route
              element={<ProtectedRoute allowedRoles={['OWNER', 'MANAGER', 'TENANT']} />}
            >
              <Route path="/leases" element={<LeasesPage />} />
            </Route>

            {/* Billing — OWNER, MANAGER, TENANT */}
            <Route
              element={<ProtectedRoute allowedRoles={['OWNER', 'MANAGER', 'TENANT']} />}
            >
              <Route path="/billing" element={<BillingLandingPage />} />
              <Route path="/billing/:leaseId" element={<BillingPage />} />
            </Route>

            {/* Maintenance — all roles */}
            <Route path="/maintenance" element={<MaintenancePage />} />

            {/* Reports — OWNER and MANAGER only */}
            <Route element={<ProtectedRoute allowedRoles={['OWNER', 'MANAGER']} />}>
              <Route path="/reports" element={<ReportsPage />} />
            </Route>
          </Route>
        </Route>

        {/* Default redirect */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </AuthProvider>
  );
}
