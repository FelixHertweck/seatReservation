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
import { t } from "i18next";

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
            {isCreating
              ? t("locationFormModal.createLocationTitle")
              : t("locationFormModal.editLocationTitle")}
          </DialogTitle>
          <DialogDescription>
            {isCreating
              ? t("locationFormModal.addLocationDescription")
              : t("locationFormModal.updateLocationDescription")}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">
              {t("locationFormModal.locationNameLabel")}
            </Label>
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
            <Label htmlFor="address">
              {t("locationFormModal.addressLabel")}
            </Label>
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
            <Label htmlFor="capacity">
              {t("locationFormModal.capacityLabel")}
            </Label>
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
              {t("locationFormModal.cancelButton")}
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading
                ? t("locationFormModal.savingButton")
                : isCreating
                  ? t("locationFormModal.createButton")
                  : t("locationFormModal.updateButton")}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
