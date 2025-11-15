"use client";

import { useT } from "@/lib/i18n/hooks";
import { Loader2 } from "lucide-react";
import type { SupervisorEventResponseDto } from "@/api";
import { formatDateTime } from "@/lib/utils";

interface LiveviewStatusProps {
  isConnected: boolean;
  isConnecting: boolean;
  isInitialLoading: boolean;
  error?: string | null;
  event?: SupervisorEventResponseDto | null;
}

export function LiveviewStatus({
  isConnected,
  isConnecting,
  isInitialLoading,
  error,
  event,
}: LiveviewStatusProps) {
  const t = useT();

  return (
    <div className="p-4 border rounded-lg bg-card">
      {/* Connection Status */}
      <div>
        <div className="flex items-center gap-2 mb-4">
          <div
            className={`w-3 h-3 rounded-full ${
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
      </div>

      {/* Event Information */}
      {event && !isInitialLoading && (
        <div className="mt-6 pt-6 border-t">
          <h2 className="text-lg font-bold mb-4">
            {t("liveview.event.title")}
          </h2>
          <div className="space-y-2 text-sm">
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
        </div>
      )}
    </div>
  );
}
