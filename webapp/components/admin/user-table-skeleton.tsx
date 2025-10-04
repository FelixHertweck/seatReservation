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
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-xl sm:text-2xl">
              {t("userManagement.title")}
            </CardTitle>
            <CardDescription className="text-sm">
              {t("userManagement.description")}
            </CardDescription>
          </div>
          <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
            {showImportButton && (
              <Button variant="outline" disabled className="w-full sm:w-auto">
                <FileText className="mr-2 h-4 w-4" />
                <span className="hidden sm:inline">
                  {t("userManagement.importJsonButton")}
                </span>
                <span className="sm:hidden">Import</span>
              </Button>
            )}
            <Button disabled className="w-full sm:w-auto">
              <Plus className="mr-2 h-4 w-4" />
              <span className="hidden sm:inline">
                {t("userManagement.addUserButton")}
              </span>
              <span className="sm:hidden">Hinzufügen</span>
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

        {/* Desktop Table View */}
        <div className="hidden md:block border rounded-lg mt-6">
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

        {/* Mobile Card View */}
        <div className="md:hidden space-y-4 mt-6">
          {Array.from({ length: 5 }).map((_, index) => (
            <Card key={index}>
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <Skeleton className="h-5 w-32 mb-2" />
                    <Skeleton className="h-4 w-24" />
                  </div>
                  <Skeleton className="h-5 w-16 ml-2" />
                </div>
              </CardHeader>
              <CardContent className="space-y-3">
                <div>
                  <Skeleton className="h-3 w-12 mb-1" />
                  <Skeleton className="h-4 w-48" />
                </div>
                <div>
                  <Skeleton className="h-3 w-12 mb-1" />
                  <div className="flex gap-1">
                    <Skeleton className="h-5 w-12" />
                    <Skeleton className="h-5 w-16" />
                  </div>
                </div>
                <div>
                  <Skeleton className="h-3 w-12 mb-1" />
                  <div className="flex gap-1">
                    <Skeleton className="h-5 w-14" />
                    <Skeleton className="h-5 w-12" />
                  </div>
                </div>
                <div className="flex gap-2 pt-2">
                  <Skeleton className="h-9 flex-1" />
                  <Skeleton className="h-9 flex-1" />
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Pagination Skeleton */}
        <div className="flex flex-col sm:flex-row items-center justify-between mt-4 gap-2">
          <Skeleton className="h-4 w-32" />
          <div className="flex gap-2">
            <Skeleton className="h-8 w-8" />
            <Skeleton className="h-8 w-8" />
            <Skeleton className="h-8 w-8" />
            <Skeleton className="h-8 w-8 hidden sm:block" />
            <Skeleton className="h-8 w-8 hidden sm:block" />
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
        <CardTitle className="text-xl sm:text-2xl">
          {t("userExport.exportDataTitle")}
        </CardTitle>
        <CardDescription className="text-sm">
          {t("userExport.exportDataDescription")}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <Skeleton className="h-10 w-full sm:w-40" />
          <Skeleton className="h-4 w-48" />
        </div>
      </CardContent>
    </Card>
  );
}
