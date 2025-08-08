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
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type {
  DetailedEventResponseDto,
  EventLocationResponseDto,
  EventRequestDto,
} from "@/api";

interface EventFormModalProps {
  allLocations: EventLocationResponseDto[];
  event: DetailedEventResponseDto | null;
  isCreating: boolean;
  onSubmit: (eventData: EventRequestDto) => Promise<void>;
  onClose: () => void;
}

export function EventFormModal({
  allLocations,
  event,
  isCreating,
  onSubmit,
  onClose,
}: EventFormModalProps) {
  const [formData, setFormData] = useState({
    name: event?.name || "",
    description: event?.description || "",
    startTime: event?.startTime
      ? new Date(event.startTime).toISOString().slice(0, 16)
      : "",
    endTime: event?.endTime
      ? new Date(event.endTime).toISOString().slice(0, 16)
      : "",
    bookingDeadline: event?.bookingDeadline
      ? new Date(event.bookingDeadline).toISOString().slice(0, 16)
      : "",
    eventLocationId: event?.eventLocationId?.toString() || "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const eventData: EventRequestDto = {
        name: formData.name,
        description: formData.description,
        startTime: new Date(formData.startTime),
        endTime: new Date(formData.endTime),
        bookingDeadline: new Date(formData.bookingDeadline),
        eventLocationId: BigInt(formData.eventLocationId),
      };
      await onSubmit(eventData);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>
            {isCreating ? "Create Event" : "Edit Event"}
          </DialogTitle>
          <DialogDescription>
            {isCreating
              ? "Add a new event to the system"
              : "Update event information"}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">Event Name</Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, name: e.target.value }))
              }
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  description: e.target.value,
                }))
              }
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="location">Location</Label>
            <Select
              value={formData.eventLocationId}
              onValueChange={(value) =>
                setFormData((prev) => ({ ...prev, eventLocationId: value }))
              }
            >
              <SelectTrigger>
                <SelectValue placeholder="Select a location" />
              </SelectTrigger>
              <SelectContent>
                {allLocations.map((location) => (
                  <SelectItem
                    key={location.id?.toString()}
                    value={location.id?.toString() ?? ""}
                  >
                    {location.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="startTime">Start Time</Label>
              <Input
                id="startTime"
                type="datetime-local"
                value={formData.startTime}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    startTime: e.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="endTime">End Time</Label>
              <Input
                id="endTime"
                type="datetime-local"
                value={formData.endTime}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, endTime: e.target.value }))
                }
                required
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="bookingDeadline">Booking Deadline</Label>
            <Input
              id="bookingDeadline"
              type="datetime-local"
              value={formData.bookingDeadline}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  bookingDeadline: e.target.value,
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
              {isLoading ? "Saving..." : isCreating ? "Create" : "Update"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
