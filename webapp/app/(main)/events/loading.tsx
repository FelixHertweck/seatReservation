import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <div className="container mx-auto p-6 animate-in fade-in duration-500">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {Array.from({ length: 6 }).map((_, index) => (
          <div
            key={index}
            className="border rounded-lg p-6 space-y-4 animate-in slide-in-from-bottom duration-500 hover:shadow-lg transition-all"
            style={{
              animationDelay: `${index * 100}ms`,
            }}
          >
            <div className="flex items-start justify-between">
              <Skeleton className="h-6 w-48 animate-pulse" />
              <Skeleton className="h-5 w-16 animate-pulse" />
            </div>
            <Skeleton className="h-4 w-full animate-pulse" />
            <Skeleton className="h-4 w-3/4 animate-pulse" />

            <div className="space-y-3">
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2 animate-pulse" />
                <Skeleton className="h-4 w-24 animate-pulse" />
              </div>
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2 animate-pulse" />
                <Skeleton className="h-4 w-32 animate-pulse" />
              </div>
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2 animate-pulse" />
                <Skeleton className="h-4 w-28 animate-pulse" />
              </div>
              <div className="flex items-center">
                <Skeleton className="h-4 w-4 mr-2 animate-pulse" />
                <Skeleton className="h-4 w-20 animate-pulse" />
              </div>
            </div>

            <Skeleton className="h-10 w-full animate-pulse" />
          </div>
        ))}
      </div>
    </div>
  );
}
