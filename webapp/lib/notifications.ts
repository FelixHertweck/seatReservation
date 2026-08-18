import { Ticket, Calendar, Info, type LucideIcon } from "lucide-react";
import type { useT } from "@/lib/i18n/hooks";

/**
 * Formats a notification timestamp as a short relative label ("5m ago"), falling back to a
 * localized absolute date once it's more than a week old. Shared between the notification bell
 * and the notifications page so both surfaces always agree on formatting.
 */
export function formatRelativeTime(
  dateInput: string | Date | undefined,
  t: ReturnType<typeof useT>,
): string {
  if (!dateInput) return "";
  const date = typeof dateInput === "string" ? new Date(dateInput) : dateInput;
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return t("notifications.relativeTime.justNow");
  if (diffMin < 60)
    return t("notifications.relativeTime.minutesAgo", { count: diffMin });
  if (diffHour < 24)
    return t("notifications.relativeTime.hoursAgo", { count: diffHour });
  if (diffDay < 7)
    return t("notifications.relativeTime.daysAgo", { count: diffDay });
  return date.toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function getCategoryIcon(category?: string): LucideIcon {
  switch (category) {
    case "BOOKING":
      return Ticket;
    case "EVENT_REMINDER":
      return Calendar;
    default:
      return Info;
  }
}

/** Tailwind classes for a category's icon badge, shared so new categories only need one edit. */
export function getCategoryColorClasses(category?: string): string {
  switch (category) {
    case "BOOKING":
      return "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400";
    case "EVENT_REMINDER":
      return "bg-purple-500/10 text-purple-600 dark:text-purple-400";
    default:
      return "bg-blue-500/10 text-blue-600 dark:text-blue-400";
  }
}
