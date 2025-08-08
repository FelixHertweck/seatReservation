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
  ReservationRequestDto,
  SeatDto,
  UserDto,
} from "@/api";

interface ReservationFormModalProps {
  users: UserDto[];
  events: DetailedEventResponseDto[];
  seats: SeatDto[];
  onSubmit: (reservationData: ReservationRequestDto) => Promise<void>;
  onClose: () => void;
}

export function ReservationFormModal({
  users,
  events,
  seats,
  onSubmit,
  onClose,
}: ReservationFormModalProps) {
  const [formData, setFormData] = useState({
    eventId: "",
    userId: "",
    seatIds: [] as string[],
    deductAllowance: true,
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const reservationData: ReservationRequestDto = {
        eventId: BigInt(formData.eventId),
        userId: BigInt(formData.userId),
        seatIds: formData.seatIds.map((id) => BigInt(id)),
        deductAllowance: formData.deductAllowance,
      };
      await onSubmit(reservationData);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSeatToggle = (seatId: string) => {
    setFormData((prev) => ({
      ...prev,
      seatIds: prev.seatIds.includes(seatId)
        ? prev.seatIds.filter((id) => id !== seatId)
        : [...prev.seatIds, seatId],
    }));
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Create Reservation</DialogTitle>
          <DialogDescription>
            Create a new reservation for multiple seats
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
            <Label>Seats (select multiple)</Label>
            <div className="max-h-32 overflow-y-auto border rounded p-2 space-y-1">
              {seats.map((seat) => (
                <div
                  key={seat.id?.toString()}
                  className="flex items-center space-x-2"
                >
                  <input
                    type="checkbox"
                    id={`seat-${seat.id}`}
                    checked={formData.seatIds.includes(
                      seat.id?.toString() ?? "",
                    )}
                    onChange={() => handleSeatToggle(seat.id?.toString() ?? "")}
                    className="rounded"
                  />
                  <label htmlFor={`seat-${seat.id}`} className="text-sm">
                    {seat.seatNumber}
                  </label>
                </div>
              ))}
            </div>
            <p className="text-xs text-gray-500">
              Selected: {formData.seatIds.length} seat(s)
            </p>
          </div>

          <div className="flex items-center space-x-2">
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
              Deduct from user allowance
            </Label>
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isLoading || formData.seatIds.length === 0}
            >
              {isLoading ? "Creating..." : "Create Reservation"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
