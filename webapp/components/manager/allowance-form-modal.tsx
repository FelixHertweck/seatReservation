"use client";

import { CommandGroup } from "@/components/ui/command";
import { CommandEmpty } from "@/components/ui/command";
import { useState, useEffect, useMemo } from "react"; // Added useMemo
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Command,
  CommandList,
  CommandInput,
  CommandItem,
} from "@/components/ui/command";
import { ChevronsUpDown, XCircle } from "lucide-react";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { toast } from "@/components/ui/use-toast";
import { t } from "i18next";

import type {
  UserDto,
  DetailedEventResponseDto,
  EventUserAllowancesDto,
  EventUserAllowanceUpdateDto,
  EventUserAllowancesCreateDto,
} from "@/api";

interface AllowanceFormModalProps {
  allowance: EventUserAllowancesDto | null; // Null for creation, object for update
  users: UserDto[];
  events: DetailedEventResponseDto[];
  isCreating: boolean;
  onSubmit: (
    allowanceData: EventUserAllowancesCreateDto | EventUserAllowanceUpdateDto,
  ) => Promise<void>;
  onClose: () => void;
}

export function AllowanceFormModal({
  allowance,
  users,
  events,
  isCreating,
  onSubmit,
  onClose,
}: AllowanceFormModalProps) {
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>(
    allowance && !isCreating ? [allowance.userId?.toString() || ""] : [],
  );
  const [selectedEventId, setSelectedEventId] = useState<string | undefined>(
    allowance?.eventId?.toString(),
  );
  const [allowedReservations, setAllowedReservations] = useState(
    allowance?.reservationsAllowedCount?.toString() || "",
  );
  const [userSearchOpen, setUserSearchOpen] = useState(false);
  const [selectedTag, setSelectedTag] = useState<string | undefined>(undefined);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (allowance && !isCreating) {
      setSelectedUserIds([allowance.userId?.toString() || ""]);
      setSelectedEventId(allowance.eventId?.toString());
      setAllowedReservations(
        allowance.reservationsAllowedCount?.toString() || "",
      );
    } else if (isCreating) {
      setSelectedUserIds([]);
      setSelectedEventId(undefined);
      setAllowedReservations("");
    }
    setSelectedTag(undefined); // Reset tag selection on modal open/mode change
  }, [allowance, isCreating]);

  const allTags = Array.from(new Set(users.flatMap((user) => user.tags || [])));

  const handleUserToggle = (userId: string, checked: boolean) => {
    setSelectedUserIds((prev) =>
      checked ? [...prev, userId] : prev.filter((id) => id !== userId),
    );
  };

  const handleSelectUsersByTag = (tag: string) => {
    setSelectedTag(tag);
    const usersWithTagIds = users
      .filter((user) => user.tags?.includes(tag))
      .map((user) => user.id?.toString() || "")
      .filter(Boolean) as string[];

    // Add users with this tag that are not already selected
    const newSelectedUsers = Array.from(
      new Set([...selectedUserIds, ...usersWithTagIds]),
    );
    setSelectedUserIds(newSelectedUsers);
  };

  const handleClearTag = () => {
    setSelectedTag(undefined);
    // Do not clear selectedUserIds automatically, user can deselect manually
  };

  const handleClearAllSelectedUsers = () => {
    setSelectedUserIds([]);
  };

  const handleSubmit = async () => {
    if (!selectedEventId || !allowedReservations) {
      toast({
        title: t("allowanceFormModal.validationErrorTitle"),
        description: t("allowanceFormModal.validationErrorDescription"),
        variant: "destructive",
      });
      return;
    }

    setIsLoading(true);
    try {
      const eventId = BigInt(selectedEventId);
      const reservations = Number.parseInt(allowedReservations, 10);

      if (isCreating) {
        if (selectedUserIds.length === 0) {
          toast({
            title: t("allowanceFormModal.validationErrorTitle"),
            description: t("allowanceFormModal.selectAtLeastOneUser"),
            variant: "destructive",
          });
          return;
        }
        const allowanceData: EventUserAllowancesCreateDto = {
          eventId,
          userIds: selectedUserIds.map((id) => BigInt(id)),
          reservationsAllowedCount: reservations,
        };
        await onSubmit(allowanceData);
        toast({
          title: t("allowanceFormModal.successTitle"),
          description: t("allowanceFormModal.allowanceCreatedSuccess"),
        });
      } else {
        // For update, ensure a single user is selected (or was pre-selected)
        if (!allowance?.id || selectedUserIds.length !== 1) {
          toast({
            title: t("allowanceFormModal.validationErrorTitle"),
            description: t("allowanceFormModal.selectExactlyOneUser"),
            variant: "destructive",
          });
          return;
        }
        const allowanceData: EventUserAllowanceUpdateDto = {
          id: allowance.id,
          eventId,
          userId: BigInt(selectedUserIds[0]),
          reservationsAllowedCount: reservations,
        };
        await onSubmit(allowanceData);
        toast({
          title: t("allowanceFormModal.successTitle"),
          description: t("allowanceFormModal.allowanceUpdatedSuccess"),
        });
      }
      onClose();
    } catch (error) {
      console.error("Failed to submit allowance:", error);
      toast({
        title: t("allowanceFormModal.submissionErrorTitle"),
        description: t("allowanceFormModal.submissionErrorDescription"),
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const filteredUsers = useMemo(() => {
    if (!isCreating && allowance?.userId) {
      // In update mode, only show the pre-selected user
      return users.filter(
        (user) => user.id?.toString() === allowance.userId?.toString(),
      );
    }
    // In creation mode, filter by tag if selected, otherwise show all
    return users.filter((user) => {
      if (!selectedTag) return true;
      return user.tags?.includes(selectedTag);
    });
  }, [users, selectedTag, isCreating, allowance]);

  const getSelectedUsernames = () => {
    return selectedUserIds
      .map((id) => users.find((u) => u.id?.toString() === id)?.username)
      .filter(Boolean) as string[];
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>
            {isCreating
              ? t("allowanceFormModal.addNewAllowanceTitle")
              : t("allowanceFormModal.editAllowanceTitle")}
          </DialogTitle>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="event" className="text-right">
              {t("allowanceFormModal.eventLabel")}
            </Label>
            <Select
              value={selectedEventId}
              onValueChange={setSelectedEventId}
              disabled={!isCreating} // Event typically not editable after creation
            >
              <SelectTrigger className="col-span-3">
                <SelectValue
                  placeholder={t("allowanceFormModal.selectEventPlaceholder")}
                />
              </SelectTrigger>
              <SelectContent>
                {events.map((event) => (
                  <SelectItem
                    key={event.id?.toString()}
                    value={event.id?.toString() || ""}
                  >
                    {event.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="grid grid-cols-4 items-start gap-4">
            <Label htmlFor="users" className="text-right pt-2">
              {t("allowanceFormModal.usersLabel")}
            </Label>
            <div className="col-span-3 flex flex-col gap-2">
              <Popover open={userSearchOpen} onOpenChange={setUserSearchOpen}>
                <PopoverTrigger asChild>
                  <Button
                    variant="outline"
                    role="combobox"
                    aria-expanded={userSearchOpen}
                    className="w-full justify-between bg-transparent"
                    disabled={!isCreating && selectedUserIds.length > 0} // Disable if not creating and user already selected for update
                  >
                    {selectedUserIds.length > 0
                      ? t("allowanceFormModal.usersSelected", {
                          count: selectedUserIds.length,
                        })
                      : t("allowanceFormModal.selectUsersPlaceholder")}
                    <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-[--radix-popover-trigger-width] p-0">
                  <Command>
                    {isCreating && allTags.length > 0 && (
                      <div className="p-2 border-b flex items-center gap-2">
                        <Select
                          onValueChange={handleSelectUsersByTag}
                          value={selectedTag}
                        >
                          <SelectTrigger className="flex-grow">
                            <SelectValue
                              placeholder={t(
                                "allowanceFormModal.filterByTagPlaceholder",
                              )}
                            />
                          </SelectTrigger>
                          <SelectContent>
                            {allTags.map((tag) => (
                              <SelectItem key={tag} value={tag}>
                                {tag}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        {selectedTag && (
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={handleClearTag}
                            aria-label={t(
                              "allowanceFormModal.clearTagSelectionAriaLabel",
                            )}
                          >
                            <XCircle className="h-4 w-4" />
                          </Button>
                        )}
                      </div>
                    )}
                    <CommandInput
                      placeholder={t(
                        "allowanceFormModal.searchUserPlaceholder",
                      )}
                    />
                    <CommandList>
                      <CommandEmpty>
                        {t("allowanceFormModal.noUserFound")}
                      </CommandEmpty>
                      <CommandGroup>
                        {filteredUsers.map((user) => (
                          <CommandItem
                            key={user.id?.toString()}
                            value={user.username}
                            onSelect={() =>
                              handleUserToggle(
                                user.id?.toString() || "",
                                !selectedUserIds.includes(
                                  user.id?.toString() || "",
                                ),
                              )
                            }
                          >
                            <Checkbox
                              checked={selectedUserIds.includes(
                                user.id?.toString() || "",
                              )}
                              onCheckedChange={(checked) =>
                                handleUserToggle(
                                  user.id?.toString() || "",
                                  checked as boolean,
                                )
                              }
                              className="mr-2"
                            />
                            {user.username}
                            {user.tags && user.tags.length > 0 && (
                              <div className="ml-auto flex gap-1">
                                {user.tags.map((tag) => (
                                  <Badge key={tag} variant="secondary">
                                    {tag}
                                  </Badge>
                                ))}
                              </div>
                            )}
                          </CommandItem>
                        ))}
                      </CommandGroup>
                    </CommandList>
                  </Command>
                </PopoverContent>
              </Popover>

              {isCreating && selectedUserIds.length > 0 && (
                <div className="flex flex-wrap gap-2 mt-2">
                  {getSelectedUsernames().map((username) => (
                    <Badge key={username} variant="outline">
                      {username}
                    </Badge>
                  ))}
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleClearAllSelectedUsers}
                    className="text-xs text-red-500 hover:text-red-600"
                  >
                    {t("allowanceFormModal.clearAllButton")}
                  </Button>
                </div>
              )}
              {!isCreating && selectedUserIds.length > 0 && (
                <div className="text-sm text-muted-foreground">
                  {t("allowanceFormModal.selectedUserLabel")}{" "}
                  {
                    users.find((u) => u.id?.toString() === selectedUserIds[0])
                      ?.username
                  }
                </div>
              )}
            </div>
          </div>

          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="allowedReservations" className="text-right">
              {t("allowanceFormModal.allowedReservationsLabel")}
            </Label>
            <Input
              id="allowedReservations"
              type="number"
              value={allowedReservations}
              onChange={(e) => setAllowedReservations(e.target.value)}
              className="col-span-3"
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={isLoading}>
            {t("allowanceFormModal.cancelButton")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={
              isLoading ||
              selectedUserIds.length === 0 ||
              !selectedEventId ||
              !allowedReservations
            }
          >
            {isLoading
              ? t("allowanceFormModal.submittingButton")
              : isCreating
                ? t("allowanceFormModal.createAllowanceButton")
                : t("allowanceFormModal.saveChangesButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
