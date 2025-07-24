import { Skeleton } from "@/components/ui/skeleton";
import { LoadingSpinner } from "@/components/ui/loading-spinner";

export default function Loading() {
  return (
    <div className="container mx-auto p-6 max-w-2xl animate-in fade-in duration-500">
      <div className="flex items-center gap-3 mb-6">
        <LoadingSpinner size="lg" />
        <div>
          <Skeleton className="h-8 w-20 mb-2 animate-pulse" />
          <Skeleton className="h-4 w-80 animate-pulse" />
        </div>
      </div>

      <div className="border rounded-lg animate-in slide-in-from-bottom duration-500">
        <div className="p-6 border-b">
          <Skeleton className="h-6 w-40 mb-1 animate-pulse" />
          <Skeleton className="h-4 w-64 animate-pulse" />
        </div>

        <div className="p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Skeleton className="h-4 w-20 animate-pulse" />
              <Skeleton className="h-10 w-full animate-pulse" />
            </div>
            <div className="space-y-2">
              <Skeleton className="h-4 w-20 animate-pulse" />
              <Skeleton className="h-10 w-full animate-pulse" />
            </div>
          </div>

          <div className="space-y-2">
            <Skeleton className="h-4 w-12 animate-pulse" />
            <Skeleton className="h-10 w-full animate-pulse" />
          </div>

          <div className="space-y-2">
            <Skeleton className="h-4 w-24 animate-pulse" />
            <Skeleton className="h-10 w-full animate-pulse" />
          </div>

          <div className="space-y-2">
            <Skeleton className="h-4 w-16 animate-pulse" />
            <Skeleton className="h-10 w-full animate-pulse" />
          </div>

          <div className="space-y-2">
            <Skeleton className="h-4 w-12 animate-pulse" />
            <Skeleton className="h-10 w-full animate-pulse" />
          </div>

          <Skeleton className="h-10 w-32 animate-pulse" />
        </div>
      </div>
    </div>
  );
}
