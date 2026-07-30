"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/custom-ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
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

type EmailStatus = "unverified" | "verified" | "send";

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
      emailStatus: (user?.emailVerified
        ? "verified"
        : "unverified") as EmailStatus,
      selectedRoles: user?.roles || [],
      tags: user?.tags || [],
    };
  }

  const formKey = customSerializer.json({ user, isCreating });
  const [formState, setFormState] = useState(() =>
    getInitialFormState(user, isCreating),
  );
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

    const emailVerified = formState.emailStatus === "verified";
    const sendEmailVerification = formState.emailStatus === "send";

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
        emailVerified,
      };
    } else {
      userData = {
        firstname: formState.firstname,
        lastname: formState.lastname,
        email: formState.email,
        roles: formState.selectedRoles,
        tags: formState.tags,
        sendEmailVerification,
        emailVerified,
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
        className="sm:max-w-xl sm:max-h-[80vh] sm:overflow-y-auto"
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
        <div className="grid gap-6 py-4">
          {/* Account */}
          <div className="space-y-4">
            <h3 className="text-sm font-medium text-muted-foreground">
              {t("userFormModal.accountSectionTitle")}
            </h3>
            <div className="space-y-2">
              <Label htmlFor="username">
                {t("userFormModal.usernameLabel")}
              </Label>
              <Input
                id="username"
                value={formState.username}
                onChange={(e) =>
                  setFormState((prev) => ({
                    ...prev,
                    username: e.target.value,
                  }))
                }
                disabled={!isCreating} // Username typically not editable after creation
                autoCapitalize="none"
                autoComplete="username"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">
                {t("userFormModal.passwordLabel")}
              </Label>
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
                <p className="text-sm text-destructive">
                  {t("userFormModal.passwordTooShort")}
                </p>
              )}
              {!isCreating && (
                <p className="text-xs text-muted-foreground">
                  {t("userFormModal.passwordUpdateHint")}
                </p>
              )}
            </div>
          </div>

          {/* Personal information */}
          <div className="space-y-4 border-t pt-6">
            <h3 className="text-sm font-medium text-muted-foreground">
              {t("userFormModal.personalInfoSectionTitle")}
            </h3>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="firstname">
                  {t("userFormModal.firstNameLabel")}
                </Label>
                <Input
                  id="firstname"
                  value={formState.firstname}
                  onChange={(e) =>
                    setFormState((prev) => ({
                      ...prev,
                      firstname: e.target.value,
                    }))
                  }
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="lastname">
                  {t("userFormModal.lastNameLabel")}
                </Label>
                <Input
                  id="lastname"
                  value={formState.lastname}
                  onChange={(e) =>
                    setFormState((prev) => ({
                      ...prev,
                      lastname: e.target.value,
                    }))
                  }
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">{t("userFormModal.emailLabel")}</Label>
              <Input
                id="email"
                type="email"
                value={formState.email}
                onChange={(e) =>
                  setFormState((prev) => ({ ...prev, email: e.target.value }))
                }
              />
            </div>
          </div>

          {/* Email verification status */}
          <div className="space-y-3 border-t pt-6">
            <h3 className="text-sm font-medium text-muted-foreground">
              {t("userFormModal.emailStatusSectionTitle")}
            </h3>
            <RadioGroup
              value={formState.emailStatus}
              onValueChange={(value) =>
                setFormState((prev) => ({
                  ...prev,
                  emailStatus: value as EmailStatus,
                }))
              }
            >
              <div className="flex items-center space-x-2">
                <RadioGroupItem
                  value="unverified"
                  id="email-status-unverified"
                />
                <Label
                  htmlFor="email-status-unverified"
                  className="font-normal"
                >
                  {t("userFormModal.emailStatusUnverifiedOption")}
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="verified" id="email-status-verified" />
                <Label htmlFor="email-status-verified" className="font-normal">
                  {t("userFormModal.verifiedLabel")}
                </Label>
              </div>
              <div className="flex items-center space-x-2">
                <RadioGroupItem value="send" id="email-status-send" />
                <Label htmlFor="email-status-send" className="font-normal">
                  {t("userFormModal.sendEmailVerificationLabel")}
                </Label>
              </div>
            </RadioGroup>
            {formState.emailStatus === "send" && (
              <p className="text-xs text-muted-foreground">
                {t("userFormModal.sendEmailVerificationDesc")}
              </p>
            )}
          </div>

          {/* Roles */}
          <div className="space-y-3 border-t pt-6">
            <h3 className="text-sm font-medium text-muted-foreground">
              {t("userFormModal.rolesLabel")}
            </h3>
            <div className="grid grid-cols-2 gap-2">
              {availableRoles.map((role) => (
                <div key={role} className="flex items-center space-x-2">
                  <Checkbox
                    id={`role-${role}`}
                    checked={formState.selectedRoles.includes(role)}
                    onCheckedChange={(checked) =>
                      handleRoleChange(role, !!checked)
                    }
                  />
                  <Label htmlFor={`role-${role}`} className="font-normal">
                    {role}
                  </Label>
                </div>
              ))}
            </div>
          </div>

          {/* Tags */}
          <div className="space-y-3 border-t pt-6">
            <h3 className="text-sm font-medium text-muted-foreground">
              {t("userFormModal.tagsLabel")}
            </h3>
            <div className="flex flex-wrap gap-2">
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
