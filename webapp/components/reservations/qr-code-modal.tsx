"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/custom-ui/dialog";
import { Button } from "@/components/custom-ui/button";
import { useT } from "@/lib/i18n/hooks";
import { sanitizeFileName } from "@/lib/utils/filename";
import { Download, Loader2, Share2, Wallet } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import QRCode from "qrcode";
import {
  getApiUserWalletReservationsByIdByProvider,
  getApiUserWalletConfig,
  type UserEventLocationResponseDto,
  type UserEventResponseDto,
  type UserReservationResponseDto,
  type WalletConfigDto,
} from "@/api";
import Image from "next/image";
import { useParams } from "next/navigation";

interface QRCodeModalProps {
  readonly isOpen: boolean;
  readonly eventName: string | undefined;
  readonly onClose: () => void;
  readonly reservations: readonly UserReservationResponseDto[];
  readonly userId: string | undefined;
  readonly event?: UserEventResponseDto | null;
  readonly location?: UserEventLocationResponseDto | null;
  readonly locationName?: string;
}

function formatEventDateTime(
  event: UserEventResponseDto | null | undefined,
  locale: string,
): string | null {
  if (!event?.startTime) return null;
  const start = new Date(event.startTime);
  const dateLocale = locale === "de" ? "de-DE" : "en-US";
  const formattedDate = start.toLocaleDateString(dateLocale, {
    weekday: "short",
    year: "numeric",
    month: "short",
    day: "numeric",
  });
  const formattedStartTime = start.toLocaleTimeString(dateLocale, {
    hour: "2-digit",
    minute: "2-digit",
  });

  if (event.endTime) {
    const end = new Date(event.endTime);
    const formattedEndTime = end.toLocaleTimeString(dateLocale, {
      hour: "2-digit",
      minute: "2-digit",
    });
    return `📅 ${formattedDate}, ${formattedStartTime} - ${formattedEndTime}`;
  }
  return `📅 ${formattedDate}, ${formattedStartTime}`;
}

function formatSeatLabels(
  location: UserEventLocationResponseDto | null | undefined,
  reservations: readonly UserReservationResponseDto[],
  t: (key: string, options?: Record<string, unknown>) => string,
): string | null {
  if (!reservations.length) return null;
  if (!location?.seats) {
    return `🪑 ${reservations.length} ${
      reservations.length === 1
        ? t("reservationCard.seatSingular")
        : t("reservationCard.seatPlural")
    }`;
  }

  const seatById = new Map(location.seats.map((s) => [s.id, s]));
  const seatLabels = reservations
    .map((r) => (r.seatId ? seatById.get(r.seatId) : undefined))
    .filter((s): s is NonNullable<typeof s> => Boolean(s))
    .map((s) => {
      if (s.seatRow && s.seatNumber) {
        return t("qrCodeModal.shareRow", {
          row: s.seatRow,
          seat: s.seatNumber,
        });
      }
      if (s.seatNumber) {
        return t("qrCodeModal.shareSeat", {
          seat: s.seatNumber,
        });
      }
      return null;
    })
    .filter((label): label is string => Boolean(label));

  if (seatLabels.length > 0) {
    return `🪑 ${seatLabels.join("; ")}`;
  }
  return `🪑 ${reservations.length} ${
    reservations.length === 1
      ? t("reservationCard.seatSingular")
      : t("reservationCard.seatPlural")
  }`;
}

function buildShareText({
  eventName,
  event,
  location,
  locationName,
  reservations,
  checkInToken,
  eventId,
  locale,
  t,
}: {
  eventName?: string;
  event?: UserEventResponseDto | null;
  location?: UserEventLocationResponseDto | null;
  locationName?: string;
  reservations: readonly UserReservationResponseDto[];
  checkInToken?: string;
  eventId?: string;
  locale: string;
  t: (key: string, options?: Record<string, unknown>) => string;
}): string {
  const lines: string[] = [];
  const name = eventName || event?.name || t("reservationCard.unknownEvent");
  lines.push(`🎟️ ${name}`);

  const dateTimeLine = formatEventDateTime(event, locale);
  if (dateTimeLine) {
    lines.push(dateTimeLine);
  }

  const locName = locationName || location?.name;
  if (locName) {
    if (location?.address) {
      lines.push(`📍 ${locName} (${location.address})`);
    } else {
      lines.push(`📍 ${locName}`);
    }
  }

  const seatLine = formatSeatLabels(location, reservations, t);
  if (seatLine) {
    lines.push(seatLine);
  }

  if (checkInToken) {
    lines.push(`🔑 ${t("qrCodeModal.reservationCode")}: ${checkInToken}`);
  }

  const targetEventId = eventId || event?.id;
  if (typeof window !== "undefined" && targetEventId) {
    const eventUrl = `${window.location.origin}/events?eventId=${targetEventId}`;
    lines.push(`🔗 ${eventUrl}`);
  }

  return lines.join("\n");
}

async function createFileFromDataUrl(
  dataUrl: string,
  fileName: string,
): Promise<File | null> {
  try {
    const res = await fetch(dataUrl);
    const blob = await res.blob();
    return new File(
      [blob],
      `${sanitizeFileName(fileName || "ticket")}-qr.png`,
      {
        type: "image/png",
      },
    );
  } catch (err) {
    console.warn("Could not create File from QR code Data URL:", err);
    return null;
  }
}

async function executeShare({
  shareTitle,
  shareText,
  shareUrl,
  qrCodeDataUrl,
  fileNamePrefix,
  onFallbackClipboard,
}: {
  shareTitle: string;
  shareText: string;
  shareUrl?: string;
  qrCodeDataUrl: string;
  fileNamePrefix: string;
  onFallbackClipboard: () => void;
}) {
  const fallbackToClipboard = async () => {
    if (typeof navigator !== "undefined" && navigator.clipboard) {
      await navigator.clipboard.writeText(shareText);
      onFallbackClipboard();
    }
  };

  try {
    const file = await createFileFromDataUrl(qrCodeDataUrl, fileNamePrefix);

    if (file && typeof navigator !== "undefined") {
      const dataWithFiles = {
        title: shareTitle,
        text: shareText,
        ...(shareUrl ? { url: shareUrl } : {}),
        files: [file],
      };
      if (navigator.canShare?.(dataWithFiles)) {
        await navigator.share(dataWithFiles);
        return;
      }
    }

    if (typeof navigator !== "undefined" && navigator.share) {
      const textShareData = {
        title: shareTitle,
        text: shareText,
        ...(shareUrl ? { url: shareUrl } : {}),
      };
      if (!navigator.canShare || navigator.canShare(textShareData)) {
        await navigator.share(textShareData);
        return;
      }

      await navigator.share({
        title: shareTitle,
        text: shareText,
      });
      return;
    }

    await fallbackToClipboard();
  } catch (err: unknown) {
    if (err instanceof Error && err.name === "AbortError") {
      return;
    }
    console.error("Error sharing ticket:", err);
    try {
      await fallbackToClipboard();
    } catch (clipErr) {
      console.error("Error copying to clipboard:", clipErr);
    }
  }
}

function QRCodeDisplay({
  hasCheckInToken,
  qrCodeDataUrl,
  noCheckInCodeText,
  generatingQRCodeText,
}: {
  readonly hasCheckInToken: boolean;
  readonly qrCodeDataUrl: string;
  readonly noCheckInCodeText: string;
  readonly generatingQRCodeText: string;
}) {
  if (!hasCheckInToken) {
    return (
      <div className="flex items-center justify-center w-full h-32">
        <p className="text-sm text-muted-foreground">{noCheckInCodeText}</p>
      </div>
    );
  }

  if (qrCodeDataUrl) {
    return (
      <div className="flex flex-col items-center gap-4">
        <Image
          src={qrCodeDataUrl}
          alt="Reservation QR Code"
          width={288}
          height={288}
          className="border-4 border-gray-300 rounded-lg p-2 bg-white"
        />
      </div>
    );
  }

  return (
    <div className="flex items-center justify-center w-full h-32">
      <p className="text-sm text-muted-foreground">{generatingQRCodeText}</p>
    </div>
  );
}

export function QRCodeModal({
  isOpen,
  eventName,
  onClose,
  reservations,
  userId,
  event,
  location,
  locationName,
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

  const handleDownloadQRCode = () => {
    if (!qrCodeDataUrl) return;

    const link = document.createElement("a");
    link.href = qrCodeDataUrl;
    const fileName = `${sanitizeFileName(eventName || "ticket")}-qr-code.png`;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();

    toast.success(t("qrCodeModal.downloadStarted"), {
      description: t("qrCodeModal.qrCodeDownloading"),
    });
  };

  const handleShare = async () => {
    if (!qrCodeDataUrl) return;

    const name = eventName || event?.name || t("reservationCard.unknownEvent");
    const targetEventId = firstReservation?.eventId || event?.id;
    const shareUrl =
      typeof window !== "undefined" && targetEventId
        ? `${window.location.origin}/events?eventId=${targetEventId}`
        : undefined;

    const shareText = buildShareText({
      eventName,
      event,
      location,
      locationName,
      reservations,
      checkInToken: firstReservation?.checkInToken,
      eventId: targetEventId,
      locale,
      t,
    });

    await executeShare({
      shareTitle: `${name} - Ticket`,
      shareText,
      shareUrl,
      qrCodeDataUrl,
      fileNamePrefix: name,
      onFallbackClipboard: () => {
        toast.success(t("qrCodeModal.copiedToClipboard"), {
          description: t("qrCodeModal.shareFallbackDescription"),
        });
      },
    });
  };

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
        a.remove();
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
          <QRCodeDisplay
            hasCheckInToken={hasCheckInToken}
            qrCodeDataUrl={qrCodeDataUrl}
            noCheckInCodeText={t("qrCodeModal.noCheckInCode")}
            generatingQRCodeText={t("qrCodeModal.generatingQRCode")}
          />
        </div>

        <DialogFooter className="flex-col sm:flex-col gap-2">
          {hasCheckInToken && qrCodeDataUrl && (
            <div className="flex flex-col sm:flex-row gap-2 w-full">
              <Button
                variant="outline"
                onClick={handleDownloadQRCode}
                className="flex-1"
              >
                <Download className="mr-2 h-4 w-4" />
                {t("qrCodeModal.downloadButton")}
              </Button>
              <Button
                variant="outline"
                onClick={handleShare}
                className="flex-1"
              >
                <Share2 className="mr-2 h-4 w-4" />
                {t("qrCodeModal.shareButton")}
              </Button>
            </div>
          )}

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
