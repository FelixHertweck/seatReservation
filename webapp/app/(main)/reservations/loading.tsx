import { Skeleton } from "@/components/ui/skeleton";
import { LoadingSpinner } from "@/components/ui/loading-spinner";

export default function Loading() {
  return (
    <div className="container mx-auto p-6 animate-in fade-in duration-500">
      <div className="flex items-center gap-3 mb-6">
        <LoadingSpinner size="lg" />
        <div>
          <Skeleton className="h-8 w-40 mb-2 animate-pulse" />
          <Skeleton className="h-4 w-80 animate-pulse" />
        </div>
      </div>

      {/* Search and Filter Skeleton */}
      <div className="space-y-4 mb-6">
        <div className="flex gap-4">
          <Skeleton className="h-10 flex-1 animate-pulse" />
        </div>
      </div>

      {/* Reservation Cards Grid Skeleton */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {Array.from({ length: 4 }).map((_, index) => (
          <div
            key={index}
            className="border rounded-lg p-6 space-y-4 animate-in slide-in-from-bottom duration-500 hover:shadow-lg transition-all"
            style={{
              animationDelay: `${index * 150}ms`,
            }}
          >
            <div className="flex items-start justify-between">
              <Skeleton className="h-6 w-36 animate-pulse" />
              <Skeleton className="h-5 w-12 animate-pulse" />
            </div>
            <Skeleton className="h-4 w-32 animate-pulse" />

            <div className="space-y-3">
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2 animate-pulse" />
                <Skeleton className="h-4 w-28 animate-pulse" />
              </div>
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2 animate-pulse" />
                <Skeleton className="h-4 w-24 animate-pulse" />
              </div>
            </div>

            <div className="flex gap-2">
              <Skeleton className="h-8 w-24 animate-pulse" />
              <Skeleton className="h-8 w-20 animate-pulse" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
