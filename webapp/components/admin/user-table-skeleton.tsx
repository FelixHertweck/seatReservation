import { Skeleton } from "@/components/ui/skeleton";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Plus, FileText } from "lucide-react";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { useT } from "@/lib/i18n/hooks";

interface UserTableSkeletonProps {
  showImportButton?: boolean;
}

export function UserTableSkeleton({
  showImportButton = false,
}: UserTableSkeletonProps) {
  const t = useT();

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>{t("userManagement.title")}</CardTitle>
            <CardDescription>{t("userManagement.description")}</CardDescription>
          </div>
          <div className="flex gap-2">
            {showImportButton && (
              <Button variant="outline" disabled>
                <FileText className="mr-2 h-4 w-4" />
                {t("userManagement.importJsonButton")}
              </Button>
            )}
            <Button disabled>
              <Plus className="mr-2 h-4 w-4" />
              {t("userManagement.addUserButton")}
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        <SearchAndFilter
          onSearch={() => {}}
          onFilter={() => {}}
          filterOptions={[]}
        />

        <div className="border rounded-lg mt-6">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("userManagement.tableHeaderUsername")}</TableHead>
                <TableHead>{t("userManagement.tableHeaderName")}</TableHead>
                <TableHead>{t("userManagement.tableHeaderEmail")}</TableHead>
                <TableHead>{t("userManagement.tableHeaderRoles")}</TableHead>
                <TableHead>{t("userManagement.tableHeaderTags")}</TableHead>
                <TableHead>{t("userManagement.tableHeaderVerified")}</TableHead>
                <TableHead>{t("userManagement.tableHeaderActions")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {Array.from({ length: 8 }).map((_, index) => (
                <TableRow key={index}>
                  <TableCell>
                    <Skeleton className="h-4 w-24" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-4 w-32" />
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-4 w-40" />
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-1">
                      <Skeleton className="h-5 w-12" />
                      <Skeleton className="h-5 w-16" />
                    </div>
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-1">
                      <Skeleton className="h-5 w-16" />
                      <Skeleton className="h-5 w-12" />
                    </div>
                  </TableCell>
                  <TableCell>
                    <Skeleton className="h-5 w-20" />
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-2">
                      <Skeleton className="h-8 w-8" />
                      <Skeleton className="h-8 w-8" />
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {/* Pagination Skeleton */}
        <div className="flex items-center justify-between mt-4">
          <Skeleton className="h-4 w-32" />
          <div className="flex gap-2">
            <Skeleton className="h-8 w-8" />
            <Skeleton className="h-8 w-8" />
            <Skeleton className="h-8 w-8" />
            <Skeleton className="h-8 w-8" />
            <Skeleton className="h-8 w-8" />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export function UserExportSkeleton() {
  const t = useT();

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("userExport.exportDataTitle")}</CardTitle>
        <CardDescription>
          {t("userExport.exportDataDescription")}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <Skeleton className="h-10 w-40" />
          <Skeleton className="h-4 w-48" />
        </div>
      </CardContent>
    </Card>
  );
}
