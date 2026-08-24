"use client";

// Drop-in replacement for `@/components/ui/alert-dialog` that renders
// full-screen on mobile and as a centered modal from the `sm` breakpoint up.
// See `dialog.tsx` for the rationale.

import * as React from "react";
import {
  AlertDialogContent as BaseAlertDialogContent,
  AlertDialogFooter as BaseAlertDialogFooter,
} from "@/components/ui/alert-dialog";
import { cn } from "@/lib/utils";

type AlertDialogContentProps = React.ComponentPropsWithoutRef<
  typeof BaseAlertDialogContent
>;

const AlertDialogContent = React.forwardRef<
  React.ComponentRef<typeof BaseAlertDialogContent>,
  AlertDialogContentProps
>(({ className, ...props }, ref) => (
  <BaseAlertDialogContent
    ref={ref}
    className={cn(
      "inset-0 top-0 left-0 flex h-full w-full max-w-none translate-x-0 translate-y-0 flex-col justify-center gap-6 overflow-y-auto rounded-none border-0 p-4",
      "sm:inset-auto sm:top-[50%] sm:left-[50%] sm:grid sm:gap-6 sm:h-auto sm:max-h-[85vh] sm:w-full sm:max-w-xl sm:-translate-x-1/2 sm:-translate-y-1/2 sm:overflow-visible sm:rounded-lg sm:border sm:p-6",
      className,
    )}
    {...props}
  />
));
AlertDialogContent.displayName = "ResponsiveAlertDialogContent";

function AlertDialogFooter({
  className,
  ...props
}: Readonly<React.HTMLAttributes<HTMLDivElement>>) {
  return (
    <BaseAlertDialogFooter
      className={cn(
        "flex-col-reverse gap-3 sm:flex-row sm:justify-end sm:gap-2",
        className,
      )}
      {...props}
    />
  );
}

export {
  AlertDialog,
  AlertDialogPortal,
  AlertDialogOverlay,
  AlertDialogTrigger,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogAction,
  AlertDialogCancel,
} from "@/components/ui/alert-dialog";
export { AlertDialogContent, AlertDialogFooter };
