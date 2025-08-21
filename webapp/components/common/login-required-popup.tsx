"use client";

import { useParams, useRouter } from "next/navigation";
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
import { useT } from "@/lib/i18n/hooks";

export function LoginRequiredPopup() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;

  const { isLoggedIn, isLoading } = useAuthStatus();
  const router = useRouter();

  const handleLoginRedirect = () => {
    router.push(`/${locale}/login`);
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
      <DialogContent className="sm:max-w-[425px]" showCloseButton={false}>
        <DialogHeader>
          <DialogTitle>{t("loginRequiredPopup.title")}</DialogTitle>
          <DialogDescription>
            {t("loginRequiredPopup.description")}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button onClick={handleLoginRedirect}>
            {t("loginRequiredPopup.loginButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
