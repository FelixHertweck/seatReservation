"use client";

import { useEffect, useState } from "react";
import { Search, X, Filter } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Card, CardContent } from "@/components/ui/card";
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
}

export function SearchAndFilter({
  onSearch,
  onFilter,
  filterOptions,
  initialFilters = {},
  initialQuery = "",
}: SearchAndFilterProps) {
  const t = useT();

  const [searchQuery, setSearchQuery] = useState(initialQuery);
  const [filters, setFilters] =
    useState<Record<string, unknown>>(initialFilters);
  const [showFilters, setShowFilters] = useState(false);

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

  const clearFilter = (key: string) => {
    const newFilters = { ...filters };
    delete newFilters[key];
    setFilters(newFilters);
    onFilter(newFilters);
  };

  const clearAllFilters = () => {
    setFilters({});
    onFilter({});
  };

  const getFilterDisplayName = (key: string, value: unknown): string => {
    const option = filterOptions.find((opt) => opt.key === key);
    if (!option) return `${key}: ${value}`;

    if (option.type === "select" && option.options) {
      const selectedOption = option.options.find((opt) => opt.value === value);
      return `${option.label}: ${selectedOption?.label || value}`;
    }

    return `${option.label}: ${value}`;
  };

  const activeFilters = Object.entries(filters).filter(
    ([, value]) => value !== "" && value !== null && value !== undefined,
  );

  return (
    <div className="space-y-4 mb-6">
      {/* Search Bar and Filter Toggle */}
      <div className="flex gap-2 items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
          <Input
            placeholder={t("searchAndFilter.searchPlaceholder")}
            value={searchQuery}
            onChange={(e) => handleSearch(e.target.value)}
            className="pl-10 h-10"
          />
        </div>
        {filterOptions.length > 0 && (
          <Button
            variant="outline"
            onClick={() => setShowFilters(!showFilters)}
            className="flex items-center gap-2 h-10"
          >
            <Filter className="h-4 w-4" />
            {t("searchAndFilter.filtersButton")}
          </Button>
        )}
      </div>

      {showFilters && filterOptions.length > 0 && (
        <Card>
          <CardContent className="pt-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filterOptions.map((option) => (
                <div key={option.key} className="space-y-2">
                  <label className="text-sm font-medium">{option.label}</label>
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
                          placeholder={t("searchAndFilter.selectPlaceholder", {
                            label: option.label,
                          })}
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
          </CardContent>
        </Card>
      )}

      {/* Active Filters Display */}
      {activeFilters.length > 0 && (
        <div className="flex flex-wrap gap-2 items-center">
          <span className="text-sm text-muted-foreground">
            {t("searchAndFilter.activeFiltersLabel")}
          </span>
          {activeFilters.map(([key, value]) => (
            <Badge
              key={key}
              variant="outline"
              className="flex items-center gap-1"
            >
              {getFilterDisplayName(key, value)}
              <Button
                variant="ghost"
                size="sm"
                className="h-auto p-0 ml-1 hover:bg-transparent"
                onClick={() => clearFilter(key)}
              >
                <X className="h-3 w-3" />
              </Button>
            </Badge>
          ))}
          <Button
            variant="ghost"
            size="sm"
            onClick={clearAllFilters}
            className="text-muted-foreground"
          >
            {t("searchAndFilter.clearAllButton")}
          </Button>
        </div>
      )}
    </div>
  );
}
