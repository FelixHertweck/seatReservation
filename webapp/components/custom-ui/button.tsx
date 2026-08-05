"use client";

// Drop-in replacement for `@/components/ui/button` that adds an `isLoading`
// prop: while true, the button's content is hidden (but keeps its layout
// space, so the button doesn't change width) behind a centered spinner, and
// the button is disabled. Import this instead of the ui/button primitive for
// any button that triggers an async action (form submit, delete, export, …).
// Ignored when `asChild` is set, since then the button merges its props onto
// a single child (e.g. a `Link`) instead of rendering its own content.

import * as React from "react";
import { Loader2Icon } from "@/components/icons";
import { Button as BaseButton, buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type BaseButtonProps = React.ComponentProps<typeof BaseButton>;

function Button({
  className,
  isLoading = false,
  disabled,
  asChild = false,
  children,
  ...props
}: BaseButtonProps & { isLoading?: boolean }) {
  if (asChild || !isLoading) {
    return (
      <BaseButton
        asChild={asChild}
        className={className}
        disabled={disabled}
        {...props}
      >
        {children}
      </BaseButton>
    );
  }

  return (
    <BaseButton
      className={cn("relative", className)}
      disabled
      aria-busy="true"
      {...props}
    >
      <Loader2Icon
        className="absolute inset-0 m-auto animate-spin"
        aria-hidden="true"
      />
      <span className="contents invisible">{children}</span>
    </BaseButton>
  );
}

export { Button, buttonVariants };
