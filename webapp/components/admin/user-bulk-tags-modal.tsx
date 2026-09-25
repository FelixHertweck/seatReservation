"use client";

import { useMemo, useState } from "react";
import { Check, Minus, Plus, Tags, X } from "lucide-react";

import { Button } from "@/components/custom-ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/custom-ui/label";
import { cn } from "@/lib/utils";
import type { UserDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

// "all": every selected user has the tag, "some": only part of them, "none": nobody.
type TagState = "all" | "some" | "none";

interface UserBulkTagsModalProps {
  isOpen: boolean;
  onClose: () => void;
  selectedUsers: UserDto[];
  suggestions: string[];
  onApply: (addTags: string[], removeTags: string[]) => Promise<void>;
}

// Cycle: partially-held tags first go to "all", then "none", then back to
// "some" (= leave untouched). Tags held by everyone toggle between all/none.
function nextState(current: TagState, initial: TagState): TagState {
  if (initial === "some") {
    return current === "some" ? "all" : current === "all" ? "none" : "some";
  }
  return current === "all" ? "none" : "all";
}

export function UserBulkTagsModal({
  isOpen,
  onClose,
  selectedUsers,
  suggestions,
  onApply,
}: UserBulkTagsModalProps) {
  const t = useT();
  const total = selectedUsers.length;

  const counts = useMemo(() => {
    const map = new Map<string, number>();
    for (const user of selectedUsers) {
      for (const tag of user.tags ?? []) {
        map.set(tag, (map.get(tag) ?? 0) + 1);
      }
    }
    return map;
  }, [selectedUsers]);

  const existingTags = useMemo(() => [...counts.keys()].sort(), [counts]);
  const initialState = (tag: string): TagState =>
    counts.get(tag) === total ? "all" : "some";

  const [overrides, setOverrides] = useState<Record<string, TagState>>({});
  const [newTags, setNewTags] = useState<string[]>([]);
  const [newTag, setNewTag] = useState("");
  const [isApplying, setIsApplying] = useState(false);

  const stateOf = (tag: string): TagState =>
    overrides[tag] ?? initialState(tag);

  const { addTags, removeTags } = useMemo(() => {
    const add = new Set(newTags);
    const remove = new Set<string>();
    for (const tag of existingTags) {
      const initial: TagState = counts.get(tag) === total ? "all" : "some";
      const current = overrides[tag] ?? initial;
      if (current === "all" && initial !== "all") add.add(tag);
      if (current === "none") remove.add(tag);
    }
    return { addTags: [...add], removeTags: [...remove] };
  }, [existingTags, counts, total, overrides, newTags]);

  // Tags of the system that are not part of the selection yet and not queued.
  const availableSuggestions = useMemo(
    () =>
      suggestions.filter((tag) => !counts.has(tag) && !newTags.includes(tag)),
    [suggestions, counts, newTags],
  );

  const affectedCount = useMemo(
    () =>
      selectedUsers.filter((user) => {
        const tags = new Set(user.tags ?? []);
        return (
          addTags.some((tag) => !tags.has(tag)) ||
          removeTags.some((tag) => tags.has(tag))
        );
      }).length,
    [selectedUsers, addTags, removeTags],
  );

  const handleClose = () => {
    setOverrides({});
    setNewTags([]);
    setNewTag("");
    onClose();
  };

  const handleAddNewTag = () => {
    const tag = newTag.trim();
    if (!tag) return;
    if (existingTags.includes(tag)) {
      setOverrides((prev) => ({ ...prev, [tag]: "all" }));
    } else if (!newTags.includes(tag)) {
      setNewTags((prev) => [...prev, tag]);
    }
    setNewTag("");
  };

  const handleApply = async () => {
    setIsApplying(true);
    try {
      await onApply(addTags, removeTags);
      handleClose();
    } catch {
      // The error is already surfaced by the toast of the caller.
    } finally {
      setIsApplying(false);
    }
  };

  const stateIcon = (state: TagState) =>
    state === "all" ? (
      <Check className="h-3.5 w-3.5" />
    ) : state === "some" ? (
      <Minus className="h-3.5 w-3.5" />
    ) : null;

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose()}>
      <DialogContent
        className="sm:max-w-lg sm:max-h-[85vh] sm:overflow-y-auto"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Tags className="h-5 w-5" />
            {t("userBulkTagsModal.title", { count: total })}
          </DialogTitle>
          <DialogDescription>
            {t("userBulkTagsModal.description")}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-2">
            <Label>{t("userBulkTagsModal.currentTagsLabel")}</Label>
            {existingTags.length === 0 && newTags.length === 0 && (
              <p className="text-sm text-muted-foreground">
                {t("userBulkTagsModal.noTags")}
              </p>
            )}
            <div className="space-y-1">
              {existingTags.map((tag) => {
                const state = stateOf(tag);
                const initial = initialState(tag);
                return (
                  <button
                    key={tag}
                    type="button"
                    onClick={() =>
                      setOverrides((prev) => ({
                        ...prev,
                        [tag]: nextState(state, initial),
                      }))
                    }
                    className="flex w-full items-center gap-3 rounded-md border px-3 py-2 text-left text-sm hover:bg-muted"
                  >
                    <span
                      className={cn(
                        "grid h-4 w-4 shrink-0 place-content-center rounded-sm border border-primary",
                        state !== "none" &&
                          "bg-primary text-primary-foreground",
                      )}
                    >
                      {stateIcon(state)}
                    </span>
                    <span className="flex-1 truncate">{tag}</span>
                    <span className="text-xs text-muted-foreground">
                      {t("userBulkTagsModal.countOf", {
                        count: counts.get(tag) ?? 0,
                        total,
                      })}
                    </span>
                    {state === "none" && (
                      <Badge variant="destructive" className="text-[10px]">
                        {t("userBulkTagsModal.willRemove")}
                      </Badge>
                    )}
                    {state === "all" && initial !== "all" && (
                      <Badge variant="secondary" className="text-[10px]">
                        {t("userBulkTagsModal.willAdd")}
                      </Badge>
                    )}
                  </button>
                );
              })}
              {newTags.map((tag) => (
                <div
                  key={tag}
                  className="flex items-center gap-3 rounded-md border px-3 py-2 text-sm"
                >
                  <span className="grid h-4 w-4 shrink-0 place-content-center rounded-sm border border-primary bg-primary text-primary-foreground">
                    <Check className="h-3.5 w-3.5" />
                  </span>
                  <span className="flex-1 truncate">{tag}</span>
                  <Badge variant="secondary" className="text-[10px]">
                    {t("userBulkTagsModal.willAdd")}
                  </Badge>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-auto p-0.5"
                    aria-label={t("userBulkTagsModal.removeNewTag")}
                    onClick={() =>
                      setNewTags((prev) => prev.filter((x) => x !== tag))
                    }
                  >
                    <X className="h-3 w-3" />
                  </Button>
                </div>
              ))}
            </div>
            <p className="text-xs text-muted-foreground">
              {t("userBulkTagsModal.hint")}
            </p>
          </div>

          <div className="space-y-2">
            <Label htmlFor="bulk-new-tag">
              {t("userBulkTagsModal.addTagLabel")}
            </Label>
            <div className="flex gap-2">
              <Input
                id="bulk-new-tag"
                value={newTag}
                maxLength={100}
                onChange={(e) => setNewTag(e.target.value)}
                placeholder={t("userBulkTagsModal.addTagPlaceholder")}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleAddNewTag();
                  }
                }}
              />
              <Button type="button" onClick={handleAddNewTag}>
                <Plus className="h-4 w-4" />
              </Button>
            </div>
            {availableSuggestions.length > 0 && (
              <div className="space-y-2 pt-1">
                <p className="text-xs text-muted-foreground">
                  {t("userBulkTagsModal.suggestionsLabel")}
                </p>
                <div className="flex max-h-24 flex-wrap gap-1.5 overflow-y-auto">
                  {availableSuggestions.map((tag) => (
                    <button
                      key={tag}
                      type="button"
                      onClick={() => setNewTags((prev) => [...prev, tag])}
                      className="rounded-full focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    >
                      <Badge variant="secondary" className="cursor-pointer">
                        {tag}
                      </Badge>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          <p className="text-sm">
            {t("userBulkTagsModal.affected", {
              count: affectedCount,
              total,
            })}
          </p>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={handleClose}>
              {t("common.cancel")}
            </Button>
            <Button
              type="button"
              onClick={handleApply}
              isLoading={isApplying}
              disabled={affectedCount === 0}
            >
              {t("userBulkTagsModal.apply")}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
