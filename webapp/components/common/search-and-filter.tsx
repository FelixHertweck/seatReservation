"use client";

import { useState } from "react";
import { Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";

interface FilterOption {
  key: string;
  label: string;
  type: "boolean" | "string" | "number";
  options?: string[];
}

interface SearchAndFilterProps {
  onSearch: (query: string) => void;
  onFilter: (filters: Record<string, unknown>) => void;
  filterOptions: FilterOption[];
}

export function SearchAndFilter({
  onSearch,
  onFilter,
  filterOptions,
}: SearchAndFilterProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [filters, setFilters] = useState<Record<string, unknown>>({});
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    onSearch(query);
  };

  const handleFilterChange = (key: string, value: unknown) => {
    const newFilters = { ...filters, [key]: value };
    setFilters(newFilters);
    onFilter(newFilters);
  };

  return (
    <div className="space-y-4 mb-6">
      <div className="flex gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
          <Input
            placeholder="Search..."
            value={searchQuery}
            onChange={(e) => handleSearch(e.target.value)}
            className="pl-10"
          />
        </div>
        {filterOptions.length > 0 && (
          <Collapsible open={isFilterOpen} onOpenChange={setIsFilterOpen}>
            <CollapsibleTrigger asChild>
              <Button variant="outline">Filters</Button>
            </CollapsibleTrigger>
          </Collapsible>
        )}
      </div>

      {filterOptions.length > 0 && (
        <Collapsible open={isFilterOpen} onOpenChange={setIsFilterOpen}>
          <CollapsibleContent className="space-y-4 p-4 border rounded-lg bg-gray-50">
            {filterOptions.map((option) => (
              <div key={option.key} className="flex items-center space-x-2">
                {option.type === "boolean" && (
                  <>
                    <Checkbox
                      id={option.key}
                      checked={!!filters[option.key]}
                      onCheckedChange={(checked) =>
                        handleFilterChange(option.key, checked)
                      }
                    />
                    <Label htmlFor={option.key}>{option.label}</Label>
                  </>
                )}
              </div>
            ))}
          </CollapsibleContent>
        </Collapsible>
      )}
    </div>
  );
}
