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
import { useT } from "@/lib/i18n/hooks";
import { useLoginRequiredPopup } from "@/hooks/use-login-popup";
import { isValidRedirectUrl } from "@/lib/utils";

export function LoginRequiredPopup() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;

  const { isOpen, setIsOpen } = useLoginRequiredPopup();
  const router = useRouter();

  const handleLoginRedirect = () => {
    setIsOpen(false); // Close the popup before redirecting
    const currentPath = window.location.pathname + window.location.search;
    if (isValidRedirectUrl(currentPath)) {
      router.push(
        `/${locale}/login?returnTo=${encodeURIComponent(currentPath)}`,
      );
      return;
    }
    // Fallback to home if currentPath is not valid
    router.push(`/${locale}/`);
  };

  return (
    <Dialog open={isOpen} onOpenChange={setIsOpen}>
      <DialogContent
        className="sm:max-w-[425px]"
        noX={true}
        onInteractOutside={(e) => e.preventDefault()}
      >
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
