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
  SeatResponseDto,
  SeatRequestDto,
  EventLocationResponseDto,
} from "@/api";

interface SeatFormModalProps {
  seat: SeatResponseDto | null;
  locations: EventLocationResponseDto[];
  isCreating: boolean;
  onSubmit: (seatData: SeatRequestDto) => Promise<void>;
  onClose: () => void;
}

export function SeatFormModal({
  seat,
  locations,
  isCreating,
  onSubmit,
  onClose,
}: SeatFormModalProps) {
  const [formData, setFormData] = useState({
    seatNumber: seat?.seatNumber || "",
    eventLocationId: seat?.location?.id?.toString() || "",
    xCoordinate: seat?.xCoordinate?.toString() || "",
    yCoordinate: seat?.yCoordinate?.toString() || "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const seatData: SeatRequestDto = {
        seatNumber: formData.seatNumber,
        eventLocationId: BigInt(formData.eventLocationId),
        xCoordinate: Number.parseInt(formData.xCoordinate),
        yCoordinate: Number.parseInt(formData.yCoordinate),
      };
      await onSubmit(seatData);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{isCreating ? "Create Seat" : "Edit Seat"}</DialogTitle>
          <DialogDescription>
            {isCreating
              ? "Add a new seat to a location"
              : "Update seat information"}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="seatNumber">Seat Number</Label>
            <Input
              id="seatNumber"
              value={formData.seatNumber}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, seatNumber: e.target.value }))
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
                {locations.map((location) => (
                  <SelectItem
                    key={location.id?.toString()}
                    value={location.id?.toString() ?? "unknown"}
                  >
                    {location.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="xCoordinate">X Coordinate</Label>
              <Input
                id="xCoordinate"
                type="number"
                value={formData.xCoordinate}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    xCoordinate: e.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="yCoordinate">Y Coordinate</Label>
              <Input
                id="yCoordinate"
                type="number"
                value={formData.yCoordinate}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    yCoordinate: e.target.value,
                  }))
                }
                required
              />
            </div>
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
