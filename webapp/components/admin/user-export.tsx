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
import { toast } from "sonner";
import { customSerializer } from "@/lib/jsonBodySerializer";
import { useT } from "@/lib/i18n/hooks";

export function UserExport() {
  const t = useT();

  const { users, isLoading } = useAdmin();

  const handleExport = () => {
    if (!users || users.length === 0) {
      toast.error(t("userExport.noUsersToExportTitle"), {
        description: t("userExport.noUsersToExportDescription"),
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

      toast.success(t("userExport.exportSuccessTitle"), {
        description: t("userExport.exportSuccessDescription"),
      });
    } catch (error) {
      console.error(t("userExport.exportErrorLog"), error);
      toast.error(t("userExport.exportFailedTitle"), {
        description: t("userExport.exportFailedDescription"),
      });
    }
  };

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
        <Button
          onClick={handleExport}
          disabled={isLoading || users.length === 0}
          className="w-full sm:w-auto"
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
