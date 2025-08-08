"use client";

import { useState, useEffect } from "react";
import { Search, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

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
}

export function SearchAndFilter({
  onSearch,
  onFilter,
  filterOptions,
  initialFilters = {},
}: SearchAndFilterProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [filters, setFilters] =
    useState<Record<string, unknown>>(initialFilters);

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    onSearch(query);
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
      {/* Active Filters Display */}
      {activeFilters.length > 0 && (
        <div className="flex flex-wrap gap-2 items-center">
          <span className="text-sm text-muted-foreground">Active filters:</span>
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
            Clear all
          </Button>
        </div>
      )}

      {/* Search Bar */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
        <Input
          placeholder="Search..."
          value={searchQuery}
          onChange={(e) => handleSearch(e.target.value)}
          className="pl-10"
        />
      </div>
    </div>
  );
}
