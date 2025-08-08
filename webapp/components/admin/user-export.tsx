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

export function UserExport() {
  const { users, isLoading } = useAdmin();

  const handleExport = () => {
    if (!users || users.length === 0) {
      toast({
        title: "Keine Nutzer zum Exportieren",
        description:
          "Es sind keine Nutzerdaten verfügbar, die exportiert werden könnten.",
        variant: "destructive",
      });
      return;
    }

    try {
      const jsonString = JSON.stringify(
        users,
        (key, value) => (typeof value === "bigint" ? value.toString() : value),
        2,
      );
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
        title: "Export erfolgreich",
        description:
          "Die Nutzerdaten wurden erfolgreich als users.json exportiert.",
      });
    } catch (error) {
      console.error("Fehler beim Exportieren der Nutzerdaten:", error);
      toast({
        title: "Export fehlgeschlagen",
        description: "Es gab einen Fehler beim Exportieren der Nutzerdaten.",
        variant: "destructive",
      });
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Nutzerdaten exportieren</CardTitle>
        <CardDescription>
          Exportieren Sie alle Nutzerdaten als JSON-Datei.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Button
          onClick={handleExport}
          disabled={isLoading || users.length === 0}
        >
          <Download className="mr-2 h-4 w-4" />
          {isLoading ? "Lade Nutzer..." : "Nutzer als JSON exportieren"}
        </Button>
        {users.length === 0 && !isLoading && (
          <p className="text-sm text-muted-foreground mt-2">
            Keine Nutzer zum Exportieren gefunden.
          </p>
        )}
      </CardContent>
    </Card>
  );
}
