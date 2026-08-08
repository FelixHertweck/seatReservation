"use client";

import { useRef } from "react";
import { Mail, Printer } from "lucide-react";

import { useT } from "@/lib/i18n/hooks";
import { Button } from "@/components/custom-ui/button";
import type { BoxOfficeReservationResponseDto } from "@/api";

interface BoxOfficeConfirmationProps {
  result: BoxOfficeReservationResponseDto;
  // The address the supervisor typed in, held only in local form state -- the
  // backend never persists or echoes it back, so it must be passed in here
  // rather than read from `result`.
  guestEmail?: string;
  // For the "known user" path: the username of the target user, whose own
  // registered email address the backend already emailed the confirmation to.
  notifiedUsername?: string;
  onCreateAnother: () => void;
}

export function BoxOfficeConfirmation({
  result,
  guestEmail,
  notifiedUsername,
  onCreateAnother,
}: BoxOfficeConfirmationProps) {
  const t = useT();
  const iframeRef = useRef<HTMLIFrameElement>(null);

  const handlePrint = () => {
    iframeRef.current?.contentWindow?.print();
  };

  return (
    <div className="flex flex-col gap-4 rounded-lg border p-4 sm:p-6">
      <div>
        <h3 className="font-medium">{t("boxOffice.confirmation.title")}</h3>
        <p className="text-sm text-muted-foreground">
          {t("boxOffice.confirmation.description")}
        </p>
      </div>

      {/* Server-rendered "Abendkasse" confirmation (see EmailService.sendBoxOfficeConfirmation)
          -- printed as-is via iframe so the printout is byte-for-byte identical to the email. */}
      <iframe
        ref={iframeRef}
        sandbox="allow-same-origin allow-modals"
        srcDoc={result.confirmationHtml ?? ""}
        title={t("boxOffice.confirmation.title")}
        className="h-[420px] w-full rounded-md border bg-white"
      />

      {guestEmail && (
        <div className="flex items-center gap-2 rounded-md border bg-muted/40 px-3 py-2 text-sm">
          <Mail className="h-4 w-4 shrink-0 text-muted-foreground" />
          {t("boxOffice.confirmation.emailedTo", { email: guestEmail })}
        </div>
      )}

      {notifiedUsername && (
        <div className="flex items-center gap-2 rounded-md border bg-muted/40 px-3 py-2 text-sm">
          <Mail className="h-4 w-4 shrink-0 text-muted-foreground" />
          {t("boxOffice.confirmation.emailedToUser", {
            username: notifiedUsername,
          })}
        </div>
      )}

      <div className="flex gap-3 border-t pt-4">
        <Button
          type="button"
          variant="outline"
          onClick={handlePrint}
          className="flex-1"
        >
          <Printer className="mr-2 h-4 w-4" />
          {t("boxOffice.confirmation.printButton")}
        </Button>
        <Button type="button" onClick={onCreateAnother} className="flex-1">
          {t("boxOffice.confirmation.createAnotherButton")}
        </Button>
      </div>
    </div>
  );
}
