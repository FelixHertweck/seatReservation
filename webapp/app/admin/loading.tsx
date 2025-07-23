import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <Skeleton className="h-8 w-36 mb-2" />
        <Skeleton className="h-4 w-64" />
      </div>

      <div className="space-y-4">
        <Skeleton className="h-10 w-40" />

        <div className="border rounded-lg">
          <div className="p-6 border-b">
            <div className="flex items-center justify-between">
              <div>
                <Skeleton className="h-6 w-32 mb-1" />
                <Skeleton className="h-4 w-48" />
              </div>
              <Skeleton className="h-10 w-24" />
            </div>
          </div>

          <div className="p-6">
            <div className="flex gap-4 mb-6">
              <Skeleton className="h-10 flex-1" />
            </div>

            <div className="space-y-4">
              <div className="flex gap-4 border-b pb-2">
                <Skeleton className="h-4 w-20" />
                <Skeleton className="h-4 w-16" />
                <Skeleton className="h-4 w-12" />
                <Skeleton className="h-4 w-12" />
                <Skeleton className="h-4 w-16" />
                <Skeleton className="h-4 w-16" />
              </div>

              {Array.from({ length: 5 }).map((_, index) => (
                <div key={index} className="flex gap-4 items-center py-2">
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-4 w-32" />
                  <div className="flex gap-1">
                    <Skeleton className="h-5 w-12" />
                    <Skeleton className="h-5 w-16" />
                  </div>
                  <Skeleton className="h-5 w-16" />
                  <div className="flex gap-2">
                    <Skeleton className="h-8 w-8" />
                    <Skeleton className="h-8 w-8" />
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
