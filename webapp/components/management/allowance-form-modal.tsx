"use client";

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/custom-ui/dialog";
import { Button } from "@/components/custom-ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { UserMultiSelect } from "@/components/common/user-multi-select";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";

import type {
  UserDto,
  EventResponseDto,
  EventUserAllowancesDto,
  EventUserAllowanceUpdateDto,
  EventUserAllowancesCreateDto,
} from "@/api";

interface AllowanceFormModalProps {
  allowance: EventUserAllowancesDto | null;
  users: UserDto[];
  events: EventResponseDto[];
  isCreating: boolean;
  hideEventSelector?: boolean;
  onSubmit: (
    allowanceData: EventUserAllowancesCreateDto | EventUserAllowanceUpdateDto,
  ) => Promise<void>;
  onClose: () => void;
}

export function AllowanceFormModal({
  allowance,
  users,
  events,
  isCreating,
  hideEventSelector = false,
  onSubmit,
  onClose,
}: AllowanceFormModalProps) {
  const t = useT();

  const [selectedUserIds, setSelectedUserIds] = useState<string[]>(
    allowance && !isCreating ? [allowance.userId?.toString() || ""] : [],
  );
  const [selectedEventId, setSelectedEventId] = useState<string | undefined>(
    allowance?.eventId?.toString(),
  );
  const [allowedReservations, setAllowedReservations] = useState(
    allowance?.reservationsAllowedCount?.toString() || "",
  );
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (allowance && !isCreating) {
      setSelectedUserIds([allowance.userId?.toString() || ""]);
      setSelectedEventId(allowance.eventId?.toString());
      setAllowedReservations(
        allowance.reservationsAllowedCount?.toString() || "",
      );
    } else if (isCreating) {
      setSelectedUserIds([]);
      setSelectedEventId(allowance?.eventId?.toString());
      setAllowedReservations("");
    }
  }, [allowance, isCreating]);

  const handleSubmit = async () => {
    if (!selectedEventId || !allowedReservations) {
      toast.error(t("allowanceFormModal.validationErrorTitle"), {
        description: t("allowanceFormModal.validationErrorDescription"),
      });
      return;
    }

    setIsLoading(true);
    try {
      const eventId = selectedEventId;
      const reservations = Number.parseInt(allowedReservations, 10);

      if (isCreating) {
        if (selectedUserIds.length === 0) {
          toast.error(t("allowanceFormModal.validationErrorTitle"), {
            description: t("allowanceFormModal.selectAtLeastOneUser"),
          });
          return;
        }
        const allowanceData: EventUserAllowancesCreateDto = {
          eventId,
          userIds: selectedUserIds,
          reservationsAllowedCount: reservations,
        };
        await onSubmit(allowanceData);
      } else {
        if (!allowance?.id || selectedUserIds.length !== 1) {
          toast.error(t("allowanceFormModal.validationErrorTitle"), {
            description: t("allowanceFormModal.selectExactlyOneUser"),
          });
          return;
        }
        const allowanceData: EventUserAllowanceUpdateDto = {
          id: allowance.id,
          eventId,
          userId: selectedUserIds[0],
          reservationsAllowedCount: reservations,
        };
        await onSubmit(allowanceData);
      }
      onClose();
    } catch (error) {
      console.error("Failed to submit allowance:", error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent
        className="sm:max-w-[600px]"
        onInteractOutside={(e) => e.preventDefault()}
        onKeyDown={(e) => {
          if (
            e.key === "Enter" &&
            !e.shiftKey &&
            !(e.target instanceof HTMLTextAreaElement)
          ) {
            e.preventDefault();
            handleSubmit();
          }
        }}
      >
        <DialogHeader>
          <DialogTitle>
            {isCreating
              ? t("allowanceFormModal.addNewAllowanceTitle")
              : t("allowanceFormModal.editAllowanceTitle")}
          </DialogTitle>
        </DialogHeader>
        <div className="grid gap-6 py-4">
          {!hideEventSelector && (
            <div className="space-y-2">
              <Label htmlFor="event">
                {t("allowanceFormModal.eventLabel")}
              </Label>
              <Select
                value={selectedEventId}
                onValueChange={setSelectedEventId}
                disabled={!isCreating}
              >
                <SelectTrigger className="w-full">
                  <SelectValue
                    placeholder={t("allowanceFormModal.selectEventPlaceholder")}
                  />
                </SelectTrigger>
                <SelectContent>
                  {events.map((event) => (
                    <SelectItem
                      key={event.id?.toString()}
                      value={event.id?.toString() || ""}
                    >
                      {event.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <div className="space-y-2">
            <Label>{t("allowanceFormModal.usersLabel")}</Label>
            <div className="space-y-2">
              {isCreating && (
                <UserMultiSelect
                  users={
                    isCreating
                      ? users
                      : users.filter(
                          (user) =>
                            user.id?.toString() ===
                            allowance?.userId?.toString(),
                        )
                  }
                  selectedUserIds={selectedUserIds}
                  onSelectionChange={setSelectedUserIds}
                  label=""
                  placeholder={t("allowanceFormModal.searchUserPlaceholder")}
                />
              )}

              {!isCreating && selectedUserIds.length > 0 && (
                <div className="text-sm text-muted-foreground mt-2">
                  {t("allowanceFormModal.selectedUserLabel")}{" "}
                  {
                    users.find((u) => u.id?.toString() === selectedUserIds[0])
                      ?.username
                  }
                </div>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="allowedReservations">
              {t("allowanceFormModal.allowedReservationsLabel")}
            </Label>
            <Input
              id="allowedReservations"
              type="number"
              value={allowedReservations}
              onChange={(e) => setAllowedReservations(e.target.value)}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={isLoading}>
            {t("allowanceFormModal.cancelButton")}
          </Button>
          <Button
            onClick={handleSubmit}
            isLoading={isLoading}
            disabled={
              isLoading ||
              selectedUserIds.length === 0 ||
              !selectedEventId ||
              !allowedReservations
            }
          >
            {isCreating
              ? t("allowanceFormModal.createAllowanceButton")
              : t("allowanceFormModal.saveChangesButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
