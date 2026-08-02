"use client";

import { Button } from "@/components/custom-ui/button";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/custom-ui/label";
import { UserSearchSelect } from "@/components/common/user-search-select";
import type { SeatDto, UserDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface ReservationActionPanelBaseProps {
  selectedSeats: SeatDto[];
  isSubmitting: boolean;
  onSubmit: () => void;
  onCancel: () => void;
}

type ReservationActionPanelProps =
  | (ReservationActionPanelBaseProps & {
      mode: "reserve";
      users: UserDto[];
      userId: string;
      onUserIdChange: (userId: string) => void;
      deductAllowance: boolean;
      onDeductAllowanceChange: (checked: boolean) => void;
    })
  | (ReservationActionPanelBaseProps & {
      mode: "block";
    });

export function ReservationActionPanel(props: ReservationActionPanelProps) {
  const t = useT();
  const { mode, selectedSeats, isSubmitting, onSubmit, onCancel } = props;

  const isValid =
    mode === "reserve"
      ? !!props.userId && selectedSeats.length > 0
      : selectedSeats.length > 0;

  return (
    <div className="flex flex-col gap-4 rounded-lg border p-4 sm:p-6">
      <div>
        <h3 className="font-medium">
          {mode === "reserve"
            ? t("management.reservations.newReservation")
            : t("management.reservations.blockSeats")}
        </h3>
        <p className="text-sm text-muted-foreground">
          {mode === "reserve"
            ? t("management.reservations.reserveDescription")
            : t("management.reservations.blockDescription")}
        </p>
      </div>

      {mode === "reserve" && (
        <UserSearchSelect
          users={props.users}
          selectedUserId={props.userId}
          onSelectionChange={props.onUserIdChange}
          label={t("management.reservations.userLabel")}
          placeholder={t("management.reservations.selectUserPlaceholder")}
        />
      )}

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

      {mode === "reserve" && (
        <div className="flex items-center space-x-3 border-t pt-4">
          <Checkbox
            id="deductAllowance"
            checked={props.deductAllowance}
            onCheckedChange={(checked) =>
              props.onDeductAllowanceChange(checked === true)
            }
          />
          <Label htmlFor="deductAllowance" className="text-sm">
            {t("management.reservations.deductAllowanceLabel")}
          </Label>
        </div>
      )}

      <div className="flex flex-col gap-3 border-t pt-4">
        {!isValid && (
          <p className="text-center text-xs text-red-500">
            {mode === "reserve"
              ? t("management.reservations.reserveValidationError")
              : t("management.reservations.blockValidationError")}
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
            {mode === "reserve"
              ? t("management.reservations.createButton")
              : selectedSeats.length === 1
                ? t("management.reservations.blockSeatButton")
                : t("management.reservations.blockSeatsButton", {
                    count: selectedSeats.length,
                  })}
          </Button>
        </div>
      </div>
    </div>
  );
}
