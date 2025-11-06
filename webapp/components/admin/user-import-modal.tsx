"use client";

import type React from "react";

import { useState } from "react";
import { Upload, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import type { AdminUserCreationDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface UserImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  availableRoles: string[];
  onImportUsers: (users: AdminUserCreationDto[]) => Promise<void>;
}

export function UserImportModal({
  isOpen,
  onClose,
  availableRoles,
  onImportUsers,
}: UserImportModalProps) {
  const t = useT();

  const [jsonData, setJsonData] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e?: React.FormEvent | React.KeyboardEvent) => {
    if (e) {
      e.preventDefault();
    }
    setIsLoading(true);
    setError("");

    try {
      const parsedData = JSON.parse(jsonData);

      // Validate that it's an array
      if (!Array.isArray(parsedData)) {
        throw new Error(t("userImportModal.usersDataArrayError"));
      }

      // Validate each user has required fields
      for (const user of parsedData) {
        if (!user.username || !user.firstname || !user.lastname) {
          throw new Error(t("userImportModal.userDataValidationError"));
        }

        // Validate roles if provided
        if (user.roles && Array.isArray(user.roles)) {
          for (const role of user.roles) {
            if (!availableRoles.includes(role)) {
              throw new Error(
                t("userImportModal.invalidRoleError", {
                  role,
                  username: user.username,
                }),
              );
            }
          }
        }
      }

      // Import users
      await onImportUsers(parsedData as AdminUserCreationDto[]);

      // Reset form and close modal
      setJsonData("");
      onClose();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : t("userImportModal.invalidJsonOrDataStructureError"),
      );
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    setJsonData("");
    setError("");
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent
        className="max-w-2xl max-h-[80vh] overflow-y-auto"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            {t("userImportModal.importUserDataTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("userImportModal.importUserDataDescription")}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Available Roles Info */}
          <div className="space-y-2">
            <Label className="text-sm font-medium">
              {t("userImportModal.availableRolesLabel")}
            </Label>
            <div className="flex gap-2 flex-wrap">
              {availableRoles.map((role) => (
                <span
                  key={role}
                  className="px-2 py-1 bg-gray-100 text-gray-700 rounded text-xs"
                >
                  {role}
                </span>
              ))}
            </div>
          </div>

          {/* JSON Input */}
          <div className="space-y-2">
            <Label htmlFor="json-input">
              {t("userImportModal.jsonDataLabel")}
            </Label>
            <Textarea
              id="json-input"
              placeholder={t("userImportModal.pasteUsersJsonPlaceholder")}
              value={jsonData}
              onChange={(e) => setJsonData(e.target.value)}
              rows={12}
              className="font-mono text-sm"
            />
          </div>

          {/* Error Display */}
          {error && (
            <div className="text-sm text-red-600 bg-red-50 p-3 rounded-md border border-red-200">
              {error}
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex justify-end gap-3 pt-4">
            <Button type="button" variant="outline" onClick={handleClose}>
              {t("userImportModal.cancelButton")}
            </Button>
            <Button type="submit" disabled={isLoading || !jsonData.trim()}>
              <Upload className="mr-2 h-4 w-4" />
              {isLoading
                ? t("userImportModal.importingButton")
                : t("userImportModal.importUsersButton")}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
