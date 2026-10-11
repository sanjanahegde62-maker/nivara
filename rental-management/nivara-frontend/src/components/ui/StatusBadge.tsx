import clsx from 'clsx';

type BadgeVariant = 'success' | 'warn' | 'error' | 'neutral' | 'info';

const VARIANT_MAP: Record<BadgeVariant, string> = {
  success: 'bg-sage-100 text-sage-500',
  warn: 'bg-amber-50 text-amber-700',
  error: 'bg-red-50 text-red-600',
  neutral: 'bg-ivory-200 text-warm-700',
  info: 'bg-blue-50 text-blue-600',
};

export default function StatusBadge({
  label,
  variant,
}: {
  label: string;
  variant: BadgeVariant;
}) {
  return (
    <span
      className={clsx(
        'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold',
        VARIANT_MAP[variant]
      )}
    >
      {label}
    </span>
  );
}

export function statusForLease(status: string): BadgeVariant {
  switch (status?.toUpperCase()) {
    case 'ACTIVE': return 'success';
    case 'EXPIRED': return 'neutral';
    case 'TERMINATED': return 'error';
    default: return 'neutral';
  }
}

export function statusForApplication(status: string): BadgeVariant {
  switch (status) {
    case 'APPROVED': return 'success';
    case 'REJECTED': return 'error';
    default: return 'warn';
  }
}

export function statusForCharge(status: string): BadgeVariant {
  switch (status) {
    case 'PAID': return 'success';
    case 'OVERDUE': return 'error';
    default: return 'warn';
  }
}

export function statusForMaintenance(status: string): BadgeVariant {
  switch (status) {
    case 'RESOLVED':
    case 'CLOSED': return 'success';
    case 'OPEN': return 'error';
    case 'IN_PROGRESS': return 'info';
    default: return 'warn';
  }
}
