"use client";

import { useT } from "@/lib/i18n/hooks";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../ui/alert-dialog";
import { useProfileUnsavedChanges } from "@/hooks/use-profile-unsaved-changes";

export function UnsavedChangesAlert() {
  const t = useT();

  const {
    showUnsavedDialog,
    setShowUnsavedDialog,
    handleDiscardChanges,
    handleSaveAndNavigate,
  } = useProfileUnsavedChanges();

  return (
    <div className="z-50">
      <AlertDialog open={showUnsavedDialog} onOpenChange={setShowUnsavedDialog}>
        <AlertDialogContent className="border-border shadow-lg">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-foreground">
              {t("sidebar.unsavedChangesTitle")}
            </AlertDialogTitle>
            <AlertDialogDescription className="text-muted-foreground">
              {t("sidebar.unsavedChangesDescription")}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => setShowUnsavedDialog(false)}
              className="bg-secondary text-secondary-foreground hover:bg-secondary/80"
            >
              {t("sidebar.cancel")}
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDiscardChanges}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {t("sidebar.discardChanges")}
            </AlertDialogAction>
            <AlertDialogAction
              onClick={handleSaveAndNavigate}
              className="bg-primary text-primary-foreground hover:bg-primary/90"
            >
              {t("sidebar.saveAndContinue")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
