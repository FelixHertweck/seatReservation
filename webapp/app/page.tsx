"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { BouncingDotsLoader } from "@/components/ui/bouncing-dots-loader";
import { useAuthStatus } from "@/hooks/use-auth-status";

export default function RootRedirectPage() {
  const { isLoggedIn, isLoading } = useAuthStatus();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (isLoggedIn) {
        // Benutzer ist angemeldet, leite zur Hauptanwendung weiter
        router.replace("/events");
      } else {
        // Benutzer ist nicht angemeldet, leite zur Startseite weiter
        router.replace("/start");
      }
    }
  }, [isLoggedIn, isLoading, router]);

  // Zeige eine Ladeanimation, während der Anmeldestatus überprüft wird
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <BouncingDotsLoader />
    </div>
  );
}
