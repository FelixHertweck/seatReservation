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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type {
  DetailedEventResponseDto,
  EventUserAllowancesDto,
  UserDto,
} from "@/api";

interface AllowanceFormModalProps {
  events: DetailedEventResponseDto[];
  users: UserDto[];
  onSubmit: (allowanceData: EventUserAllowancesDto) => Promise<void>;
  onClose: () => void;
}

export function AllowanceFormModal({
  events,
  users,
  onSubmit,
  onClose,
}: AllowanceFormModalProps) {
  const [formData, setFormData] = useState({
    eventId: "",
    userId: "",
    reservationsAllowedCount: "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const allowanceData: EventUserAllowancesDto = {
        eventId: BigInt(formData.eventId),
        userId: BigInt(formData.userId),
        reservationsAllowedCount: Number.parseInt(
          formData.reservationsAllowedCount,
        ),
      };
      await onSubmit(allowanceData);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Create Reservation Allowance</DialogTitle>
          <DialogDescription>
            Set reservation limits for a user on a specific event
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
                    value={event.id?.toString() ?? "unkown"}
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
            <Label htmlFor="count">Allowed Reservations</Label>
            <Input
              id="count"
              type="number"
              min="0"
              value={formData.reservationsAllowedCount}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  reservationsAllowedCount: e.target.value,
                }))
              }
              required
            />
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "Creating..." : "Create Allowance"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
