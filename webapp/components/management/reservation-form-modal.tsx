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
import SeatmapLegend from "@/components/common/seatmap-legend";
import type {
  EventResponseDto,
  ReservationRequestDto,
  SeatDto,
  UserDto,
  ReservationResponseDto,
  EventLocationMakerDto,
  SeatStatusDto,
  EventLocationResponseDto,
  AreaDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface ReservationFormModalProps {
  users: UserDto[];
  seats: SeatDto[];
  locations: EventLocationResponseDto[];
  events: EventResponseDto[];
  reservations?: ReservationResponseDto[];
  onSubmit: (reservationData: ReservationRequestDto) => Promise<void>;
  onClose: () => void;
}

export function ReservationFormModal({
  users,
  locations,
  seats,
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
  const [selectedSeats, setSelectedSeats] = useState<SeatDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [userSearch, setUserSearch] = useState("");

  const selectedEvent = events?.find(
    (event) => event.id?.toString() === formData.eventId,
  );
  const eventLocation = locations.find(
    (loc) => loc.id === selectedEvent?.eventLocationId,
  );
  const availableSeats: SeatDto[] =
    seats.filter((seat) => seat.locationId === eventLocation?.id) ?? [];
  const availableSeatStatuses: SeatStatusDto[] =
    selectedEvent?.seatStatuses ?? [];
  const availableMarkers: EventLocationMakerDto[] =
    eventLocation?.markers ?? [];
  const availableAreas: AreaDto[] = eventLocation?.areas ?? [];

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

  const handleSubmit = async (e?: React.FormEvent | React.KeyboardEvent) => {
    if (e) {
      e.preventDefault();
    }
    setIsLoading(true);

    try {
      const reservationData: ReservationRequestDto = {
        eventId: formData.eventId,
        userId: formData.userId,
        seatIds: selectedSeats.map((seat) => seat.id!),
        deductAllowance: formData.deductAllowance,
      };
      if (formData.userId !== "") {
        await onSubmit(reservationData);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleSeatSelect = (seat: SeatDto) => {
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

  const userReservedSeats: SeatDto[] =
    formData.userId && formData.eventId
      ? reservations
          .filter(
            (reservation) =>
              reservation.user?.id?.toString() === formData.userId &&
              reservation.eventId?.toString() === formData.eventId &&
              reservation.status === "RESERVED",
          )
          .map((reservation) => reservation.seat)
          .filter((seat): seat is SeatDto => seat !== undefined)
      : [];

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent
        className="max-w-6xl max-h-[90vh] h-[80vh] flex flex-col p-0 sm:p-6"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader className="pb-2 px-4 pt-4 sm:px-0 sm:pt-0">
          <DialogTitle className="text-lg">
            {t("reservationFormModal.createReservationTitle")}
          </DialogTitle>
          <DialogDescription className="text-sm">
            {t("reservationFormModal.createReservationDescription")}
          </DialogDescription>
        </DialogHeader>

        <form
          onSubmit={handleSubmit}
          onKeyDown={(e) => {
            if (
              e.key === "Enter" &&
              !e.shiftKey &&
              !(e.target instanceof HTMLTextAreaElement)
            ) {
              e.preventDefault();
              handleSubmit(e);
            }
          }}
          className="flex-1 flex flex-col lg:flex-row gap-6 min-h-0 overflow-y-auto lg:overflow-y-visible px-4 sm:px-0"
        >
          {/* Left side - Seat Map */}
          <div className="flex-1 flex flex-col min-h-0 lg:max-w-[calc(100%-20rem)] order-2 lg:order-1">
            <SeatmapLegend
              variant="selection"
              layout="bar"
              areas={availableAreas}
              userReservedLabel={t("reservationFormModal.userReservedStatus")}
              className="mb-4"
            />

            {formData.eventId && availableSeats.length > 0 ? (
              <div className="flex-1 flex items-center justify-center">
                <div className="w-full h-full max-h-[60vh] flex items-center justify-center">
                  <SeatMap
                    seats={availableSeats}
                    seatStatuses={availableSeatStatuses}
                    markers={availableMarkers}
                    areas={availableAreas}
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

          <div className="w-full lg:w-80 flex flex-col border p-4 sm:p-6 rounded-lg order-1 lg:order-2">
            <div className="space-y-4 sm:space-y-6">
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
                <div className="space-y-2 sm:space-y-3 border-t pt-4">
                  <h4 className="font-medium text-sm">
                    {t("reservationFormModal.selectedSeatsTitle")}
                  </h4>
                  <div className="flex flex-wrap gap-2 max-h-20 sm:max-h-12 overflow-y-auto">
                    {selectedSeats.map((seat) => (
                      <Badge
                        key={seat.id?.toString()}
                        variant="outline"
                        className="bg-blue-100 dark:bg-blue-900 border-blue-300 dark:border-blue-700"
                      >
                        {seat.seatNumber +
                          (seat.seatRow ? " (" + seat.seatRow + ")" : "")}
                      </Badge>
                    ))}
                  </div>
                  <p className="text-xs text-gray-600 dark:text-gray-400">
                    {selectedSeats.length > 1
                      ? t("reservationFormModal.multipleSeatsSelected", {
                          count: selectedSeats.length,
                        })
                      : t("reservationFormModal.seatSelected")}
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
            <div className="flex flex-col gap-3 lg:mt-auto pt-4 sm:pt-6 border-t">
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
