"use client";

import dynamic from "next/dynamic";
import { Loader2, MapPinOff } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Label } from "@/components/custom-ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/custom-ui/button";
import { Skeleton } from "@/components/custom-ui/skeleton";
import { useSyncedField } from "@/components/management/location-editor/use-synced-field";
import { useGeocode } from "@/hooks/use-geocode";
import type { useLocationEditorSave } from "@/components/management/location-editor/use-location-editor-save";
import type { LocationMeta } from "@/components/management/location-editor/types";

const AddressMap = dynamic(
  () => import("@/components/management/location-editor/address-map"),
  { ssr: false, loading: () => <Skeleton className="h-full w-full" /> },
);

function AddressMapPlaceholder({
  isGeocoding,
  notFound,
}: {
  isGeocoding: boolean;
  notFound: boolean;
}) {
  const t = useT();

  if (isGeocoding) {
    return (
      <>
        <Loader2 className="h-4 w-4 animate-spin" />
        {t("management.locationEditor.details.locating")}
      </>
    );
  }
  if (notFound) {
    return (
      <>
        <MapPinOff className="h-4 w-4" />
        {t("management.locationEditor.details.addressNotFound")}
      </>
    );
  }
  return <>{t("management.locationEditor.details.addressMapPlaceholder")}</>;
}

interface DetailsPanelProps {
  meta: LocationMeta;
  autosave: ReturnType<typeof useLocationEditorSave>;
  onSaved?: () => void;
}

export function DetailsPanel({ meta, autosave, onSaved }: DetailsPanelProps) {
  const t = useT();
  const [name, setName] = useSyncedField(meta.name);
  const [address, setAddress] = useSyncedField(meta.address);
  const [capacity, setCapacity] = useSyncedField(String(meta.capacity));
  const {
    result: geocoded,
    isLoading: isGeocoding,
    notFound,
  } = useGeocode(address);

  const parsedCapacity = Number.parseInt(capacity, 10);
  const isCapacityValid = !Number.isNaN(parsedCapacity);
  const isDirty =
    name !== meta.name ||
    address !== meta.address ||
    (isCapacityValid && parsedCapacity !== meta.capacity);

  const handleSave = () => {
    const changes: Partial<{
      name: string;
      address: string;
      capacity: number;
    }> = {};
    if (name !== meta.name) changes.name = name;
    if (address !== meta.address) changes.address = address;
    if (isCapacityValid && parsedCapacity !== meta.capacity) {
      changes.capacity = parsedCapacity;
    }
    if (Object.keys(changes).length > 0) {
      autosave.updateMeta(changes);
    }
    onSaved?.();
  };

  return (
    <div className="space-y-3">
      <div className="space-y-1.5">
        <Label htmlFor="editor-name">
          {t("management.locationEditor.details.nameLabel")}
        </Label>
        <Input
          id="editor-name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="editor-address">
          {t("management.locationEditor.details.addressLabel")}
        </Label>
        <Input
          id="editor-address"
          value={address}
          onChange={(e) => setAddress(e.target.value)}
        />
      </div>

      <div
        data-slot="address-map"
        className="relative aspect-video w-full overflow-hidden rounded-md border"
      >
        {geocoded ? (
          <AddressMap
            lat={geocoded.lat}
            lon={geocoded.lon}
            displayName={geocoded.displayName}
          />
        ) : (
          <div className="flex h-full w-full flex-col items-center justify-center gap-1.5 bg-muted/30 px-4 text-center text-xs text-muted-foreground">
            <AddressMapPlaceholder
              isGeocoding={isGeocoding}
              notFound={notFound}
            />
          </div>
        )}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="editor-capacity">
          {t("management.locationEditor.details.capacityLabel")}
        </Label>
        <Input
          id="editor-capacity"
          type="number"
          min={0}
          value={capacity}
          onChange={(e) => setCapacity(e.target.value)}
        />
      </div>

      <Button
        type="button"
        className="w-full"
        disabled={!isDirty}
        onClick={handleSave}
      >
        {t("management.locationEditor.details.saveButton")}
      </Button>
    </div>
  );
}
