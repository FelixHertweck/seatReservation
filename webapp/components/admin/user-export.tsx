"use client";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Download } from "lucide-react";
import { useAdmin } from "@/hooks/use-admin";
import { toast } from "@/components/ui/use-toast";
import { customSerializer } from "@/lib/jsonBodySerializer";
import { t } from "i18next";

export function UserExport() {
  const { users, isLoading } = useAdmin();

  const handleExport = () => {
    if (!users || users.length === 0) {
      toast({
        title: t("userExport.noUsersToExportTitle"),
        description: t("userExport.noUsersToExportDescription"),
        variant: "destructive",
      });
      return;
    }

    try {
      const jsonString = customSerializer.json(users);

      const blob = new Blob([jsonString], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "users.json";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);

      toast({
        title: t("userExport.exportSuccessTitle"),
        description: t("userExport.exportSuccessDescription"),
      });
    } catch (error) {
      console.error(t("userExport.exportErrorLog"), error);
      toast({
        title: t("userExport.exportFailedTitle"),
        description: t("userExport.exportFailedDescription"),
        variant: "destructive",
      });
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("userExport.exportDataTitle")}</CardTitle>
        <CardDescription>
          {t("userExport.exportDataDescription")}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Button
          onClick={handleExport}
          disabled={isLoading || users.length === 0}
        >
          <Download className="mr-2 h-4 w-4" />
          {isLoading
            ? t("userExport.loadingUsers")
            : t("userExport.exportUsersAsJson")}
        </Button>
        {users.length === 0 && !isLoading && (
          <p className="text-sm text-muted-foreground mt-2">
            {t("userExport.noUsersFoundToExport")}
          </p>
        )}
      </CardContent>
    </Card>
  );
}
