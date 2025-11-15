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
  EventResponseDto,
  EventLocationResponseDto,
  EventRequestDto,
  LimitedUserInfoDto,
} from "@/api";
import { UserMultiSelect } from "@/components/common/user-multi-select";
import { useT } from "@/lib/i18n/hooks";

interface EventFormModalProps {
  allLocations: EventLocationResponseDto[];
  event: EventResponseDto | null;
  isCreating: boolean;
  onSubmit: (eventData: EventRequestDto) => Promise<void>;
  onClose: () => void;
  users?: LimitedUserInfoDto[];
}

export function EventFormModal({
  allLocations,
  event,
  isCreating,
  onSubmit,
  onClose,
  users = [],
}: EventFormModalProps) {
  const t = useT();

  const [formData, setFormData] = useState({
    name: event?.name || "",
    description: event?.description || "",
    startTime: event?.startTime
      ? new Date(event.startTime).toLocaleString("sv-SE").slice(0, 16)
      : "",
    endTime: event?.endTime
      ? new Date(event.endTime).toLocaleString("sv-SE").slice(0, 16)
      : "",
    bookingDeadline: event?.bookingDeadline
      ? new Date(event.bookingDeadline).toLocaleString("sv-SE").slice(0, 16)
      : "",
    bookingStartTime: event?.bookingStartTime
      ? new Date(event.bookingStartTime).toLocaleString("sv-SE").slice(0, 16)
      : "",
    reminderSendDate: event?.reminderSendDate
      ? new Date(event.reminderSendDate).toLocaleString("sv-SE").slice(0, 16)
      : "",
    eventLocationId: event?.eventLocationId?.toString() || "",
    supervisorIds:
      event?.supervisorIds?.map((id: bigint) => id.toString()) || [],
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e?: React.FormEvent | React.KeyboardEvent) => {
    if (e) {
      e.preventDefault();
    }
    setIsLoading(true);

    try {
      const eventData: EventRequestDto = {
        name: formData.name,
        description: formData.description,
        startTime: new Date(formData.startTime),
        endTime: new Date(formData.endTime),
        bookingDeadline: new Date(formData.bookingDeadline),
        bookingStartTime: new Date(formData.bookingStartTime),
        reminderSendDate: formData.reminderSendDate
          ? new Date(formData.reminderSendDate)
          : undefined,
        eventLocationId: BigInt(formData.eventLocationId),
      };
      // Attach supervisors if provided
      const payload: EventRequestDto = {
        ...eventData,
        supervisorIds:
          formData.supervisorIds?.map((id: string) => BigInt(id)) || [],
      };
      await onSubmit(payload);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent
        className="max-w-md max-h-[80vh] overflow-y-auto"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle>
            {isCreating
              ? t("eventFormModal.createEventTitle")
              : t("eventFormModal.editEventTitle")}
          </DialogTitle>
          <DialogDescription>
            {isCreating
              ? t("eventFormModal.addEventDescription")
              : t("eventFormModal.updateEventDescription")}
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
              handleSubmit(e);
            }
          }}
          className="space-y-4"
        >
          <div className="space-y-2">
            <Label htmlFor="name">{t("eventFormModal.eventNameLabel")}</Label>
            <Input
              id="name"
              value={formData.name}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, name: e.target.value }))
              }
              required
            />
          </div>

          <div className="border-t border-gray-200 my-4" />
          <div className="space-y-2">
            <UserMultiSelect
              users={users}
              selectedUserIds={formData.supervisorIds}
              onSelectionChange={(sel) =>
                setFormData((prev) => ({ ...prev, supervisorIds: sel }))
              }
              label={t("eventFormModal.supervisorsLabel")}
              placeholder={t("eventFormModal.supervisorsPlaceholder")}
            />
          </div>
          <div className="border-t border-gray-200 my-4" />

          <div className="space-y-2">
            <Label htmlFor="description">
              {t("eventFormModal.descriptionLabel")}
            </Label>
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
            <Label htmlFor="location">
              {t("eventFormModal.locationLabel")}
            </Label>
            <Select
              value={formData.eventLocationId}
              onValueChange={(value) =>
                setFormData((prev) => ({ ...prev, eventLocationId: value }))
              }
            >
              <SelectTrigger>
                <SelectValue
                  placeholder={t("eventFormModal.selectLocationPlaceholder")}
                />
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
              <Label htmlFor="startTime">
                {t("eventFormModal.startTimeLabel")}
              </Label>
              <Input
                id="startTime"
                type="datetime-local"
                min="1900-01-01T00:00"
                max="2100-12-31T23:59"
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
              <Label htmlFor="endTime">
                {t("eventFormModal.endTimeLabel")}
              </Label>
              <Input
                id="endTime"
                type="datetime-local"
                max="2100-12-31T23:59"
                value={formData.endTime}
                min={formData.startTime}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, endTime: e.target.value }))
                }
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="bookingStartTime">
                {t("eventFormModal.bookingStartTimeLabel")}
              </Label>
              <Input
                id="bookingStartTime"
                type="datetime-local"
                value={formData.bookingStartTime}
                min="1900-01-01T00:00"
                max={formData.bookingDeadline || formData.startTime}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    bookingStartTime: e.target.value,
                  }))
                }
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="bookingDeadline">
                {t("eventFormModal.bookingDeadlineLabel")}
              </Label>
              <Input
                id="bookingDeadline"
                type="datetime-local"
                value={formData.bookingDeadline}
                min={formData.bookingStartTime}
                max={formData.startTime}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    bookingDeadline: e.target.value,
                  }))
                }
                required
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="reminderSendDate">
              {t("eventFormModal.reminderSendDateLabel")}
            </Label>
            <Input
              id="reminderSendDate"
              type="datetime-local"
              value={formData.reminderSendDate}
              min={formData.bookingStartTime}
              max={formData.startTime}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  reminderSendDate: e.target.value,
                }))
              }
            />
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              {t("eventFormModal.cancelButton")}
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading
                ? t("eventFormModal.savingButton")
                : isCreating
                  ? t("eventFormModal.createButton")
                  : t("eventFormModal.updateButton")}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
