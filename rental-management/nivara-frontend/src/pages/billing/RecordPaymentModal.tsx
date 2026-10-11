import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { billingService } from '../../services/billingService';
import { X } from 'lucide-react';

const schema = z.object({
  amount: z.coerce.number().min(1, 'Amount must be > 0'),
  paymentMethod: z.string().min(1, 'Payment method is required'),
  reference: z.string().optional(),
});
type FormData = z.infer<typeof schema>;

const PAYMENT_METHODS = ['UPI', 'BANK_TRANSFER', 'CASH', 'CHEQUE', 'CARD'];

interface Props {
  rentChargeId: number;
  onClose: () => void;
  onRecorded: () => void;
}

export default function RecordPaymentModal({ rentChargeId, onClose, onRecorded }: Props) {
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { paymentMethod: 'UPI' },
  });

  async function onSubmit(data: FormData) {
    setServerError('');
    try {
      await billingService.recordPayment({ rentChargeId, ...data });
      onRecorded();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message ?? 'Payment failed.';
      setServerError(msg);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-warm-900/30 backdrop-blur-sm p-4">
      <div className="card w-full max-w-sm p-6 shadow-soft">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="font-display text-xl font-semibold text-warm-900">Record payment</h2>
          <button onClick={onClose} className="btn-ghost p-1.5"><X size={18} /></button>
        </div>
        {serverError && (
          <div className="mb-4 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">
            {serverError}
          </div>
        )}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="label">Amount (₹)</label>
            <input type="number" min="1" placeholder="25000" className="input-field" {...register('amount')} />
            {errors.amount && <p className="mt-1.5 text-xs text-red-600">{errors.amount.message}</p>}
          </div>
          <div>
            <label className="label">Payment method</label>
            <select className="input-field" {...register('paymentMethod')}>
              {PAYMENT_METHODS.map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Reference / Transaction ID (optional)</label>
            <input type="text" placeholder="TXN123456" className="input-field" {...register('reference')} />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={onClose} className="btn-secondary">Cancel</button>
            <button type="submit" disabled={isSubmitting} className="btn-primary">
              {isSubmitting ? 'Processing…' : 'Record payment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
