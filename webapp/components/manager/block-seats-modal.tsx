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
import { Checkbox } from "@/components/ui/checkbox";
import type {
  BlockSeatsRequestDto,
  DetailedEventResponseDto,
  SeatDto,
} from "@/api";

interface BlockSeatsModalProps {
  events: DetailedEventResponseDto[];
  onSubmit: (blockData: BlockSeatsRequestDto) => Promise<void>;
  onClose: () => void;
}

export function BlockSeatsModal({
  events,
  onSubmit,
  onClose,
}: BlockSeatsModalProps) {
  const [formData, setFormData] = useState({
    eventId: "",
    seatIds: [] as string[],
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const blockData: BlockSeatsRequestDto = {
        eventId: formData.eventId ? BigInt(formData.eventId) : undefined,
        seatIds: formData.seatIds.map((id) => BigInt(id)),
      };
      await onSubmit(blockData);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSeatToggle = (seatId: string, checked: boolean) => {
    setFormData((prev) => ({
      ...prev,
      seatIds: checked
        ? [...prev.seatIds, seatId]
        : prev.seatIds.filter((id) => id !== seatId),
    }));
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Block Seats</DialogTitle>
          <DialogDescription>
            Block seats to prevent reservations
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="event">Event (Optional)</Label>
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
            <Label>Seats to Block</Label>
            <div className="space-y-2 max-h-40 overflow-y-auto">
              {getSeatsForEvent(formData.eventId, events).map((seat) => (
                <div
                  key={seat.id?.toString()}
                  className="flex items-center space-x-2"
                >
                  <Checkbox
                    id={seat.id?.toString()}
                    checked={formData.seatIds.includes(
                      seat.id?.toString() ?? "unknown",
                    )}
                    onCheckedChange={(checked) =>
                      handleSeatToggle(
                        seat.id?.toString() ?? "unknown",
                        checked as boolean,
                      )
                    }
                  />
                  <Label htmlFor={seat.id?.toString()}>{seat.seatNumber}</Label>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isLoading || formData.seatIds.length === 0}
            >
              {isLoading ? "Blocking..." : "Block Seats"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

const getSeatsForEvent = (
  eventId: string,
  events: DetailedEventResponseDto[],
): SeatDto[] => {
  const event = events.find((e) => e.id?.toString() === eventId);
  return event ? event.location?.seats || [] : [];
};
