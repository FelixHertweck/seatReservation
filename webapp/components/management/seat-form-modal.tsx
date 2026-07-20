"use client";

import type React from "react";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
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
import type { SeatRequestDto, EventLocationResponseDto, SeatDto } from "@/api";
import { getApiManagerEntrancesOptions } from "@/api/@tanstack/react-query.gen";
import { useT } from "@/lib/i18n/hooks";

interface SeatFormModalProps {
  seat: SeatDto | null;
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
  const t = useT();

  const [formData, setFormData] = useState({
    seatNumber: seat?.seatNumber || "",
    seatRow: seat?.seatRow || "",
    eventLocationId: seat?.locationId?.toString() || "",
    xCoordinate: seat?.coordinate?.xCoordinate?.toString() || "",
    yCoordinate: seat?.coordinate?.yCoordinate?.toString() || "",
    entranceId: seat?.entranceId?.toString() || "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const { data: entrances = [] } = useQuery({
    ...getApiManagerEntrancesOptions({
      query: { eventLocationId: BigInt(formData.eventLocationId || 0) },
    }),
    enabled: !!formData.eventLocationId,
  });

  const handleSubmit = async (e?: React.FormEvent | React.KeyboardEvent) => {
    if (e) {
      e.preventDefault();
    }
    setIsLoading(true);

    try {
      const seatData: SeatRequestDto = {
        seatNumber: formData.seatNumber,
        seatRow: formData.seatRow,
        eventLocationId: BigInt(formData.eventLocationId),
        coordinate: {
          xCoordinate: Number.parseInt(formData.xCoordinate),
          yCoordinate: Number.parseInt(formData.yCoordinate),
        },
        entranceId: formData.entranceId
          ? BigInt(formData.entranceId)
          : undefined,
      };
      await onSubmit(seatData);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent
        className="max-w-md"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle>
            {isCreating
              ? t("seatFormModal.createSeatTitle")
              : t("seatFormModal.editSeatTitle")}
          </DialogTitle>
          <DialogDescription>
            {isCreating
              ? t("seatFormModal.addSeatDescription")
              : t("seatFormModal.updateSeatDescription")}
          </DialogDescription>
        </DialogHeader>

        <form
          onSubmit={handleSubmit}
          onKeyDown={(e) => {
            if (
              e.key === "Enter" &&
              !e.shiftKey &&
              !(e.target instanceof HTMLTextAreaElement)
            ) {
              e.preventDefault();
              handleSubmit(e as React.FormEvent<HTMLFormElement>);
            }
          }}
          className="space-y-4"
        >
          <div className="space-y-2">
            <Label htmlFor="seatNumber">
              {t("seatFormModal.seatNumberLabel")}
            </Label>
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
            <Label htmlFor="location">{t("seatFormModal.locationLabel")}</Label>
            <Select
              value={formData.eventLocationId}
              onValueChange={(value) =>
                setFormData((prev) => ({ ...prev, eventLocationId: value }))
              }
            >
              <SelectTrigger>
                <SelectValue
                  placeholder={t("seatFormModal.selectLocationPlaceholder")}
                />
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

          <div className="space-y-2">
            <Label htmlFor="seatRow">{t("seatFormModal.rowLabel")}</Label>
            <Input
              id="seatRow"
              type="text"
              value={formData.seatRow}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  seatRow: e.target.value,
                }))
              }
              required
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="xCoordinate">
                {t("seatFormModal.xCoordinateLabel")}
              </Label>
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
              <Label htmlFor="yCoordinate">
                {t("seatFormModal.yCoordinateLabel")}
              </Label>
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
          <div className="space-y-2">
            <Label htmlFor="entrance">{t("seatFormModal.entrance")}</Label>
            <Select
              value={formData.entranceId}
              onValueChange={(value) =>
                setFormData((prev) => ({ ...prev, entranceId: value }))
              }
              disabled={!formData.eventLocationId}
            >
              <SelectTrigger id="entrance">
                <SelectValue
                  placeholder={t("seatFormModal.selectEntrancePlaceholder")}
                />
              </SelectTrigger>
              <SelectContent>
                {entrances.map((entrance) => (
                  <SelectItem
                    key={entrance.id?.toString()}
                    value={entrance.id?.toString() ?? "unknown"}
                  >
                    {entrance.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              {t("seatFormModal.cancelButton")}
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading
                ? t("seatFormModal.savingButton")
                : isCreating
                  ? t("seatFormModal.createButton")
                  : t("seatFormModal.updateButton")}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
