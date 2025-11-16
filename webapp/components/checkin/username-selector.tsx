"use client";

import { useState, useEffect } from "react";
import { useT } from "@/lib/i18n/hooks";
import { Check, Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface UsernameSelectorProps {
  onSelectUsername: (username: string) => void;
  eventId: bigint;
  getUsernamesByEventId: (eventId: bigint) => Promise<string[] | undefined>;
  resetTrigger: boolean;
}

export const UsernameSelector = ({
  getUsernamesByEventId,
  eventId,
  onSelectUsername,
  resetTrigger,
}: UsernameSelectorProps) => {
  const t = useT();
  const [open, setOpen] = useState(false);
  const [selectedUsername, setSelectedUsername] = useState<string | null>(null);
  const [searchValue, setSearchValue] = useState("");
  const [usernames, setUsernames] = useState<string[]>([]);
  const [isLoadingUsernames, setIsLoadingUsernames] = useState(false);

  // Load usernames when input is focused or when component mounts
  useEffect(() => {
    const loadUsernames = async () => {
      if (eventId) {
        setIsLoadingUsernames(true);
        try {
          const fetchedUsernames = await getUsernamesByEventId(eventId);
          setUsernames(fetchedUsernames || []);
        } catch (error) {
          console.error("Error loading usernames:", error);
          setUsernames([]);
        } finally {
          setIsLoadingUsernames(false);
        }
      }
    };

    loadUsernames();
  }, [eventId, getUsernamesByEventId]);

  // Reset when resetTrigger changes
  useEffect(() => {
    setSelectedUsername(null);
    setSearchValue("");
    setOpen(false);
  }, [resetTrigger]);

  const filteredUsernames = usernames?.filter((username) =>
    username.toLowerCase().includes(searchValue.toLowerCase()),
  );

  const handleSelectUsername = (username: string) => {
    setSelectedUsername(username);
    setSearchValue(username);
    setOpen(false);
    // Automatically fetch reservations when user is selected
    onSelectUsername(username);
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
              }, 200);
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
          <div className="absolute top-full left-0 right-0 z-50 border border-gray-300 rounded-md bg-white shadow-lg">
            <div className="w-full">
              <div>
                {isLoadingUsernames ? (
                  <div className="p-4 flex items-center justify-center">
                    <Loader2 className="h-4 w-4 animate-spin mr-2 text-gray-500" />
                    <span className="text-sm text-gray-500">
                      {t("checkin.usernameSelector.loadingUsers")}
                    </span>
                  </div>
                ) : filteredUsernames && filteredUsernames.length === 0 ? (
                  <div className="p-4 text-left text-sm text-muted-foreground">
                    {t("checkin.usernameSelector.noUserFound")}
                  </div>
                ) : (
                  <div>
                    {filteredUsernames?.map((username) => (
                      <div
                        key={username}
                        onMouseDown={(e) => {
                          e.preventDefault();
                          handleSelectUsername(username);
                        }}
                        className="cursor-pointer px-2 py-1.5 text-sm rounded hover:bg-accent hover:text-accent-foreground flex items-center"
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
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

UsernameSelector.displayName = "UsernameSelector";
