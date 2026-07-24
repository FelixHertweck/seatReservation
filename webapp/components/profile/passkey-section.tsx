"use client";

import { useState } from "react";
import { Check, KeyRound, Pencil, Plus, Trash2, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
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
  const { isSupported, addPasskey, deleteCredential, renameCredential } =
    useWebAuthn();
  const { status, credentials, isCredentialsLoading } =
    useWebAuthnStatus(isSupported);

  const [isAdding, setIsAdding] = useState(false);
  const [pendingDelete, setPendingDelete] =
    useState<WebAuthnCredentialDto | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editValue, setEditValue] = useState("");
  const [isRenaming, setIsRenaming] = useState(false);

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

  const startEditing = (credential: WebAuthnCredentialDto) => {
    if (credential.id === undefined) return;
    setEditingId(credential.id);
    setEditValue(credential.label ?? "");
  };

  const cancelEditing = () => {
    setEditingId(null);
    setEditValue("");
  };

  const handleConfirmRename = async () => {
    if (editingId === null || !editValue.trim()) return;
    setIsRenaming(true);
    try {
      await renameCredential(editingId, editValue.trim());
      cancelEditing();
    } catch {
      // Errors are surfaced via toast inside the hook.
    } finally {
      setIsRenaming(false);
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

  const credentialList = (
    <ul className="space-y-2">
      {credentials?.map((credential, index) => {
        const isEditing =
          credential.id !== undefined && editingId === credential.id;
        return (
          <li
            key={credential.id !== undefined ? String(credential.id) : index}
            className="flex items-center justify-between rounded-lg border bg-muted/30 p-3"
          >
            <div className="flex flex-1 items-center gap-3">
              <KeyRound className="h-5 w-5 shrink-0 text-muted-foreground" />
              {isEditing ? (
                <Input
                  autoFocus
                  value={editValue}
                  onChange={(e) => setEditValue(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      handleConfirmRename();
                    } else if (e.key === "Escape") {
                      cancelEditing();
                    }
                  }}
                  maxLength={64}
                  className="h-8"
                  disabled={isRenaming}
                />
              ) : (
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
              )}
            </div>
            {isEditing ? (
              <div className="flex shrink-0 items-center gap-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={handleConfirmRename}
                  disabled={isRenaming || !editValue.trim()}
                  aria-label={t("webauthn.manage.saveRename")}
                >
                  <Check className="h-4 w-4" />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={cancelEditing}
                  disabled={isRenaming}
                  aria-label={t("webauthn.manage.cancel")}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            ) : (
              <div className="flex shrink-0 items-center gap-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => startEditing(credential)}
                  aria-label={t("webauthn.manage.renameButton")}
                >
                  <Pencil className="h-4 w-4" />
                </Button>
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
              </div>
            )}
          </li>
        );
      })}
    </ul>
  );

  const isExpandable = !isCredentialsLoading && hasPasskey;

  return (
    <div className="border-t pt-4">
      <Accordion type="single" collapsible>
        <AccordionItem value="passkeys" className="border-none">
          <div className="flex items-center gap-2">
            <AccordionTrigger
              className="flex-1 py-0 hover:no-underline disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:no-underline"
              disabled={!isExpandable}
            >
              <div className="flex flex-col items-start gap-0.5 text-left">
                <span className="flex items-center gap-2 text-base font-medium">
                  {t("webauthn.manage.title")}
                  {isCredentialsLoading ? (
                    <Skeleton className="h-5 w-6 rounded-full" />
                  ) : (
                    hasPasskey && (
                      <Badge variant="secondary">{credentials?.length}</Badge>
                    )
                  )}
                </span>
                <span className="text-sm font-normal text-muted-foreground">
                  {!isCredentialsLoading && !hasPasskey
                    ? status?.hasPassword === false
                      ? t("webauthn.manage.emptyDescriptionNoPassword")
                      : t("webauthn.manage.emptyDescription")
                    : t("webauthn.manage.description")}
                </span>
              </div>
            </AccordionTrigger>
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="shrink-0"
              onClick={handleAdd}
              disabled={isAdding}
            >
              <Plus className="mr-1 h-4 w-4" />
              {isAdding
                ? t("webauthn.manage.adding")
                : t("webauthn.manage.addButton")}
            </Button>
          </div>

          <AccordionContent>
            <div className="max-h-32 space-y-2 overflow-y-auto pt-2 pr-1">
              {isCredentialsLoading ? (
                <>
                  <Skeleton className="h-14 w-full" />
                  <Skeleton className="h-14 w-full" />
                </>
              ) : (
                credentialList
              )}
            </div>
          </AccordionContent>
        </AccordionItem>
      </Accordion>

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
