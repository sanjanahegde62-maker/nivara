import { Loader2 } from 'lucide-react';

export default function LoadingSpinner({ message = 'Loading…' }: { message?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-20 text-warm-700">
      <Loader2 size={32} className="animate-spin text-terracotta-400" />
      <p className="text-sm">{message}</p>
    </div>
  );
}
