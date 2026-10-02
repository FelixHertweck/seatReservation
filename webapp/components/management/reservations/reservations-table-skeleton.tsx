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

const NAME_WIDTHS = ["w-28", "w-20", "w-32", "w-24", "w-28", "w-20"];

/**
 * Mirrors the table's default state - user groups collapsed - and, unlike
 * the real table, has no min width so it never gets cut off on phones.
 */
export function ReservationsTableSkeleton({
  rowCount = 6,
}: Readonly<ReservationsTableSkeletonProps>) {
  const rowIds = Array.from({ length: rowCount }, (_, i) => i);
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-10 pl-3 pr-1">
            <Skeleton className="h-4 w-4 rounded" />
          </TableHead>
          <TableHead>
            <Skeleton className="h-4 w-12 rounded" />
          </TableHead>
          <TableHead className="w-28">
            <Skeleton className="h-4 w-14 rounded" />
          </TableHead>
          <TableHead className="w-36">
            <Skeleton className="h-4 w-16 rounded" />
          </TableHead>
          <TableHead className="w-20 py-2 pl-2 pr-4" />
        </TableRow>
      </TableHeader>
      <TableBody>
        {rowIds.map((i) => (
          <TableRow key={i} className="bg-muted/40 hover:bg-muted/40">
            <TableCell colSpan={4} className="py-2.5 pl-3 pr-2">
              <div className="flex items-center gap-2">
                <Skeleton className="h-6 w-6 shrink-0 rounded" />
                <Skeleton className="h-4 w-4 shrink-0 rounded" />
                <Skeleton
                  className={`h-4 ${NAME_WIDTHS[i % NAME_WIDTHS.length]} rounded`}
                />
                <Skeleton className="h-4 w-6 shrink-0 rounded-full" />
              </div>
            </TableCell>
            <TableCell className="py-2.5 pl-2 pr-4">
              <div className="flex justify-end gap-1.5">
                <Skeleton className="h-8 w-9 rounded-md" />
                <Skeleton className="h-8 w-9 rounded-md" />
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
