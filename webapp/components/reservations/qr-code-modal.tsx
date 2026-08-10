"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { useT } from "@/lib/i18n/hooks";
import { sanitizeFileName } from "@/lib/utils/filename";
import { Loader2, Wallet } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import QRCode from "qrcode";
import {
  getApiUserWalletReservationsByIdByProvider,
  getApiUserWalletConfig,
  type UserReservationResponseDto,
  type WalletConfigDto,
} from "@/api";
import Image from "next/image";
import { useParams } from "next/navigation";

interface QRCodeModalProps {
  isOpen: boolean;
  eventName: string | undefined;
  onClose: () => void;
  reservations: UserReservationResponseDto[];
  userId: string | undefined;
}

export function QRCodeModal({
  isOpen,
  eventName,
  onClose,
  reservations,
  userId,
}: QRCodeModalProps) {
  const t = useT();
  const params = useParams();
  const locale = (params?.locale as string) ?? "en";

  // Official wallet badges — locale-aware as required by Apple and Google brand guidelines
  const googleBadgeSrc =
    locale === "de"
      ? "/wallet/google/de_add_to_google_wallet_add-wallet-badge.svg"
      : "/wallet/google/enUS_add_to_google_wallet_add-wallet-badge.svg";
  const appleBadgeSrc =
    locale === "de"
      ? "/wallet/apple/DE_Add_to_Apple_Wallet_RGB_101421.svg"
      : "/wallet/apple/US-UK_Add_to_Apple_Wallet_RGB_101421.svg";

  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string>("");
  const [walletConfig, setWalletConfig] = useState<WalletConfigDto | null>(
    null,
  );

  const firstReservation = reservations[0];
  const hasCheckInToken = !!firstReservation?.checkInToken;

  // Generate QR code data when modal opens
  useEffect(() => {
    if (
      !isOpen ||
      !reservations ||
      reservations.length === 0 ||
      !userId ||
      !hasCheckInToken
    ) {
      return;
    }

    const generateQRCode = async () => {
      const firstReservation = reservations[0];
      const eventId = firstReservation.eventId;

      // Generate the code: userId;eventId;checkInToken -- must match
      // EmailService.generateQrCodeContent and the check-in scanner's parsing order.
      const token = firstReservation.checkInToken;
      const code = `${userId};${eventId};${token}`;

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
  }, [isOpen, reservations, userId, hasCheckInToken, t]);

  useEffect(() => {
    if (!isOpen) return;

    const fetchWalletConfig = async () => {
      try {
        const response = await getApiUserWalletConfig();
        if (response.data) {
          setWalletConfig(response.data);
        }
      } catch (error) {
        console.error("Error fetching wallet config:", error);
      }
    };

    fetchWalletConfig();
  }, [isOpen]);

  const [loadingProvider, setLoadingProvider] = useState<
    "GOOGLE" | "APPLE" | "GENERIC_PKPASS" | null
  >(null);

  const handleWalletPass = async (
    provider: "GOOGLE" | "APPLE" | "GENERIC_PKPASS",
  ) => {
    if (!firstReservation?.id) return;
    setLoadingProvider(provider);

    try {
      const response = await getApiUserWalletReservationsByIdByProvider({
        path: {
          id: firstReservation.id,
          provider: provider as "GOOGLE" | "APPLE",
        },
        parseAs: provider === "GOOGLE" ? "json" : "blob",
      });

      if (response.error || !response.data) {
        throw new Error(`Failed to generate ${provider} wallet pass`);
      }

      if (provider === "GOOGLE") {
        const data = response.data as { url?: string };
        if (data.url) {
          window.open(data.url, "_blank");
        }
      } else {
        const blob = response.data as unknown as Blob;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        const ext = reservations.length > 1 ? "pkpasses" : "pkpass";
        a.download = `${sanitizeFileName(eventName || "ticket")}.${ext}`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      }
    } catch (error) {
      console.error(`Error adding to ${provider} wallet:`, error);
      toast.error(t("qrCodeModal.walletError"));
    } finally {
      setLoadingProvider(null);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{t("qrCodeModal.title")}</DialogTitle>
          <DialogDescription>{t("qrCodeModal.description")}</DialogDescription>
        </DialogHeader>

        <div className="flex flex-col items-center justify-center gap-6 py-6">
          {!hasCheckInToken ? (
            <div className="flex items-center justify-center w-full h-32">
              <p className="text-sm text-muted-foreground">
                {t("qrCodeModal.noCheckInCode")}
              </p>
            </div>
          ) : qrCodeDataUrl ? (
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

        <DialogFooter className="flex-col sm:flex-col gap-2">
          {hasCheckInToken &&
            (walletConfig?.googleEnabled ||
              walletConfig?.appleEnabled ||
              walletConfig?.genericEnabled) && (
              <div className="flex flex-col sm:flex-row gap-3 w-full pt-2 items-center justify-center flex-wrap">
                {/* Google Wallet — official badge, must not be modified per Google brand guidelines */}
                {walletConfig?.googleEnabled && (
                  <button
                    type="button"
                    onClick={() => handleWalletPass("GOOGLE")}
                    disabled={loadingProvider !== null}
                    aria-label={t("qrCodeModal.googleWalletButton")}
                    className="flex items-center justify-center w-48 h-12 rounded-lg overflow-hidden disabled:opacity-50 transition-opacity hover:opacity-85 active:opacity-70"
                  >
                    {loadingProvider === "GOOGLE" ? (
                      <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                    ) : (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        src={googleBadgeSrc}
                        alt={t("qrCodeModal.googleWalletButton")}
                        className="h-full w-full object-contain"
                      />
                    )}
                  </button>
                )}

                {/* Apple Wallet — official badge, must not be modified per Apple brand guidelines */}
                {walletConfig?.appleEnabled && (
                  <button
                    type="button"
                    onClick={() => handleWalletPass("APPLE")}
                    disabled={loadingProvider !== null}
                    aria-label={t("qrCodeModal.appleWalletButton")}
                    className="flex items-center justify-center w-48 h-12 rounded-lg overflow-hidden disabled:opacity-50 transition-opacity hover:opacity-85 active:opacity-70"
                  >
                    {loadingProvider === "APPLE" ? (
                      <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                    ) : (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img
                        src={appleBadgeSrc}
                        alt={t("qrCodeModal.appleWalletButton")}
                        className="h-full w-full object-contain"
                      />
                    )}
                  </button>
                )}

                {/* Generic Wallet / PKPass Button */}
                {walletConfig?.genericEnabled && (
                  <button
                    type="button"
                    onClick={() => handleWalletPass("GENERIC_PKPASS")}
                    disabled={loadingProvider !== null}
                    aria-label={t("qrCodeModal.genericWalletButton")}
                    className="flex items-center justify-center gap-2 px-4 py-2.5 h-12 rounded-lg bg-slate-900 text-white hover:bg-slate-800 active:bg-slate-700 disabled:opacity-50 transition-all font-medium text-sm border border-slate-700 shadow-sm min-w-[192px]"
                  >
                    {loadingProvider === "GENERIC_PKPASS" ? (
                      <Loader2 className="h-5 w-5 animate-spin" />
                    ) : (
                      <>
                        <Wallet className="h-5 w-5 text-white" />
                        <span>{t("qrCodeModal.genericWalletButton")}</span>
                      </>
                    )}
                  </button>
                )}
              </div>
            )}

          {hasCheckInToken &&
            (walletConfig?.googleEnabled || walletConfig?.appleEnabled) && (
              <p className="text-xs text-muted-foreground text-center">
                {t("qrCodeModal.trademarkNotice")}
              </p>
            )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
