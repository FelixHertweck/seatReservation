import Link from "next/link";
import type { LucideIcon } from "lucide-react";

import { cn, formatCompactNumber } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";

interface StatCardProps {
  label: string;
  value: number;
  suffix?: string;
  subLabel?: string;
  icon: LucideIcon;
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
  const content = (
    <CardContent className="flex items-start justify-between gap-3 py-2">
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
      <Icon className="h-5 w-5 shrink-0 text-muted-foreground" />
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
        <Link href={href}>{content}</Link>
      </Card>
    );
  }

  return <Card className={className}>{content}</Card>;
}
