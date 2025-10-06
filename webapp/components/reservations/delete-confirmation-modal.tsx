"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useT } from "@/lib/i18n/hooks";

interface DeleteConfirmationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  selectedCount: number;
  seats: string[];
}

export function DeleteConfirmationModal({
  isOpen,
  onClose,
  onConfirm,
  selectedCount,
  seats,
}: DeleteConfirmationModalProps) {
  const t = useT();

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent onInteractOutside={(e) => e.preventDefault()}>
        <DialogHeader>
          <DialogTitle>
            {selectedCount === 1
              ? t("reservationDeleteModal.title")
              : t("reservationDeleteModal.titleMultiple", {
                  count: selectedCount,
                })}
          </DialogTitle>
          <DialogDescription>
            {selectedCount === 1
              ? t("reservationDeleteModal.description", {
                  seatNumber: seats[0],
                })
              : t("reservationDeleteModal.descriptionMultiple", {
                  count: selectedCount,
                })}
          </DialogDescription>
          {selectedCount > 1 && seats.length <= 10 && (
            <div className="mt-2 text-sm">
              <p className="font-medium mb-1">
                {t("reservationDeleteModal.seatsToDelete")}:
              </p>
              <div className="flex flex-wrap gap-1">
                {seats.map((seat, index) => (
                  <span
                    key={index}
                    className="bg-secondary text-secondary-foreground px-2 py-0.5 rounded text-xs"
                  >
                    {seat}
                  </span>
                ))}
              </div>
            </div>
          )}
        </DialogHeader>
        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={onClose}>
            {t("reservationDeleteModal.cancelButton")}
          </Button>
          <Button variant="destructive" onClick={onConfirm}>
            {t("reservationDeleteModal.confirmButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
