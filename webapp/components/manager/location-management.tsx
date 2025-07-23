"use client";

import { useState } from "react";
import { Plus, Edit, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { LocationFormModal } from "@/components/manager/location-form-modal";
import { LocationJsonUpload } from "@/components/manager/location-json-upload";
import type {
  EventLocationResponseDto,
  EventLocationRequestDto,
  EventLocationRegistrationDto,
} from "@/api";

export interface LocationManagementProps {
  locations: EventLocationResponseDto[];
  createLocation: (
    location: EventLocationRequestDto,
  ) => Promise<EventLocationResponseDto>;
  updateLocation: (
    id: bigint,
    location: EventLocationRequestDto,
  ) => Promise<EventLocationResponseDto>;
  deleteLocation: (id: bigint) => Promise<unknown>;
  registerLocationWithSeats: (
    data: EventLocationRegistrationDto,
  ) => Promise<EventLocationResponseDto>;
}

export function LocationManagement({
  locations,
  createLocation,
  updateLocation,
  deleteLocation,
  registerLocationWithSeats,
}: LocationManagementProps) {
  const [filteredLocations, setFilteredLocations] = useState(locations);
  const [selectedLocation, setSelectedLocation] =
    useState<EventLocationResponseDto | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const handleSearch = (query: string) => {
    const filtered = locations.filter(
      (location) =>
        location.name?.toLowerCase().includes(query.toLowerCase()) ||
        location.address?.toLowerCase().includes(query.toLowerCase()),
    );
    setFilteredLocations(filtered);
  };

  const handleCreateLocation = () => {
    setSelectedLocation(null);
    setIsCreating(true);
    setIsModalOpen(true);
  };

  const handleEditLocation = (location: EventLocationResponseDto) => {
    setSelectedLocation(location);
    setIsCreating(false);
    setIsModalOpen(true);
  };

  const handleDeleteLocation = async (location: EventLocationResponseDto) => {
    if (
      location.id !== undefined &&
      confirm(`Are you sure you want to delete ${location.name}?`)
    ) {
      await deleteLocation(BigInt(location.id));
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Location Management</CardTitle>
            <CardDescription>Create and manage event locations</CardDescription>
          </div>
          <Button onClick={handleCreateLocation}>
            <Plus className="mr-2 h-4 w-4" />
            Add Location
          </Button>
        </div>
      </CardHeader>

      <CardContent>
        <Tabs defaultValue="list" className="space-y-4">
          <TabsList>
            <TabsTrigger value="list">Location List</TabsTrigger>
            <TabsTrigger value="upload">JSON Upload</TabsTrigger>
          </TabsList>

          <TabsContent value="list" className="space-y-4">
            <SearchAndFilter
              onSearch={handleSearch}
              onFilter={() => {}}
              filterOptions={[]}
            />

            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Address</TableHead>
                  <TableHead>Capacity</TableHead>
                  <TableHead>Manager</TableHead>
                  <TableHead>Seats</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredLocations.map((location) => (
                  <TableRow key={location.id?.toString()}>
                    <TableCell className="font-medium">
                      {location.name}
                    </TableCell>
                    <TableCell>{location.address}</TableCell>
                    <TableCell>{location.capacity}</TableCell>
                    <TableCell>{location.manager?.username}</TableCell>
                    <TableCell>{location.seats?.length || 0}</TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleEditLocation(location)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleDeleteLocation(location)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TabsContent>

          <TabsContent value="upload">
            <LocationJsonUpload onUpload={registerLocationWithSeats} />
          </TabsContent>
        </Tabs>
      </CardContent>

      {isModalOpen && (
        <LocationFormModal
          location={selectedLocation}
          isCreating={isCreating}
          onSubmit={async (locationData) => {
            if (isCreating) {
              await createLocation(locationData);
            } else if (selectedLocation?.id !== undefined) {
              await updateLocation(BigInt(selectedLocation.id), locationData);
            }
            setIsModalOpen(false);
          }}
          onClose={() => setIsModalOpen(false)}
        />
      )}
    </Card>
  );
}
