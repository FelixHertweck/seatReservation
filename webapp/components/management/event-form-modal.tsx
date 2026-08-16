"use client";

import type React from "react";

import { useState } from "react";
import { AlertTriangle, Calendar, Clock, MapPin, Users } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/custom-ui/alert-dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/custom-ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
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
      event?.supervisorIds?.map((id: string) => id.toString()) || [],
    managerIds: event?.managerIds?.map((id: string) => id.toString()) || [],
  });
  const [isLoading, setIsLoading] = useState(false);
  const [showConfirmReschedule, setShowConfirmReschedule] = useState(false);

  const hasExistingReservations =
    !isCreating && (event?.reservedCount ?? 0) > 0;

  const isBookingStarted =
    !isCreating &&
    !!event?.bookingStartTime &&
    new Date(event.bookingStartTime) <= new Date();

  const isLocationLocked = isBookingStarted || hasExistingReservations;

  const doSubmit = async () => {
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
        eventLocationId: formData.eventLocationId,
      };
      // Attach supervisors and managers if provided
      const payload: EventRequestDto = {
        ...eventData,
        supervisorIds: formData.supervisorIds || [],
        managerIds: formData.managerIds || [],
      };
      await onSubmit(payload);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (e?: React.FormEvent | React.KeyboardEvent) => {
    if (e) {
      e.preventDefault();
    }

    const originalStartTime = event?.startTime
      ? new Date(event.startTime).toLocaleString("sv-SE").slice(0, 16)
      : "";
    const originalEndTime = event?.endTime
      ? new Date(event.endTime).toLocaleString("sv-SE").slice(0, 16)
      : "";
    const originalBookingDeadline = event?.bookingDeadline
      ? new Date(event.bookingDeadline).toLocaleString("sv-SE").slice(0, 16)
      : "";
    const originalLocationId = event?.eventLocationId?.toString() || "";

    const relevantFieldsChanged =
      formData.startTime !== originalStartTime ||
      formData.endTime !== originalEndTime ||
      formData.bookingDeadline !== originalBookingDeadline ||
      formData.eventLocationId !== originalLocationId;

    if (hasExistingReservations && relevantFieldsChanged) {
      setShowConfirmReschedule(true);
      return;
    }

    await doSubmit();
  };

  return (
    <>
      <Dialog open onOpenChange={onClose}>
        <DialogContent
          className="sm:max-w-xl sm:max-h-[80vh] sm:overflow-y-auto"
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

          {hasExistingReservations && (
            <Alert variant="warning" className="my-2">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>
                {t("eventFormModal.existingReservationsWarning", {
                  count: event?.reservedCount ?? 0,
                })}
              </AlertDescription>
            </Alert>
          )}

          <form
            onSubmit={handleSubmit}
            className="space-y-5 py-2"
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
          >
            {/* Basic information */}
            <div className="space-y-4">
              <h3 className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                <Calendar className="h-4 w-4" />
                {t("eventFormModal.basicInfoSectionTitle")}
              </h3>
              <div className="space-y-2">
                <Label htmlFor="name">
                  {t("eventFormModal.eventNameLabel")}
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
            </div>

            {/* Location */}
            <div className="space-y-4 border-t pt-4">
              <h3 className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                <MapPin className="h-4 w-4" />
                {t("eventFormModal.locationSectionTitle")}
              </h3>
              <div className="space-y-2">
                <Label htmlFor="location">
                  {t("eventFormModal.locationLabel")}
                </Label>
                {isLocationLocked ? (
                  <TooltipProvider>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div className="cursor-not-allowed">
                          <Select
                            disabled
                            value={formData.eventLocationId}
                            onValueChange={(value) =>
                              setFormData((prev) => ({
                                ...prev,
                                eventLocationId: value,
                              }))
                            }
                          >
                            <SelectTrigger className="opacity-75 cursor-not-allowed">
                              <SelectValue
                                placeholder={t(
                                  "eventFormModal.selectLocationPlaceholder",
                                )}
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
                      </TooltipTrigger>
                      <TooltipContent>
                        <p>
                          {t(
                            isBookingStarted
                              ? "management.events.locationDisabledTooltip"
                              : "management.events.locationDisabledReservationsTooltip",
                          )}
                        </p>
                      </TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                ) : (
                  <Select
                    value={formData.eventLocationId}
                    onValueChange={(value) =>
                      setFormData((prev) => ({
                        ...prev,
                        eventLocationId: value,
                      }))
                    }
                  >
                    <SelectTrigger>
                      <SelectValue
                        placeholder={t(
                          "eventFormModal.selectLocationPlaceholder",
                        )}
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
                )}
              </div>
            </div>

            {/* Schedule */}
            <div className="space-y-4 border-t pt-4">
              <h3 className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                <Clock className="h-4 w-4" />
                {t("eventFormModal.scheduleSectionTitle")}
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
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
                      setFormData((prev) => ({
                        ...prev,
                        endTime: e.target.value,
                      }))
                    }
                    required
                  />
                </div>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
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
            </div>

            {/* Supervisors */}
            <div className="space-y-4 border-t pt-4">
              <h3 className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                <Users className="h-4 w-4" />
                {t("eventFormModal.supervisorsSectionTitle")}
              </h3>
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

            {/* Managers */}
            <div className="space-y-4 border-t pt-4">
              <h3 className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
                <Users className="h-4 w-4" />
                {t("eventFormModal.managersSectionTitle")}
              </h3>
              <UserMultiSelect
                users={users}
                selectedUserIds={formData.managerIds}
                onSelectionChange={(sel) =>
                  setFormData((prev) => ({ ...prev, managerIds: sel }))
                }
                label={t("eventFormModal.managersLabel")}
                placeholder={t("eventFormModal.managersPlaceholder")}
              />
            </div>

            <DialogFooter className="mt-6">
              <Button type="button" variant="outline" onClick={onClose}>
                {t("eventFormModal.cancelButton")}
              </Button>
              <Button type="submit" isLoading={isLoading} disabled={isLoading}>
                {isCreating
                  ? t("eventFormModal.createButton")
                  : t("eventFormModal.updateButton")}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <AlertDialog
        open={showConfirmReschedule}
        onOpenChange={setShowConfirmReschedule}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t("eventFormModal.rescheduleConfirmTitle")}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t("eventFormModal.rescheduleConfirmDescription")}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => setShowConfirmReschedule(false)}
              disabled={isLoading}
            >
              {t("eventFormModal.rescheduleCancelButton")}
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={async (e) => {
                e.preventDefault();
                setShowConfirmReschedule(false);
                await doSubmit();
              }}
              disabled={isLoading}
            >
              {t("eventFormModal.rescheduleConfirmButton")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
