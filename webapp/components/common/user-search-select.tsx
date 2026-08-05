"use client";
import { useState, useMemo, useRef, useEffect } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/custom-ui/button";
import { Check, Plus, Search, X } from "@/components/icons";
import type { UserDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { cn } from "@/lib/utils";

interface UserSearchSelectProps {
  users: UserDto[];
  selectedUserId: string;
  onSelectionChange: (userId: string) => void;
  label?: string;
  placeholder?: string;
}

export function UserSearchSelect({
  users,
  selectedUserId,
  onSelectionChange,
  label = "",
  placeholder = "",
}: UserSearchSelectProps) {
  const t = useT();
  const [searchTerm, setSearchTerm] = useState("");
  const [activeTags, setActiveTags] = useState<string[]>([]);
  const [isResultsOpen, setIsResultsOpen] = useState(false);
  const [isTagPickerOpen, setIsTagPickerOpen] = useState(false);
  const [tagQuery, setTagQuery] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);

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

  const selectedUser = useMemo(
    () => users.find((user) => user.id?.toString() === selectedUserId),
    [users, selectedUserId],
  );

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

  const handleSelectUser = (userId: string) => {
    onSelectionChange(userId);
    setIsResultsOpen(false);
  };

  const handleClearSelection = () => {
    onSelectionChange("");
  };

  return (
    <div className="space-y-2" ref={containerRef}>
      {label && <Label className="text-sm font-medium">{label}</Label>}

      {/* Name / email search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder={placeholder || t("userSearchSelect.searchPlaceholder")}
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          onFocus={() => setIsResultsOpen(true)}
          className="w-full pl-9"
        />
      </div>

      {/* Currently selected user */}
      {selectedUser && (
        <div className="flex items-center justify-between gap-2 rounded-md border bg-muted/40 px-3 py-1.5">
          <span className="text-xs text-muted-foreground">
            {t("userSearchSelect.selectedLabel")}{" "}
            <span className="font-medium text-foreground">
              {selectedUser.username}
            </span>
          </span>
          <button
            type="button"
            onClick={handleClearSelection}
            className="hover:bg-muted rounded-full p-0.5 transition-colors shrink-0"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* Tag filter */}
      {availableTags.length > 0 && (
        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <Label className="text-xs font-medium text-muted-foreground">
              {t("userSearchSelect.tagsFilterLabel")}
            </Label>
            {hasActiveFilters && (
              <button
                type="button"
                onClick={handleResetFilters}
                className="text-xs text-muted-foreground hover:text-foreground transition-colors"
              >
                {t("userSearchSelect.resetFilters")}
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
                {t("userSearchSelect.addTagButton")}
              </Button>

              {isTagPickerOpen && (
                <div className="absolute z-50 mt-1 w-48 bg-background border rounded-md shadow-lg p-1.5 space-y-1.5">
                  <Input
                    autoFocus
                    placeholder={t("userSearchSelect.addTagPlaceholder")}
                    value={tagQuery}
                    onChange={(e) => setTagQuery(e.target.value)}
                    className="h-7 text-xs"
                  />
                  <div className="max-h-40 overflow-y-auto">
                    {addableTags.length === 0 ? (
                      <div className="text-center text-muted-foreground py-2 text-xs">
                        {t("userSearchSelect.noTagsFound")}
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

      {/* Results */}
      {isResultsOpen && (
        <div className="border rounded-md shadow-sm max-h-[260px] overflow-y-auto">
          <div className="px-2 py-1.5 border-b text-xs text-muted-foreground">
            {filteredUsers.length === 1
              ? t("userSearchSelect.resultsCountSingular")
              : t("userSearchSelect.resultsCount", {
                  count: filteredUsers.length,
                })}
          </div>

          {filteredUsers.length === 0 ? (
            <div className="text-center text-muted-foreground py-4 text-sm px-2">
              {activeTags.length > 0
                ? t("userSearchSelect.noUsersFoundWithTags")
                : t("userSearchSelect.noUsersFound")}{" "}
              {hasActiveFilters && (
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="text-foreground underline underline-offset-2"
                >
                  {t("userSearchSelect.resetFilters")}
                </button>
              )}
            </div>
          ) : (
            <div className="p-1">
              {filteredUsers.map((user) => {
                const userId = user.id?.toString() || "";
                const isSelected = userId === selectedUserId;

                return (
                  <div
                    key={userId}
                    className="flex items-center space-x-3 p-2 rounded-md hover:bg-muted/50 cursor-pointer transition-colors"
                    onClick={() => handleSelectUser(userId)}
                  >
                    <Check
                      className={cn(
                        "h-4 w-4 shrink-0",
                        isSelected ? "opacity-100" : "opacity-0",
                      )}
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
  );
}
