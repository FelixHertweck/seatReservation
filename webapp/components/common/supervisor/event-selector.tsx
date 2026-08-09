"use client";

import React from "react";
import { useT } from "@/lib/i18n/hooks";
import { CalendarDays, Loader2 } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { SupervisorEventResponseDto } from "@/api";

type Props = {
  events?: SupervisorEventResponseDto[] | null;
  isLoadingEvents?: boolean;
  selectedEventId?: string | null;
  onEventSelect: (eventId: string) => void;
  placeholderKey?: string; // translation key for select placeholder
  noEventsKey?: string; // translation key for no events text
};

// Docked into the shared page header (via PageHeader's search slot), the
// same spot the search bar usually occupies, so the current event is always
// visible and switchable without scrolling back to the top of the page body.
export default function EventSelector({
  events,
  isLoadingEvents,
  selectedEventId,
  onEventSelect,
  placeholderKey = "common.eventSelector.placeholder",
  noEventsKey = "common.eventSelector.noEvents",
}: Props) {
  const t = useT();

  if (isLoadingEvents) {
    return (
      <div className="flex h-10 w-full items-center justify-center gap-2 rounded-md border border-input bg-background text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin" />
      </div>
    );
  }

  // Surfaced directly in the (always-visible) trigger rather than only inside
  // the disabled dropdown item, so the header itself explains why there's
  // nothing to pick instead of implying a choice that doesn't exist yet.
  const hasNoEvents = !events || events.length === 0;

  return (
    <Select
      value={selectedEventId?.toString() || ""}
      onValueChange={onEventSelect}
      disabled={hasNoEvents}
    >
      <SelectTrigger
        className={
          selectedEventId || hasNoEvents
            ? "gap-2"
            : "gap-2 border-primary/60 ring-2 ring-primary/20 data-[placeholder]:text-foreground data-[placeholder]:font-medium"
        }
      >
        <CalendarDays className="h-4 w-4 shrink-0 opacity-70" />
        <SelectValue
          placeholder={hasNoEvents ? t(noEventsKey) : t(placeholderKey)}
        />
      </SelectTrigger>
      <SelectContent>
        {events && events.length > 0 ? (
          events.map((event) => (
            <SelectItem
              key={event.id?.toString()}
              value={event.id?.toString() || ""}
            >
              {event.name || t("eventsPage.title")}
            </SelectItem>
          ))
        ) : (
          <SelectItem value="__no_events" disabled>
            {t(noEventsKey)}
          </SelectItem>
        )}
      </SelectContent>
    </Select>
  );
}
