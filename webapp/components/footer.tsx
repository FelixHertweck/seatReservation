import { createTranslation } from "@/lib/i18n/server";
import Link from "next/link";

export async function AppFooter({ locale }: { locale: string }) {
  const t = await createTranslation(locale);
  return (
    <footer className="border-t border-border/50 bg-background px-4 py-2">
      <div className="flex justify-center gap-4 text-xs">
        <Link
          href="/privacy"
          className="text-muted-foreground hover:text-foreground transition-colors"
        >
          {t("footer.privacyPolicy")}
        </Link>
        <Link
          href="/legal-notice"
          className="text-muted-foreground hover:text-foreground transition-colors"
        >
          {t("footer.legalNotice")}
        </Link>
      </div>
    </footer>
  );
}
