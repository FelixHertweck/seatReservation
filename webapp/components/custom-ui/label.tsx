"use client";

// Drop-in replacement for `@/components/ui/label` with `leading-tight`
// instead of `leading-none`, so labels that wrap to multiple lines (long
// checkbox/radio descriptions, form fields) don't look cramped.

import * as React from "react";
import { Label as BaseLabel } from "@/components/ui/label";
import { cn } from "@/lib/utils";

const Label = React.forwardRef<
  React.ComponentRef<typeof BaseLabel>,
  React.ComponentPropsWithoutRef<typeof BaseLabel>
>(({ className, ...props }, ref) => (
  <BaseLabel ref={ref} className={cn("leading-tight", className)} {...props} />
));
Label.displayName = "Label";

export { Label };
