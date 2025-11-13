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
import { Copy, Download } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "@/hooks/use-toast";
import QRCode from "qrcode";
import type { UserReservationResponseDto } from "@/api";

interface QRCodeModalProps {
  isOpen: boolean;
  onClose: () => void;
  reservations: UserReservationResponseDto[];
  userId: bigint | undefined;
}

export function QRCodeModal({
  isOpen,
  onClose,
  reservations,
  userId,
}: QRCodeModalProps) {
  const t = useT();
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string>("");
  const [reservationCode, setReservationCode] = useState<string>("");

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
        .map((r) => r.id)
        .filter((c) => c)
        .join(",");

      const code = `${eventId};${userId};${codes}`;
      setReservationCode(code);

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
        toast({
          title: t("qrCodeModal.generationError"),
          description: t("qrCodeModal.generationErrorDescription"),
          variant: "destructive",
        });
      }
    };

    generateQRCode();
  }, [isOpen, reservations, userId, t]);

  const handleCopyCode = () => {
    if (!reservationCode) return;

    navigator.clipboard.writeText(reservationCode).then(() => {
      toast({
        title: t("qrCodeModal.copiedToClipboard"),
        description: t("qrCodeModal.codeReady"),
      });
    });
  };

  const handleDownloadQRCode = () => {
    if (!qrCodeDataUrl) return;

    const link = document.createElement("a");
    link.href = qrCodeDataUrl;
    link.download = `reservation-qr-code-${reservations[0]?.eventId}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    toast({
      title: t("qrCodeModal.downloadStarted"),
      description: t("qrCodeModal.qrCodeDownloading"),
    });
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{t("qrCodeModal.title")}</DialogTitle>
          <DialogDescription>
            {t("qrCodeModal.description")}
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col items-center justify-center gap-6 py-6">
          {qrCodeDataUrl ? (
            <div className="flex flex-col items-center gap-4">
              <img
                src={qrCodeDataUrl}
                alt="Reservation QR Code"
                className="border-4 border-gray-300 rounded-lg p-2 bg-white"
              />
              <div className="text-center w-full">
                <p className="text-sm text-muted-foreground mb-2">
                  {t("qrCodeModal.reservationCode")}
                </p>
                <div className="bg-secondary p-2 rounded text-xs font-mono break-all max-h-20 overflow-y-auto">
                  {reservationCode}
                </div>
              </div>
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
            variant="outline"
            onClick={handleCopyCode}
            disabled={!reservationCode}
            className="flex-1"
          >
            <Copy className="mr-2 h-4 w-4" />
            {t("qrCodeModal.copyCodeButton")}
          </Button>
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
