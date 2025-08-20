"use client";

import type React from "react";

import { useState, useEffect } from "react";
import { useProfile } from "@/hooks/use-profile";
import { useAuthStatus } from "@/hooks/use-auth-status";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { X } from "lucide-react";
import { toast } from "@/hooks/use-toast";
import type { UserProfileUpdateDto } from "@/api";
import LoadingSkeleton from "./loading";
import { useT } from "@/lib/i18n/hooks";

export default function ProfilePage() {
  const t = useT();

  const { user, updateProfile, isLoading, resendConfirmation } = useProfile();
  const { isLoggedIn: isAuthenticated } = useAuthStatus();

  const [firstname, setFirstname] = useState("");
  const [lastname, setLastname] = useState("");
  const [email, setEmail] = useState("");
  const [originalEmail, setOriginalEmail] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [newTag, setNewTag] = useState("");

  useEffect(() => {
    if (user) {
      setFirstname(user.firstname || "");
      setLastname(user.lastname || "");
      setEmail(user.email || "");
      setOriginalEmail(user.email || "");
      setTags(user.tags || []);
    }
  }, [user]);

  const handleAddTag = () => {
    if (newTag.trim() && !tags.includes(newTag.trim())) {
      setTags([...tags, newTag.trim()]);
      setNewTag("");
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setTags(tags.filter((tag) => tag !== tagToRemove));
  };

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

    const updatedProfile: UserProfileUpdateDto = {
      firstname,
      lastname,
      email,
      tags,
    };

    await updateProfile(updatedProfile);
    console.log("Profile updated successfully");
    toast({
      title: t("profilePage.profileUpdatedTitle"),
      description: t("profilePage.profileUpdatedDescription"),
    });

    if (email !== originalEmail) {
      toast({
        title: t("profilePage.confirmationEmailSentTitle"),
        description: t("profilePage.confirmationEmailSentDescription"),
      });
      setOriginalEmail(email);
    }
  };

  if (isLoading) {
    return <LoadingSkeleton />;
  }

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
              <Input id="username" value={user?.username || ""} disabled />
            </div>
            <div>
              <Label htmlFor="firstname">
                {t("profilePage.firstNameLabel")}
              </Label>
              <Input
                id="firstname"
                value={firstname}
                onChange={(e) => setFirstname(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="lastname">{t("profilePage.lastNameLabel")}</Label>
              <Input
                id="lastname"
                value={lastname}
                onChange={(e) => setLastname(e.target.value)}
              />
            </div>
            <div>
              <div className="flex items-center justify-between mb-1">
                <Label htmlFor="email">{t("profilePage.emailLabel")}</Label>
                {user?.emailVerified ? (
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
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mb-2"
              />
              {!user?.emailVerified && (
                <div className="flex flex-col items-start gap-2">
                  <span className="text-xs text-gray-500">
                    {t("profilePage.confirmationEmailInfo")}
                  </span>
                  <Button
                    type="button"
                    className="text-xs"
                    size={"sm"}
                    onClick={async () => {
                      await resendConfirmation();
                      toast({
                        title: t("profilePage.confirmationEmailResentTitle"),
                        description: t(
                          "profilePage.confirmationEmailResentDescription",
                        ),
                      });
                    }}
                  >
                    {t("profilePage.resendButton")}
                  </Button>
                </div>
              )}
            </div>
            <div>
              <Label htmlFor="tags">{t("profilePage.tagsLabel")}</Label>
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
            </div>
            <Button type="submit" className="w-full">
              {t("profilePage.saveChangesButton")}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
