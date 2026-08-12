import { cn } from "@/lib/utils";
import { Skeleton } from "@/components/custom-ui/skeleton";
import { SeatMap } from "@/components/common/seat-map";

interface SeatMapSkeletonProps {
  className?: string;
  showLegend?: boolean;
}

export function SeatMapSkeleton({
  className,
  showLegend = true,
}: SeatMapSkeletonProps) {
  return (
    <div
      className={cn(
        "flex flex-col gap-3 w-full h-full min-h-[350px]",
        className,
      )}
    >
      {showLegend && (
        <div className="flex flex-wrap items-center gap-3 border-b border-border/40 pb-2 min-h-[34px]">
          <Skeleton className="h-4 w-20 rounded-full" />
          <Skeleton className="h-4 w-24 rounded-full" />
          <Skeleton className="h-4 w-20 rounded-full" />
          <Skeleton className="h-4 w-16 rounded-full" />
          <Skeleton className="h-4 w-28 rounded-full" />
        </div>
      )}
      <div className="flex-1 min-h-0 relative flex flex-col">
        <SeatMap
          seats={[]}
          seatStatuses={[]}
          markers={[]}
          selectedSeats={[]}
          onSeatSelect={() => {}}
          isLoading={true}
        />
      </div>
    </div>
  );
}
