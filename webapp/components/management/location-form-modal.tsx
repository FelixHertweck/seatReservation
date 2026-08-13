"use client";

import type React from "react";
import { useState } from "react";
import { MapPin, Users } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { Button } from "@/components/custom-ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import { UserMultiSelect } from "@/components/common/user-multi-select";
import type { EventLocationResponseDto, EventLocationRequestDto, UserDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

interface LocationFormModalProps {
  location: EventLocationResponseDto | null;
  users: UserDto[];
  onSubmit: (locationData: EventLocationRequestDto) => Promise<void>;
  onClose: () => void;
}

export function LocationFormModal({
  location,
  users,
  onSubmit,
  onClose,
}: Readonly<LocationFormModalProps>) {
  const t = useT();

  const [formData, setFormData] = useState({
    name: location?.name || "",
    address: location?.address || "",
    managerIds: location?.managerIds?.map((id: string) => id.toString()) || [],
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) {
      e.preventDefault();
    }
    setIsLoading(true);

    try {
      const payload: EventLocationRequestDto = {
        name: formData.name,
        address: formData.address,
        managerIds: formData.managerIds || [],
      };
      await onSubmit(payload);
      onClose();
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Dialog open onOpenChange={onClose}>
      <DialogContent
        className="sm:max-w-xl sm:max-h-[85vh] sm:overflow-y-auto"
        onInteractOutside={(e) => e.preventDefault()}
      >
        <DialogHeader className="space-y-2 pb-2">
          <DialogTitle>
            {t("management.locations.editLocation")}
          </DialogTitle>
          <DialogDescription>
            {t("management.locations.editDescription")}
          </DialogDescription>
        </DialogHeader>

        <form
          onSubmit={handleSubmit}
          className="space-y-6 py-2"
        >
          {/* Basic information */}
          <div className="space-y-5">
            <h3 className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <MapPin className="h-4 w-4" />
              {t("management.locations.form.nameLabel")} & {t("management.locations.form.addressLabel")}
            </h3>
            <div className="space-y-3">
              <Label htmlFor="location-name" className="text-sm font-medium">
                {t("management.locations.form.nameLabel")}
              </Label>
              <Input
                id="location-name"
                value={formData.name}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, name: e.target.value }))
                }
                placeholder={t("management.locations.form.namePlaceholder")}
                required
              />
            </div>
            <div className="space-y-3">
              <Label htmlFor="location-address" className="text-sm font-medium">
                {t("management.locations.form.addressLabel")}
              </Label>
              <Input
                id="location-address"
                value={formData.address}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, address: e.target.value }))
                }
                placeholder={t("management.locations.form.addressPlaceholder")}
                required
              />
            </div>
          </div>

          {/* Managers */}
          <div className="space-y-5 border-t pt-6">
            <h3 className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <Users className="h-4 w-4" />
              {t("management.locations.form.managersSectionTitle")}
            </h3>
            <UserMultiSelect
              users={users}
              selectedUserIds={formData.managerIds}
              onSelectionChange={(sel) =>
                setFormData((prev) => ({ ...prev, managerIds: sel }))
              }
              label={t("management.locations.form.managersLabel")}
              placeholder={t("management.locations.form.managersPlaceholder")}
            />
          </div>

          <DialogFooter className="mt-8 pt-4 border-t">
            <Button type="button" variant="outline" onClick={onClose} disabled={isLoading}>
              {t("management.locations.form.cancelButton")}
            </Button>
            <Button type="submit" isLoading={isLoading} disabled={isLoading}>
              {t("management.locations.saveButton")}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
