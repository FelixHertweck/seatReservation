"use client";

import { useState } from "react";
import { UserX } from "lucide-react";
import { Button } from "@/components/custom-ui/button";
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
import { useT } from "@/lib/i18n/hooks";

interface CancelUncheckedButtonProps {
  seatCount: number;
  guestCount: number;
  isCancelling: boolean;
  onCancel: () => void;
}

export function CancelUncheckedButton({
  seatCount,
  guestCount,
  isCancelling,
  onCancel,
}: CancelUncheckedButtonProps) {
  const t = useT();
  const [isConfirmOpen, setIsConfirmOpen] = useState(false);
  const label = t("boxOffice.cancelUnchecked.button");

  return (
    <>
      <Button
        variant="outline"
        disabled={isCancelling}
        title={label}
        aria-label={`${label} (${seatCount})`}
        onClick={() => setIsConfirmOpen(true)}
      >
        <UserX />
        <span className="hidden lg:inline">{label}</span>
        <span className="tabular-nums">{seatCount}</span>
      </Button>

      <AlertDialog open={isConfirmOpen} onOpenChange={setIsConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              {t("boxOffice.cancelUnchecked.confirmTitle")}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {t("boxOffice.cancelUnchecked.confirmDescription", {
                seats: seatCount,
                guests: guestCount,
              })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t("common.cancel")}</AlertDialogCancel>
            <AlertDialogAction onClick={onCancel}>
              {t("boxOffice.cancelUnchecked.confirmButton")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
