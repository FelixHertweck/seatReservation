"use client";

// Drop-in replacement for `@/components/ui/alert-dialog` that renders
// full-screen on mobile and as a centered modal from the `sm` breakpoint up.
// See `dialog.tsx` for the rationale.

import * as React from "react";
import { AlertDialogContent as BaseAlertDialogContent } from "@/components/ui/alert-dialog";
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
      "inset-0 top-0 left-0 flex h-full w-full max-w-none translate-x-0 translate-y-0 flex-col justify-center overflow-y-auto rounded-none border-0 p-4",
      "sm:inset-auto sm:top-[50%] sm:left-[50%] sm:h-auto sm:max-h-[85vh] sm:w-full sm:max-w-lg sm:-translate-x-1/2 sm:-translate-y-1/2 sm:block sm:overflow-visible sm:rounded-lg sm:border sm:p-6",
      className,
    )}
    {...props}
  />
));
AlertDialogContent.displayName = "ResponsiveAlertDialogContent";

export {
  AlertDialog,
  AlertDialogPortal,
  AlertDialogOverlay,
  AlertDialogTrigger,
  AlertDialogHeader,
  AlertDialogFooter,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogAction,
  AlertDialogCancel,
} from "@/components/ui/alert-dialog";
export { AlertDialogContent };
