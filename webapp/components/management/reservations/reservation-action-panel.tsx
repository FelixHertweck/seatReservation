"use client";

import { X } from "lucide-react";
import { Button } from "@/components/custom-ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/custom-ui/label";
import { UserSearchSelect } from "@/components/common/user-search-select";
import type { EventUserAllowancesDto, SeatDto, UserDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";

interface ReservationActionPanelBaseProps {
  className?: string;
  selectedSeats: SeatDto[];
  highlightedSeatId?: string | null;
  onSeatChipClick?: (seatId: string) => void;
  onSeatRemove?: (seat: SeatDto) => void;
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
      userReservedCount: number;
      deductAllowance: boolean;
      onDeductAllowanceChange: (checked: boolean) => void;
    })
  | (ReservationActionPanelBaseProps & {
      mode: "block";
    });

function SelectedSeatsSummary({
  selectedSeats,
  isAllowanceExceeded,
  highlightedSeatId,
  onSeatChipClick,
  onSeatRemove,
}: Readonly<{
  selectedSeats: SeatDto[];
  isAllowanceExceeded: boolean;
  highlightedSeatId: string | null;
  onSeatChipClick?: (seatId: string) => void;
  onSeatRemove?: (seat: SeatDto) => void;
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
      <div className="flex max-h-28 flex-wrap gap-2 overflow-y-auto">
        {selectedSeats.map((seat) => {
          const isHighlighted = !!seat.id && seat.id === highlightedSeatId;
          return (
            <div
              key={seat.id?.toString()}
              className={cn(
                "flex items-center gap-1 rounded-md border py-1 pl-1 pr-1 text-sm transition-colors",
                isHighlighted
                  ? "bg-primary/10 border-primary"
                  : "bg-blue-100 border-blue-300 hover:bg-blue-200 dark:bg-blue-900 dark:border-blue-700 dark:hover:bg-blue-800",
              )}
            >
              <button
                type="button"
                onClick={() => seat.id && onSeatChipClick?.(seat.id)}
                className="px-1"
              >
                {seat.seatNumber +
                  (seat.seatRow ? " (" + seat.seatRow + ")" : "")}
              </button>
              {onSeatRemove && (
                <button
                  type="button"
                  aria-label={t("management.reservations.removeSeatAriaLabel")}
                  onClick={() => onSeatRemove(seat)}
                  className="rounded-full p-0.5 transition-colors hover:bg-destructive/20 hover:text-destructive"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              )}
            </div>
          );
        })}
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
  const {
    className,
    mode,
    selectedSeats,
    highlightedSeatId = null,
    onSeatChipClick,
    onSeatRemove,
    isSubmitting,
    onSubmit,
    onCancel,
  } = props;

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

  let validationText: string | null = null;
  if (isAllowanceExceeded) {
    validationText = t("management.reservations.allowanceExceeded", {
      count: selectedSeats.length,
      allowance: userAllowance ?? 0,
    });
  } else if (!isValid) {
    validationText =
      mode === "reserve"
        ? t("management.reservations.reserveValidationError")
        : t("management.reservations.blockValidationError");
  }

  return (
    <div
      className={cn(
        "flex flex-col gap-4 rounded-lg border p-4 sm:p-6",
        className,
      )}
    >
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
            <div className="space-y-1 rounded-md bg-muted/40 px-3 py-2 text-xs">
              <div className="flex items-center justify-between">
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
              <div className="flex items-center justify-between">
                <span className="text-muted-foreground">
                  {t("management.reservations.userReservedCountLabel")}
                </span>
                <span className="font-semibold text-foreground">
                  {props.userReservedCount}
                </span>
              </div>
            </div>
          )}
        </div>
      )}

      <div className="space-y-2 border-t pt-4">
        <SelectedSeatsSummary
          selectedSeats={selectedSeats}
          isAllowanceExceeded={isAllowanceExceeded}
          highlightedSeatId={highlightedSeatId}
          onSeatChipClick={onSeatChipClick}
          onSeatRemove={onSeatRemove}
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
        {validationText && (
          <p
            className={cn(
              "text-center text-xs",
              isAllowanceExceeded
                ? "font-medium text-red-500"
                : "text-muted-foreground",
            )}
          >
            {validationText}
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
