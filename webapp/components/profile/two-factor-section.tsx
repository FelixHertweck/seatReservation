"use client";

import { useState } from "react";
import { useT } from "@/lib/i18n/hooks";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { useTwoFactor } from "@/hooks/use-two-factor";
import { Skeleton } from "@/components/ui/skeleton";
import { ShieldCheck, ShieldAlert } from "lucide-react";
import QRCode from "qrcode";

export function TwoFactorSection() {
  const t = useT();
  const {
    settings,
    isLoading,
    updateSettings,
    setupTotp,
    enable,
    disable,
  } = useTwoFactor();
  const [isSetupOpen, setIsSetupOpen] = useState(false);
  const [qrCodeUrl, setQrCodeUrl] = useState<string>("");
  const [secret, setSecret] = useState<string>("");
  const [code, setCode] = useState<string>("");
  const [backupCodes, setBackupCodes] = useState<string[]>([]);

  const handleEnable = async () => {
    try {
      const data = await setupTotp();
      setSecret(data.secret || "");
      if (data.qrCodeUri) {
        const url = await QRCode.toDataURL(data.qrCodeUri);
        setQrCodeUrl(url);
      }
      setIsSetupOpen(true);
    } catch (e) {
      console.error(e);
    }
  };

  const handleVerify = async () => {
    try {
      const data = await enable(code);
      if (data && Array.isArray(data)) {
        setBackupCodes(data as string[]);
      }
      setCode("");
    } catch (e) {
      console.error(e);
    }
  };

  const closeSetup = () => {
    setIsSetupOpen(false);
    setBackupCodes([]);
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-6 w-32" />
        <Skeleton className="h-20 w-full" />
      </div>
    );
  }

  const enabled = settings?.twoFactorEnabled ?? false;
  const requireForPasskey = settings?.passkeyRequiresTwoFactor ?? false;
  const isEmail = settings?.twoFactorType === "EMAIL";

  return (
    <div className="space-y-6 pt-4 border-t">
      <div>
        <h3 className="text-lg font-medium">
          {t("profilePage.twoFactorTitle")}
        </h3>
        <p className="text-sm text-muted-foreground">
          {t("profilePage.twoFactorDescription")}
        </p>
      </div>

      <div className="flex items-center justify-between rounded-lg border p-4">
        <div className="space-y-0.5">
          <Label className="text-base">
            {enabled ? (
              <ShieldCheck className="inline mr-2 text-green-500" />
            ) : (
              <ShieldAlert className="inline mr-2 text-yellow-500" />
            )}
            {enabled
              ? t("profilePage.twoFactorStatusEnabled")
              : t("profilePage.twoFactorStatusDisabled")}
          </Label>
          <p className="text-sm text-muted-foreground">
            {enabled
              ? t("profilePage.twoFactorEnabledDesc")
              : t("profilePage.twoFactorDisabledDesc")}
          </p>
        </div>
        <Switch
          checked={enabled}
          onCheckedChange={(checked) => {
            if (checked) {
              handleEnable();
            } else {
              disable();
            }
          }}
        />
      </div>

      {enabled && (
        <div className="space-y-4 rounded-lg border p-4">
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label>{t("profilePage.twoFactorTypeTitle")}</Label>
              <p className="text-sm text-muted-foreground">
                {t("profilePage.twoFactorTypeDesc")}
              </p>
            </div>
            <select
              className="px-3 py-1 bg-background border rounded-md"
              value={isEmail ? "EMAIL" : "TOTP"}
              onChange={(e) =>
                updateSettings({
                  ...settings!,
                  twoFactorType: e.target.value,
                })
              }
            >
              <option value="TOTP">Authenticator App (TOTP)</option>
              <option value="EMAIL">Email</option>
            </select>
          </div>

          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label>{t("profilePage.passkeyRequiresTwoFactor")}</Label>
              <p className="text-sm text-muted-foreground">
                {t("profilePage.passkeyRequiresTwoFactorDesc")}
              </p>
            </div>
            <Switch
              checked={requireForPasskey}
              onCheckedChange={(checked) =>
                updateSettings({
                  ...settings!,
                  passkeyRequiresTwoFactor: checked,
                })
              }
            />
          </div>
        </div>
      )}

      {isSetupOpen && !enabled && (
        <div className="rounded-lg border p-4 bg-muted/50 space-y-4 mt-4">
          <h4 className="font-medium">
            {t("profilePage.twoFactorSetupTitle")}
          </h4>
          <div className="flex flex-col md:flex-row gap-6 items-center">
            {qrCodeUrl && (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={qrCodeUrl}
                alt="QR Code"
                className="w-48 h-48 bg-white p-2 rounded-md"
              />
            )}
            <div className="space-y-4 flex-1">
              <p className="text-sm">
                {t("profilePage.twoFactorSetupInstructions")}
              </p>
              <div className="bg-background p-2 rounded font-mono text-sm break-all text-center border">
                {secret}
              </div>
              <div className="flex gap-2">
                <input
                  type="text"
                  placeholder="000000"
                  className="flex-1 px-3 py-2 border rounded-md bg-background"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                />
                <Button onClick={handleVerify} disabled={code.length < 6}>
                  {t("login.verifyButton")}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {backupCodes.length > 0 && (
        <div className="rounded-lg border border-yellow-200 bg-yellow-50 dark:bg-yellow-900/20 dark:border-yellow-800 p-4 space-y-4">
          <h4 className="font-bold text-yellow-800 dark:text-yellow-200">
            {t("profilePage.backupCodesTitle")}
          </h4>
          <p className="text-sm text-yellow-700 dark:text-yellow-300">
            {t("profilePage.backupCodesDesc")}
          </p>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-2">
            {backupCodes.map((c, i) => (
              <div
                key={i}
                className="font-mono bg-background/50 border p-2 text-center rounded text-sm"
              >
                {c}
              </div>
            ))}
          </div>
          <Button onClick={closeSetup} variant="outline" className="w-full">
            {t("common.close")}
          </Button>
        </div>
      )}
    </div>
  );
}
