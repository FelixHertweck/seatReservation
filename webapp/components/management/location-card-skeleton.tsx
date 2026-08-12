import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
} from "@/components/ui/card";
import { Skeleton } from "@/components/custom-ui/skeleton";

export function LocationCardSkeleton() {
  return (
    <Card className="relative flex flex-col overflow-hidden border border-border/40 bg-card/60">
      <CardHeader className="relative z-10 space-y-2">
        <div className="flex items-center gap-2">
          <Skeleton className="h-4 w-4 shrink-0 rounded-full" />
          <Skeleton className="h-5 w-40 rounded-md" />
        </div>
        <Skeleton className="h-4 w-56 rounded-md" />
      </CardHeader>
      <CardContent className="relative z-10 min-h-36 flex-1 space-y-4 pt-2">
        <div className="flex flex-wrap items-center gap-2">
          <Skeleton className="h-6 w-20 rounded-full" />
          <Skeleton className="h-6 w-24 rounded-full" />
          <Skeleton className="h-6 w-16 rounded-full" />
        </div>
        <div className="flex items-center gap-2 pt-2">
          <Skeleton className="h-3.5 w-3.5 rounded-full" />
          <Skeleton className="h-4 w-28 rounded-md" />
        </div>
      </CardContent>
      <CardFooter className="relative z-10 flex gap-2 border-t border-border/30 pt-3">
        <Skeleton className="h-9 flex-1 rounded-md" />
        <Skeleton className="h-9 w-9 rounded-md" />
      </CardFooter>
    </Card>
  );
}
