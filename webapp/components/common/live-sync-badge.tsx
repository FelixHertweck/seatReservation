import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";

interface LiveSyncBadgeProps {
  label?: string;
  isSyncing?: boolean;
  className?: string;
}

export function LiveSyncBadge({
  label,
  isSyncing = true,
  className,
}: LiveSyncBadgeProps) {
  const t = useT();

  if (!isSyncing) return null;

  return (
    <div
      className={cn(
        "inline-flex items-center gap-2 rounded-full border border-primary/20 bg-background/80 px-3 py-1 text-xs font-medium text-foreground shadow-md backdrop-blur-md transition-all duration-300 animate-in fade-in zoom-in-95",
        className,
      )}
    >
      <span className="relative flex h-2 w-2">
        <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
        <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-500" />
      </span>
      <span>
        {label ?? t("common.liveSyncing", "Live-Synchronisierung...")}
      </span>
    </div>
  );
}
