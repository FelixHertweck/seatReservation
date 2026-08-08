"use client";

import { Button } from "@/components/custom-ui/button";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import { UserSearchSelect } from "@/components/common/user-search-select";
import type { LimitedUserInfoDto, SeatDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";

export type BoxOfficeReserveMode = "user" | "guest";

interface BoxOfficeActionPanelProps {
  reserveMode: BoxOfficeReserveMode;
  onReserveModeChange: (mode: BoxOfficeReserveMode) => void;
  users: LimitedUserInfoDto[];
  userId: string;
  onUserIdChange: (userId: string) => void;
  deductAllowance: boolean;
  onDeductAllowanceChange: (checked: boolean) => void;
  guestName: string;
  onGuestNameChange: (name: string) => void;
  guestEmail: string;
  onGuestEmailChange: (email: string) => void;
  checkedIn: boolean;
  onCheckedInChange: (checked: boolean) => void;
  selectedSeats: SeatDto[];
  isSubmitting: boolean;
  onSubmit: () => void;
}

export function BoxOfficeActionPanel({
  reserveMode,
  onReserveModeChange,
  users,
  userId,
  onUserIdChange,
  deductAllowance,
  onDeductAllowanceChange,
  guestName,
  onGuestNameChange,
  guestEmail,
  onGuestEmailChange,
  checkedIn,
  onCheckedInChange,
  selectedSeats,
  isSubmitting,
  onSubmit,
}: BoxOfficeActionPanelProps) {
  const t = useT();

  const isValid =
    selectedSeats.length > 0 &&
    (reserveMode === "user" ? !!userId : !!guestName.trim());

  return (
    <div className="flex flex-col gap-4 rounded-lg border p-4 sm:p-6">
      <div>
        <h3 className="font-medium">{t("boxOffice.newReservation")}</h3>
        <p className="text-sm text-muted-foreground">
          {t("boxOffice.description")}
        </p>
      </div>

      <div className="flex gap-2 rounded-md border p-1">
        <button
          type="button"
          onClick={() => onReserveModeChange("user")}
          className={cn(
            "flex-1 rounded-sm px-3 py-1.5 text-sm font-medium transition-colors",
            reserveMode === "user"
              ? "bg-primary text-primary-foreground"
              : "text-muted-foreground hover:bg-muted",
          )}
        >
          {t("boxOffice.modeUser")}
        </button>
        <button
          type="button"
          onClick={() => onReserveModeChange("guest")}
          className={cn(
            "flex-1 rounded-sm px-3 py-1.5 text-sm font-medium transition-colors",
            reserveMode === "guest"
              ? "bg-primary text-primary-foreground"
              : "text-muted-foreground hover:bg-muted",
          )}
        >
          {t("boxOffice.modeGuest")}
        </button>
      </div>

      {reserveMode === "user" ? (
        <UserSearchSelect
          users={users}
          selectedUserId={userId}
          onSelectionChange={onUserIdChange}
          label={t("boxOffice.userLabel")}
          placeholder={t("boxOffice.selectUserPlaceholder")}
        />
      ) : (
        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="boxOfficeGuestName" className="text-sm font-medium">
              {t("boxOffice.guestNameLabel")}
            </Label>
            <Input
              id="boxOfficeGuestName"
              type="text"
              value={guestName}
              onChange={(e) => onGuestNameChange(e.target.value)}
              placeholder={t("boxOffice.guestNamePlaceholder")}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="boxOfficeGuestEmail" className="text-sm font-medium">
              {t("boxOffice.guestEmailLabel")}
            </Label>
            <Input
              id="boxOfficeGuestEmail"
              type="email"
              value={guestEmail}
              onChange={(e) => onGuestEmailChange(e.target.value)}
              placeholder={t("boxOffice.guestEmailPlaceholder")}
            />
            <p className="text-xs text-muted-foreground">
              {t("boxOffice.guestEmailHint")}
            </p>
          </div>
        </div>
      )}

      <div className="space-y-2 border-t pt-4">
        {selectedSeats.length === 0 ? (
          <p className="text-sm text-muted-foreground">
            {t("boxOffice.selectSeatsHint")}
          </p>
        ) : (
          <>
            <h4 className="text-sm font-medium">
              {t("boxOffice.selectedSeatsTitle")}
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
          </>
        )}
      </div>

      {reserveMode === "user" && (
        <div className="flex items-center space-x-3 border-t pt-4">
          <Checkbox
            id="boxOfficeDeductAllowance"
            checked={deductAllowance}
            onCheckedChange={(checked) =>
              onDeductAllowanceChange(checked === true)
            }
          />
          <Label htmlFor="boxOfficeDeductAllowance" className="text-sm">
            {t("boxOffice.deductAllowanceLabel")}
          </Label>
        </div>
      )}

      <div className="flex items-center space-x-3 border-t pt-4">
        <Checkbox
          id="boxOfficeCheckedIn"
          checked={checkedIn}
          onCheckedChange={(checked) => onCheckedInChange(checked === true)}
        />
        <Label htmlFor="boxOfficeCheckedIn" className="text-sm">
          {t("boxOffice.checkedInLabel")}
        </Label>
      </div>

      <div className="flex flex-col gap-3 border-t pt-4">
        {!isValid && (
          <p className="text-center text-xs text-red-500">
            {reserveMode === "user"
              ? t("boxOffice.userValidationError")
              : t("boxOffice.guestValidationError")}
          </p>
        )}
        <Button
          type="button"
          isLoading={isSubmitting}
          disabled={isSubmitting || !isValid}
          onClick={onSubmit}
        >
          {t("boxOffice.createButton")}
        </Button>
      </div>
    </div>
  );
}
