// Drop-in replacement for `@/components/ui/skeleton` with a shimmer sweep
// layered on top of the base pulse, instead of a plain pulsing block.

import { Skeleton as BaseSkeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

function Skeleton({
  className,
  ...props
}: React.ComponentProps<typeof BaseSkeleton>) {
  return (
    <BaseSkeleton
      className={cn(
        "relative isolate overflow-hidden",
        "before:absolute before:inset-0 before:-translate-x-full before:animate-shimmer before:bg-linear-to-r before:from-transparent before:via-foreground/10 before:to-transparent before:content-['']",
        className,
      )}
      {...props}
    />
  );
}

export { Skeleton };
