"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Plus,
  Upload,
  Trash2,
  ArrowRight,
  MapPinned,
  Users,
  ArrowUp,
  ArrowDown,
  ArrowUpDown,
} from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { PaginationWrapper } from "@/components/common/pagination-wrapper";
import { useSortableData } from "@/lib/table-sorting";
import { useManagementLocations } from "@/hooks/use-management-locations";
import { LocationImportModal } from "@/components/management/location-import-modal";
import { LocationCardMapBackground } from "@/components/management/location-card-map-background";

export default function ManagementLocationsPage() {
  const t = useT();
  const router = useRouter();
  const { locations, isLoading, createLocation, deleteLocation } =
    useManagementLocations();

  const [searchQuery, setSearchQuery] = useState("");
  const [isImportOpen, setIsImportOpen] = useState(false);

  const filteredLocations = useMemo(() => {
    const query = searchQuery.toLowerCase();
    if (!query) return locations;
    return locations.filter(
      (location) =>
        location.name?.toLowerCase().includes(query) ||
        location.address?.toLowerCase().includes(query),
    );
  }, [locations, searchQuery]);

  const { sortedData, sortKey, sortDirection, handleSort } = useSortableData(
    filteredLocations,
    "name",
    "asc",
  );

  let SortDirectionIcon = ArrowUpDown;
  if (sortDirection === "asc") SortDirectionIcon = ArrowUp;
  else if (sortDirection === "desc") SortDirectionIcon = ArrowDown;

  const handleDelete = async (id: string, name: string | undefined) => {
    if (confirm(t("management.locations.deleteConfirm", { name }))) {
      await deleteLocation([id]);
    }
  };

  const handleImport = async (data: Parameters<typeof createLocation>[0]) => {
    const location = await createLocation(data);
    router.push(`/management/locations/${location.id}`);
    return location;
  };

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader
        title={t("management.locations.title")}
        description={t("management.locations.description")}
        search={
          <SearchAndFilter
            onSearch={setSearchQuery}
            onFilter={() => {}}
            filterOptions={[]}
            initialQuery={searchQuery}
            className="w-full"
          />
        }
      />

      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-2">
          <span className="text-sm text-muted-foreground">
            {t("management.locations.sortLabel")}
          </span>
          <Select
            value={sortKey ?? "name"}
            onValueChange={(value) => handleSort(value)}
          >
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="name">
                {t("management.locations.sortByName")}
              </SelectItem>
              <SelectItem value="capacity">
                {t("management.locations.sortByCapacity")}
              </SelectItem>
              <SelectItem value="address">
                {t("management.locations.sortByAddress")}
              </SelectItem>
            </SelectContent>
          </Select>
          <Button
            variant="outline"
            size="icon"
            onClick={() => handleSort(sortKey ?? "name")}
            aria-label={t("management.locations.sortLabel")}
          >
            <SortDirectionIcon className="h-4 w-4" />
          </Button>
        </div>

        <div className="flex justify-end gap-2">
          <Button
            variant="outline"
            onClick={() => setIsImportOpen(true)}
            className="w-full sm:w-auto"
          >
            <Upload className="h-4 w-4" />
            {t("management.locations.importJson")}
          </Button>
          <Button asChild className="w-full sm:w-auto">
            <Link href="/management/locations/new">
              <Plus className="h-4 w-4" />
              {t("management.locations.newLocation")}
            </Link>
          </Button>
        </div>
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <Skeleton key={i} className="h-48 rounded-lg" />
          ))}
        </div>
      )}

      {!isLoading && locations.length === 0 && (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            {t("management.locations.empty")}
          </CardContent>
        </Card>
      )}

      {!isLoading && locations.length > 0 && (
        <PaginationWrapper
          data={sortedData}
          itemsPerPage={12}
          paginationLabel={t("management.locations.paginationLabel")}
        >
          {(paginatedData) =>
            paginatedData.length === 0 ? (
              <Card>
                <CardContent className="py-12 text-center text-muted-foreground">
                  {t("management.locations.noResults")}
                </CardContent>
              </Card>
            ) : (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {paginatedData.map((location) => (
                  <Card
                    key={location.id}
                    className="relative flex flex-col overflow-hidden"
                  >
                    {location.address && (
                      <LocationCardMapBackground address={location.address} />
                    )}
                    <CardHeader className="relative z-10">
                      <CardTitle className="flex items-center gap-2 truncate">
                        <MapPinned className="h-4 w-4 shrink-0 text-muted-foreground" />
                        <span className="truncate">{location.name}</span>
                      </CardTitle>
                      {location.address && (
                        <CardDescription className="truncate">
                          {location.address}
                        </CardDescription>
                      )}
                    </CardHeader>
                    <CardContent className="relative z-10 min-h-48 flex-1 space-y-2">
                      <div className="flex flex-wrap items-center gap-2">
                        <Badge variant="secondary">
                          {t("management.locations.capacityLabel")}:{" "}
                          {location.capacity ?? 0}
                        </Badge>
                        <Badge variant="secondary">
                          {t("management.locations.seatsLabel", {
                            count: location.seatIds?.length ?? 0,
                          })}
                        </Badge>
                        {location.markers && location.markers.length > 0 && (
                          <Badge variant="secondary">
                            {t("management.locations.markersLabel", {
                              count: location.markers.length,
                            })}
                          </Badge>
                        )}
                        {location.areas && location.areas.length > 0 && (
                          <Badge variant="secondary">
                            {t("management.locations.areasLabel", {
                              count: location.areas.length,
                            })}
                          </Badge>
                        )}
                      </div>
                      {location.manager?.username && (
                        <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
                          <Users className="h-3.5 w-3.5" />
                          {location.manager.username}
                        </p>
                      )}
                    </CardContent>
                    <CardFooter className="relative z-10 flex gap-2">
                      <Button asChild className="flex-1">
                        <Link href={`/management/locations/${location.id}`}>
                          {t("management.locations.openEditor")}
                          <ArrowRight className="h-4 w-4" />
                        </Link>
                      </Button>
                      <Button
                        variant="destructive"
                        size="icon"
                        onClick={() =>
                          location.id &&
                          handleDelete(location.id, location.name)
                        }
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </CardFooter>
                  </Card>
                ))}
              </div>
            )
          }
        </PaginationWrapper>
      )}

      {isImportOpen && (
        <LocationImportModal
          isOpen={isImportOpen}
          onClose={() => setIsImportOpen(false)}
          onImportLocation={handleImport}
        />
      )}
    </div>
  );
}
