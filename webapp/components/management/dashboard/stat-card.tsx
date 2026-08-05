import Link from "next/link";
import { useMemo, useRef, cloneElement, type ReactElement, type Ref } from "react";
import type { AppIcon, AnimatedIconHandle } from "@/lib/icon-type";

import { cn, formatCompactNumber } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";

interface StatCardProps {
  label: string;
  value: number;
  suffix?: string;
  subLabel?: string;
  icon: AppIcon;
  href?: string;
  className?: string;
}

export function StatCard({
  label,
  value,
  suffix,
  subLabel,
  icon: Icon,
  href,
  className,
}: StatCardProps) {
  // A Map (mutated via .set/.delete, never reassigned) instead of a plain
  // ref value - see custom-ui/button.tsx's useIconHover for why.
  const iconHandles = useRef<Map<0, AnimatedIconHandle>>(new Map());

  const icon = useMemo(
    () =>
      cloneElement(
        <Icon
          size={20}
          className="shrink-0 text-muted-foreground"
        /> as ReactElement<{ ref?: Ref<AnimatedIconHandle> }>,
        {
          ref: (handle: AnimatedIconHandle | null) => {
            if (handle) iconHandles.current.set(0, handle);
            else iconHandles.current.delete(0);
          },
        },
      ),
    [Icon],
  );

  const content = (
    <CardContent className="flex h-full items-start justify-between gap-3 py-2">
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm text-muted-foreground">{label}</p>
        <p className="text-2xl font-semibold tabular-nums">
          {formatCompactNumber(value)}
          {suffix}
        </p>
        {subLabel && (
          <p className="truncate text-xs text-muted-foreground">{subLabel}</p>
        )}
      </div>
      {icon}
    </CardContent>
  );

  if (href) {
    return (
      <Card
        className={cn(
          "transition-colors hover:bg-accent/40 hover:border-accent-foreground/20",
          className,
        )}
      >
        <Link
          href={href}
          onMouseEnter={() => iconHandles.current.get(0)?.startAnimation()}
          onMouseLeave={() => iconHandles.current.get(0)?.stopAnimation()}
          className="block h-full"
        >
          {content}
        </Link>
      </Card>
    );
  }

  return <Card className={className}>{content}</Card>;
}
