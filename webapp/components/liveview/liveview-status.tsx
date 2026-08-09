"use client";

import { Info } from "lucide-react";
import { useT } from "@/lib/i18n/hooks";
import { formatDateTime } from "@/lib/utils";
import type { SupervisorEventResponseDto } from "@/api";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

interface LiveviewConnectionBadgeProps {
  isConnected: boolean;
  isConnecting: boolean;
  error?: string | null;
  event?: SupervisorEventResponseDto | null;
}

export function LiveviewConnectionBadge({
  isConnected,
  isConnecting,
  error,
  event,
}: LiveviewConnectionBadgeProps) {
  const t = useT();

  return (
    <div className="flex items-center gap-2 text-sm">
      <div className="flex items-center gap-1.5 rounded-full border bg-card px-2.5 py-1">
        <div
          className={`h-2 w-2 rounded-full ${
            isConnected
              ? "bg-green-500"
              : isConnecting
                ? "bg-yellow-500"
                : "bg-red-500"
          }`}
        />
        <span className="hidden font-medium whitespace-nowrap sm:inline">
          {isConnected
            ? t("liveview.status.connected")
            : isConnecting
              ? t("liveview.status.connecting")
              : t("liveview.status.disconnected")}
        </span>
        {event && (
          <DropdownMenu>
            <DropdownMenuTrigger
              className="ml-0.5 -mr-1 rounded-full p-0.5 text-muted-foreground hover:bg-muted hover:text-foreground"
              aria-label={t("liveview.event.title")}
            >
              <Info className="h-3.5 w-3.5" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-72 space-y-2 p-3">
              <h4 className="text-sm font-semibold">
                {t("liveview.event.title")}
              </h4>
              <div className="space-y-1.5 text-sm">
                {event.name && (
                  <p>
                    <strong>{t("liveview.event.name")}</strong>: {event.name}
                  </p>
                )}
                {event.description && (
                  <p className="text-muted-foreground line-clamp-3">
                    <strong>{t("liveview.event.description")}</strong>
                    {": "}
                    {event.description}
                  </p>
                )}
                {event.startTime &&
                  (() => {
                    const formatted = formatDateTime(event.startTime);
                    return formatted ? (
                      <p>
                        <strong>{t("liveview.event.startTime")}</strong>{" "}
                        {formatted.date} {formatted.time}
                      </p>
                    ) : null;
                  })()}
                {event.endTime &&
                  (() => {
                    const formatted = formatDateTime(event.endTime);
                    return formatted ? (
                      <p>
                        <strong>{t("liveview.event.endTime")}</strong>{" "}
                        {formatted.date} {formatted.time}
                      </p>
                    ) : null;
                  })()}
              </div>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>
      {error && (
        <span
          className="text-xs text-destructive whitespace-nowrap"
          title={error}
        >
          {t("liveview.error.title")}
        </span>
      )}
    </div>
  );
}
