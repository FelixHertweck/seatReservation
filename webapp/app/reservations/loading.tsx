import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <Skeleton className="h-8 w-40 mb-2" />
        <Skeleton className="h-4 w-80" />
      </div>

      {/* Search and Filter Skeleton */}
      <div className="space-y-4 mb-6">
        <div className="flex gap-4">
          <Skeleton className="h-10 flex-1" />
        </div>
      </div>

      {/* Reservation Cards Grid Skeleton */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="border rounded-lg p-6 space-y-4">
            <div className="flex items-start justify-between">
              <Skeleton className="h-6 w-36" />
              <Skeleton className="h-5 w-12" />
            </div>
            <Skeleton className="h-4 w-32" />

            <div className="space-y-3">
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2" />
                <Skeleton className="h-4 w-28" />
              </div>
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2" />
                <Skeleton className="h-4 w-24" />
              </div>
            </div>

            <div className="flex gap-2">
              <Skeleton className="h-8 w-24" />
              <Skeleton className="h-8 w-20" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
