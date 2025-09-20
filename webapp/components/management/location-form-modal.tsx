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
import { Plus, Trash2 } from "lucide-react";
import type {
  EventLocationResponseDto,
  EventLocationRequestDto,
  EventLocationMakerRequestDto,
} from "@/api";
import { useT } from "@/lib/i18n/hooks";

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
  const t = useT();

  const [formData, setFormData] = useState({
    name: location?.name || "",
    address: location?.address || "",
    capacity: location?.capacity?.toString() || "",
  });
  const [markers, setMarkers] = useState<EventLocationMakerRequestDto[]>(
    location?.markers?.map((m) => ({
      label: m.label || "",
      xCoordinate: m.xCoordinate || 0,
      yCoordinate: m.yCoordinate || 0,
    })) || [],
  );
  const [isLoading, setIsLoading] = useState(false);

  const addMarker = () => {
    setMarkers([...markers, { label: "", xCoordinate: 0, yCoordinate: 0 }]);
  };

  const removeMarker = (index: number) => {
    setMarkers(markers.filter((_, i) => i !== index));
  };

  const updateMarker = (
    index: number,
    field: keyof EventLocationMakerRequestDto,
    value: string | number,
  ) => {
    const updatedMarkers = [...markers];
    updatedMarkers[index] = { ...updatedMarkers[index], [field]: value };
    setMarkers(updatedMarkers);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      const locationData: EventLocationRequestDto = {
        name: formData.name,
        address: formData.address,
        capacity: Number.parseInt(formData.capacity),
        markers: markers.length > 0 ? markers : undefined,
      };
      await onSubmit(locationData);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-lg max-h-[80vh] overflow-y-auto">
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

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>Marker</Label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={addMarker}
                className="h-8 px-2"
              >
                <Plus className="h-3 w-3 mr-1" />
                {t("locationFormModal.addMarkerButton")}
              </Button>
            </div>

            {markers.map((marker, index) => (
              <div
                key={index}
                className="border rounded-md p-3 space-y-2 bg-gray-50 dark:bg-gray-800/50 dark:border-gray-700"
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {t("locationFormModal.markerLabel")} {index + 1}
                  </span>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => removeMarker(index)}
                    className="h-6 px-2"
                  >
                    <Trash2 className="h-3 w-3" />
                  </Button>
                </div>

                <div className="grid grid-cols-3 gap-2">
                  <div>
                    <Label
                      htmlFor={`marker-label-${index}`}
                      className="text-xs text-gray-700 dark:text-gray-300"
                    >
                      {t("locationFormModal.markerLabel")}
                    </Label>
                    <Input
                      id={`marker-label-${index}`}
                      value={marker.label}
                      onChange={(e) =>
                        updateMarker(index, "label", e.target.value)
                      }
                      placeholder="Label"
                      className="h-8 text-sm"
                      required
                    />
                  </div>

                  <div>
                    <Label
                      htmlFor={`marker-x-${index}`}
                      className="text-xs text-gray-700 dark:text-gray-300"
                    >
                      {t("locationFormModal.xCoordinateLabel")}
                    </Label>
                    <Input
                      id={`marker-x-${index}`}
                      type="number"
                      value={marker.xCoordinate}
                      onChange={(e) =>
                        updateMarker(
                          index,
                          "xCoordinate",
                          Number(e.target.value),
                        )
                      }
                      placeholder="X"
                      className="h-8 text-sm"
                      required
                    />
                  </div>

                  <div>
                    <Label
                      htmlFor={`marker-y-${index}`}
                      className="text-xs text-gray-700 dark:text-gray-300"
                    >
                      {t("locationFormModal.yCoordinateLabel")}
                    </Label>
                    <Input
                      id={`marker-y-${index}`}
                      type="number"
                      value={marker.yCoordinate}
                      onChange={(e) =>
                        updateMarker(
                          index,
                          "yCoordinate",
                          Number(e.target.value),
                        )
                      }
                      placeholder="Y"
                      className="h-8 text-sm"
                      required
                    />
                  </div>
                </div>
              </div>
            ))}
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
