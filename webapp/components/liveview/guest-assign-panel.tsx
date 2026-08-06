"use client";

import { Button } from "@/components/custom-ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import type { SeatDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface GuestAssignPanelProps {
  selectedSeats: SeatDto[];
  guestName: string;
  onGuestNameChange: (name: string) => void;
  isSubmitting: boolean;
  onSubmit: () => void;
  onCancel: () => void;
}

export function GuestAssignPanel({
  selectedSeats,
  guestName,
  onGuestNameChange,
  isSubmitting,
  onSubmit,
  onCancel,
}: GuestAssignPanelProps) {
  const t = useT();

  const isValid = guestName.trim().length > 0 && selectedSeats.length > 0;

  return (
    <div className="flex flex-col gap-4 rounded-lg border p-4 sm:p-6 bg-card">
      <div>
        <h3 className="font-medium text-lg">
          {t("liveview.assignMode") || "Plätze vergeben"}
        </h3>
        <p className="text-sm text-muted-foreground">
          {t("liveview.assignDescription") ||
            "Plätze auf dem Saalplan auswählen und für einen Gast reservieren / einchecken."}
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="guestName" className="text-sm font-medium">
          {t("liveview.guestNameLabel") || "Gast Name"}
        </Label>
        <Input
          id="guestName"
          type="text"
          value={guestName}
          onChange={(e) => onGuestNameChange(e.target.value)}
          placeholder={
            t("liveview.guestNamePlaceholder") || "z.B. Max Mustermann"
          }
        />
      </div>

      <div className="space-y-2 border-t pt-4">
        {selectedSeats.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("management.reservations.selectSeatsHint")}
          </p>
        ) : (
          <>
            <h4 className="text-sm font-medium">
              {t("management.reservations.selectedSeatsTitle")}
            </h4>
            <div className="flex max-h-20 flex-wrap gap-2 overflow-y-auto">
              {selectedSeats.map((seat) => (
                <Badge
                  key={seat.id?.toString()}
                  variant="outline"
                  className="bg-blue-100 border-blue-300 dark:bg-blue-900 dark:border-blue-700"
                >
                  {seat.seatNumber +
                    (seat.seatRow ? " (" + seat.seatRow + ")" : "")}
                </Badge>
              ))}
            </div>
            <p className="text-xs text-muted-foreground">
              {selectedSeats.length > 1
                ? t("management.reservations.multipleSeatsSelected", {
                    count: selectedSeats.length,
                  })
                : t("management.reservations.seatSelected")}
            </p>
          </>
        )}
      </div>

      <div className="flex flex-col gap-3 border-t pt-4">
        {!isValid && (
          <p className="text-center text-xs text-red-500">
            {selectedSeats.length === 0
              ? t("management.reservations.selectSeatsHint")
              : t("liveview.guestNameRequired") || "Bitte Namen des Gastes angeben"}
          </p>
        )}
        <div className="flex gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            className="flex-1 bg-transparent"
          >
            {t("common.cancel")}
          </Button>
          <Button
            type="button"
            isLoading={isSubmitting}
            disabled={isSubmitting || !isValid}
            onClick={onSubmit}
            className="flex-1"
          >
            {t("liveview.assignButton") || "Plätze vergeben & einchecken"}
          </Button>
        </div>
      </div>
    </div>
  );
}
