"use client";

import { Button } from "@/components/custom-ui/button";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/custom-ui/label";
import { UserSearchSelect } from "@/components/common/user-search-select";
import type { EventUserAllowancesDto, SeatDto, UserDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";

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
      allowances?: EventUserAllowancesDto[];
      userId: string;
      onUserIdChange: (userId: string) => void;
      deductAllowance: boolean;
      onDeductAllowanceChange: (checked: boolean) => void;
    })
  | (ReservationActionPanelBaseProps & {
      mode: "block";
    });

function SelectedSeatsSummary({
  selectedSeats,
  isAllowanceExceeded,
}: Readonly<{
  selectedSeats: SeatDto[];
  isAllowanceExceeded: boolean;
}>) {
  const t = useT();
  if (selectedSeats.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        {t("management.reservations.selectSeatsHint")}
      </p>
    );
  }

  return (
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
            {seat.seatNumber + (seat.seatRow ? " (" + seat.seatRow + ")" : "")}
          </Badge>
        ))}
      </div>
      <p
        className={cn(
          "text-xs",
          isAllowanceExceeded
            ? "font-medium text-red-500"
            : "text-muted-foreground",
        )}
      >
        {selectedSeats.length > 1
          ? t("management.reservations.multipleSeatsSelected", {
              count: selectedSeats.length,
            })
          : t("management.reservations.seatSelected")}
      </p>
    </>
  );
}

export function ReservationActionPanel(
  props: Readonly<ReservationActionPanelProps>,
) {
  const t = useT();
  const { mode, selectedSeats, isSubmitting, onSubmit, onCancel } = props;

  const userAllowance =
    mode === "reserve" && props.userId
      ? (props.allowances?.find((a) => a.userId?.toString() === props.userId)
          ?.reservationsAllowedCount ?? 0)
      : undefined;

  const isAllowanceExceeded =
    mode === "reserve" &&
    props.deductAllowance &&
    !!props.userId &&
    selectedSeats.length > (userAllowance ?? 0);

  const isValid =
    mode === "reserve"
      ? !!props.userId && selectedSeats.length > 0 && !isAllowanceExceeded
      : selectedSeats.length > 0;

  let submitButtonLabel = t("management.reservations.createButton");
  if (mode === "block") {
    submitButtonLabel =
      selectedSeats.length === 1
        ? t("management.reservations.blockSeatButton")
        : t("management.reservations.blockSeatsButton", {
            count: selectedSeats.length,
          });
  }

  let validationErrorText: string | null = null;
  if (isAllowanceExceeded) {
    validationErrorText = t("management.reservations.allowanceExceeded", {
      count: selectedSeats.length,
      allowance: userAllowance ?? 0,
    });
  } else if (!isValid) {
    validationErrorText =
      mode === "reserve"
        ? t("management.reservations.reserveValidationError")
        : t("management.reservations.blockValidationError");
  }

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
        <div className="space-y-2">
          <UserSearchSelect
            users={props.users}
            selectedUserId={props.userId}
            onSelectionChange={props.onUserIdChange}
            label={t("management.reservations.userLabel")}
            placeholder={t("management.reservations.selectUserPlaceholder")}
          />
          {props.userId && (
            <div className="flex items-center justify-between rounded-md bg-muted/40 px-3 py-2 text-xs">
              <span className="text-muted-foreground">
                {t("management.reservations.userAllowanceLabel")}
              </span>
              <span
                className={cn(
                  "font-semibold",
                  (userAllowance ?? 0) === 0
                    ? "text-red-500 dark:text-red-400"
                    : "text-foreground",
                )}
              >
                {userAllowance ?? 0}
              </span>
            </div>
          )}
        </div>
      )}

      <div className="space-y-2 border-t pt-4">
        <SelectedSeatsSummary
          selectedSeats={selectedSeats}
          isAllowanceExceeded={isAllowanceExceeded}
        />
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
        {validationErrorText && (
          <p className="text-center text-xs font-medium text-red-500">
            {validationErrorText}
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
            {submitButtonLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
