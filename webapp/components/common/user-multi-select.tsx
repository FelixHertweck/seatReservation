"use client";
import { useState, useMemo } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Button } from "@/components/ui/button";
import type { UserDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface UserMultiSelectProps {
  users: UserDto[];
  selectedUserIds: string[];
  onSelectionChange: (userIds: string[]) => void;
  label?: string;
  placeholder?: string;
  maxHeight?: string;
}

export function UserMultiSelect({
  users,
  selectedUserIds,
  onSelectionChange,
  label = "",
  placeholder = "",
  maxHeight = "300px",
}: UserMultiSelectProps) {
  const t = useT();
  const [searchTerm, setSearchTerm] = useState("");

  // Get all unique tags from users
  const availableTags = useMemo(() => {
    const tagSet = new Set<string>();
    users.forEach((user) => {
      user.tags?.forEach((tag) => tagSet.add(tag));
    });
    return Array.from(tagSet).sort();
  }, [users]);

  // Filter users based on search term
  const filteredUsers = useMemo(() => {
    if (!searchTerm.trim()) return users;

    const search = searchTerm.toLowerCase();
    return users.filter((user) => {
      const username = user.username?.toLowerCase() || "";
      const firstname = user.firstname?.toLowerCase() || "";
      const lastname = user.lastname?.toLowerCase() || "";
      const email = user.email?.toLowerCase() || "";
      const tags = user.tags?.map((tag) => tag.toLowerCase()).join(" ") || "";

      return (
        username.includes(search) ||
        firstname.includes(search) ||
        lastname.includes(search) ||
        email.includes(search) ||
        tags.includes(search)
      );
    });
  }, [users, searchTerm]);

  // Handle individual user selection
  const handleUserToggle = (userId: string) => {
    const isSelected = selectedUserIds.includes(userId);
    if (isSelected) {
      onSelectionChange(selectedUserIds.filter((id) => id !== userId));
    } else {
      onSelectionChange([...selectedUserIds, userId]);
    }
  };

  // Handle tag-based selection
  const handleTagSelect = (tag: string) => {
    const usersWithTag = users
      .filter((user) => user.tags?.includes(tag))
      .map((user) => user.id?.toString())
      .filter((id): id is string => id !== undefined);

    // Check if all users with this tag are already selected
    const allTagUsersSelected = usersWithTag.every((id) =>
      selectedUserIds.includes(id),
    );

    if (allTagUsersSelected) {
      // Deselect all users with this tag
      onSelectionChange(
        selectedUserIds.filter((id) => !usersWithTag.includes(id)),
      );
    } else {
      // Select all users with this tag (add to existing selection)
      const newSelection = [...new Set([...selectedUserIds, ...usersWithTag])];
      onSelectionChange(newSelection);
    }
  };

  // Clear all selections
  const handleClearAll = () => {
    onSelectionChange([]);
  };

  // Select all filtered users
  const handleSelectAll = () => {
    const allFilteredIds = filteredUsers
      .map((user) => user.id?.toString())
      .filter((id): id is string => id !== undefined);
    const newSelection = [...new Set([...selectedUserIds, ...allFilteredIds])];
    onSelectionChange(newSelection);
  };

  const selectedCount = selectedUserIds.length;
  const filteredCount = filteredUsers.length;

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <Label className="text-sm font-medium">{label}</Label>
        <Input
          placeholder={placeholder}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full"
        />
      </div>

      {/* Tags Section */}
      {availableTags.length > 0 && (
        <div className="space-y-2">
          <Label className="text-xs font-medium text-muted-foreground">
            {t("userMultiSelect.tagsHint")}
          </Label>
          <div className="flex flex-wrap gap-2">
            {availableTags.map((tag) => {
              const usersWithTag = users.filter((user) =>
                user.tags?.includes(tag),
              );
              const selectedUsersWithTag = usersWithTag.filter((user) =>
                selectedUserIds.includes(user.id?.toString() || ""),
              );
              const isFullySelected =
                usersWithTag.length > 0 &&
                selectedUsersWithTag.length === usersWithTag.length;
              const isPartiallySelected =
                selectedUsersWithTag.length > 0 &&
                selectedUsersWithTag.length < usersWithTag.length;

              return (
                <Badge
                  key={tag}
                  variant={
                    isFullySelected
                      ? "default"
                      : isPartiallySelected
                        ? "secondary"
                        : "outline"
                  }
                  className="cursor-pointer transition-colors"
                  onClick={() => handleTagSelect(tag)}
                >
                  {tag} ({usersWithTag.length})
                </Badge>
              );
            })}
          </div>
        </div>
      )}

      {/* Selection Controls */}
      <div className="flex items-center justify-between text-sm">
        <span className="text-muted-foreground">
          {t("userMultiSelect.usersSelected", {
            selectedCount,
            totalUsers: users.length,
          })}
          {searchTerm && t("userMultiSelect.filtered", { filteredCount })}
        </span>
        <div className="flex gap-2">
          {selectedCount > 0 && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleClearAll}
              className="h-7 px-2 text-xs"
            >
              {t("userMultiSelect.clearAll")}
            </Button>
          )}
          {filteredCount > 0 && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleSelectAll}
              className="h-7 px-2 text-xs"
            >
              {t("userMultiSelect.selectAll")}
            </Button>
          )}
        </div>
      </div>

      {/* Users List */}
      <ScrollArea className="border rounded-md" style={{ height: maxHeight }}>
        <div className="p-2 space-y-1">
          {filteredUsers.length === 0 ? (
            <div className="text-center text-muted-foreground py-4">
              {searchTerm
                ? t("userMultiSelect.noUsersFound")
                : t("userMultiSelect.noUsersAvailable")}
            </div>
          ) : (
            filteredUsers.map((user) => {
              const userId = user.id?.toString() || "";
              const isSelected = selectedUserIds.includes(userId);

              return (
                <div
                  key={userId}
                  className="flex items-center space-x-3 p-2 rounded-md hover:bg-muted/50 cursor-pointer"
                  onClick={() => handleUserToggle(userId)}
                >
                  <Checkbox
                    checked={isSelected}
                    onChange={() => handleUserToggle(userId)}
                    className="pointer-events-none"
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-sm">
                        {user.username}
                      </span>
                    </div>
                    {user.tags && user.tags.length > 0 && (
                      <div className="flex flex-wrap gap-1 mt-1">
                        {user.tags.map((tag) => (
                          <Badge
                            key={tag}
                            variant="outline"
                            className="text-xs px-1 py-0 h-4"
                          >
                            {tag}
                          </Badge>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </ScrollArea>
    </div>
  );
}
