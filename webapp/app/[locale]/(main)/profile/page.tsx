"use client";

import type React from "react";

import { useState, useEffect } from "react";
import { useProfile } from "@/hooks/use-profile";
import { useTwoFactor } from "@/hooks/use-2fa";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/custom-ui/button";
import { Label } from "@/components/custom-ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/custom-ui/skeleton";
import { X, AtSign, User, Mail, Lock, Tag, Save } from "lucide-react";
import { toast } from "sonner";
import type { UserProfileUpdateDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { useUnsavedChanges } from "@/hooks/use-unsaved-changes";
import { useRouter, useParams } from "next/navigation";
import { PasskeySection } from "@/components/profile/passkey-section";
import { TwoFactorSection } from "@/components/profile/two-factor-section";
import { TwoFactorCodeInput } from "@/components/common/two-factor-code-input";
import { PageHeader } from "@/components/page-header";

interface FormData {
  firstname: string;
  lastname: string;
  email: string;
  tags: string[];
}

export default function ProfilePage() {
  const t = useT();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const { user, updateProfile, isLoading, isUpdating } = useProfile();
  const {
    status: twoFactorStatus,
    sendSetupEmail,
    isSetupLoading,
  } = useTwoFactor();

  const initialFormData: FormData = {
    firstname: user?.firstname || "",
    lastname: user?.lastname || "",
    email: user?.email || "",
    tags: user?.tags || [],
  };

  const [formData, setFormData] = useState<FormData>(initialFormData);
  const [originalEmail, setOriginalEmail] = useState(initialFormData.email);
  const [originalFormData, setOriginalFormData] =
    useState<FormData>(initialFormData);
  const [newTag, setNewTag] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPasswordSection, setShowPasswordSection] =
    useState<boolean>(false);

  // Changing the email address while 2FA is enabled requires proving continued possession of it first
  const [emailChangeCode, setEmailChangeCode] = useState("");
  const [isEmailChangeDialogOpen, setIsEmailChangeDialogOpen] = useState(false);

  const { hasUnsavedChanges, setHasUnsavedChanges } = useUnsavedChanges();

  const isEmailChanging = formData.email !== originalEmail;
  const requiresCodeForEmailChange =
    isEmailChanging && !!twoFactorStatus?.twoFactorEnabled;

  if (
    user &&
    (user.firstname !== originalFormData.firstname ||
      user.lastname !== originalFormData.lastname ||
      user.email !== originalFormData.email ||
      JSON.stringify(user.tags || []) !== JSON.stringify(originalFormData.tags))
  ) {
    const newData: FormData = {
      firstname: user.firstname || "",
      lastname: user.lastname || "",
      email: user.email || "",
      tags: user.tags || [],
    };
    setFormData(newData);
    setOriginalFormData(newData);
    setOriginalEmail(newData.email);
    setHasUnsavedChanges(false);
  }

  useEffect(() => {
    const hasChanges =
      JSON.stringify(formData) !== JSON.stringify(originalFormData) ||
      (showPasswordSection && (newPassword || confirmPassword));

    setHasUnsavedChanges(!!hasChanges);

    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasChanges) {
        e.preventDefault();
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [
    formData,
    originalFormData,
    showPasswordSection,
    newPassword,
    confirmPassword,
    setHasUnsavedChanges,
  ]);

  const handleAddTag = () => {
    if (newTag.trim() && !formData.tags.includes(newTag.trim())) {
      setFormData((prev) => ({
        ...prev,
        tags: [...prev.tags, newTag.trim()],
      }));
      setNewTag("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setFormData((prev) => ({
      ...prev,
      tags: prev.tags.filter((tag) => tag !== tagToRemove),
    }));
  };

  const isPasswordValid = newPassword.length >= 8;
  const doPasswordsMatch = newPassword === confirmPassword;
  const isPasswordUpdateValid = showPasswordSection
    ? isPasswordValid && doPasswordsMatch
    : true;

  const handlePasswordUpdate = async () => {
    if (!isPasswordValid || !doPasswordsMatch) {
      toast.error(t("profilePage.passwordValidationErrorTitle"), {
        description: t("profilePage.passwordValidationErrorDescription"),
      });
      return;
    }

    await updateProfile({
      ...originalFormData,
      password: newPassword,
    });

    setNewPassword("");
    setConfirmPassword("");
    setShowPasswordSection(false);
  };

  const performUpdate = async (twoFactorCode?: string) => {
    const updatedProfile: UserProfileUpdateDto = {
      ...formData,
      ...(showPasswordSection && newPassword ? { password: newPassword } : {}),
      ...(twoFactorCode ? { twoFactorCode } : {}),
    };

    await updateProfile(updatedProfile);

    setOriginalFormData(formData);
    setHasUnsavedChanges(false);
    setEmailChangeCode("");
    setIsEmailChangeDialogOpen(false);

    if (showPasswordSection) {
      setNewPassword("");
      setConfirmPassword("");
      setShowPasswordSection(false);
    }

    if (formData.email !== originalEmail) {
      toast.info(t("email.confirmationEmailSentTitle"), {
        description: t("email.confirmationEmailSentDescription"),
      });
      setOriginalEmail(formData.email);

      setTimeout(() => {
        router.push(`/${locale}/verify`);
      }, 700);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (showPasswordSection && !isPasswordUpdateValid) {
      toast.error(t("profilePage.passwordValidationErrorTitle"), {
        description: t("profilePage.passwordValidationErrorDescription"),
      });
      return;
    }

    if (requiresCodeForEmailChange) {
      setIsEmailChangeDialogOpen(true);
      return;
    }

    await performUpdate();
  };

  const handleConfirmEmailChange = async () => {
    if (!emailChangeCode.trim()) return;
    await performUpdate(emailChangeCode.trim());
  };

  return (
    <div className="container mx-auto px-2 py-3 md:p-6">
      <PageHeader
        title={t("profilePage.profileSettingsTitle")}
        description={t("profilePage.profileSettingsDescription")}
      />

      <Card className="max-w-2xl mx-auto rounded-none border-0 bg-transparent shadow-none md:rounded-lg md:border md:bg-card md:shadow-sm">
        <CardContent className="p-0 md:p-6 md:pt-0">
          <form onSubmit={handleSubmit} className="space-y-4 mt-5">
            <div>
              <Label
                htmlFor="username"
                className="flex items-center gap-2 pb-2"
              >
                <AtSign className="h-4 w-4 text-muted-foreground" />
                {t("profilePage.usernameLabel")}
              </Label>
              {isLoading ? (
                <Skeleton className="h-10 w-full" />
              ) : (
                <Input id="username" value={user?.username || ""} disabled />
              )}
            </div>
            <div>
              <Label
                htmlFor="firstname"
                className="flex items-center gap-2 pb-2"
              >
                <User className="h-4 w-4 text-muted-foreground" />
                {t("profilePage.firstNameLabel")}
              </Label>
              {isLoading ? (
                <Skeleton className="h-10 w-full" />
              ) : (
                <Input
                  id="firstname"
                  value={formData.firstname}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      firstname: e.target.value,
                    }))
                  }
                />
              )}
            </div>
            <div>
              <Label
                htmlFor="lastname"
                className="flex items-center gap-2 pb-2"
              >
                <User className="h-4 w-4 text-muted-foreground" />
                {t("profilePage.lastNameLabel")}
              </Label>
              {isLoading ? (
                <Skeleton className="h-10 w-full" />
              ) : (
                <Input
                  id="lastname"
                  value={formData.lastname}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      lastname: e.target.value,
                    }))
                  }
                />
              )}
            </div>
            <div>
              <div className="flex items-center justify-between pb-2">
                <Label htmlFor="email" className="flex items-center gap-2">
                  <Mail className="h-4 w-4 text-muted-foreground" />
                  {t("profilePage.emailLabel")}
                </Label>
                {isLoading ? (
                  <Skeleton className="h-5 w-16" />
                ) : user?.emailVerified ? (
                  <Badge
                    variant="default"
                    className="bg-green-500 hover:bg-green-500"
                  >
                    {t("profilePage.verifiedBadge")}
                  </Badge>
                ) : (
                  <Badge
                    variant="destructive"
                    className="flex items-center gap-1"
                  >
                    {t("profilePage.notVerifiedBadge")}
                  </Badge>
                )}
              </div>
              {isLoading ? (
                <Skeleton className="h-10 w-full mb-2" />
              ) : (
                <Input
                  id="email"
                  type="email"
                  value={formData.email}
                  onChange={(e) =>
                    setFormData((prev) => ({ ...prev, email: e.target.value }))
                  }
                  className="mb-2"
                />
              )}
              <EmailSubButtons
                email={formData.email}
                originalEmail={originalEmail}
              />
              {requiresCodeForEmailChange && (
                <p className="mt-2 text-xs text-muted-foreground">
                  {t("profilePage.emailChangeCodeHint")}
                </p>
              )}
            </div>

            <div className="border-t pt-4">
              <div className="flex items-center justify-between mb-4">
                <Label className="flex items-center gap-2 text-base font-medium">
                  <Lock className="h-5 w-5 text-primary" />
                  {t("profilePage.passwordSectionTitle")}
                </Label>
                {isLoading ? (
                  <Skeleton className="h-9 w-32" />
                ) : (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      const newShowPasswordSection = !showPasswordSection;
                      setShowPasswordSection(newShowPasswordSection);
                      if (newShowPasswordSection === false) {
                        setNewPassword("");
                        setConfirmPassword("");
                      }
                    }}
                  >
                    {showPasswordSection
                      ? t("profilePage.cancelPasswordUpdate")
                      : t("profilePage.changePassword")}
                  </Button>
                )}
              </div>

              {showPasswordSection && !isLoading && (
                <div className="space-y-4 bg-muted/50 p-4 rounded-lg">
                  <div>
                    <Label htmlFor="newPassword" className="pb-2">
                      {t("profilePage.newPasswordLabel")}
                    </Label>
                    <Input
                      id="newPassword"
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder={t("profilePage.newPasswordPlaceholder")}
                    />
                    {newPassword.length > 0 && !isPasswordValid && (
                      <p className="text-sm text-destructive mt-1">
                        {t("profilePage.passwordTooShort")}
                      </p>
                    )}
                  </div>
                  <div>
                    <Label htmlFor="confirmPassword" className="pb-2">
                      {t("profilePage.confirmPasswordLabel")}
                    </Label>
                    <Input
                      id="confirmPassword"
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder={t("profilePage.confirmPasswordPlaceholder")}
                    />
                    {confirmPassword.length > 0 && !doPasswordsMatch && (
                      <p className="text-sm text-destructive mt-1">
                        {t("profilePage.passwordsDoNotMatch")}
                      </p>
                    )}
                  </div>
                  <Button
                    type="button"
                    className="w-full"
                    onClick={handlePasswordUpdate}
                    isLoading={isUpdating}
                    disabled={
                      isUpdating || !isPasswordValid || !doPasswordsMatch
                    }
                  >
                    {t("profilePage.savePasswordButton")}
                  </Button>
                </div>
              )}
            </div>

            <PasskeySection />

            <TwoFactorSection />

            <div>
              <Label htmlFor="tags" className="flex items-center gap-2 pb-2">
                <Tag className="h-4 w-4 text-muted-foreground" />
                {t("profilePage.tagsLabel")}
              </Label>
              {isLoading ? (
                <>
                  <div className="flex flex-wrap gap-2 mb-2">
                    <Skeleton className="h-6 w-16" />
                    <Skeleton className="h-6 w-20" />
                    <Skeleton className="h-6 w-12" />
                  </div>
                  <div className="flex gap-2">
                    <Skeleton className="h-10 flex-1" />
                    <Skeleton className="h-10 w-20" />
                  </div>
                </>
              ) : (
                <>
                  <div className="flex flex-wrap gap-2 mb-2">
                    {formData.tags.map((tag) => (
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
                      placeholder={t("profilePage.addTagPlaceholder")}
                      onKeyPress={(e) => {
                        if (e.key === "Enter") {
                          e.preventDefault();
                          handleAddTag();
                        }
                      }}
                    />
                    <Button type="button" onClick={handleAddTag}>
                      {t("profilePage.addButton")}
                    </Button>
                  </div>
                </>
              )}
            </div>
            {isLoading ? (
              <div className="flex justify-center pt-2">
                <Skeleton className="h-10 w-full sm:w-1/2 max-w-xs" />
              </div>
            ) : (
              <div
                className={
                  hasUnsavedChanges
                    ? "sticky bottom-0 z-30 -mx-2 px-2 md:-mx-6 md:px-6 md:-mb-6 py-3.5 bg-card/95 backdrop-blur-md border-t border-border shadow-2xl flex justify-center transition-all duration-300 md:rounded-b-lg"
                    : "relative pt-2 flex justify-center transition-all duration-300"
                }
              >
                <Button
                  type="submit"
                  className={`w-full sm:w-1/2 max-w-xs transition-all duration-200 ${
                    hasUnsavedChanges
                      ? "bg-primary hover:bg-primary/90 text-primary-foreground shadow-md hover:shadow-lg"
                      : "bg-muted hover:bg-muted/80 text-muted-foreground shadow-none"
                  }`}
                  aria-label={
                    !hasUnsavedChanges
                      ? t("profilePage.noSaveChangesButton")
                      : t("profilePage.saveChangesButton")
                  }
                  isLoading={isUpdating}
                  disabled={isUpdating || !hasUnsavedChanges}
                >
                  <Save className="mr-2 h-4 w-4" />
                  {t("profilePage.saveChangesButton")}
                </Button>
              </div>
            )}
          </form>
        </CardContent>
      </Card>

      <Dialog
        open={isEmailChangeDialogOpen}
        onOpenChange={(open) => {
          setIsEmailChangeDialogOpen(open);
          if (!open) setEmailChangeCode("");
        }}
      >
        <DialogContent>
          <DialogHeader className="text-left">
            <DialogTitle>{t("profilePage.emailChangeCodeTitle")}</DialogTitle>
            <DialogDescription>
              {t("profilePage.emailChangeCodeDescription")}
            </DialogDescription>
          </DialogHeader>

          <TwoFactorCodeInput
            id="emailChangeCode"
            totpAvailable={!!twoFactorStatus?.totpEnabled}
            emailAvailable={!!twoFactorStatus?.emailEnabled}
            code={emailChangeCode}
            onCodeChange={setEmailChangeCode}
            onRequestEmailCode={() => sendSetupEmail()}
            isRequestingEmailCode={isSetupLoading}
            autoSendEmailCode
            autoFocus
          />

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsEmailChangeDialogOpen(false)}
            >
              {t("common.cancel")}
            </Button>
            <Button
              type="button"
              onClick={handleConfirmEmailChange}
              isLoading={isUpdating}
              disabled={!emailChangeCode.trim() || isUpdating}
            >
              {t("profilePage.confirmEmailChangeButton")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

const EmailSubButtons = ({
  email,
  originalEmail,
}: {
  email: string;
  originalEmail: string;
}) => {
  const t = useT();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const { user, isLoading, resendConfirmation, isResendingConfirmation } =
    useProfile();

  if (isLoading) return;
  if (!user?.email) return;
  if (user?.emailVerified) return;
  if (email !== originalEmail) return;

  if (user?.emailVerificationSent) {
    return (
      <div className="flex flex-col items-start gap-2">
        <span className="text-xs text-gray-500">
          {t("profilePage.confirmationEmailInfo")}
        </span>
        <div className="flex gap-2">
          <Button
            type="button"
            className="text-xs"
            size={"sm"}
            isLoading={isResendingConfirmation}
            onClick={() => {
              resendConfirmation();
            }}
          >
            {t("profilePage.resendButton")}
          </Button>
          <Button
            type="button"
            variant="outline"
            className="text-xs bg-transparent"
            size={"sm"}
            onClick={() => router.push(`/${locale}/verify`)}
          >
            {t("profilePage.verifyEmailButton")}
          </Button>
        </div>
      </div>
    );
  } else {
    return (
      <div className="flex flex-col items-start gap-2">
        <span className="text-xs text-gray-500">
          {t("profilePage.noConfirmationEmailSentInfo")}
        </span>
        <div className="flex gap-2">
          <Button
            type="button"
            className="text-xs"
            size={"sm"}
            isLoading={isResendingConfirmation}
            onClick={() => {
              resendConfirmation().then(() => {
                setTimeout(() => {
                  router.push(`/${locale}/verify`);
                }, 700);
              });
            }}
          >
            {t("profilePage.sendConfirmationEmailButton")}
          </Button>
        </div>
      </div>
    );
  }
};
