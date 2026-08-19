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
}: Readonly<ReservationsTableSkeletonProps>) {
  const rowIds = Array.from({ length: rowCount }, (_, i) => `row-${i}`);
  return (
    <div className="w-full overflow-hidden rounded-lg border border-border/40">
      <Table className="min-w-[500px]">
        <TableHeader>
          <TableRow className="bg-muted/40">
            <TableHead className="w-10 pl-3 pr-1">
              <Skeleton className="h-4 w-4 rounded" />
            </TableHead>
            <TableHead>
              <Skeleton className="h-4 w-20 rounded" />
            </TableHead>
            <TableHead className="w-28">
              <Skeleton className="h-4 w-16 rounded" />
            </TableHead>
            <TableHead className="w-36">
              <Skeleton className="h-4 w-24 rounded" />
            </TableHead>
            <TableHead className="w-20 py-2 pl-2 pr-4 text-right" />
          </TableRow>
        </TableHeader>
        <TableBody>
          {rowIds.map((rowId) => (
            <TableRow key={rowId} className="hover:bg-transparent">
              <TableCell className="py-2.5 pl-3 pr-1">
                <Skeleton className="h-4 w-4 rounded" />
              </TableCell>
              <TableCell className="py-2.5 px-3">
                <Skeleton className="h-4 w-24 rounded" />
              </TableCell>
              <TableCell className="py-2.5 px-3">
                <Skeleton className="h-5 w-20 rounded-full" />
              </TableCell>
              <TableCell className="py-2.5 px-3">
                <Skeleton className="h-4 w-28 rounded" />
              </TableCell>
              <TableCell className="py-2.5 pl-2 pr-4 text-right">
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
