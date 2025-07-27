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
  EventUserAllowancesRequestDto,
  UserDto,
} from "@/api";
import {Checkbox} from "@/components/ui/checkbox";

interface AllowanceFormModalProps {
  events: DetailedEventResponseDto[];
  users: UserDto[];
  onSubmit: (allowanceData: EventUserAllowancesRequestDto) => Promise<void>;
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
    userIds: [] as string[],
    reservationsAllowedCount: "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const allowanceData: EventUserAllowancesRequestDto = {
        eventId: BigInt(formData.eventId),
        userIds: formData.userIds.map((id) => BigInt(id)),
        reservationsAllowedCount: Number.parseInt(
          formData.reservationsAllowedCount,
        ),
      };
      await onSubmit(allowanceData);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUserToggle = (userId: string, checked: boolean) => {
    setFormData((prev) => ({
      ...prev,
      userIds: checked ? [...prev.userIds, userId] : prev.userIds.filter((id) => id !== userId),
    }))
  }

  return (
      <Dialog open onOpenChange={onClose}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Create Reservation Allowance</DialogTitle>
            <DialogDescription>Set reservation limits for users on a specific event</DialogDescription>
          </DialogHeader>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="event">Event</Label>
              <Select
                  value={formData.eventId}
                  onValueChange={(value) => setFormData((prev) => ({ ...prev, eventId: value }))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select an event" />
                </SelectTrigger>
                <SelectContent>
                  {events.map((event) => (
                      <SelectItem key={event.id?.toString()} value={event.id?.toString() ?? "unknown"}>
                        {event.name}
                      </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Users</Label>
              <div className="space-y-2 max-h-40 overflow-y-auto border p-2 rounded">
                {users.map((user) => (
                    <div key={user.id?.toString()} className="flex items-center space-x-2">
                      <Checkbox
                          id={`user-${user.id?.toString()}`}
                          checked={formData.userIds.includes(user.id?.toString() ?? "unknown")}
                          onCheckedChange={(checked) => handleUserToggle(user.id?.toString() ?? "unknown", checked as boolean)}
                      />
                      <Label htmlFor={`user-${user.id?.toString()}`}>{user.username}</Label>
                    </div>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="count">Allowed Reservations</Label>
              <Input
                  id="count"
                  type="number"
                  min="0"
                  value={formData.reservationsAllowedCount}
                  onChange={(e) => setFormData((prev) => ({ ...prev, reservationsAllowedCount: e.target.value }))}
                  required
              />
            </div>

            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit" disabled={isLoading || formData.userIds.length === 0}>
                {isLoading ? "Creating..." : "Create Allowance"}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
  );
}
