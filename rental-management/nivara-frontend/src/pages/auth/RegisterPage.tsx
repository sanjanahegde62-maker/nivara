import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../../context/AuthContext';
import { Building2, Eye, EyeOff } from 'lucide-react';
import type { Role } from '../../types/api';

const schema = z.object({
  name: z.string().min(2, 'Full name is required'),
  email: z.string().email('Enter a valid email'),
  password: z.string().min(8, 'Password must be at least 8 characters'),
  phone: z.string().optional(),
  role: z.enum(['OWNER', 'MANAGER', 'TENANT', 'MAINTENANCE_STAFF'] as const),
});
type FormData = z.infer<typeof schema>;

const ROLES: { value: Role; label: string }[] = [
  { value: 'TENANT', label: 'Tenant — looking for a home' },
  { value: 'OWNER', label: 'Owner — I own properties' },
  { value: 'MANAGER', label: 'Manager — I manage properties' },
  { value: 'MAINTENANCE_STAFF', label: 'Maintenance Staff' },
];

export default function RegisterPage() {
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();
  const [showPw, setShowPw] = useState(false);
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { role: 'TENANT' },
  });

  async function onSubmit(data: FormData) {
    setServerError('');
    try {
      await registerUser(data);
      navigate('/dashboard');
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Registration failed. Please try again.';
      setServerError(msg);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-ivory-100 px-4 py-10">
      <div className="w-full max-w-md">
        {/* Brand */}
        <div className="mb-8 flex flex-col items-center gap-3 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-terracotta-500 shadow-soft">
            <Building2 size={28} className="text-white" />
          </div>
          <div>
            <h1 className="font-display text-3xl font-semibold text-warm-900">Create account</h1>
            <p className="mt-1 text-sm text-warm-700">Join Nivara today</p>
          </div>
        </div>

        <div className="card p-8">
          {serverError && (
            <div className="mb-5 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
              {serverError}
            </div>
          )}
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div>
              <label className="label">Full name</label>
              <input type="text" placeholder="Alex Johnson" className="input-field" {...register('name')} />
              {errors.name && <p className="mt-1.5 text-xs text-red-600">{errors.name.message}</p>}
            </div>

            <div>
              <label className="label">Email address</label>
              <input type="email" placeholder="you@example.com" className="input-field" {...register('email')} />
              {errors.email && <p className="mt-1.5 text-xs text-red-600">{errors.email.message}</p>}
            </div>

            <div>
              <label className="label">Phone (optional)</label>
              <input type="tel" placeholder="+91 98765 43210" className="input-field" {...register('phone')} />
            </div>

            <div>
              <label className="label">I am a…</label>
              <select className="input-field" {...register('role')}>
                {ROLES.map((r) => (
                  <option key={r.value} value={r.value}>{r.label}</option>
                ))}
              </select>
              {errors.role && <p className="mt-1.5 text-xs text-red-600">{errors.role.message}</p>}
            </div>

            <div>
              <label className="label">Password</label>
              <div className="relative">
                <input
                  type={showPw ? 'text' : 'password'}
                  placeholder="min 8 characters"
                  className="input-field pr-10"
                  {...register('password')}
                />
                <button
                  type="button"
                  onClick={() => setShowPw((p) => !p)}
                  className="absolute inset-y-0 right-3 flex items-center text-warm-400 hover:text-warm-700"
                >
                  {showPw ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {errors.password && <p className="mt-1.5 text-xs text-red-600">{errors.password.message}</p>}
            </div>

            <button type="submit" disabled={isSubmitting} className="btn-primary w-full py-3">
              {isSubmitting ? 'Creating account…' : 'Create account'}
            </button>
          </form>
        </div>

        <p className="mt-5 text-center text-sm text-warm-700">
          Already have an account?{' '}
          <Link to="/login" className="font-semibold text-terracotta-500 hover:text-terracotta-600">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
