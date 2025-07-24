import { Skeleton } from "@/components/ui/skeleton";
import { LoadingSpinner } from "@/components/ui/loading-spinner";

export default function Loading() {
  return (
    <div className="container mx-auto p-6 animate-in fade-in duration-500">
      <div className="flex items-center gap-3 mb-6">
        <LoadingSpinner size="lg" />
        <div>
          <Skeleton className="h-8 w-40 mb-2 animate-pulse" />
          <Skeleton className="h-4 w-72 animate-pulse" />
        </div>
      </div>

      <div className="space-y-4">
        <div className="grid w-full grid-cols-5 gap-1 bg-muted p-1 rounded-lg animate-in slide-in-from-top duration-300">
          <Skeleton className="h-8 w-full animate-pulse" />
          <Skeleton className="h-8 w-full animate-pulse" />
          <Skeleton className="h-8 w-full animate-pulse" />
          <Skeleton className="h-8 w-full animate-pulse" />
          <Skeleton className="h-8 w-full animate-pulse" />
        </div>

        <div className="border rounded-lg animate-in slide-in-from-bottom duration-500">
          <div className="p-6 border-b">
            <div className="flex items-center justify-between">
              <div>
                <Skeleton className="h-6 w-32 mb-1 animate-pulse" />
                <Skeleton className="h-4 w-48 animate-pulse" />
              </div>
              <Skeleton className="h-10 w-24 animate-pulse" />
            </div>
          </div>

          <div className="p-6">
            <div className="flex gap-4 mb-6">
              <Skeleton className="h-10 flex-1 animate-pulse" />
            </div>

            <div className="space-y-4">
              <div className="flex gap-4 border-b pb-2">
                <Skeleton className="h-4 w-16 animate-pulse" />
                <Skeleton className="h-4 w-24 animate-pulse" />
                <Skeleton className="h-4 w-20 animate-pulse" />
                <Skeleton className="h-4 w-20 animate-pulse" />
                <Skeleton className="h-4 w-16 animate-pulse" />
                <Skeleton className="h-4 w-16 animate-pulse" />
              </div>

              {Array.from({ length: 4 }).map((_, index) => (
                <div
                  key={index}
                  className="flex gap-4 items-center py-2 animate-in slide-in-from-left duration-300"
                  style={{
                    animationDelay: `${index * 100}ms`,
                  }}
                >
                  <Skeleton className="h-4 w-24 animate-pulse" />
                  <Skeleton className="h-4 w-32 animate-pulse" />
                  <Skeleton className="h-4 w-28 animate-pulse" />
                  <Skeleton className="h-4 w-28 animate-pulse" />
                  <Skeleton className="h-4 w-20 animate-pulse" />
                  <div className="flex gap-2">
                    <Skeleton className="h-8 w-8 animate-pulse" />
                    <Skeleton className="h-8 w-8 animate-pulse" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
