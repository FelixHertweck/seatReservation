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
import type { EventLocationResponseDto, EventLocationRequestDto } from "@/api";

interface LocationFormModalProps {
  location: EventLocationResponseDto | null;
  isCreating: boolean;
  onSubmit: (locationData: EventLocationRequestDto) => Promise<void>;
  onClose: () => void;
}

export function LocationFormModal({
  location,
  isCreating,
  onSubmit,
  onClose,
}: LocationFormModalProps) {
  const [formData, setFormData] = useState({
    name: location?.name || "",
    address: location?.address || "",
    capacity: location?.capacity?.toString() || "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const locationData: EventLocationRequestDto = {
        name: formData.name,
        address: formData.address,
        capacity: Number.parseInt(formData.capacity),
      };
      await onSubmit(locationData);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>
            {isCreating ? "Create Location" : "Edit Location"}
          </DialogTitle>
          <DialogDescription>
            {isCreating
              ? "Add a new event location"
              : "Update location information"}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">Location Name</Label>
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
            <Label htmlFor="address">Address</Label>
            <Textarea
              id="address"
              value={formData.address}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, address: e.target.value }))
              }
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="capacity">Capacity</Label>
            <Input
              id="capacity"
              type="number"
              value={formData.capacity}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, capacity: e.target.value }))
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
