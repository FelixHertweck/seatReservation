"use client";
import { useState, useMemo, useRef, useEffect } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/custom-ui/button";
import { Plus, Search, X } from "@/components/icons";
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
  const [activeTags, setActiveTags] = useState<string[]>([]);
  const [isResultsOpen, setIsResultsOpen] = useState(false);
  const [isTagPickerOpen, setIsTagPickerOpen] = useState(false);
  const [tagQuery, setTagQuery] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);

  // Close popovers when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        setIsResultsOpen(false);
        setIsTagPickerOpen(false);
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

  const addableTags = useMemo(() => {
    const query = tagQuery.toLowerCase().trim();
    return availableTags.filter(
      (tag) => !activeTags.includes(tag) && tag.toLowerCase().includes(query),
    );
  }, [availableTags, activeTags, tagQuery]);

  const hasActiveFilters = searchTerm.trim() !== "" || activeTags.length > 0;

  // Filter users based on name/email search and active tag filters
  const filteredUsers = useMemo(() => {
    const search = searchTerm.toLowerCase().trim();
    return users.filter((user) => {
      if (
        activeTags.length > 0 &&
        !activeTags.every((tag) => user.tags?.includes(tag))
      ) {
        return false;
      }
      if (!search) return true;

      const username = user.username?.toLowerCase() || "";
      const firstname = user.firstname?.toLowerCase() || "";
      const lastname = user.lastname?.toLowerCase() || "";
      const email = user.email?.toLowerCase() || "";

      return (
        username.includes(search) ||
        firstname.includes(search) ||
        lastname.includes(search) ||
        email.includes(search)
      );
    });
  }, [users, searchTerm, activeTags]);

  // Get selected users
  const selectedUsers = useMemo(() => {
    return users.filter((user) =>
      selectedUserIds.includes(user.id?.toString() || ""),
    );
  }, [users, selectedUserIds]);

  const filteredUserIds = useMemo(
    () =>
      filteredUsers
        .map((user) => user.id?.toString())
        .filter((id): id is string => id !== undefined),
    [filteredUsers],
  );

  const allFilteredSelected =
    filteredUserIds.length > 0 &&
    filteredUserIds.every((id) => selectedUserIds.includes(id));

  // Handle individual user selection
  const handleUserToggle = (userId: string) => {
    const isSelected = selectedUserIds.includes(userId);
    if (isSelected) {
      onSelectionChange(selectedUserIds.filter((id) => id !== userId));
    } else {
      onSelectionChange([...selectedUserIds, userId]);
    }
  };

  // Select/deselect every user matching the current search + tag filters
  const handleToggleSelectAllFiltered = () => {
    if (allFilteredSelected) {
      onSelectionChange(
        selectedUserIds.filter((id) => !filteredUserIds.includes(id)),
      );
    } else {
      onSelectionChange([...new Set([...selectedUserIds, ...filteredUserIds])]);
    }
  };

  const handleAddTag = (tag: string) => {
    setActiveTags((prev) => [...prev, tag]);
    setTagQuery("");
    setIsTagPickerOpen(false);
    setIsResultsOpen(true);
  };

  const handleRemoveTag = (tag: string) => {
    setActiveTags((prev) => prev.filter((t) => t !== tag));
  };

  const handleResetFilters = () => {
    setSearchTerm("");
    setActiveTags([]);
  };

  // Remove selected user
  const handleRemoveUser = (userId: string) => {
    onSelectionChange(selectedUserIds.filter((id) => id !== userId));
  };

  return (
    <div className="space-y-3" ref={containerRef}>
      {label && <Label className="text-sm font-medium">{label}</Label>}

      {/* Name / email search + floating results popup */}
      <div className="relative">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder={placeholder || t("userMultiSelect.searchPlaceholder")}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            onFocus={() => setIsResultsOpen(true)}
            className="w-full pl-9"
          />
        </div>

        {/* Results popup - absolutely positioned so it overlays instead of shifting layout */}
        {isResultsOpen && (
          <div className="absolute left-0 right-0 top-full z-50 mt-1 border rounded-md shadow-lg bg-background max-h-[300px] overflow-y-auto">
            <div className="flex items-center justify-between px-2 py-1.5 border-b text-xs text-muted-foreground">
              <span>
                {filteredUsers.length === 1
                  ? t("userMultiSelect.resultsCountSingular")
                  : t("userMultiSelect.resultsCount", {
                      count: filteredUsers.length,
                    })}
              </span>
              {filteredUserIds.length > 0 && (
                <button
                  type="button"
                  onClick={handleToggleSelectAllFiltered}
                  className="text-foreground hover:underline underline-offset-2"
                >
                  {allFilteredSelected
                    ? t("userMultiSelect.deselectAllFiltered")
                    : t("userMultiSelect.selectAllFiltered")}
                </button>
              )}
            </div>

            {filteredUsers.length === 0 ? (
              <div className="text-center text-muted-foreground py-4 text-sm px-2">
                {activeTags.length > 0
                  ? t("userMultiSelect.noUsersFoundWithTags")
                  : t("userMultiSelect.noUsersFound")}{" "}
                {hasActiveFilters && (
                  <button
                    type="button"
                    onClick={handleResetFilters}
                    className="text-foreground underline underline-offset-2"
                  >
                    {t("userMultiSelect.resetFilters")}
                  </button>
                )}
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
                      onClick={(e) => {
                        // Radix's Checkbox renders a hidden native input for
                        // form integration and re-dispatches an untrusted
                        // "click" that bubbles here whenever `checked`
                        // changes, so ignore it to avoid toggling twice
                        // (which would otherwise flip-flop forever).
                        if (!e.isTrusted) return;
                        handleUserToggle(userId);
                      }}
                    >
                      <Checkbox
                        checked={isSelected}
                        onCheckedChange={() => handleUserToggle(userId)}
                        className="pointer-events-none"
                      />
                      <div className="flex-1 min-w-0">
                        <div className="font-medium text-sm truncate">
                          {user.username}
                        </div>
                        {user.email && (
                          <div className="text-xs text-muted-foreground truncate">
                            {user.email}
                          </div>
                        )}
                        {user.tags && user.tags.length > 0 && (
                          <div className="text-xs text-muted-foreground mt-0.5 truncate">
                            {user.tags.join(" · ")}
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

      {/* Tag filter */}
      {availableTags.length > 0 && (
        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <Label className="text-xs font-medium text-muted-foreground">
              {t("userMultiSelect.tagsFilterLabel")}
            </Label>
            {hasActiveFilters && (
              <button
                type="button"
                onClick={handleResetFilters}
                className="text-xs text-muted-foreground hover:text-foreground transition-colors"
              >
                {t("userMultiSelect.resetFilters")}
              </button>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-1.5">
            {activeTags.map((tag) => (
              <Badge
                key={tag}
                variant="secondary"
                className="pl-2 pr-1 py-1 gap-1"
              >
                <span className="text-xs">{tag}</span>
                <button
                  type="button"
                  onClick={() => handleRemoveTag(tag)}
                  className="hover:bg-muted rounded-full p-0.5 transition-colors"
                >
                  <X className="h-3 w-3" />
                </button>
              </Badge>
            ))}

            <div className="relative">
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="h-6 px-2 text-xs gap-1"
                onClick={() => setIsTagPickerOpen((prev) => !prev)}
              >
                <Plus className="h-3 w-3" />
                {t("userMultiSelect.addTagButton")}
              </Button>

              {isTagPickerOpen && (
                <div className="absolute z-50 mt-1 w-48 bg-background border rounded-md shadow-lg p-1.5 space-y-1.5">
                  <Input
                    autoFocus
                    placeholder={t("userMultiSelect.addTagPlaceholder")}
                    value={tagQuery}
                    onChange={(e) => setTagQuery(e.target.value)}
                    className="h-7 text-xs"
                  />
                  <div className="max-h-40 overflow-y-auto">
                    {addableTags.length === 0 ? (
                      <div className="text-center text-muted-foreground py-2 text-xs">
                        {t("userMultiSelect.noTagsFound")}
                      </div>
                    ) : (
                      addableTags.map((tag) => (
                        <div
                          key={tag}
                          onClick={() => handleAddTag(tag)}
                          className="cursor-pointer px-2 py-1 rounded text-xs hover:bg-muted/50 transition-colors"
                        >
                          {tag}
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

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
