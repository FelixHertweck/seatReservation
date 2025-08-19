"use client";

import { useRouter } from "next/navigation";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { useAuthStatus } from "@/hooks/use-auth-status";
import { setToastsDisabled } from "@/hooks/use-toast";
import { useEffect } from "react";

export function LoginRequiredPopup() {
  const { isLoggedIn, isLoading } = useAuthStatus();
  const router = useRouter();

  const handleLoginRedirect = () => {
    router.push("/login");
  };

  useEffect(() => {
    if (!isLoading && !isLoggedIn) {
      setToastsDisabled(true);
    }

    return () => {
      setToastsDisabled(false);
    };
  }, [isLoading, isLoggedIn]);

  return (
    <Dialog open={!isLoading && !isLoggedIn}>
      <DialogContent className="sm:max-w-[425px]" noX={true}>
        <DialogHeader>
          <DialogTitle>Anmeldung erforderlich</DialogTitle>
          <DialogDescription>
            Sie müssen angemeldet sein, um auf diese Seite zugreifen zu können.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button onClick={handleLoginRedirect}>Anmelden</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
