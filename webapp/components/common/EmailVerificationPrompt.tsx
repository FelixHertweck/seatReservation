"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/use-auth";
import Link from "next/link";
import { usePathname } from "next/navigation";

export function EmailVerificationPrompt() {
  const { user, isLoggedIn, isLoading } = useAuth();
  const router = useRouter();
  const [showPopup, setShowPopup] = useState(false);
  const currentpath = usePathname();

  useEffect(() => {
    if (
      !isLoading &&
      isLoggedIn &&
      user &&
      (!user.emailVerified || !user.email) &&
      currentpath !== "/profile"
    ) {
      setShowPopup(true);
    } else {
      setShowPopup(false);
    }
  }, [user, isLoggedIn, isLoading, currentpath]);

  const handleGoToProfile = () => {
    setShowPopup(false);
    router.push("/profile");
  };

  if (!showPopup) {
    return null;
  }

  return (
    <Dialog open={showPopup} onOpenChange={setShowPopup}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>E-Mail-Verifizierung erforderlich</DialogTitle>
          <DialogDescription>
            {user?.email ? (
              <>
                Eine Bestätigungs-E-Mail wurde an{" "}
                <span className="font-semibold">{user.email}</span> gesendet.
                Bitte klicken Sie auf den Link in der E-Mail, um Ihre Adresse zu
                bestätigen und fortzufahren.
                <br />
                <br />
                Sollte diese E-Mail-Adresse falsch sein, können Sie diese auf
                Ihrer{" "}
                <Link
                  href="/profile"
                  className="text-primary hover:underline"
                  onClick={() => setShowPopup(false)}
                >
                  Profilseite
                </Link>{" "}
                ändern.
              </>
            ) : (
              <>
                Es ist keine E-Mail-Adresse für Ihr Konto registriert. Bitte
                fügen Sie eine E-Mail-Adresse hinzu und bestätigen Sie diese, um
                fortzufahren.
              </>
            )}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button onClick={handleGoToProfile}>Zum Profil</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
