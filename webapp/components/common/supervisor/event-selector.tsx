"use client";

import React from "react";
import { useT } from "@/lib/i18n/hooks";
import { CalendarDays, Loader2 } from "@/components/icons";
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
  placeholderKey = "liveview.eventSelector.placeholder",
  noEventsKey = "eventsPage.noEventsAvailable",
}: Props) {
  const t = useT();

  if (isLoadingEvents) {
    return (
      <div className="flex h-10 w-full items-center justify-center gap-2 rounded-md border border-input bg-background text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin" />
      </div>
    );
  }

  return (
    <Select
      value={selectedEventId?.toString() || ""}
      onValueChange={onEventSelect}
    >
      <SelectTrigger
        className={
          selectedEventId
            ? "gap-2"
            : "gap-2 border-primary/60 ring-2 ring-primary/20 data-[placeholder]:text-foreground data-[placeholder]:font-medium"
        }
      >
        <CalendarDays className="h-4 w-4 shrink-0 opacity-70" />
        <SelectValue placeholder={t(placeholderKey)} />
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
