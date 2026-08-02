"use client";

// Drop-in replacement for `@/components/ui/dialog` that renders full-screen
// on mobile and as a centered modal from the `sm` breakpoint up. Import this
// instead of the ui/dialog primitives for any new dialog in the app, and use
// `sm:`-prefixed sizing overrides (sm:max-w-*, sm:max-h-*) so the mobile
// full-screen layout isn't clipped.

import * as React from "react";
import {
  DialogContent as BaseDialogContent,
  DialogFooter as BaseDialogFooter,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

type DialogContentProps = React.ComponentPropsWithoutRef<
  typeof BaseDialogContent
>;

const DialogContent = React.forwardRef<
  React.ComponentRef<typeof BaseDialogContent>,
  DialogContentProps
>(({ className, ...props }, ref) => (
  <BaseDialogContent
    ref={ref}
    className={cn(
      "inset-0 top-0 left-0 flex h-full w-full max-w-none flex-col translate-x-0 translate-y-0 overflow-y-auto rounded-none border-0 p-4",
      "sm:inset-auto sm:top-[50%] sm:left-[50%] sm:grid sm:h-auto sm:max-h-[85vh] sm:w-full sm:max-w-xl sm:-translate-x-1/2 sm:-translate-y-1/2 sm:overflow-visible sm:rounded-lg sm:border sm:p-6",
      className,
    )}
    {...props}
  />
));
DialogContent.displayName = "ResponsiveDialogContent";

function DialogFooter({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <BaseDialogFooter
      className={cn("flex-col-reverse gap-2 sm:flex-row", className)}
      {...props}
    />
  );
}

export {
  Dialog,
  DialogPortal,
  DialogOverlay,
  DialogClose,
  DialogTrigger,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
export { DialogContent, DialogFooter };
