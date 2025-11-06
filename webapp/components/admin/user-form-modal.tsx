"use client";

import { useState } from "react";
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
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { X } from "lucide-react";
import type { UserDto, AdminUserUpdateDto, AdminUserCreationDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { customSerializer } from "@/lib/jsonBodySerializer";

interface UserFormModalProps {
  user: UserDto | null;
  availableRoles: string[];
  isCreating: boolean;
  onSubmit: (
    userData: AdminUserCreationDto | AdminUserUpdateDto,
  ) => Promise<void>;
  onClose: () => void;
}

export function UserFormModal({
  user,
  availableRoles,
  isCreating,
  onSubmit,
  onClose,
}: UserFormModalProps) {
  const t = useT();

  function getInitialFormState(user: UserDto | null, isCreating: boolean) {
    return {
      username: user?.username || "",
      firstname: user?.firstname || "",
      lastname: user?.lastname || "",
      email: user?.email || "",
      password: isCreating ? "" : "••••••••",
      emailVerified: user?.emailVerified || false,
      selectedRoles: user?.roles || [],
      tags: user?.tags || [],
    };
  }

  const formKey = customSerializer.json({ user, isCreating });
  const [formState, setFormState] = useState(() =>
    getInitialFormState(user, isCreating),
  );
  const [sendEmailVerification, setSendEmailVerification] = useState(false);
  const [newTag, setNewTag] = useState("");

  const [isFormLoading, setIsFormLoading] = useState(false);

  const isPasswordTooShort =
    formState.password.length > 0 &&
    formState.password.length < 8 &&
    formState.password !== "••••••••";

  const handleRoleChange = (role: string, checked: boolean) => {
    setFormState((prev) => ({
      ...prev,
      selectedRoles: checked
        ? [...prev.selectedRoles, role]
        : prev.selectedRoles.filter((r) => r !== role),
    }));
  };

  const handleAddTag = () => {
    if (newTag.trim() && !formState.tags.includes(newTag.trim())) {
      setFormState((prev) => ({
        ...prev,
        tags: [...prev.tags, newTag.trim()],
      }));
      setNewTag("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setFormState((prev) => ({
      ...prev,
      tags: prev.tags.filter((tag) => tag !== tagToRemove),
    }));
  };

  const handleSubmit = async () => {
    setIsFormLoading(true);

    let userData: AdminUserCreationDto | AdminUserUpdateDto;

    if (isCreating) {
      userData = {
        username: formState.username,
        firstname: formState.firstname,
        lastname: formState.lastname,
        email: formState.email,
        password: formState.password,
        roles: formState.selectedRoles,
        tags: formState.tags,
        sendEmailVerification,
        emailVerified: formState.emailVerified,
      };
    } else {
      userData = {
        firstname: formState.firstname,
        lastname: formState.lastname,
        email: formState.email,
        roles: formState.selectedRoles,
        tags: formState.tags,
        sendEmailVerification,
        emailVerified: formState.emailVerified,
      };

      if (formState.password !== "••••••••") {
        userData.password = formState.password;
      }
    }

    try {
      await onSubmit(userData);
    } finally {
      setIsFormLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent
        key={formKey}
        className="sm:max-w-[425px]"
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
              ? t("userFormModal.addNewUserTitle")
              : t("userFormModal.editUserTitle")}
          </DialogTitle>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="username" className="text-right">
              {t("userFormModal.usernameLabel")}
            </Label>
            <Input
              id="username"
              value={formState.username}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, username: e.target.value }))
              }
              className="col-span-3"
              disabled={!isCreating} // Username typically not editable after creation
              autoCapitalize="none"
              autoComplete="username"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="password" className="text-right">
              {t("userFormModal.passwordLabel")}
            </Label>
            <div className="col-span-3">
              <Input
                id="password"
                type="password"
                value={formState.password}
                autoCapitalize="none"
                autoComplete={isCreating ? "new-password" : "current-password"}
                onChange={(e) =>
                  setFormState((prev) => ({
                    ...prev,
                    password: e.target.value,
                  }))
                }
                placeholder={
                  isCreating
                    ? t("userFormModal.passwordPlaceholder")
                    : t("userFormModal.passwordUpdatePlaceholder")
                }
                required={isCreating}
                onFocus={() => {
                  if (!isCreating && formState.password === "••••••••") {
                    setFormState((prev) => ({ ...prev, password: "" }));
                  }
                }}
              />
              {isPasswordTooShort && (
                <p className="text-sm text-destructive mt-1">
                  {t("userFormModal.passwordTooShort")}
                </p>
              )}
              {!isCreating && (
                <p className="text-xs text-muted-foreground mt-1">
                  {t("userFormModal.passwordUpdateHint")}
                </p>
              )}
            </div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="firstname" className="text-right">
              {t("userFormModal.firstNameLabel")}
            </Label>
            <Input
              id="firstname"
              value={formState.firstname}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, firstname: e.target.value }))
              }
              className="col-span-3"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="lastname" className="text-right">
              {t("userFormModal.lastNameLabel")}
            </Label>
            <Input
              id="lastname"
              value={formState.lastname}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, lastname: e.target.value }))
              }
              className="col-span-3"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="email" className="text-right">
              {t("userFormModal.emailLabel")}
            </Label>
            <Input
              id="email"
              type="email"
              value={formState.email}
              onChange={(e) =>
                setFormState((prev) => ({ ...prev, email: e.target.value }))
              }
              className="col-span-3"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="emailVerified" className="text-right">
              {t("userFormModal.verifiedLabel")}
            </Label>
            <Checkbox
              id="emailVerified"
              checked={formState.emailVerified}
              onCheckedChange={(checked) => {
                setFormState((prev) => ({ ...prev, emailVerified: !!checked }));
                if (checked) {
                  setSendEmailVerification(false);
                }
              }}
              className="col-span-3"
              disabled={sendEmailVerification}
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="sendEmailVerification" className="text-right">
              {t("userFormModal.sendEmailVerificationLabel")}
            </Label>
            <Checkbox
              id="sendEmailVerification"
              checked={sendEmailVerification}
              onCheckedChange={(checked) => {
                setSendEmailVerification(!!checked);
                if (checked) {
                  setFormState((prev) => ({ ...prev, emailVerified: false }));
                }
              }}
              className="col-span-3"
              aria-describedby="sendEmailVerification-desc"
              disabled={formState.emailVerified}
            />
            {/* Visually hidden description for accessibility */}
            <span id="sendEmailVerification-desc" className="sr-only">
              {t("userFormModal.sendEmailVerificationDesc")}
            </span>
          </div>
          <div className="grid grid-cols-4 items-start gap-4">
            <Label className="text-right pt-2">
              {t("userFormModal.rolesLabel")}
            </Label>
            <div className="col-span-3 flex flex-col gap-2">
              {availableRoles.map((role) => (
                <div key={role} className="flex items-center space-x-2">
                  <Checkbox
                    id={`role-${role}`}
                    checked={formState.selectedRoles.includes(role)}
                    onCheckedChange={(checked) =>
                      handleRoleChange(role, !!checked)
                    }
                    aria-describedby="mutual-exclusivity-info"
                  />
                  <Label htmlFor={`role-${role}`}>{role}</Label>
                </div>
              ))}
            </div>
          </div>
          <div className="grid grid-cols-4 items-start gap-4">
            <Label htmlFor="tags" className="text-right pt-2">
              {t("userFormModal.tagsLabel")}
            </Label>
            <div className="col-span-3">
              <div className="flex flex-wrap gap-2 mb-2">
                {formState.tags.map((tag) => (
                  <Badge
                    key={tag}
                    variant="secondary"
                    className="flex items-center gap-1"
                  >
                    {tag}
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => handleRemoveTag(tag)}
                      className="h-auto p-0.5"
                    >
                      <X className="h-3 w-3" />
                    </Button>
                  </Badge>
                ))}
              </div>
              <div className="flex gap-2">
                <Input
                  id="newTag"
                  value={newTag}
                  onChange={(e) => setNewTag(e.target.value)}
                  placeholder={t("userFormModal.addTagPlaceholder")}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault();
                      handleAddTag();
                    }
                  }}
                />
                <Button type="button" onClick={handleAddTag}>
                  {t("userFormModal.addButton")}
                </Button>
              </div>
            </div>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            {t("userFormModal.cancelButton")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={isFormLoading || isPasswordTooShort}
          >
            {isCreating
              ? isFormLoading
                ? t("userFormModal.createUserButtonLoading")
                : t("userFormModal.createUserButton")
              : isFormLoading
                ? t("userFormModal.saveChangesButtonLoading")
                : t("userFormModal.saveChangesButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
