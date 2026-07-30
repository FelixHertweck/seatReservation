"use client";

import { useEffect, useState } from "react";
import { Search, Filter } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useT } from "@/lib/i18n/hooks";

interface FilterOption {
  key: string;
  label: string;
  type: "boolean" | "string" | "number" | "select";
  options?: { value: string; label: string }[];
}

interface SearchAndFilterProps {
  onSearch: (query: string) => void;
  onFilter: (filters: Record<string, unknown>) => void;
  filterOptions: FilterOption[];
  initialFilters?: Record<string, string>;
  initialQuery?: string;
  className?: string;
}

export function SearchAndFilter({
  onSearch,
  onFilter,
  filterOptions,
  initialFilters = {},
  initialQuery = "",
  className = "w-full",
}: SearchAndFilterProps) {
  const t = useT();

  const [searchQuery, setSearchQuery] = useState(initialQuery);
  const [filters, setFilters] =
    useState<Record<string, unknown>>(initialFilters);

  useEffect(() => {
    setSearchQuery(initialQuery);
  }, [initialQuery]);

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    onSearch(query);
  };

  const handleFilterChange = (key: string, value: unknown) => {
    const newFilters = { ...filters };
    if (
      value === false ||
      value === "" ||
      value === null ||
      value === undefined
    ) {
      delete newFilters[key];
    } else {
      newFilters[key] = value;
    }
    setFilters(newFilters);
    onFilter(newFilters);
  };

  return (
    <div className={className}>
      {/* Search Bar and Filter Toggle */}
      <div className="group flex w-full items-center justify-end gap-3">
        <div className="relative h-10 w-10 shrink-0 max-sm:has-[input:focus]:w-full max-sm:group-has-[button]:has-[input:focus]:w-[calc(100%-3.25rem)] sm:w-auto sm:min-w-0 sm:flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <Input
            placeholder={t("searchAndFilter.searchPlaceholder")}
            value={searchQuery}
            onChange={(e) => handleSearch(e.target.value)}
            className="h-10 pl-8 pr-1 max-sm:not-focus:placeholder:text-transparent focus:pr-3 sm:pr-3"
          />
        </div>
        {filterOptions.length > 0 && (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="outline"
                size="icon-lg"
                className="h-10 w-10 shrink-0 sm:w-auto sm:px-4"
              >
                <Filter className="h-4 w-4" />
                <span className="hidden sm:inline">
                  {t("searchAndFilter.filtersButton")}
                </span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              align="end"
              className="w-80 p-4 max-w-[calc(100vw-2rem)]"
            >
              <div className="grid grid-cols-1 gap-4">
                {filterOptions.map((option) => (
                  <div key={option.key} className="space-y-2">
                    <label className="text-sm font-medium">
                      {option.label}
                    </label>
                    {option.type === "boolean" && (
                      <div className="flex items-center space-x-2">
                        <Checkbox
                          id={option.key}
                          checked={!!filters[option.key]}
                          onCheckedChange={(checked) =>
                            handleFilterChange(option.key, checked)
                          }
                        />
                        <label htmlFor={option.key} className="text-sm">
                          {option.label}
                        </label>
                      </div>
                    )}
                    {option.type === "select" && option.options && (
                      <Select
                        value={(filters[option.key] as string) || ""}
                        onValueChange={(value) =>
                          handleFilterChange(option.key, value)
                        }
                      >
                        <SelectTrigger>
                          <SelectValue
                            placeholder={t(
                              "searchAndFilter.selectPlaceholder",
                              { label: option.label },
                            )}
                          />
                        </SelectTrigger>
                        <SelectContent>
                          {option.options.map((opt) => (
                            <SelectItem key={opt.value} value={opt.value}>
                              {opt.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    )}
                    {option.type === "string" && (
                      <Input
                        placeholder={t("searchAndFilter.enterPlaceholder", {
                          label: option.label,
                        })}
                        value={(filters[option.key] as string) || ""}
                        onChange={(e) =>
                          handleFilterChange(option.key, e.target.value)
                        }
                      />
                    )}
                  </div>
                ))}
              </div>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>
    </div>
  );
}
