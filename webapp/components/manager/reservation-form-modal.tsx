"use client";

import type React from "react";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { SeatMap } from "@/components/common/seat-map";
import type {
  ManagerEventResponseDto,
  SeatWithStatusDto,
  UserDto,
  ManagerReservationRequestDto,
  ManagerReservationResponseDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface ReservationFormModalProps {
  users: UserDto[];
  events: ManagerEventResponseDto[];
  reservations?: ManagerReservationResponseDto[];
  onSubmit: (reservationData: ManagerReservationRequestDto) => Promise<void>;
  onClose: () => void;
}

export function ReservationFormModal({
  users,
  events,
  reservations = [],
  onSubmit,
  onClose,
}: ReservationFormModalProps) {
  const t = useT();

  const [formData, setFormData] = useState({
    eventId: "",
    userId: "",
    seatIds: [] as string[],
    deductAllowance: true,
  });
  const [selectedSeats, setSelectedSeats] = useState<SeatWithStatusDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [userSearch, setUserSearch] = useState("");

  const selectedEvent = events?.find(
    (event) => event.id?.toString() === formData.eventId,
  );
  const availableSeats: SeatWithStatusDto[] = selectedEvent?.eventLocation?.seats ?? [];

  const filteredUsers = users
    .filter((user) => {
      const username = user.username?.toLowerCase() || "";
      const searchTerm = userSearch.toLowerCase();
      return username.includes(searchTerm);
    })
    .sort((a, b) => {
      const usernameA = a.username?.toLowerCase() || "";
      const usernameB = b.username?.toLowerCase() || "";
      return usernameA.localeCompare(usernameB);
    });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const reservationData: ManagerReservationRequestDto = {
        eventId: BigInt(formData.eventId),
        userId: BigInt(formData.userId),
        seatIds: selectedSeats.map((seat) => seat.id!),
        deductAllowance: formData.deductAllowance,
      };
      await onSubmit(reservationData);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSeatSelect = (seat: SeatWithStatusDto) => {
    setSelectedSeats((prev) => {
      const isSelected = prev.some((s) => s.id === seat.id);
      if (isSelected) {
        return prev.filter((s) => s.id !== seat.id);
      } else {
        return [...prev, seat];
      }
    });
  };

  const handleEventChange = (value: string) => {
    setFormData((prev) => ({ ...prev, eventId: value }));
    setSelectedSeats([]);
  };

  const isFormValid =
    formData.eventId && formData.userId && selectedSeats.length > 0;

  const userReservedSeats: SeatWithStatusDto[] =
    formData.userId && formData.eventId
      ? reservations
          .filter(
            (reservation) =>
              reservation.user?.id?.toString() === formData.userId &&
              reservation.eventId?.toString() === formData.eventId,
          )
          .map((reservation) => reservation.seat)
          .filter((seat): seat is SeatWithStatusDto => seat !== undefined)
      : [];

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-6xl max-h-[90vh] h-[80vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>
            {t("reservationFormModal.createReservationTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("reservationFormModal.createReservationDescription")}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="flex-1 flex gap-6 min-h-0">
          {/* Left side - Seat Map */}
          <div className="flex-1 flex flex-col min-h-0 max-w-[calc(100%-20rem)]">
            <div className="flex gap-4 text-sm mb-4">
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-green-500 dark:bg-green-400 rounded"></div>
                <span>{t("reservationFormModal.availableStatus")}</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-blue-500 dark:bg-blue-400 rounded"></div>
                <span>{t("reservationFormModal.selectedStatus")}</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-red-500 dark:bg-red-400 rounded"></div>
                <span>{t("reservationFormModal.reservedStatus")}</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-yellow-500 dark:bg-yellow-400 rounded"></div>
                <span>{t("reservationFormModal.userReservedStatus")}</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 bg-gray-500 dark:bg-gray-400 rounded"></div>
                <span>{t("reservationFormModal.blockedStatus")}</span>
              </div>
            </div>

            {formData.eventId && availableSeats.length > 0 ? (
              <div className="flex-1 flex items-center justify-center">
                <div className="w-full h-full max-h-[60vh] flex items-center justify-center">
                  <SeatMap
                    seats={availableSeats}
                    selectedSeats={selectedSeats}
                    userReservedSeats={userReservedSeats}
                    onSeatSelect={handleSeatSelect}
                  />
                </div>
              </div>
            ) : (
              <div className="flex-1 flex items-center justify-center text-gray-500">
                {!formData.eventId
                  ? t("reservationFormModal.selectEventToViewSeats")
                  : t("reservationFormModal.noSeatsAvailableForEvent")}
              </div>
            )}
          </div>

          <div className="w-80 flex flex-col bg-gray-50 dark:bg-gray-900 p-6 rounded-lg">
            <div className="space-y-6">
              {/* Event and User Selection in a grid */}
              <div className="grid grid-cols-1 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="event" className="text-sm font-medium">
                    {t("reservationFormModal.eventLabel")}
                  </Label>
                  <Select
                    value={formData.eventId}
                    onValueChange={handleEventChange}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue
                        placeholder={t(
                          "reservationFormModal.selectEventPlaceholder",
                        )}
                      />
                    </SelectTrigger>
                    <SelectContent>
                      {events.map((event) => (
                        <SelectItem
                          key={event.id?.toString()}
                          value={event.id?.toString() ?? "unknown"}
                        >
                          {event.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="user" className="text-sm font-medium">
                    {t("reservationFormModal.userLabel")}
                  </Label>
                  <Input
                    placeholder={t(
                      "reservationFormModal.filterUsersPlaceholder",
                    )}
                    value={userSearch}
                    onChange={(e) => setUserSearch(e.target.value)}
                    className="w-full"
                  />
                  <Select
                    value={formData.userId}
                    onValueChange={(value) =>
                      setFormData((prev) => ({ ...prev, userId: value }))
                    }
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue
                        placeholder={t(
                          "reservationFormModal.selectUserPlaceholder",
                        )}
                      />
                    </SelectTrigger>
                    <SelectContent>
                      {filteredUsers.map((user) => (
                        <SelectItem
                          key={user.id?.toString()}
                          value={user.id?.toString() ?? "unknown"}
                        >
                          {user.username}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* Selected Seats Section */}
              {selectedSeats.length > 0 && (
                <div className="space-y-3 border-t pt-4">
                  <h4 className="font-medium text-sm">
                    {t("reservationFormModal.selectedSeatsTitle")}
                  </h4>
                  <div className="flex flex-wrap gap-2">
                    {selectedSeats.map((seat) => (
                      <Badge
                        key={seat.id?.toString()}
                        variant="outline"
                        className="bg-blue-100 dark:bg-blue-900 border-blue-300 dark:border-blue-700"
                      >
                        {seat.seatNumber}
                      </Badge>
                    ))}
                  </div>
                  <p className="text-xs text-gray-600 dark:text-gray-400">
                    {t("reservationFormModal.seatsSelected", {
                      count: selectedSeats.length,
                    })}
                  </p>
                </div>
              )}

              {/* Deduct Allowance Option */}
              <div className="flex items-center space-x-3 border-t pt-4">
                <input
                  type="checkbox"
                  id="deductAllowance"
                  checked={formData.deductAllowance}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      deductAllowance: e.target.checked,
                    }))
                  }
                  className="rounded"
                />
                <Label htmlFor="deductAllowance" className="text-sm">
                  {t("reservationFormModal.deductAllowanceLabel")}
                </Label>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex flex-col gap-3 mt-auto pt-6 border-t">
              {!isFormValid && (
                <p className="text-xs text-red-500 text-center">
                  {t("reservationFormModal.validationError")}
                </p>
              )}
              <div className="flex gap-3">
                <Button
                  type="button"
                  variant="outline"
                  onClick={onClose}
                  className="flex-1 bg-transparent"
                >
                  {t("reservationFormModal.cancelButton")}
                </Button>
                <Button
                  type="submit"
                  disabled={isLoading || !isFormValid}
                  className="flex-1"
                >
                  {isLoading
                    ? t("reservationFormModal.creatingButton")
                    : t("reservationFormModal.createButton")}
                </Button>
              </div>
            </div>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
