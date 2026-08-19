"use client";

import type { ReactNode } from "react";
import { MoreVertical } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Button, type buttonVariants } from "@/components/custom-ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import type { VariantProps } from "class-variance-authority";

export type OverflowAction = {
  key: string;
  label: ReactNode;
  icon: ReactNode;
  onClick: () => void;
  variant?: VariantProps<typeof buttonVariants>["variant"];
  isLoading?: boolean;
  disabled?: boolean;
};

// Secondary page actions (e.g. export, block, delete-selected) rendered
// inline on sm+ screens; collapsed behind a "more actions" kebab menu on
// mobile so the header row never has to wrap across multiple lines.
export function OverflowActionBar({ actions }: { actions: OverflowAction[] }) {
  const t = useT();

  if (actions.length === 0) return null;

  return (
    <>
      <div className="hidden items-center gap-2 sm:flex">
        {actions.map((action) => (
          <Button
            key={action.key}
            variant={action.variant ?? "outline"}
            onClick={action.onClick}
            isLoading={action.isLoading}
            disabled={action.disabled}
          >
            {action.icon}
            {action.label}
          </Button>
        ))}
      </div>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="outline"
            size="icon"
            className="sm:hidden"
            aria-label={t("common.moreActions")}
          >
            <MoreVertical className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {actions.map((action) => (
            <DropdownMenuItem
              key={action.key}
              onClick={action.onClick}
              disabled={action.isLoading || action.disabled}
              className={
                action.variant === "destructive"
                  ? "text-destructive focus:text-destructive"
                  : undefined
              }
            >
              {action.icon}
              {action.label}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    </>
  );
}
