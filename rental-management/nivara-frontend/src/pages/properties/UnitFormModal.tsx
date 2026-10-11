import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { unitService } from '../../services/propertyService';
import type { UnitResponse } from '../../types/api';
import { X } from 'lucide-react';

const schema = z.object({
  unitNumber: z.string().min(1, 'Unit number is required'),
  unitType: z.string().min(1, 'Unit type is required'),
  monthlyRent: z.coerce.number().min(1, 'Rent must be > 0'),
  status: z.string().min(1, 'Status is required'),
});
type FormData = z.infer<typeof schema>;

interface Props {
  propertyId: number;
  existing: UnitResponse | null;
  onClose: () => void;
  onSaved: () => void;
}

const UNIT_TYPES = ['1BHK', '2BHK', '3BHK', 'STUDIO', 'PENTHOUSE', 'COMMERCIAL'];
const STATUSES = ['VACANT', 'OCCUPIED', 'MAINTENANCE'];

export default function UnitFormModal({ propertyId, existing, onClose, onSaved }: Props) {
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: existing
      ? { unitNumber: existing.unitNumber, unitType: existing.unitType, monthlyRent: existing.monthlyRent, status: existing.status }
      : { status: 'VACANT' },
  });

  async function onSubmit(data: FormData) {
    setServerError('');
    try {
      if (existing) {
        await unitService.update(propertyId, existing.id, data);
      } else {
        await unitService.create(propertyId, data);
      }
      onSaved();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Save failed.';
      setServerError(msg);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-warm-900/30 backdrop-blur-sm p-4">
      <div className="card w-full max-w-md p-6 shadow-soft">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="font-display text-xl font-semibold text-warm-900">
            {existing ? 'Edit unit' : 'Add unit'}
          </h2>
          <button onClick={onClose} className="btn-ghost p-1.5"><X size={18} /></button>
        </div>

        {serverError && (
          <div className="mb-4 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
            {serverError}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label">Unit number</label>
            <input type="text" placeholder="e.g. A-101" className="input-field" {...register('unitNumber')} />
            {errors.unitNumber && <p className="mt-1.5 text-xs text-red-600">{errors.unitNumber.message}</p>}
          </div>
          <div>
            <label className="label">Unit type</label>
            <select className="input-field" {...register('unitType')}>
              <option value="">Select type…</option>
              {UNIT_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
            {errors.unitType && <p className="mt-1.5 text-xs text-red-600">{errors.unitType.message}</p>}
          </div>
          <div>
            <label className="label">Monthly rent (₹)</label>
            <input type="number" min="0" placeholder="25000" className="input-field" {...register('monthlyRent')} />
            {errors.monthlyRent && <p className="mt-1.5 text-xs text-red-600">{errors.monthlyRent.message}</p>}
          </div>
          <div>
            <label className="label">Status</label>
            <select className="input-field" {...register('status')}>
              {STATUSES.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
            </select>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="btn-primary">
              {isSubmitting ? 'Saving…' : existing ? 'Save changes' : 'Add unit'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
