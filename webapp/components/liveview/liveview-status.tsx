"use client";

import { Info } from "lucide-react";
import { Button } from "@/components/ui/button";
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
  const statusLabel = isConnected
    ? t("liveview.status.connected")
    : isConnecting
      ? t("liveview.status.connecting")
      : t("liveview.status.disconnected");

  const statusDot = (
    <span className="relative flex h-2 w-2 shrink-0">
      {isConnected && (
        <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-green-400 opacity-75" />
      )}
      <span
        className={`relative inline-flex h-2 w-2 rounded-full ${
          isConnected
            ? "bg-green-500"
            : isConnecting
              ? "bg-yellow-500"
              : "bg-red-500"
        }`}
      />
    </span>
  );

  return (
    <div className="flex items-center gap-2 text-sm">
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="outline"
            className="h-10 min-w-10 shrink-0 px-0 sm:px-3"
            aria-label={statusLabel}
          >
            {statusDot}
            <span className="hidden sm:inline">{statusLabel}</span>
            {event && (
              <Info className="hidden text-muted-foreground sm:block" />
            )}
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="max-w-72 space-y-2 p-3">
          <p className="flex items-center gap-2 text-sm font-semibold">
            {statusDot}
            {statusLabel}
          </p>
          {event && (
            <>
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
            </>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
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
