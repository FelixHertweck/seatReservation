import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

/** Error message shown below an invalid form field. */
export function FieldError({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <p className={cn("text-sm text-destructive", className)}>{children}</p>
  );
}

/** className for an Input/Textarea/etc. marked invalid via `aria-invalid`. */
export function invalidFieldClassName(isInvalid: boolean, className?: string) {
  return cn(isInvalid && "focus-visible:ring-destructive", className);
}
