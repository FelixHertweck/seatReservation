"use client";

import type React from "react";

import { useState, useEffect } from "react";
import { useProfile } from "@/hooks/use-profile";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { X } from "lucide-react";
import { toast } from "@/hooks/use-toast";
import type { UserProfileUpdateDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { useAuth } from "@/hooks/use-auth";
import { useProfileUnsavedChanges } from "@/hooks/use-profile-unsaved-changes";
import { useRouter, useParams } from "next/navigation";

export default function ProfilePage() {
  const t = useT();
  const router = useRouter();
  const params = useParams();
  const locale = params.locale as string;

  const { user, updateProfile, isLoading, resendConfirmation } = useProfile();
  const { isLoggedIn: isAuthenticated } = useAuth();

  const [firstname, setFirstname] = useState("");
  const [lastname, setLastname] = useState("");
  const [email, setEmail] = useState("");
  const [originalEmail, setOriginalEmail] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [newTag, setNewTag] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPasswordSection, setShowPasswordSection] =
    useState<boolean>(false);

  const { hasUnsavedChanges, setHasUnsavedChanges } =
    useProfileUnsavedChanges();
  const [originalFormData, setOriginalFormData] = useState({
    firstname: "",
    lastname: "",
    email: "",
    tags: [] as string[],
  });

  useEffect(() => {
    if (user) {
      const initialData = {
        firstname: user.firstname || "",
        lastname: user.lastname || "",
        email: user.email || "",
        tags: user.tags || [],
      };

      setFirstname(initialData.firstname);
      setLastname(initialData.lastname);
      setEmail(initialData.email);
      setOriginalEmail(initialData.email);
      setTags(initialData.tags);

      setOriginalFormData(initialData);
      setHasUnsavedChanges(false);
    }
  }, [user, setHasUnsavedChanges]);

  useEffect(() => {
    const currentData = { firstname, lastname, email, tags };
    const hasChanges =
      JSON.stringify(currentData) !== JSON.stringify(originalFormData) ||
      (showPasswordSection && (newPassword || confirmPassword));

    setHasUnsavedChanges(!!hasChanges);

    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasChanges) {
        e.preventDefault();
        e.returnValue = "";
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [
    firstname,
    lastname,
    email,
    tags,
    originalFormData,
    showPasswordSection,
    newPassword,
    confirmPassword,
    setHasUnsavedChanges,
  ]);

  const handleAddTag = () => {
    if (newTag.trim() && !tags.includes(newTag.trim())) {
      setTags([...tags, newTag.trim()]);
      setNewTag("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((tag) => tag !== tagToRemove));
  };

  const isPasswordValid = newPassword.length >= 8;
  const doPasswordsMatch = newPassword === confirmPassword;
  const isPasswordUpdateValid = showPasswordSection
    ? isPasswordValid && doPasswordsMatch
    : true;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isAuthenticated) {
      toast({
        title: t("profilePage.authRequiredTitle"),
        description: t("profilePage.authRequiredDescription"),
        variant: "destructive",
      });
      return;
    }

    if (showPasswordSection && !isPasswordUpdateValid) {
      toast({
        title: t("profilePage.passwordValidationErrorTitle"),
        description: t("profilePage.passwordValidationErrorDescription"),
        variant: "destructive",
      });
      return;
    }

    const updatedProfile: UserProfileUpdateDto = {
      firstname,
      lastname,
      email,
      tags,
      ...(showPasswordSection && newPassword ? { password: newPassword } : {}),
    };

    await updateProfile(updatedProfile);
    console.log("Profile updated successfully");
    toast({
      title: t("profilePage.profileUpdatedTitle"),
      description: t("profilePage.profileUpdatedDescription"),
    });

    const newFormData = { firstname, lastname, email, tags };
    setOriginalFormData(newFormData);
    setHasUnsavedChanges(false);

    if (showPasswordSection) {
      setNewPassword("");
      setConfirmPassword("");
      setShowPasswordSection(false);
    }

    if (email !== originalEmail) {
      toast({
        title: t("email.confirmationEmailSentTitle"),
        description: t("email.confirmationEmailSentDescription"),
      });
      setOriginalEmail(email);

      setTimeout(() => {
        router.push(`/${locale}/verify`);
      }, 700);
      return;
    }
  };

  return (
    <div className="container mx-auto py-8">
      <Card className="max-w-2xl mx-auto">
        <CardHeader>
          <CardTitle>{t("profilePage.profileSettingsTitle")}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label htmlFor="username">{t("profilePage.usernameLabel")}</Label>
              {isLoading ? (
                <Skeleton className="h-10 w-full mt-2" />
              ) : (
                <Input id="username" value={user?.username || ""} disabled />
              )}
            </div>
            <div>
              <Label htmlFor="firstname">
                {t("profilePage.firstNameLabel")}
              </Label>
              {isLoading ? (
                <Skeleton className="h-10 w-full mt-2" />
              ) : (
                <Input
                  id="firstname"
                  value={firstname}
                  onChange={(e) => setFirstname(e.target.value)}
                />
              )}
            </div>
            <div>
              <Label htmlFor="lastname">{t("profilePage.lastNameLabel")}</Label>
              {isLoading ? (
                <Skeleton className="h-10 w-full mt-2" />
              ) : (
                <Input
                  id="lastname"
                  value={lastname}
                  onChange={(e) => setLastname(e.target.value)}
                />
              )}
            </div>
            <div>
              <div className="flex items-center justify-between mb-1">
                <Label htmlFor="email">{t("profilePage.emailLabel")}</Label>
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
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="mb-2"
                />
              )}
              {!isLoading &&
                !user?.emailVerified &&
                user?.email &&
                email === originalEmail && (
                  <div className="flex flex-col items-start gap-2">
                    <span className="text-xs text-gray-500">
                      {t("profilePage.confirmationEmailInfo")}
                    </span>
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        className="text-xs"
                        size={"sm"}
                        onClick={async () => {
                          await resendConfirmation();
                          toast({
                            title: t(
                              "profilePage.confirmationEmailResentTitle",
                            ),
                            description: t(
                              "profilePage.confirmationEmailResentDescription",
                            ),
                          });
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
                )}
            </div>

            <div className="border-t pt-4">
              <div className="flex items-center justify-between mb-4">
                <Label className="text-base font-medium">
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
                </div>
              )}
            </div>

            <div>
              <Label htmlFor="tags" className="pb-2">
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
              <Skeleton className="h-10 w-full" />
            ) : (
              <Button
                type="submit"
                className={`w-full transition-all duration-200 ${
                  hasUnsavedChanges
                    ? "bg-primary hover:bg-primary/90 text-primary-foreground"
                    : "bg-muted hover:bg-muted/80 text-muted-foreground"
                }`}
              >
                {t("profilePage.saveChangesButton")}
              </Button>
            )}
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
