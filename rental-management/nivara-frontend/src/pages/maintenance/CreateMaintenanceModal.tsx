import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { maintenanceService } from '../../services/maintenanceService';
import { leaseService } from '../../services/leaseService';
import type { LeaseResponse } from '../../types/api';
import { X } from 'lucide-react';

const schema = z.object({
  unitId: z.coerce.number().min(1, 'Select a unit'),
  description: z.string().min(10, 'Description must be at least 10 characters'),
  priority: z.enum(['HIGH', 'MEDIUM', 'LOW'] as const),
});
type FormData = z.infer<typeof schema>;

interface Props {
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateMaintenanceModal({ onClose, onCreated }: Props) {
  const [serverError, setServerError] = useState('');
  const [leases, setLeases] = useState<LeaseResponse[]>([]);

  useEffect(() => {
    leaseService.list().then(setLeases).catch(() => {});
  }, []);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { priority: 'MEDIUM' },
  });

  async function onSubmit(data: FormData) {
    setServerError('');
    try {
      await maintenanceService.create(data.unitId, data.description, data.priority);
      onCreated();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to submit request.';
      setServerError(msg);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-warm-900/30 backdrop-blur-sm p-4">
      <div className="card w-full max-w-md p-6 shadow-soft">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="font-display text-xl font-semibold text-warm-900">Submit maintenance request</h2>
          <button onClick={onClose} className="btn-ghost p-1.5"><X size={18} /></button>
        </div>
        {serverError && (
          <div className="mb-4 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
            {serverError}
          </div>
        )}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label">Unit</label>
            <select className="input-field" {...register('unitId')}>
              <option value="">Select your unit…</option>
              {leases.map((l) => (
                <option key={l.unitId} value={l.unitId}>
                  Unit {l.unitNumber} — {l.propertyName}
                </option>
              ))}
            </select>
            {errors.unitId && <p className="mt-1.5 text-xs text-red-600">{errors.unitId.message}</p>}
          </div>
          <div>
            <label className="label">Priority</label>
            <select className="input-field" {...register('priority')}>
              <option value="HIGH">High — Urgent</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
          <div>
            <label className="label">Description</label>
            <textarea
              rows={4}
              placeholder="Describe the issue in detail…"
              className="input-field resize-none"
              {...register('description')}
            />
            {errors.description && <p className="mt-1.5 text-xs text-red-600">{errors.description.message}</p>}
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="btn-primary">
              {isSubmitting ? 'Submitting…' : 'Submit request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
