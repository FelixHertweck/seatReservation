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

export default function ProfilePage() {
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
        title: "Authentication Required",
        description: "Please log in to update your profile.",
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
      title: "Profile Updated",
      description: "Your profile has been successfully updated.",
    });

    if (email !== originalEmail) {
      toast({
        title: "Bestätigungsemail gesendet",
        description:
          "Eine Bestätigungsemail wurde an Ihre Adresse gesendet. Bitte bestätigen sie diese!",
      });
      setOriginalEmail(email); // Update originalEmail after successful change
    }
  };

  if (isLoading) {
    return <LoadingSkeleton />;
  }

  return (
    <div className="container mx-auto py-8">
      <Card className="max-w-2xl mx-auto">
        <CardHeader>
          <CardTitle>Profile Settings</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label htmlFor="username">Username</Label>
              <Input id="username" value={user?.username || ""} disabled />
            </div>
            <div>
              <Label htmlFor="firstname">First Name</Label>
              <Input
                id="firstname"
                value={firstname}
                onChange={(e) => setFirstname(e.target.value)}
              />
            </div>
            <div>
              <Label htmlFor="lastname">Last Name</Label>
              <Input
                id="lastname"
                value={lastname}
                onChange={(e) => setLastname(e.target.value)}
              />
            </div>
            <div>
              <div className="flex items-center justify-between mb-1">
                <Label htmlFor="email">Email</Label>
                {user?.emailVerified ? (
                  <Badge
                    variant="default"
                    className="bg-green-500 hover:bg-green-500"
                  >
                    Verifiziert
                  </Badge>
                ) : (
                  <Badge
                    variant="destructive"
                    className="flex items-center gap-1"
                  >
                    Nicht verifiziert
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
                    Eine Bestätigungsemail wurde versendet und muss über den
                    Link bestätigt werden.
                  </span>
                  <Button
                    type="button"
                    className="text-xs"
                    size={"sm"}
                    onClick={async () => {
                      await resendConfirmation();
                      toast({
                        title: "Bestätigungsemail erneut gesendet",
                        description:
                          "Eine neue Bestätigungsemail wurde an Ihre Adresse gesendet.",
                      });
                    }}
                  >
                    Erneut senden
                  </Button>
                </div>
              )}
            </div>
            <div>
              <Label htmlFor="tags">Tags</Label>
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
                  placeholder="Add new tag"
                  onKeyPress={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      handleAddTag();
                    }
                  }}
                />
                <Button type="button" onClick={handleAddTag}>
                  Add
                </Button>
              </div>
            </div>
            <Button type="submit" className="w-full">
              Save Changes
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
