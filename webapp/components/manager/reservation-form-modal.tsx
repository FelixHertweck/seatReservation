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
import type {
  DetailedEventResponseDto,
  DetailedReservationResponseDto,
  ReservationRequestDto,
  SeatDto,
  UserDto,
} from "@/api";

interface ReservationFormModalProps {
  users: UserDto[];
  events: DetailedEventResponseDto[];
  seats: SeatDto[];
  reservation: DetailedReservationResponseDto | null;
  isCreating: boolean;
  onSubmit: (reservationData: ReservationRequestDto) => Promise<void>;
  onClose: () => void;
}

export function ReservationFormModal({
  users,
  events,
  seats,
  reservation,
  isCreating,
  onSubmit,
  onClose,
}: ReservationFormModalProps) {
  const [formData, setFormData] = useState({
    eventId: reservation?.event?.id?.toString() || "",
    userId: reservation?.user?.id?.toString() || "",
    seatId: reservation?.seat?.id?.toString() || "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const reservationData: ReservationRequestDto = {
        eventId: BigInt(formData.eventId),
        userId: BigInt(formData.userId),
        seatId: BigInt(formData.seatId),
      };
      await onSubmit(reservationData);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>
            {isCreating ? "Create Reservation" : "Edit Reservation"}
          </DialogTitle>
          <DialogDescription>
            {isCreating
              ? "Create a new reservation"
              : "Update reservation details"}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="event">Event</Label>
            <Select
              value={formData.eventId}
              onValueChange={(value) =>
                setFormData((prev) => ({ ...prev, eventId: value }))
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Select an event" />
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
            <Label htmlFor="user">User</Label>
            <Select
              value={formData.userId}
              onValueChange={(value) =>
                setFormData((prev) => ({ ...prev, userId: value }))
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Select a user" />
              </SelectTrigger>
              <SelectContent>
                {users.map((user) => (
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

          <div className="space-y-2">
            <Label htmlFor="seat">Seat</Label>
            <Select
              value={formData.seatId}
              onValueChange={(value) =>
                setFormData((prev) => ({ ...prev, seatId: value }))
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Select a seat" />
              </SelectTrigger>
              <SelectContent>
                {seats.map((seat) => (
                  <SelectItem
                    key={seat.id?.toString()}
                    value={seat.id?.toString() ?? "unknown"}
                  >
                    {seat.seatNumber}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "Saving..." : isCreating ? "Create" : "Update"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
