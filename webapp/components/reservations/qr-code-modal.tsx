"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useT } from "@/lib/i18n/hooks";
import { sanitizeFileName } from "@/lib/utils/filename";
import { Download } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import QRCode from "qrcode";
import type { UserReservationResponseDto } from "@/api";
import Image from "next/image";

interface QRCodeModalProps {
  isOpen: boolean;
  eventName: string | undefined;
  onClose: () => void;
  reservations: UserReservationResponseDto[];
  userId: bigint | undefined;
}

export function QRCodeModal({
  isOpen,
  eventName,
  onClose,
  reservations,
  userId,
}: QRCodeModalProps) {
  const t = useT();
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string>("");

  // Generate QR code data when modal opens
  useEffect(() => {
    if (!isOpen || !reservations || reservations.length === 0 || !userId) {
      return;
    }

    const generateQRCode = async () => {
      const firstReservation = reservations[0];
      const eventId = firstReservation.eventId;

      // Generate the code: eventId;userId;reservationId1,reservationId2,...
      const codes = reservations
        .map((r) => r.checkInCode)
        .filter((c) => c)
        .join(",");

      const code = `${eventId};${userId};${codes}`;

      try {
        const dataUrl = await QRCode.toDataURL(code, {
          width: 300,
          margin: 2,
          color: {
            dark: "#000000",
            light: "#FFFFFF",
          },
        });
        setQrCodeDataUrl(dataUrl);
      } catch (error) {
        console.error("Error generating QR code:", error);
        toast.error(t("qrCodeModal.generationError"), {
          description: t("qrCodeModal.generationErrorDescription"),
        });
      }
    };

    generateQRCode();
  }, [isOpen, reservations, userId, t]);

  const handleDownloadQRCode = () => {
    if (!qrCodeDataUrl) return;

    const link = document.createElement("a");
    link.href = qrCodeDataUrl;
    const fileName = `${sanitizeFileName(eventName)}-check-in-code.png`;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    toast.success(t("qrCodeModal.downloadStarted"), {
      description: t("qrCodeModal.qrCodeDownloading"),
    });
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent noX={true} className="max-w-md">
        <DialogHeader>
          <DialogTitle>{t("qrCodeModal.title")}</DialogTitle>
          <DialogDescription>{t("qrCodeModal.description")}</DialogDescription>
        </DialogHeader>

        <div className="flex flex-col items-center justify-center gap-6 py-6">
          {qrCodeDataUrl ? (
            <div className="flex flex-col items-center gap-4">
              <Image
                src={qrCodeDataUrl}
                alt="Reservation QR Code"
                width={288}
                height={288}
                className="border-4 border-gray-300 rounded-lg p-2 bg-white"
              />
            </div>
          ) : (
            <div className="flex items-center justify-center w-full h-32">
              <p className="text-sm text-muted-foreground">
                {t("qrCodeModal.generatingQRCode")}
              </p>
            </div>
          )}
        </div>

        <DialogFooter className="gap-2">
          <Button
            onClick={handleDownloadQRCode}
            disabled={!qrCodeDataUrl}
            className="flex-1"
          >
            <Download className="mr-2 h-4 w-4" />
            {t("qrCodeModal.downloadButton")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
