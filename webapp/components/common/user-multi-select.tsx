"use client";
import { useState, useMemo, useRef, useEffect } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { X, Search } from "lucide-react";
import type { UserDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface UserMultiSelectProps {
  users: UserDto[];
  selectedUserIds: string[];
  onSelectionChange: (userIds: string[]) => void;
  label?: string;
  placeholder?: string;
}

export function UserMultiSelect({
  users,
  selectedUserIds,
  onSelectionChange,
  label = "",
  placeholder = "",
}: UserMultiSelectProps) {
  const t = useT();
  const [searchTerm, setSearchTerm] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Close popover when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

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

  // Get selected users
  const selectedUsers = useMemo(() => {
    return users.filter((user) =>
      selectedUserIds.includes(user.id?.toString() || ""),
    );
  }, [users, selectedUserIds]);

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

    const allTagUsersSelected = usersWithTag.every((id) =>
      selectedUserIds.includes(id),
    );

    if (allTagUsersSelected) {
      onSelectionChange(
        selectedUserIds.filter((id) => !usersWithTag.includes(id)),
      );
    } else {
      const newSelection = [...new Set([...selectedUserIds, ...usersWithTag])];
      onSelectionChange(newSelection);
    }
  };

  // Remove selected user
  const handleRemoveUser = (userId: string) => {
    onSelectionChange(selectedUserIds.filter((id) => id !== userId));
  };

  return (
    <div className="space-y-3">
      {label && <Label className="text-sm font-medium">{label}</Label>}

      {/* Tags Section */}
      {availableTags.length > 0 && (
        <div className="space-y-2">
          <Label className="text-xs font-medium text-muted-foreground">
            {t("userMultiSelect.quickSelectByTags")}
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
                  className="cursor-pointer transition-colors hover:opacity-80"
                  onClick={() => handleTagSelect(tag)}
                >
                  {tag} ({usersWithTag.length})
                </Badge>
              );
            })}
          </div>
        </div>
      )}

      {/* Search Input with Popover */}
      <div className="relative" ref={containerRef}>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder={placeholder || t("userMultiSelect.searchPlaceholder")}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onFocus={() => setIsOpen(true)}
            className="w-full pl-9"
          />
        </div>

        {/* Popover with filtered users */}
        {isOpen && (
          <div className="absolute z-50 w-full mt-1 bg-background border rounded-md shadow-lg max-h-[300px] overflow-y-auto">
            {filteredUsers.length === 0 ? (
              <div className="text-center text-muted-foreground py-4 text-sm">
                {searchTerm
                  ? t("userMultiSelect.noUsersFound")
                  : t("userMultiSelect.noUsersAvailable")}
              </div>
            ) : (
              <div className="p-1">
                {filteredUsers.map((user) => {
                  const userId = user.id?.toString() || "";
                  const isSelected = selectedUserIds.includes(userId);

                  return (
                    <div
                      key={userId}
                      className="flex items-center space-x-3 p-2 rounded-md hover:bg-muted/50 cursor-pointer transition-colors"
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
                                className="text-xs px-1.5 py-0 h-4"
                              >
                                {tag}
                              </Badge>
                            ))}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Selected Users List - Always visible */}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <Label className="text-xs font-medium text-muted-foreground">
            <span className="inline-block min-w-[2ch] text-right">
              {t("userMultiSelect.selectedUsers")} ({selectedUsers.length}
            </span>
            )
          </Label>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => onSelectionChange([])}
            className={`h-6 px-2 text-xs ${selectedUsers.length === 0 ? "invisible" : ""}`}
            aria-hidden={selectedUsers.length === 0}
            disabled={selectedUsers.length === 0}
          >
            {t("userMultiSelect.removeAll")}
          </Button>
        </div>
        <div className="border rounded-md p-2 min-h-[60px] max-h-[60px] overflow-y-auto">
          {selectedUsers.length === 0 ? (
            <div className="flex items-center justify-center h-full text-xs text-muted-foreground">
              {t("userMultiSelect.noUsersSelected")}
            </div>
          ) : (
            <div className="flex flex-wrap gap-1.5">
              {selectedUsers.map((user) => {
                const userId = user.id?.toString() || "";

                return (
                  <Badge
                    key={userId}
                    variant="secondary"
                    className="pl-2 pr-1 py-1 gap-1 hover:bg-secondary/80 transition-colors"
                  >
                    <span className="text-xs font-medium">{user.username}</span>
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleRemoveUser(userId);
                      }}
                      className="hover:bg-muted rounded-full p-0.5 transition-colors"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
