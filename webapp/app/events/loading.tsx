import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <Skeleton className="h-8 w-32 mb-2" />
        <Skeleton className="h-4 w-96" />
      </div>

      {/* Search and Filter Skeleton */}
      <div className="space-y-4 mb-6">
        <div className="flex gap-4">
          <Skeleton className="h-10 flex-1" />
          <Skeleton className="h-10 w-20" />
        </div>
      </div>

      {/* Event Cards Grid Skeleton */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {Array.from({ length: 6 }).map((_, index) => (
          <div key={index} className="border rounded-lg p-6 space-y-4">
            <div className="flex items-start justify-between">
              <Skeleton className="h-6 w-48" />
              <Skeleton className="h-5 w-16" />
            </div>
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-3/4" />

            <div className="space-y-3">
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2" />
                <Skeleton className="h-4 w-24" />
              </div>
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2" />
                <Skeleton className="h-4 w-32" />
              </div>
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2" />
                <Skeleton className="h-4 w-28" />
              </div>
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2" />
                <Skeleton className="h-4 w-20" />
              </div>
            </div>

            <Skeleton className="h-10 w-full" />
          </div>
        ))}
      </div>
    </div>
  );
}
