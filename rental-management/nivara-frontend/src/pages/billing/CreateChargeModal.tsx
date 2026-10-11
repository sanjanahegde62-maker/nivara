import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { billingService } from '../../services/billingService';
import { X } from 'lucide-react';

const schema = z.object({
  billingMonth: z.string().regex(/^\d{4}-\d{2}$/, 'Format must be YYYY-MM'),
});
type FormData = z.infer<typeof schema>;

interface Props {
  leaseId: number;
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateChargeModal({ leaseId, onClose, onCreated }: Props) {
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { billingMonth: new Date().toISOString().slice(0, 7) },
  });

  async function onSubmit(data: FormData) {
    setServerError('');
    try {
      await billingService.createCharge(leaseId, data.billingMonth);
      onCreated();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Failed to create charge.';
      setServerError(msg);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-warm-900/30 backdrop-blur-sm p-4">
      <div className="card w-full max-w-sm p-6 shadow-soft">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="font-display text-xl font-semibold text-warm-900">Create rent charge</h2>
          <button onClick={onClose} className="btn-ghost p-1.5"><X size={18} /></button>
        </div>
        {serverError && (
          <div className="mb-4 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
            {serverError}
          </div>
        )}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label">Billing month (YYYY-MM)</label>
            <input type="month" className="input-field" {...register('billingMonth')} />
            {errors.billingMonth && <p className="mt-1.5 text-xs text-red-600">{errors.billingMonth.message}</p>}
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="btn-primary">
              {isSubmitting ? 'Creating…' : 'Create charge'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
