import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { propertyService } from '../../services/propertyService';
import type { PropertyResponse } from '../../types/api';
import { X } from 'lucide-react';

const schema = z.object({
  name: z.string().min(2, 'Name is required'),
  address: z.string().min(5, 'Address is required'),
  propertyType: z.string().min(1, 'Type is required'),
  description: z.string().optional(),
});
type FormData = z.infer<typeof schema>;

const PROPERTY_TYPES = ['APARTMENT', 'HOUSE', 'VILLA', 'COMMERCIAL', 'STUDIO', 'PENTHOUSE'];

interface Props {
  existing: PropertyResponse | null;
  onClose: () => void;
  onSaved: () => void;
}

export default function PropertyFormModal({ existing, onClose, onSaved }: Props) {
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: existing
      ? { name: existing.name, address: existing.address, propertyType: existing.propertyType, description: existing.description ?? '' }
      : {},
  });

  async function onSubmit(data: FormData) {
    setServerError('');
    try {
      if (existing) {
        await propertyService.update(existing.id, data);
      } else {
        await propertyService.create(data);
      }
      onSaved();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Save failed.';
      setServerError(msg);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-warm-900/30 backdrop-blur-sm p-4">
      <div className="card w-full max-w-lg p-6 shadow-soft">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="font-display text-xl font-semibold text-warm-900">
            {existing ? 'Edit property' : 'Add property'}
          </h2>
          <button onClick={onClose} className="btn-ghost p-1.5">
            <X size={18} />
          </button>
        </div>

        {serverError && (
          <div className="mb-4 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
            {serverError}
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label">Property name</label>
            <input type="text" placeholder="e.g. Sunrise Apartments" className="input-field" {...register('name')} />
            {errors.name && <p className="mt-1.5 text-xs text-red-600">{errors.name.message}</p>}
          </div>
          <div>
            <label className="label">Address</label>
            <input type="text" placeholder="123 Main St, City" className="input-field" {...register('address')} />
            {errors.address && <p className="mt-1.5 text-xs text-red-600">{errors.address.message}</p>}
          </div>
          <div>
            <label className="label">Property type</label>
            <select className="input-field" {...register('propertyType')}>
              <option value="">Select type…</option>
              {PROPERTY_TYPES.map((t) => (
                <option key={t} value={t}>{t.charAt(0) + t.slice(1).toLowerCase()}</option>
              ))}
            </select>
            {errors.propertyType && <p className="mt-1.5 text-xs text-red-600">{errors.propertyType.message}</p>}
          </div>
          <div>
            <label className="label">Description (optional)</label>
            <textarea
              rows={3}
              placeholder="Brief description of the property…"
              className="input-field resize-none"
              {...register('description')}
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="btn-primary">
              {isSubmitting ? 'Saving…' : existing ? 'Save changes' : 'Add property'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
