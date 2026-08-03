"use client";

import type React from "react";

import { useState } from "react";
import { useRouter } from "next/navigation";

import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/custom-ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/custom-ui/label";
import { useManagementLocations } from "@/hooks/use-management-locations";

export default function NewLocationPage() {
  const t = useT();
  const router = useRouter();
  const { createLocation } = useManagementLocations();

  const [formData, setFormData] = useState({
    name: "",
    address: "",
  });
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const location = await createLocation({
        name: formData.name,
        address: formData.address,
      });
      router.replace(`/management/locations/${location.id}`);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="container mx-auto p-4 sm:p-6">
      <PageHeader title={t("management.locations.newLocation")} />

      <Card className="max-w-xl">
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4 mt-6">
            <div className="space-y-2">
              <Label htmlFor="name">
                {t("management.locations.form.nameLabel")}
              </Label>
              <Input
                id="name"
                required
                placeholder={t("management.locations.form.namePlaceholder")}
                value={formData.name}
                onChange={(e) =>
                  setFormData({ ...formData, name: e.target.value })
                }
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="address">
                {t("management.locations.form.addressLabel")}
              </Label>
              <Input
                id="address"
                required
                placeholder={t("management.locations.form.addressPlaceholder")}
                value={formData.address}
                onChange={(e) =>
                  setFormData({ ...formData, address: e.target.value })
                }
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <Button
                type="button"
                variant="outline"
                onClick={() => router.push("/management/locations")}
              >
                {t("management.locations.form.cancelButton")}
              </Button>
              <Button type="submit" isLoading={isLoading} disabled={isLoading}>
                {t("management.locations.form.submitButton")}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
