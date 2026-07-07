"use client";

import { useState } from "react";
import { KeyRound, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useT } from "@/lib/i18n/hooks";
import { useWebAuthn, useWebAuthnStatus } from "@/hooks/use-webauthn";
import type { WebAuthnCredentialDto } from "@/api";

/**
 * Profile section for managing passkeys: lists the user's registered
 * credentials, lets them add a new one, and delete existing ones. When the
 * account has no passkey yet, it nudges the user to create one.
 */
export function PasskeySection() {
  const t = useT();
  const { isSupported, addPasskey, deleteCredential } = useWebAuthn();
  const { status, credentials, isCredentialsLoading } = useWebAuthnStatus();

  const [isAdding, setIsAdding] = useState(false);
  const [pendingDelete, setPendingDelete] =
    useState<WebAuthnCredentialDto | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleAdd = async () => {
    setIsAdding(true);
    try {
      await addPasskey();
    } catch {
      // Errors are surfaced via toast inside the hook.
    } finally {
      setIsAdding(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (pendingDelete?.id === undefined) return;
    setIsDeleting(true);
    try {
      await deleteCredential(pendingDelete.id);
      setPendingDelete(null);
    } catch {
      // Errors are surfaced via toast inside the hook.
    } finally {
      setIsDeleting(false);
    }
  };

  const formatDate = (value?: Date) =>
    value ? new Date(value).toLocaleDateString() : "—";

  if (!isSupported) {
    return (
      <div className="border-t pt-4">
        <Label>{t("webauthn.manage.title")}</Label>
        <p className="mt-2 text-sm text-muted-foreground">
          {t("webauthn.manage.unsupported")}
        </p>
      </div>
    );
  }

  const hasPasskey = (credentials?.length ?? 0) > 0;

  const credentialList = hasPasskey ? (
    <ul className="space-y-2">
      {credentials?.map((credential) => (
        <li
          key={String(credential.id)}
          className="flex items-center justify-between rounded-lg border bg-muted/30 p-3"
        >
          <div className="flex items-center gap-3">
            <KeyRound className="h-5 w-5 text-muted-foreground" />
            <div>
              <p className="text-sm font-medium">
                {credential.label || t("webauthn.manage.unnamedPasskey")}
              </p>
              <p className="text-xs text-muted-foreground">
                {t("webauthn.manage.createdOn", {
                  date: formatDate(credential.createdAt),
                })}
                {credential.lastUsedAt
                  ? " · " +
                    t("webauthn.manage.lastUsed", {
                      date: formatDate(credential.lastUsedAt),
                    })
                  : ""}
              </p>
            </div>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="text-destructive hover:text-destructive"
            onClick={() => setPendingDelete(credential)}
            aria-label={t("webauthn.manage.deleteButton")}
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </li>
      ))}
    </ul>
  ) : (
    <div className="rounded-lg border border-dashed bg-muted/30 p-4 text-center">
      <KeyRound className="mx-auto mb-2 h-6 w-6 text-muted-foreground" />
      <p className="text-sm font-medium">{t("webauthn.manage.emptyTitle")}</p>
      <p className="mt-1 text-sm text-muted-foreground">
        {status?.hasPassword === false
          ? t("webauthn.manage.emptyDescriptionNoPassword")
          : t("webauthn.manage.emptyDescription")}
      </p>
    </div>
  );

  return (
    <div className="border-t pt-4">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <Label className="text-base font-medium">
            {t("webauthn.manage.title")}
          </Label>
          <p className="text-sm text-muted-foreground">
            {t("webauthn.manage.description")}
          </p>
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleAdd}
          disabled={isAdding}
        >
          <Plus className="mr-1 h-4 w-4" />
          {isAdding
            ? t("webauthn.manage.adding")
            : t("webauthn.manage.addButton")}
        </Button>
      </div>

      {isCredentialsLoading ? (
        <div className="space-y-2">
          <Skeleton className="h-14 w-full" />
          <Skeleton className="h-14 w-full" />
        </div>
      ) : (
        credentialList
      )}

      <AlertDialog
        open={pendingDelete !== null}
        onOpenChange={(open) => !open && setPendingDelete(null)}
      >
        <AlertDialogContent className="border-border shadow-lg">
          <AlertDialogHeader>
            <AlertDialogTitle className="text-foreground">
              {t("webauthn.manage.deleteConfirmTitle")}
            </AlertDialogTitle>
            <AlertDialogDescription className="text-muted-foreground">
              {t("webauthn.manage.deleteConfirmDescription", {
                name:
                  pendingDelete?.label || t("webauthn.manage.unnamedPasskey"),
              })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={() => setPendingDelete(null)}
              className="bg-secondary text-secondary-foreground hover:bg-secondary/80"
            >
              {t("webauthn.manage.cancel")}
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={handleConfirmDelete}
              disabled={isDeleting}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {t("webauthn.manage.deleteButton")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
