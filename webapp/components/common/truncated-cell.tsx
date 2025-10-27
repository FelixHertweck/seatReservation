"use client";

import React, { useRef, useState, useEffect } from "react";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { TableCell } from "@/components/ui/table";

interface TruncatedCellProps {
  content: string | number | null | undefined;
  className?: string;
}

export function TruncatedCell({ content, className = "" }: TruncatedCellProps) {
  const cellRef = useRef<HTMLDivElement>(null);
  const [isTruncated, setIsTruncated] = useState(false);

  useEffect(() => {
    const element = cellRef.current;
    if (element) {
      // Check if content is truncated
      setIsTruncated(element.scrollWidth > element.clientWidth);
    }
  }, [content]);

  const displayContent = content?.toString() ?? "";

  const cellContent = (
    <div
      ref={cellRef}
      className="overflow-hidden text-ellipsis whitespace-nowrap"
    >
      {displayContent}
    </div>
  );

  if (isTruncated) {
    return (
      <TableCell className={className}>
        <TooltipProvider delayDuration={300}>
          <Tooltip>
            <TooltipTrigger asChild>
              <div className="cursor-default">{cellContent}</div>
            </TooltipTrigger>
            <TooltipContent side="top" className="max-w-sm">
              <p className="break-words whitespace-normal">{displayContent}</p>
            </TooltipContent>
          </Tooltip>
        </TooltipProvider>
      </TableCell>
    );
  }

  return <TableCell className={className}>{cellContent}</TableCell>;
}
