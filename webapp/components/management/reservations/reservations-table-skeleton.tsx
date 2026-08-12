import { Skeleton } from "@/components/custom-ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

interface ReservationsTableSkeletonProps {
  rowCount?: number;
}

export function ReservationsTableSkeleton({
  rowCount = 6,
}: ReservationsTableSkeletonProps) {
  return (
    <div className="w-full overflow-hidden rounded-lg border border-border/40">
      <Table>
        <TableHeader>
          <TableRow className="bg-muted/40">
            <TableHead className="w-10 pl-4 pr-2">
              <Skeleton className="h-4 w-4 rounded" />
            </TableHead>
            <TableHead className="w-[28%]">
              <Skeleton className="h-4 w-20 rounded" />
            </TableHead>
            <TableHead className="w-[20%]">
              <Skeleton className="h-4 w-16 rounded" />
            </TableHead>
            <TableHead className="w-[18%]">
              <Skeleton className="h-4 w-16 rounded" />
            </TableHead>
            <TableHead className="w-[24%]">
              <Skeleton className="h-4 w-24 rounded" />
            </TableHead>
            <TableHead className="w-20 py-2 pl-2 pr-4 text-right" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {Array.from({ length: rowCount }).map((_, index) => (
            <TableRow key={index} className="hover:bg-transparent">
              <TableCell className="pl-4 pr-2 py-3">
                <Skeleton className="h-4 w-4 rounded" />
              </TableCell>
              <TableCell className="py-3">
                <div className="flex items-center gap-2">
                  <Skeleton className="h-7 w-7 rounded-full shrink-0" />
                  <Skeleton className="h-4 w-28 rounded" />
                </div>
              </TableCell>
              <TableCell className="py-3">
                <Skeleton className="h-6 w-16 rounded-full" />
              </TableCell>
              <TableCell className="py-3">
                <Skeleton className="h-6 w-20 rounded-full" />
              </TableCell>
              <TableCell className="py-3">
                <Skeleton className="h-4 w-28 rounded" />
              </TableCell>
              <TableCell className="py-3 pl-2 pr-4 text-right">
                <div className="flex justify-end gap-1">
                  <Skeleton className="h-7 w-7 rounded-md" />
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
