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
  seatNumber?: string;
}

export function DeleteConfirmationModal({
  isOpen,
  onClose,
  onConfirm,
  seatNumber,
}: DeleteConfirmationModalProps) {
  const t = useT();

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t("reservationDeleteModal.title")}</DialogTitle>
          <DialogDescription>
            {t("reservationDeleteModal.description", { seatNumber })}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
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
