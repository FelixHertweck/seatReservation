"use client";

import type React from "react";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
import type { UserDto, UserCreationDto, AdminUserUpdateDto } from "@/api";

interface UserFormModalProps {
  user: UserDto | null;
  availableRoles: string[];
  isCreating: boolean;
  onSubmit: (userData: UserCreationDto | AdminUserUpdateDto) => Promise<void>;
  onClose: () => void;
}

export function UserFormModal({
  user,
  availableRoles,
  isCreating,
  onSubmit,
  onClose,
}: UserFormModalProps) {
  const [formData, setFormData] = useState({
    username: user?.username || "",
    email: user?.email || "",
    firstname: user?.firstName || "",
    lastname: user?.lastName || "",
    password: "",
    roles: user?.roles || [],
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      if (isCreating) {
        const userData: UserCreationDto = {
          username: formData.username,
          email: formData.email,
          password: formData.password,
          firstname: formData.firstname,
          lastname: formData.lastname,
        };
        await onSubmit(userData);
      } else {
        const userData: AdminUserUpdateDto = {
          email: formData.email,
          firstname: formData.firstname,
          lastname: formData.lastname,
          password: formData.password || undefined,
          roles: formData.roles,
        };
        await onSubmit(userData);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleRoleChange = (role: string, checked: boolean) => {
    setFormData((prev) => ({
      ...prev,
      roles: checked
        ? [...prev.roles, role]
        : prev.roles.filter((r) => r !== role),
    }));
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{isCreating ? "Create User" : "Edit User"}</DialogTitle>
          <DialogDescription>
            {isCreating
              ? "Add a new user to the system"
              : "Update user information and roles"}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {isCreating && (
            <div className="space-y-2">
              <Label htmlFor="username">Username</Label>
              <Input
                id="username"
                value={formData.username}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, username: e.target.value }))
                }
                required
              />
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="firstname">First Name</Label>
              <Input
                id="firstname"
                value={formData.firstname}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    firstname: e.target.value,
                  }))
                }
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="lastname">Last Name</Label>
              <Input
                id="lastname"
                value={formData.lastname}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, lastname: e.target.value }))
                }
                required
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              value={formData.email}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, email: e.target.value }))
              }
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">
              {isCreating
                ? "Password"
                : "New Password (leave empty to keep current)"}
            </Label>
            <Input
              id="password"
              type="password"
              value={formData.password}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, password: e.target.value }))
              }
              required={isCreating}
            />
          </div>

          <div className="space-y-2">
            <Label>Roles</Label>
            <div className="space-y-2">
              {availableRoles.map((role) => (
                <div key={role} className="flex items-center space-x-2">
                  <Checkbox
                    id={role}
                    checked={formData.roles.includes(role)}
                    onCheckedChange={(checked) =>
                      handleRoleChange(role, checked as boolean)
                    }
                  />
                  <Label htmlFor={role}>{role}</Label>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? "Saving..." : isCreating ? "Create" : "Update"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
