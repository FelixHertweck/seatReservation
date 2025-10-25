"use client";

import { useState, useEffect } from "react";
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

  const [username, setUsername] = useState(user?.username || "");
  const [firstname, setFirstname] = useState(user?.firstname || "");
  const [lastname, setLastname] = useState(user?.lastname || "");
  const [email, setEmail] = useState(user?.email || "");
  const [password, setPassword] = useState(isCreating ? "" : "••••••••");
  const [emailVerified, setEmailVerified] = useState(
    user?.emailVerified || false,
  );
  const [sendEmailVerification, setSendEmailVerification] = useState(false);
  const [selectedRoles, setSelectedRoles] = useState<string[]>(
    user?.roles || [],
  );
  const [tags, setTags] = useState<string[]>(user?.tags || []);
  const [newTag, setNewTag] = useState("");

  const [isFormLoading, setIsFormLoading] = useState(false);

  const isPasswordTooShort =
    password.length > 0 && password.length < 8 && password !== "••••••••";

  useEffect(() => {
    if (user) {
      setUsername(user.username || "");
      setFirstname(user.firstname || "");
      setLastname(user.lastname || "");
      setEmail(user.email || "");
      setEmailVerified(user.emailVerified || false);
      setSelectedRoles(user.roles || []);
      setTags(user.tags || []);
      setPassword(isCreating ? "" : "••••••••");
    } else {
      setUsername("");
      setFirstname("");
      setLastname("");
      setEmail("");
      setPassword("");
      setEmailVerified(false);
      setSelectedRoles([]);
      setTags([]);
      setSendEmailVerification(false);
    }
  }, [user, isCreating]);

  const handleRoleChange = (role: string, checked: boolean) => {
    setSelectedRoles((prev) =>
      checked ? [...prev, role] : prev.filter((r) => r !== role),
    );
  };

  const handleAddTag = () => {
    if (newTag.trim() && !tags.includes(newTag.trim())) {
      setTags([...tags, newTag.trim()]);
      setNewTag("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((tag) => tag !== tagToRemove));
  };

  const handleSubmit = async () => {
    setIsFormLoading(true);

    let userData: AdminUserCreationDto | AdminUserUpdateDto;

    if (isCreating) {
      userData = {
        username,
        firstname,
        lastname,
        email,
        password,
        roles: selectedRoles,
        tags,
        sendEmailVerification,
        emailVerified,
      };
    } else {
      userData = {
        firstname,
        lastname,
        email,
        roles: selectedRoles,
        tags,
        sendEmailVerification,
        emailVerified,
      };

      if (password !== "••••••••") {
        userData.password = password;
      }
    }

    await onSubmit(userData);

    setIsFormLoading(false);
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent
        className="sm:max-w-[425px]"
        onInteractOutside={(e) => e.preventDefault()}
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
            <div className="col-span-3">
              <Input
                id="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={!isCreating} // Username typically not editable after creation
                autoCapitalize="none"
                autoComplete="username"
              />
              {isCreating && (
                <p className="text-xs text-muted-foreground mt-1">
                  {t("validation.usernameHint")}
                </p>
              )}
            </div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="password" className="text-right">
              {t("userFormModal.passwordLabel")}
            </Label>
            <div className="col-span-3">
              <Input
                id="password"
                type="password"
                value={password}
                autoCapitalize="none"
                autoComplete={isCreating ? "new-password" : "current-password"}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={
                  isCreating
                    ? t("userFormModal.passwordPlaceholder")
                    : t("userFormModal.passwordUpdatePlaceholder")
                }
                required={isCreating}
                onFocus={() => {
                  if (!isCreating && password === "••••••••") {
                    setPassword("");
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
              value={firstname}
              onChange={(e) => setFirstname(e.target.value)}
              className="col-span-3"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="lastname" className="text-right">
              {t("userFormModal.lastNameLabel")}
            </Label>
            <Input
              id="lastname"
              value={lastname}
              onChange={(e) => setLastname(e.target.value)}
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
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="col-span-3"
            />
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="emailVerified" className="text-right">
              {t("userFormModal.verifiedLabel")}
            </Label>
            <Checkbox
              id="emailVerified"
              checked={emailVerified}
              onCheckedChange={(checked) => {
                setEmailVerified(!!checked);
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
                  setEmailVerified(false);
                }
              }}
              className="col-span-3"
              aria-describedby="sendEmailVerification-desc"
              disabled={emailVerified}
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
                    checked={selectedRoles.includes(role)}
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
                {tags.map((tag) => (
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
