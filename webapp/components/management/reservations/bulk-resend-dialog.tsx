"use client";

import { useState } from "react";
import { Mail } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/custom-ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface BulkResendDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  userCount: number;
  onSend: (message: string) => Promise<void>;
}

export function BulkResendDialog({
  open,
  onOpenChange,
  userCount,
  onSend,
}: Readonly<BulkResendDialogProps>) {
  const t = useT();
  const [message, setMessage] = useState("");
  const [isSending, setIsSending] = useState(false);

  if (!open && message) setMessage("");

  const handleSend = async () => {
    setIsSending(true);
    try {
      await onSend(message);
    } finally {
      setIsSending(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(o) => !isSending && onOpenChange(o)}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {t("management.reservations.bulkResendTitle")}
          </DialogTitle>
          <DialogDescription>
            {t("management.reservations.bulkResendDescription", {
              count: userCount,
            })}
          </DialogDescription>
        </DialogHeader>
        <div className="mt-3 flex flex-col gap-4">
          <label htmlFor="bulk-resend-message" className="text-sm font-medium">
            {t("management.reservations.resendMessageLabel")}
          </label>
          <Textarea
            id="bulk-resend-message"
            value={message}
            maxLength={1000}
            rows={4}
            onChange={(e) => setMessage(e.target.value)}
            placeholder={t("management.reservations.resendMessagePlaceholder")}
          />
        </div>
        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isSending}
          >
            {t("common.cancel")}
          </Button>
          <Button onClick={handleSend} isLoading={isSending}>
            <Mail className="h-4 w-4" />
            {t("management.reservations.resendEmailButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
