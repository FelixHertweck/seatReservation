"use client";

import { useEffect, useRef, useState } from "react";
import { Mail, Printer, Loader2 } from "lucide-react";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/custom-ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface ReservationConfirmationModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  userName: string;
  emailData?: { subject?: string; htmlContent?: string } | null;
  isLoading: boolean;
  isError: boolean;
  onResend: () => Promise<void>;
}

export function ReservationConfirmationModal({
  open,
  onOpenChange,
  userName,
  emailData,
  isLoading,
  isError,
  onResend,
}: ReservationConfirmationModalProps) {
  const t = useT();
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const [isSending, setIsSending] = useState(false);
  const [isIframeLoaded, setIsIframeLoaded] = useState(false);

  useEffect(() => {
    setIsIframeLoaded(false);
  }, [emailData?.htmlContent]);

  const handlePrint = () => {
    if (iframeRef.current?.contentWindow && isIframeLoaded) {
      iframeRef.current.contentWindow.focus();
      iframeRef.current.contentWindow.print();
    }
  };

  const handleResend = async () => {
    setIsSending(true);
    try {
      await onResend();
      toast.success(t("management.reservations.resendSuccess"));
    } catch {
      toast.error(t("management.reservations.resendError"));
    } finally {
      setIsSending(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl flex max-h-[90vh] flex-col">
        <DialogHeader>
          <DialogTitle>
            {t("management.reservations.confirmationModalTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("management.reservations.confirmationModalDescription", {
              user: userName,
            })}
          </DialogDescription>
        </DialogHeader>

        <div className="relative my-2 h-[520px] max-h-[65vh] w-full overflow-hidden rounded-md border bg-white">
          {isLoading ? (
            <div className="absolute inset-0 flex items-center justify-center bg-white text-muted-foreground">
              <Loader2 className="h-8 w-8 animate-spin" />
            </div>
          ) : isError || !emailData?.htmlContent ? (
            <div className="absolute inset-0 flex items-center justify-center p-4 text-center text-sm text-destructive">
              {t("management.reservations.loadError")}
            </div>
          ) : (
            <iframe
              ref={iframeRef}
              sandbox="allow-same-origin allow-modals"
              srcDoc={emailData.htmlContent}
              title={
                emailData.subject ||
                t("management.reservations.confirmationModalTitle")
              }
              onLoad={() => setIsIframeLoaded(true)}
              className="absolute inset-0 h-full w-full border-0 bg-white"
            />
          )}
        </div>

        <DialogFooter className="flex-col gap-2 sm:flex-row">
          <Button
            variant="outline"
            onClick={handlePrint}
            disabled={
              isLoading || isError || !emailData?.htmlContent || !isIframeLoaded
            }
          >
            <Printer className="h-4 w-4" />
            {t("management.reservations.printButton")}
          </Button>

          <Button
            onClick={handleResend}
            isLoading={isSending}
            disabled={isLoading || isError || isSending}
          >
            <Mail className="h-4 w-4" />
            {isSending
              ? t("management.reservations.resendSending")
              : t("management.reservations.resendEmailButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
