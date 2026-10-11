import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { propertyService, userDirectoryService } from '../../services/propertyService';
import type { ManagerAssignmentResponse, UserSummaryResponse } from '../../types/api';
import { X, UserPlus, Trash2, User } from 'lucide-react';
import LoadingSpinner from '../../components/ui/LoadingSpinner';

const schema = z.object({
  managerId: z.coerce.number().min(1, 'Select a manager'),
});
type FormData = z.infer<typeof schema>;

interface Props {
  propertyId: number;
  currentManagers: ManagerAssignmentResponse[];
  onClose: () => void;
  onChanged: () => void;
}

export default function ManagerAssignModal({
  propertyId,
  currentManagers,
  onClose,
  onChanged,
}: Props) {
  const [allManagers, setAllManagers] = useState<UserSummaryResponse[]>([]);
  const [loadingDir, setLoadingDir] = useState(true);
  const [serverError, setServerError] = useState('');
  const [removingId, setRemovingId] = useState<number | null>(null);

  const assignedIds = new Set(currentManagers.map((m) => m.managerId));
  const eligible = allManagers.filter((m) => !assignedIds.has(m.id));

  useEffect(() => {
    userDirectoryService
      .listManagers()
      .then(setAllManagers)
      .catch(() => setServerError('Could not load manager directory.'))
      .finally(() => setLoadingDir(false));
  }, []);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onAssign(data: FormData) {
    setServerError('');
    try {
      await propertyService.assignManager(propertyId, data.managerId);
      reset();
      onChanged();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to assign manager.';
      setServerError(msg);
    }
  }

  async function handleRemove(managerId: number) {
    setRemovingId(managerId);
    setServerError('');
    try {
      await propertyService.removeManager(propertyId, managerId);
      onChanged();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Failed to remove manager.';
      setServerError(msg);
    } finally {
      setRemovingId(null);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-warm-900/30 backdrop-blur-sm p-4">
      <div className="card w-full max-w-lg p-6 shadow-soft">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="font-display text-xl font-semibold text-warm-900 flex items-center gap-2">
            <UserPlus size={20} className="text-terracotta-500" /> Manage Assignments
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

        {/* Current assignments */}
        <div className="mb-5">
          <p className="label mb-2">Currently assigned</p>
          {currentManagers.length === 0 ? (
            <p className="text-sm text-warm-600">No managers assigned.</p>
          ) : (
            <ul className="space-y-2">
              {currentManagers.map((m) => (
                <li
                  key={m.managerId}
                  className="flex items-center gap-3 rounded-xl bg-ivory-100 px-4 py-3"
                >
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-terracotta-100 text-xs font-bold text-terracotta-600 uppercase">
                    {m.managerName.slice(0, 2)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-warm-900 truncate">{m.managerName}</p>
                    <p className="text-xs text-warm-600 truncate">{m.managerEmail}</p>
                  </div>
                  <button
                    onClick={() => handleRemove(m.managerId)}
                    disabled={removingId === m.managerId}
                    className="flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs font-medium text-red-500 hover:bg-red-50 transition-colors disabled:opacity-50"
                  >
                    <Trash2 size={13} />
                    {removingId === m.managerId ? 'Removing…' : 'Remove'}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="border-t border-border pt-5">
          <p className="label mb-3">Assign a new manager</p>
          {loadingDir ? (
            <LoadingSpinner message="Loading manager directory…" />
          ) : eligible.length === 0 ? (
            <p className="text-sm text-warm-600">
              {allManagers.length === 0
                ? 'No managers are registered. Ask a user to register with the Manager role.'
                : 'All registered managers are already assigned.'}
            </p>
          ) : (
            <form onSubmit={handleSubmit(onAssign)} className="space-y-4">
              <div>
                <select className="input-field" {...register('managerId')}>
                  <option value="">Select manager…</option>
                  {eligible.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.name} — {m.email}
                    </option>
                  ))}
                </select>
                {errors.managerId && (
                  <p className="mt-1.5 text-xs text-red-600">{errors.managerId.message}</p>
                )}
              </div>
              <div className="flex justify-end gap-3">
                <button type="button" onClick={onClose} className="btn-secondary">
                  Close
                </button>
                <button type="submit" disabled={isSubmitting} className="btn-primary">
                  <User size={15} />
                  {isSubmitting ? 'Assigning…' : 'Assign'}
                </button>
              </div>
            </form>
          )}
          {eligible.length === 0 && (
            <div className="mt-4 flex justify-end">
              <button onClick={onClose} className="btn-secondary">Close</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
