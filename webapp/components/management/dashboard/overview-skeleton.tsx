import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/custom-ui/skeleton";

export function OverviewSkeleton() {
  return (
    <div className="space-y-6">
      {/* 4 Stat Cards Skeleton */}
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Card
            key={i}
            className="overflow-hidden border border-border/40 bg-card/60"
          >
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <Skeleton className="h-4 w-24 rounded-md" />
              <Skeleton className="h-5 w-5 rounded-md" />
            </CardHeader>
            <CardContent className="space-y-2">
              <Skeleton className="h-8 w-16 rounded-md" />
              <Skeleton className="h-3 w-32 rounded-md" />
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Main Panels Row 1 */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* Upcoming Events Panel Skeleton */}
        <Card className="lg:col-span-2 border border-border/40 bg-card/60">
          <CardHeader className="flex flex-row items-center justify-between pb-3">
            <Skeleton className="h-5 w-44 rounded-md" />
            <Skeleton className="h-4 w-20 rounded-md" />
          </CardHeader>
          <CardContent className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div
                key={i}
                className="flex items-center justify-between gap-3 p-2 rounded-md border border-border/30 bg-muted/20"
              >
                <div className="flex items-center gap-3">
                  <Skeleton className="h-8 w-8 rounded-full shrink-0" />
                  <div className="space-y-1">
                    <Skeleton className="h-4 w-36 rounded-md" />
                    <Skeleton className="h-3 w-24 rounded-md" />
                  </div>
                </div>
                <Skeleton className="h-5 w-20 rounded-full" />
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Quick Actions Panel Skeleton */}
        <Card className="border border-border/40 bg-card/60">
          <CardHeader>
            <Skeleton className="h-5 w-32 rounded-md" />
          </CardHeader>
          <CardContent className="space-y-2">
            <Skeleton className="h-10 w-full rounded-lg" />
            <Skeleton className="h-10 w-full rounded-lg" />
            <Skeleton className="h-10 w-full rounded-lg" />
          </CardContent>
        </Card>
      </div>

      {/* Main Panels Row 2 */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* Deadlines Warning Panel Skeleton */}
        <Card className="border border-border/40 bg-card/60">
          <CardHeader>
            <Skeleton className="h-5 w-36 rounded-md" />
          </CardHeader>
          <CardContent className="space-y-3">
            {Array.from({ length: 2 }).map((_, i) => (
              <div
                key={i}
                className="p-3 rounded-md border border-border/30 space-y-2"
              >
                <Skeleton className="h-4 w-32 rounded-md" />
                <Skeleton className="h-3 w-28 rounded-md" />
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Large Stat Card Skeleton */}
        <Card className="lg:col-span-2 border border-border/40 bg-card/60">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <Skeleton className="h-5 w-40 rounded-md" />
            <Skeleton className="h-5 w-5 rounded-md" />
          </CardHeader>
          <CardContent className="space-y-2">
            <Skeleton className="h-8 w-20 rounded-md" />
            <Skeleton className="h-3 w-48 rounded-md" />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
