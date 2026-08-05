"use client";

import type React from "react";
import { useState, useEffect } from "react";
import { Upload, FileText, Pencil } from "@/components/icons";
import { Button } from "@/components/custom-ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/custom-ui/label";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";
import type { AdminUserCreationDto, AdminUserUpdateDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { UserFormModal } from "@/components/admin/user-form-modal";

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
  const [parsedUsers, setParsedUsers] = useState<AdminUserCreationDto[]>([]);
  const [parseError, setParseError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [editingUser, setEditingUser] = useState<AdminUserCreationDto | null>(
    null,
  );

  // Debounced live parsing of JSON input
  useEffect(() => {
    if (!jsonData.trim()) {
      setParsedUsers([]);
      setParseError("");
      return;
    }

    const timer = setTimeout(() => {
      try {
        const parsed = JSON.parse(jsonData);
        if (!Array.isArray(parsed)) {
          setParseError(t("userImportModal.usersDataArrayError"));
          return;
        }
        setParsedUsers(parsed as AdminUserCreationDto[]);
        setParseError("");
      } catch {
        // Fallback: keep previous parsed state during active typing to avoid flickering
        setParseError(t("userImportModal.invalidJsonOrDataStructureError"));
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [jsonData, t]);

  const handleSubmit = async (e?: React.FormEvent | React.KeyboardEvent) => {
    if (e) {
      e.preventDefault();
    }
    setIsLoading(true);
    setError("");

    try {
      let dataToImport = parsedUsers;

      if (!jsonData.trim()) {
        throw new Error(t("userImportModal.usersDataArrayError"));
      }

      // Re-parse current json if needed to ensure accuracy
      const parsedData = JSON.parse(jsonData);
      if (!Array.isArray(parsedData)) {
        throw new Error(t("userImportModal.usersDataArrayError"));
      }
      dataToImport = parsedData;

      // Validate each user has required fields
      for (const user of dataToImport) {
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

      await onImportUsers(dataToImport);

      handleClose();
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
    setParsedUsers([]);
    setParseError("");
    setError("");
    setEditingIndex(null);
    setEditingUser(null);
    onClose();
  };

  const handleEditCard = (index: number) => {
    setEditingIndex(index);
    setEditingUser(parsedUsers[index]);
  };

  const handleSaveUserEdit = async (
    updatedData: AdminUserCreationDto | AdminUserUpdateDto,
  ) => {
    if (editingIndex === null) return;

    const newUsers = [...parsedUsers];
    newUsers[editingIndex] = {
      ...newUsers[editingIndex],
      ...updatedData,
    } as AdminUserCreationDto;

    setParsedUsers(newUsers);
    setJsonData(JSON.stringify(newUsers, null, 2));
    setEditingIndex(null);
    setEditingUser(null);
  };

  return (
    <>
      <Dialog open={isOpen} onOpenChange={handleClose}>
        <DialogContent
          className="sm:max-w-4xl sm:max-h-[85vh] sm:overflow-y-auto"
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
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
              {/* Left Column: JSON Editor */}
              <div className="space-y-4 flex flex-col">
                <div className="space-y-2">
                  <Label className="text-sm font-medium">
                    {t("userImportModal.availableRolesLabel")}
                  </Label>
                  <div className="flex gap-2 flex-wrap">
                    {availableRoles.map((role) => (
                      <span
                        key={role}
                        className="px-2 py-1 bg-muted text-muted-foreground rounded text-xs font-mono"
                      >
                        {role}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="space-y-2 flex-1 flex flex-col">
                  <Label htmlFor="json-input">
                    {t("userImportModal.jsonDataLabel")}
                  </Label>
                  <Textarea
                    id="json-input"
                    placeholder={t("userImportModal.pasteUsersJsonPlaceholder")}
                    value={jsonData}
                    onChange={(e) => setJsonData(e.target.value)}
                    rows={12}
                    className="font-mono text-sm flex-1 min-h-[250px]"
                  />
                </div>

                {parseError && (
                  <div className="text-xs text-amber-600 bg-amber-50 dark:bg-amber-950/40 p-2.5 rounded border border-amber-200 dark:border-amber-800">
                    {parseError}
                  </div>
                )}
              </div>

              {/* Right Column: Live Cards List */}
              <div className="space-y-3 flex flex-col max-h-[420px]">
                <div className="flex items-center justify-between">
                  <Label className="text-sm font-medium">
                    {t("userImportModal.parsedUsersPreview") || "Vorschau"}
                  </Label>
                  <Badge variant="secondary">{parsedUsers.length}</Badge>
                </div>

                <div className="overflow-y-auto flex-1 space-y-2.5 pr-1 border rounded-md p-3 bg-slate-50/50 dark:bg-slate-900/50">
                  {parsedUsers.length === 0 ? (
                    <div className="text-center py-12 text-sm text-muted-foreground">
                      {t("userImportModal.noUsersParsed") ||
                        "Keine gültigen Benutzer im JSON."}
                    </div>
                  ) : (
                    parsedUsers.map((user, idx) => (
                      <Card
                        key={idx}
                        className="p-3 border shadow-sm flex items-start justify-between gap-3 bg-background"
                      >
                        <div className="space-y-1 text-sm overflow-hidden">
                          <div className="font-semibold flex items-center gap-2 truncate">
                            <span>
                              {user.firstname} {user.lastname}
                            </span>
                            <span className="text-xs text-muted-foreground font-normal">
                              (@{user.username})
                            </span>
                          </div>
                          {user.email && (
                            <div className="text-xs text-muted-foreground truncate">
                              {user.email}
                            </div>
                          )}
                          <div className="flex gap-1 flex-wrap pt-1">
                            {user.roles?.map((role) => (
                              <Badge
                                key={role}
                                variant="outline"
                                className="text-[10px] py-0 px-1.5"
                              >
                                {role}
                              </Badge>
                            ))}
                            {user.tags?.map((tag) => (
                              <Badge
                                key={tag}
                                variant="secondary"
                                className="text-[10px] py-0 px-1.5"
                              >
                                {tag}
                              </Badge>
                            ))}
                          </div>
                        </div>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          onClick={() => handleEditCard(idx)}
                          className="h-8 px-2 shrink-0"
                        >
                          <Pencil className="h-3.5 w-3.5 mr-1" />
                          <span className="text-xs">
                            {t("userImportModal.editUserButton") ||
                              "Bearbeiten"}
                          </span>
                        </Button>
                      </Card>
                    ))
                  )}
                </div>
              </div>
            </div>

            {/* Global Error Display */}
            {error && (
              <div className="text-sm text-red-600 bg-red-50 p-3 rounded-md border border-red-200">
                {error}
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex justify-end gap-3 pt-2 border-t">
              <Button type="button" variant="outline" onClick={handleClose}>
                {t("userImportModal.cancelButton")}
              </Button>
              <Button
                type="submit"
                isLoading={isLoading}
                disabled={isLoading || parsedUsers.length === 0}
              >
                <Upload className="mr-2 h-4 w-4" />
                {t("userImportModal.importUsersButton")}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* Edit Form Modal for locally editing a parsed JSON user */}
      {editingUser && (
        <UserFormModal
          user={editingUser}
          availableRoles={availableRoles}
          isCreating={true}
          onSubmit={handleSaveUserEdit}
          onClose={() => {
            setEditingIndex(null);
            setEditingUser(null);
          }}
        />
      )}
    </>
  );
}
