"use client";

import { useState } from "react";
import { useT } from "@/lib/i18n/hooks";
import { ChevronDown, Loader2 } from "lucide-react";
import type { SupervisorEventResponseDto } from "@/api";
import { formatDateTime, cn } from "@/lib/utils";

interface LiveviewStatusProps {
  isConnected: boolean;
  isConnecting: boolean;
  isInitialLoading: boolean;
  error?: string | null;
  event?: SupervisorEventResponseDto | null;
  defaultOpen?: boolean;
}

export function LiveviewStatus({
  isConnected,
  isConnecting,
  isInitialLoading,
  error,
  event,
  defaultOpen = false,
}: LiveviewStatusProps) {
  const t = useT();
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="border rounded-lg bg-card overflow-hidden">
      {/* Header Button */}
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className="w-full flex items-center justify-between p-4 font-medium text-sm hover:bg-muted/50 transition-colors text-left"
      >
        <div className="flex items-center gap-2">
          <div
            className={`w-3 h-3 rounded-full shrink-0 ${
              isConnected
                ? "bg-green-500"
                : isConnecting
                  ? "bg-yellow-500"
                  : "bg-red-500"
            }`}
          />
          <span className="text-sm font-medium">
            {isConnected && t("liveview.status.connected")}
            {isConnecting && !isConnected && t("liveview.status.connecting")}
            {!isConnected && !isConnecting && t("liveview.status.disconnected")}
          </span>
        </div>
        <ChevronDown
          className={cn(
            "h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-200",
            isOpen && "rotate-180",
          )}
        />
      </button>

      {/* Collapsible Content */}
      {isOpen && (
        <div className="px-4 pb-4 border-t pt-3 space-y-3 text-sm">
          {isInitialLoading && (
            <div className="flex items-center gap-2 text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span className="text-sm">{t("liveview.status.loading")}</span>
            </div>
          )}

          {error && (
            <div className="text-sm text-destructive">
              <p className="font-medium">{t("liveview.error.title")}:</p>
              <p>{error}</p>
            </div>
          )}

          {event && !isInitialLoading && (
            <div className="space-y-2">
              <h2 className="text-base font-bold mb-2">
                {t("liveview.event.title")}
              </h2>
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
              {event.startTime && (
                <p>
                  <strong>{t("liveview.event.startTime")}</strong>{" "}
                  {(() => {
                    const formatted = formatDateTime(event.startTime);
                    return formatted ? (
                      <span className="flex flex-col text-sm">
                        <span>{formatted.date}</span>
                        <span>{formatted.time}</span>
                      </span>
                    ) : (
                      "-"
                    );
                  })()}
                </p>
              )}
              {event.endTime && (
                <p>
                  <strong>{t("liveview.event.endTime")}</strong>
                  {": "}
                  {(() => {
                    const formatted = formatDateTime(event.endTime);
                    return formatted ? (
                      <span className="flex flex-col text-sm">
                        <span>{formatted.date}</span>
                        <span>{formatted.time}</span>
                      </span>
                    ) : (
                      "-"
                    );
                  })()}
                </p>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
