"use client";

import { useState } from "react";
import { CheckIcon } from "@/components/ui/check";
import { CopyIcon } from "@/components/ui/copy";
import { toast } from "sonner";
import { Button } from "@/components/custom-ui/button";
import { useT } from "@/lib/i18n/hooks";

export function BackupCodesDisplay({ codes }: { codes: string[] }) {
  const t = useT();
  const [isCopied, setIsCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(codes.join("\n"));
    setIsCopied(true);
    toast.success(t("twoFactor.backupCodesCopied"));
    setTimeout(() => setIsCopied(false), 2000);
  };

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-2 gap-2 font-mono text-sm">
        {codes.map((code, idx) => (
          <div
            key={idx}
            className="rounded-md border bg-muted/40 p-2 text-center font-bold"
          >
            {code}
          </div>
        ))}
      </div>
      <Button
        type="button"
        variant="outline"
        className="w-full"
        onClick={handleCopy}
      >
        {isCopied ? (
          <CheckIcon size={16} className="mr-2 text-green-500" />
        ) : (
          <CopyIcon size={16} className="mr-2" />
        )}
        {t("twoFactor.copyBackupCodes")}
      </Button>
    </div>
  );
}
