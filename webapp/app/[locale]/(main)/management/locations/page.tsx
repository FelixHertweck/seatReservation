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
  Edit,
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
import { Button } from "@/components/custom-ui/button";
import { Badge } from "@/components/ui/badge";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { PaginationWrapper } from "@/components/common/pagination-wrapper";
import { useSortableData } from "@/lib/table-sorting";
import { useManagementLocations } from "@/hooks/use-management-locations";
import { LocationImportModal } from "@/components/management/location-import-modal";
import { LocationFormModal } from "@/components/management/location-form-modal";
import { LocationCardMapBackground } from "@/components/management/location-card-map-background";
import { LocationCardSkeleton } from "@/components/management/location-card-skeleton";
import type { EventLocationResponseDto } from "@/api";

export default function ManagementLocationsPage() {
  const t = useT();
  const router = useRouter();
  const {
    locations,
    users,
    isLoading,
    createLocation,
    updateLocation,
    deleteLocation,
  } = useManagementLocations();

  const [searchQuery, setSearchQuery] = useState("");
  const [filters, setFilters] = useState<Record<string, unknown>>({});
  const [isImportOpen, setIsImportOpen] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [selectedLocationForEdit, setSelectedLocationForEdit] =
    useState<EventLocationResponseDto | null>(null);

  const handleCreate = () => {
    setSelectedLocationForEdit(null);
    setIsCreating(true);
    setIsFormOpen(true);
  };

  const handleEdit = (location: EventLocationResponseDto) => {
    setSelectedLocationForEdit(location);
    setIsCreating(false);
    setIsFormOpen(true);
  };

  const managerOptions = useMemo(() => {
    const managers = new Map<string, string>();
    for (const location of locations) {
      if (location.createdBy?.id && location.createdBy.username) {
        managers.set(location.createdBy.id, location.createdBy.username);
      }
    }
    return [...managers.entries()].map(([value, label]) => ({
      value,
      label,
    }));
  }, [locations]);

  const filteredLocations = useMemo(() => {
    const query = searchQuery.toLowerCase();
    const managerId = filters.managerId as string | undefined;
    return locations.filter((location) => {
      const matchesQuery =
        !query ||
        location.name?.toLowerCase().includes(query) ||
        location.address?.toLowerCase().includes(query);
      const matchesManager = !managerId || location.createdBy?.id === managerId;
      return matchesQuery && matchesManager;
    });
  }, [locations, searchQuery, filters]);

  const { sortedData, sortKey, sortDirection, handleSort } = useSortableData(
    filteredLocations,
    "name",
    "asc",
  );

  const [deletingLocationId, setDeletingLocationId] = useState<string | null>(
    null,
  );

  const handleDelete = async (id: string, name: string | undefined) => {
    if (confirm(t("management.locations.deleteConfirm", { name }))) {
      setDeletingLocationId(id);
      try {
        await deleteLocation([id]);
      } finally {
        setDeletingLocationId(null);
      }
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
        actions={
          <>
            <Button
              variant="outline"
              onClick={() => setIsImportOpen(true)}
              aria-label={t("management.locations.importJson")}
            >
              <Upload className="h-4 w-4" />
              <span className="hidden sm:inline">
                {t("management.locations.importJson")}
              </span>
            </Button>

            <Button
              onClick={handleCreate}
              aria-label={t("management.locations.newLocation")}
            >
              <Plus className="h-4 w-4" />
              <span className="hidden sm:inline">
                {t("management.locations.newLocation")}
              </span>
            </Button>
          </>
        }
        search={
          <SearchAndFilter
            onSearch={setSearchQuery}
            onFilter={setFilters}
            filterOptions={
              managerOptions.length > 0
                ? [
                    {
                      key: "managerId",
                      label: t("management.locations.managerLabel"),
                      type: "select",
                      options: managerOptions,
                    },
                  ]
                : []
            }
            sortOptions={[
              { key: "name", label: t("management.locations.sortByName") },
              {
                key: "address",
                label: t("management.locations.sortByAddress"),
              },
            ]}
            sortKey={sortKey}
            sortDirection={sortDirection}
            onSort={handleSort}
            initialQuery={searchQuery}
            className="w-full"
          />
        }
      />

      {isLoading && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <LocationCardSkeleton key={i} />
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
                          {t("management.locations.seatsLabel", {
                            count: location.seatCount ?? 0,
                          })}
                        </Badge>
                        {(location.markerCount ?? 0) > 0 && (
                          <Badge variant="secondary">
                            {t("management.locations.markersLabel", {
                              count: location.markerCount,
                            })}
                          </Badge>
                        )}
                        {(location.areaCount ?? 0) > 0 && (
                          <Badge variant="secondary">
                            {t("management.locations.areasLabel", {
                              count: location.areaCount,
                            })}
                          </Badge>
                        )}
                      </div>
                      {location.createdBy?.username && (
                        <p className="flex items-center gap-1.5 text-sm text-muted-foreground">
                          <Users className="h-3.5 w-3.5" />
                          {location.createdBy.username}
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
                        variant="outline"
                        size="icon"
                        onClick={() => handleEdit(location)}
                        title={t("management.locations.editButton")}
                      >
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="destructive"
                        size="icon"
                        onClick={() =>
                          location.id &&
                          handleDelete(location.id, location.name)
                        }
                        isLoading={deletingLocationId === location.id}
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

      {isFormOpen && (
        <LocationFormModal
          location={selectedLocationForEdit}
          isCreating={isCreating}
          users={users}
          onSubmit={async (data) => {
            if (isCreating) {
              const created = await createLocation(data);
              setIsFormOpen(false);
              router.push(`/management/locations/${created.id}`);
            } else if (selectedLocationForEdit?.id) {
              await updateLocation(selectedLocationForEdit.id, data);
              setIsFormOpen(false);
              setSelectedLocationForEdit(null);
            }
          }}
          onClose={() => {
            setIsFormOpen(false);
            setSelectedLocationForEdit(null);
          }}
        />
      )}
    </div>
  );
}
