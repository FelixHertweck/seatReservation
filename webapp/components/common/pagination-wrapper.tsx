"use client";

import type React from "react";

import { useState, useMemo } from "react";
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useT } from "@/lib/i18n/hooks";

interface PaginationWrapperProps<T> {
  data: T[];
  itemsPerPage?: number;
  paginationLabel?: string;
  children: (
    paginatedData: T[],
    currentPage: number,
    totalPages: number,
  ) => React.ReactNode;
}

export function PaginationWrapper<T>({
  data,
  itemsPerPage = 100,
  paginationLabel = "entries",
  children,
}: PaginationWrapperProps<T>) {
  const [selectedPage, setSelectedPage] = useState(1);

  const t = useT();

  const currentPage = useMemo(() => {
    const maxPage = Math.ceil(data.length / itemsPerPage);
    return Math.min(selectedPage, Math.max(1, maxPage));
  }, [selectedPage, data.length, itemsPerPage]);

  const { paginatedData, totalPages, startIndex, endIndex } = useMemo(() => {
    const totalPages = Math.ceil(data.length / itemsPerPage);
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, data.length);
    const paginatedData = data.slice(startIndex, endIndex);

    return { paginatedData, totalPages, startIndex, endIndex };
  }, [data, currentPage, itemsPerPage]);

  // Dropdown-based page selector to avoid horizontal overflow on mobile
  const pageSelector = (
    <Select
      value={String(currentPage)}
      onValueChange={(val) => setSelectedPage(Number(val))}
    >
      <SelectTrigger className="w-24 sm:w-28" aria-label="Select page">
        <SelectValue placeholder={`${currentPage} / ${totalPages}`} />
      </SelectTrigger>
      <SelectContent className="max-h-60">
        {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
          <SelectItem key={p} value={String(p)}>
            {p}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );

  return (
    <div className="space-y-4">
      {children(paginatedData, currentPage, totalPages)}

      {totalPages > 1 && (
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div className="text-sm text-muted-foreground text-center sm:text-left whitespace-nowrap">
            {t("pagination-wrapper.label", {
              fromCount: startIndex + 1,
              toCount: endIndex,
              totalCount: data.length,
            })}{" "}
            {" " + paginationLabel}
          </div>
          <Pagination className="justify-center sm:justify-end">
            <PaginationContent>
              <PaginationItem>
                <PaginationPrevious
                  onClick={() => setSelectedPage(Math.max(1, currentPage - 1))}
                  className={
                    currentPage === 1
                      ? "pointer-events-none opacity-50"
                      : "cursor-pointer"
                  }
                />
              </PaginationItem>

              <PaginationItem className="mx-1">{pageSelector}</PaginationItem>

              <PaginationItem>
                <PaginationNext
                  onClick={() =>
                    setSelectedPage(Math.min(totalPages, currentPage + 1))
                  }
                  className={
                    currentPage === totalPages
                      ? "pointer-events-none opacity-50"
                      : "cursor-pointer"
                  }
                />
              </PaginationItem>
            </PaginationContent>
          </Pagination>
        </div>
      )}
    </div>
  );
}
