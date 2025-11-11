"use client";

import { forwardRef, useImperativeHandle, useRef, useState } from "react";
import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import { Check, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface UsernameSelectorProps {
  usernames: string[] | undefined;
  onSelectUsername: (username: string) => void;
  isLoadingUsernames: boolean;
  isLoadingInfo: boolean;
}

export interface UsernameSelectorRef {
  resetSelectedUsername: () => void;
}

export const UsernameSelector = forwardRef<
  UsernameSelectorRef,
  UsernameSelectorProps
>(
  (
    {
      usernames,
      onSelectUsername,
      isLoadingUsernames,
      isLoadingInfo,
    }: UsernameSelectorProps,
    ref,
  ) => {
    const t = useT();
    const [open, setOpen] = useState(false);
    const [selectedUsername, setSelectedUsername] = useState<string | null>(
      null,
    );
    const [searchValue, setSearchValue] = useState("");
    const popoverRef = useRef<HTMLDivElement>(null);

    useImperativeHandle(ref, () => ({
      resetSelectedUsername: () => {
        setSelectedUsername(null);
        setSearchValue("");
        setOpen(false);
      },
    }));

    const filteredUsernames = usernames?.filter((username) =>
      username.toLowerCase().includes(searchValue.toLowerCase()),
    );

    const handleSelectUsername = (username: string) => {
      setSelectedUsername(username);
      setSearchValue(username);
      setOpen(false);
    };

    const handleFetchReservations = () => {
      if (selectedUsername) {
        onSelectUsername(selectedUsername);
        setSelectedUsername(null);
        setSearchValue("");
      }
    };

    return (
      <div className="space-y-4">
        <div className="space-y-2 relative">
          <div className="relative">
            <input
              type="text"
              placeholder={t("checkin.usernameSelector.searchUser")}
              value={searchValue}
              onChange={(e) => {
                setSearchValue(e.target.value);
                setSelectedUsername(null);
              }}
              onFocus={() => setOpen(true)}
              onBlur={() => {
                setTimeout(() => {
                  setOpen(false);
                }, 100);
              }}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-1 focus:ring-blue-500 focus:border-blue-500"
              disabled={isLoadingUsernames}
            />
            {isLoadingUsernames && (
              <div className="absolute right-3 top-1/2 -translate-y-1/2">
                <Loader2 className="h-4 w-4 animate-spin text-gray-500" />
              </div>
            )}
          </div>
          {open && (
            <div
              ref={popoverRef}
              className="absolute top-full left-0 right-0 z-50 border border-gray-300 rounded-md bg-white shadow-lg"
            >
              <Command className="w-full">
                <CommandList>
                  {isLoadingUsernames ? (
                    <div className="p-4 flex items-center justify-center">
                      <Loader2 className="h-4 w-4 animate-spin mr-2 text-gray-500" />
                      <span className="text-sm text-gray-500">
                        {t("checkin.usernameSelector.loadingUsers")}
                      </span>
                    </div>
                  ) : filteredUsernames && filteredUsernames.length === 0 ? (
                    <CommandEmpty>
                      {t("checkin.usernameSelector.noUserFound")}
                    </CommandEmpty>
                  ) : (
                    <CommandGroup>
                      {filteredUsernames?.map((username) => (
                        <CommandItem
                          key={username}
                          value={username ?? "unknown"}
                          onSelect={() => {
                            handleSelectUsername(username);
                          }}
                        >
                          <Check
                            className={cn(
                              "mr-2 h-4 w-4",
                              selectedUsername === username
                                ? "opacity-100"
                                : "opacity-0",
                            )}
                          />
                          {username}
                        </CommandItem>
                      ))}
                    </CommandGroup>
                  )}
                </CommandList>
              </Command>
            </div>
          )}
        </div>
        <Button
          onClick={handleFetchReservations}
          disabled={!selectedUsername || isLoadingInfo || open}
          className="w-full"
        >
          {isLoadingInfo && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {t("checkin.usernameSelector.fetchReservations")}
        </Button>
      </div>
    );
  },
);
