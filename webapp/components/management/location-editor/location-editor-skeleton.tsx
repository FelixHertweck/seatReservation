import { Skeleton } from "@/components/custom-ui/skeleton";

export function LocationEditorSkeleton() {
  return (
    <div className="flex h-full w-full flex-col overflow-hidden rounded-lg border border-border/50 bg-background shadow-xs">
      {/* Editor Toolbar Skeleton */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border/40 bg-card/60 p-2.5 backdrop-blur-xs">
        {/* View Mode Tabs */}
        <div className="flex items-center gap-1.5 rounded-lg border border-border/40 bg-muted/40 p-1">
          <Skeleton className="h-7 w-16 rounded-md" />
          <Skeleton className="h-7 w-20 rounded-md" />
          <Skeleton className="h-7 w-14 rounded-md" />
        </div>

        {/* Action / Tool Buttons */}
        <div className="flex items-center gap-2">
          <Skeleton className="h-8 w-24 rounded-md" />
          <Skeleton className="h-8 w-8 rounded-md" />
          <Skeleton className="h-8 w-8 rounded-md" />
          <div className="h-5 w-px bg-border/60" />
          <Skeleton className="h-8 w-28 rounded-md" />
        </div>
      </div>

      {/* Main Grid Layout (Canvas + Side Panel) */}
      <div className="grid min-h-0 flex-1 grid-cols-1 gap-3 overflow-hidden p-3 lg:grid-cols-[minmax(0,1fr)_22rem]">
        {/* Canvas Area Skeleton */}
        <div className="relative flex min-h-[350px] w-full flex-col items-center justify-center overflow-hidden rounded-xl border border-border/40 bg-seatmap/30 p-6">
          {/* Subtle grid pattern background */}
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_1px_1px,var(--color-border)_1px,transparent_0)] bg-[size:24px_24px] opacity-20 pointer-events-none" />

          {/* Ambient center blur */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-primary/5 rounded-full blur-3xl pointer-events-none" />

          {/* Mock Canvas Content */}
          <div className="relative z-10 flex flex-col items-center justify-center gap-6 w-full max-w-lg">
            {/* Stage Indicator */}
            <div className="w-2/3 h-3 rounded-b-lg bg-muted/70 overflow-hidden">
              <Skeleton className="w-full h-full opacity-50" />
            </div>

            {/* Canvas seat grid wires */}
            <div className="w-full border-2 border-dashed border-primary/20 rounded-2xl p-6 bg-background/30 backdrop-blur-xs">
              <div className="grid grid-cols-6 gap-4 justify-items-center">
                {Array.from({ length: 24 }).map((_, i) => (
                  <Skeleton key={i} className="h-7 w-7 rounded-md" />
                ))}
              </div>
            </div>
          </div>

          {/* Mock Canvas Controls */}
          <div className="absolute bottom-3 right-3 flex flex-col gap-1.5 p-1 rounded-lg border border-border/40 bg-background/80 shadow-xs">
            <Skeleton className="h-7 w-7 rounded-md" />
            <Skeleton className="h-7 w-7 rounded-md" />
            <Skeleton className="h-7 w-7 rounded-md" />
          </div>
        </div>

        {/* Side Panel Properties Skeleton */}
        <div className="flex flex-col gap-4 rounded-xl border border-border/40 bg-card/60 p-4 shadow-xs">
          <div className="flex items-center justify-between border-b border-border/40 pb-3">
            <Skeleton className="h-5 w-32 rounded-md" />
            <Skeleton className="h-6 w-16 rounded-full" />
          </div>

          {/* Form field shimmers */}
          <div className="space-y-4 flex-1">
            <div className="space-y-2">
              <Skeleton className="h-4 w-24 rounded-md" />
              <Skeleton className="h-9 w-full rounded-md" />
            </div>
            <div className="space-y-2">
              <Skeleton className="h-4 w-20 rounded-md" />
              <Skeleton className="h-9 w-full rounded-md" />
            </div>
            <div className="space-y-2">
              <Skeleton className="h-4 w-28 rounded-md" />
              <Skeleton className="h-20 w-full rounded-md" />
            </div>
          </div>

          {/* Bottom actions in side panel */}
          <div className="pt-3 border-t border-border/40 flex justify-end gap-2">
            <Skeleton className="h-9 w-20 rounded-md" />
            <Skeleton className="h-9 w-24 rounded-md" />
          </div>
        </div>
      </div>
    </div>
  );
}
