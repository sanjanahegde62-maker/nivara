import { Inbox } from 'lucide-react';

export default function EmptyState({ message = 'Nothing here yet.' }: { message?: string }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-border bg-ivory-50 p-12 text-center">
      <Inbox size={28} className="text-warm-300" />
      <p className="text-sm text-warm-700">{message}</p>
    </div>
  );
}
