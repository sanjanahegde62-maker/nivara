import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { leaseService } from '../../services/leaseService';
import type { ApplicationResponse } from '../../types/api';
import { X, FileText } from 'lucide-react';

const schema = z.object({
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().min(1, 'End date is required'),
}).refine((d) => new Date(d.endDate) > new Date(d.startDate), {
  message: 'End date must be after start date',
  path: ['endDate'],
});
type FormData = z.infer<typeof schema>;

interface Props {
  application: ApplicationResponse;
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateLeaseModal({ application, onClose, onCreated }: Props) {
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    setServerError('');
    try {
      await leaseService.create(application.id, data.startDate, data.endDate);
      onCreated();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to create lease.';
      setServerError(msg);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-warm-900/30 backdrop-blur-sm p-4">
      <div className="card w-full max-w-md p-6 shadow-soft">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="font-display text-xl font-semibold text-warm-900 flex items-center gap-2">
            <FileText size={20} className="text-terracotta-500" /> Create lease
          </h2>
          <button onClick={onClose} className="btn-ghost p-1.5"><X size={18} /></button>
        </div>

        <div className="mb-4 rounded-xl bg-ivory-100 p-4 text-sm">
          <p className="text-warm-700">Tenant: <span className="font-semibold text-warm-900">{application.tenantName}</span></p>
          <p className="mt-1 text-warm-700">Unit: <span className="font-semibold text-warm-900">{application.unitNumber}</span></p>
        </div>

        {serverError && (
          <div className="mb-4 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
            {serverError}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label">Start date</label>
            <input type="date" className="input-field" {...register('startDate')} />
            {errors.startDate && <p className="mt-1.5 text-xs text-red-600">{errors.startDate.message}</p>}
          </div>
          <div>
            <label className="label">End date</label>
            <input type="date" className="input-field" {...register('endDate')} />
            {errors.endDate && <p className="mt-1.5 text-xs text-red-600">{errors.endDate.message}</p>}
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="btn-primary">
              {isSubmitting ? 'Creating…' : 'Create lease'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
