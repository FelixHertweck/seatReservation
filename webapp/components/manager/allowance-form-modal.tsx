"use client";

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
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
import { UserMultiSelect } from "@/components/common/user-multi-select";
import { toast } from "@/hooks/use-toast";
import { useT } from "@/lib/i18n/hooks";

import type {
  UserDto,
  EventUserAllowanceUpdateDto,
  EventUserAllowancesCreateDto,
  EventUserAllowancesResponseDto,
  ManagerEventResponseDto,
} from "@/api";

interface AllowanceFormModalProps {
  allowance: EventUserAllowancesResponseDto | null;
  users: UserDto[];
  events: ManagerEventResponseDto[];
  isCreating: boolean;
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
      setSelectedEventId(undefined);
      setAllowedReservations("");
    }
  }, [allowance, isCreating]);

  const handleSubmit = async () => {
    if (!selectedEventId || !allowedReservations) {
      toast({
        title: t("allowanceFormModal.validationErrorTitle"),
        description: t("allowanceFormModal.validationErrorDescription"),
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      const eventId = BigInt(selectedEventId);
      const reservations = Number.parseInt(allowedReservations, 10);

      if (isCreating) {
        if (selectedUserIds.length === 0) {
          toast({
            title: t("allowanceFormModal.validationErrorTitle"),
            description: t("allowanceFormModal.selectAtLeastOneUser"),
            variant: "destructive",
          });
          return;
        }
        const allowanceData: EventUserAllowancesCreateDto = {
          eventId,
          userIds: selectedUserIds.map((id) => BigInt(id)),
          reservationsAllowedCount: reservations,
        };
        await onSubmit(allowanceData);
        toast({
          title: t("allowanceFormModal.successTitle"),
          description: t("allowanceFormModal.allowanceCreatedSuccess"),
        });
      } else {
        if (!allowance?.id || selectedUserIds.length !== 1) {
          toast({
            title: t("allowanceFormModal.validationErrorTitle"),
            description: t("allowanceFormModal.selectExactlyOneUser"),
            variant: "destructive",
          });
          return;
        }
        const allowanceData: EventUserAllowanceUpdateDto = {
          id: allowance.id,
          eventId,
          userId: BigInt(selectedUserIds[0]),
          reservationsAllowedCount: reservations,
        };
        await onSubmit(allowanceData);
        toast({
          title: t("allowanceFormModal.successTitle"),
          description: t("allowanceFormModal.allowanceUpdatedSuccess"),
        });
      }
      onClose();
    } catch (error) {
      console.error("Failed to submit allowance:", error);
      toast({
        title: t("allowanceFormModal.submissionErrorTitle"),
        description: t("allowanceFormModal.submissionErrorDescription"),
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle>
            {isCreating
              ? t("allowanceFormModal.addNewAllowanceTitle")
              : t("allowanceFormModal.editAllowanceTitle")}
          </DialogTitle>
        </DialogHeader>
        <div className="grid gap-6 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="event" className="text-right">
              {t("allowanceFormModal.eventLabel")}
            </Label>
            <Select
              value={selectedEventId}
              onValueChange={setSelectedEventId}
              disabled={!isCreating}
            >
              <SelectTrigger className="col-span-3">
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

          <div className="grid grid-cols-4 items-start gap-4">
            <Label className="text-right pt-2">
              {t("allowanceFormModal.usersLabel")}
            </Label>
            <div className="col-span-3">
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
                  maxHeight="250px"
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

          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="allowedReservations" className="text-right">
              {t("allowanceFormModal.allowedReservationsLabel")}
            </Label>
            <Input
              id="allowedReservations"
              type="number"
              value={allowedReservations}
              onChange={(e) => setAllowedReservations(e.target.value)}
              className="col-span-3"
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={isLoading}>
            {t("allowanceFormModal.cancelButton")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={
              isLoading ||
              selectedUserIds.length === 0 ||
              !selectedEventId ||
              !allowedReservations
            }
          >
            {isLoading
              ? t("allowanceFormModal.submittingButton")
              : isCreating
                ? t("allowanceFormModal.createAllowanceButton")
                : t("allowanceFormModal.saveChangesButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
