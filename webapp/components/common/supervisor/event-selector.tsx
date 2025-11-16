"use client";

import React from "react";
import { useT } from "@/lib/i18n/hooks";
import { Loader2 } from "lucide-react";
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
  selectedEventId?: bigint | null;
  onEventSelect: (eventId: string) => void;
  labelKey?: string; // translation key for label
  placeholderKey?: string; // translation key for select placeholder
  noEventsKey?: string; // translation key for no events text
};

export default function EventSelector({
  events,
  isLoadingEvents,
  selectedEventId,
  onEventSelect,
  labelKey = "liveview.eventSelector.label",
  placeholderKey = "liveview.eventSelector.placeholder",
  noEventsKey = "eventsPage.noEventsAvailable",
}: Props) {
  const t = useT();

  return (
    <div className="mb-8 p-4 border rounded-lg bg-muted/50">
      <label className="text-sm font-medium mb-2 block">{t(labelKey)}</label>
      <div className="flex gap-2 items-center">
        {isLoadingEvents ? (
          <div className="flex items-center gap-2 text-muted-foreground w-full justify-center py-2">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="text-sm">
              {t("liveview.eventSelector.loading")}
            </span>
          </div>
        ) : (
          <Select
            value={selectedEventId?.toString() || ""}
            onValueChange={onEventSelect}
          >
            <SelectTrigger className="w-full">
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
        )}
      </div>
    </div>
  );
}
